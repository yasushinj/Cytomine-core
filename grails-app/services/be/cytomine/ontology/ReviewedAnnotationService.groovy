package be.cytomine.ontology

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.AnnotationDomain
import be.cytomine.command.*
import be.cytomine.meta.Property
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.sql.ReviewedAnnotationListing
import be.cytomine.utils.GeometryUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.vividsolutions.jts.geom.Geometry
import grails.converters.JSON
import groovy.sql.Sql

import static org.springframework.security.acls.domain.BasePermission.READ

class ReviewedAnnotationService extends ModelService {

    static transactional = true
    def propertyService
    def cytomineService
    def transactionService
    def algoAnnotationTermService
    def modelService
    def dataSource
    def kmeansGeometryService
    def annotationListingService
    def securityACLService

    def currentDomain() {
        return ReviewedAnnotation
    }

    ReviewedAnnotation read(def id) {
        def annotation = ReviewedAnnotation.read(id)
        if (annotation) {
            securityACLService.check(annotation.container(),READ)
            checkDeleted(annotation)
        }
        annotation
    }

    def count(User user) {
        return ReviewedAnnotation.countByUser(user)
    }

    def countByProject(Project project, Date startDate, Date endDate) {
        String request = "SELECT COUNT(*) FROM ReviewedAnnotation WHERE project = $project.id " +
                (startDate ? "AND created > '$startDate' " : "") +
                (endDate ? "AND created < '$endDate' " : "")
        def result = ReviewedAnnotation.executeQuery(request)
        return result[0]
    }

    def list(Project project, def propertiesToShow = null) {
        securityACLService.check(project.container(),READ)
        ReviewedAnnotationListing al = new ReviewedAnnotationListing(
                columnToPrint: propertiesToShow,
                project: project.id
        )
        annotationListingService.executeRequest(al)

    }

    def stats(ImageInstance image) {
        String request = "SELECT user_id, count(*), sum(count_reviewed_annotations) as total \n" +
                "FROM user_annotation ua\n" +
                "WHERE ua.image_id = ${image.id}\n" +
                "GROUP BY user_id\n" +
                "UNION\n" +
                "SELECT user_id, count(*), sum(count_reviewed_annotations) as total \n" +
                "FROM algo_annotation aa\n" +
                "WHERE aa.image_id = ${image.id}\n" +
                "GROUP BY user_id\n" +
                "ORDER BY total desc;"

        def data = []

        def sql = new Sql(dataSource)
        sql.eachRow(request) {
             data << [user : it[0],all : it[1],reviewed : it[2]
            ]
        }
        try {
            sql.close()
        }catch (Exception e) {}
        data
    }


    def listIncluded(ImageInstance image, String geometry, List<Long> terms, AnnotationDomain annotation = null, def propertiesToShow = null) {
        securityACLService.check(image.container(),READ)

        ReviewedAnnotationListing al = new ReviewedAnnotationListing(
                columnToPrint: propertiesToShow,
                image: image.id,
                terms : terms,
                excludedAnnotation: annotation?.id,
                bbox : geometry
        )
        annotationListingService.executeRequest(al)

    }

    /**
     * List validate annotation
     * @param image Image filter
     * @param bbox Boundary area filter
     * @return Reviewed Annotation list
     */
    def list(ImageInstance image, String bbox, def propertiesToShow = null) {
        Geometry boundingbox = GeometryUtils.createBoundingBox(bbox)
        list(image, boundingbox,propertiesToShow)
    }

    /**
     * List validate annotation
     * @param image Image filter
     * @param bbox Boundary area filter
     * @return Reviewed Annotation list
     */
    def list(ImageInstance image, Geometry bbox, def propertiesToShow = null) {
        securityACLService.check(image.container(),READ)


            def rule = kmeansGeometryService.mustBeReduce(image,null,bbox)
            if(rule==kmeansGeometryService.FULL) {
                /**
                 * We will sort annotation so that big annotation that covers a lot of annotation comes first (appear behind little annotation so we can select annotation behind other)
                 * We compute in 'gc' the set of all other annotation that must be list
                 * For each review annotation, we compute the number of other annotation that cover it (ST_CoveredBy => t or f => 0 or 1)
                 *
                 * ST_CoveredBy will return false if the annotation is not perfectly "under" the compare annotation (if some points are outside)
                 * So in gc, we increase the size of each compare annotation just for the check
                 * So if an annotation x is under y but x has some point next outside y, x will appear top (if no resize, it will appear top or behind).
                 */
                def xfactor = "1.28"
                def yfactor = "1.28"
                //TODO:: get zoom info from UI client, display with scaling only with hight zoom (< annotations)

                double imageWidth = image.baseImage.width
                double bboxWidth = bbox.getEnvelopeInternal().width
                double ratio = bboxWidth/imageWidth*100

                boolean zoomToLow = ratio > 50

                log.info "imageWidth=$imageWidth"
                log.info "bboxWidth=$bboxWidth"
                log.info "ratio=$ratio"
                log.info  "zoomToLow="+zoomToLow
                String subRequest
                if (zoomToLow) {
                    subRequest = "(SELECT SUM(ST_CoveredBy(ga.location,gb.location )::integer) "
                } else {
                    //too heavy to use with little zoom
                    subRequest = "(SELECT SUM(ST_CoveredBy(ga.location,ST_Translate(ST_Scale(gb.location, $xfactor, $yfactor), ST_X(ST_Centroid(gb.location))*(1 - $xfactor), ST_Y(ST_Centroid(gb.location))*(1 - $yfactor) ))::integer) "

                }

                subRequest =  subRequest +
                        "FROM reviewed_annotation ga, reviewed_annotation gb " +
                    "WHERE ga.id=a.id " +
                    "AND ga.id<>gb.id " +
                    "AND ga.image_id=gb.image_id " +
                    "AND ST_Intersects(gb.location,ST_GeometryFromText('" + bbox.toString() + "',0)))\n"

                ReviewedAnnotationListing al = new ReviewedAnnotationListing(
                        columnToPrint: propertiesToShow,
                        image: image.id,
                        bbox : bbox,
                        orderBy: ['id':'asc']
                        //orderBy: ['numberOfCoveringAnnotation':'asc','id':'asc']
                )

                //DISABLE COVER BY FOR PERF
                //al.addExtraColumn("numberOfCoveringAnnotation",subRequest)
                annotationListingService.executeRequest(al)

            } else {

                ReviewedAnnotationListing al = new ReviewedAnnotationListing(
                        columnToPrint: propertiesToShow,
                        kmeans: true,
                        image: image.id,
                        avoidEmptyCentroid : true,
                        bbox : bbox
                )
                if(rule==kmeansGeometryService.KMEANSFULL){

                    kmeansGeometryService.doKeamsFullRequest(al.getAnnotationsRequest())
                } else {
                     kmeansGeometryService.doKeamsSoftRequest(al.getAnnotationsRequest())
                }
            }
    }


    def listTerms(ReviewedAnnotation annotation) {
        return annotation.terms().collect {[term:it.id, user:annotation.image?.reviewUser?.id ]}

    }




    /**
//     * Execute request and format result into a list of map
//     */
//    private def selectReviewedAnnotationFull(String request) {
//        def data = []
//        long lastAnnotationId = -1
//        long lastTermId = -1
//        boolean first = true;
//        def optionalColumn = ["area","perimeter","x","y","originalfilename"]
//        def realColumn = []
//
//        new Sql(dataSource).eachRow(request) {
//            /**
//             * If an annotation has n multiple term, it will be on "n" lines.
//             * For the first line for this annotation (it.id!=lastAnnotationId), add the annotation data,
//             * For the other lines, we add term data to the last annotation
//             */
//            if (it.id != lastAnnotationId) {
//
//                if(first) {
//                    optionalColumn.each { columnName ->
//                          if(columnExist(it,columnName)) {
//                              realColumn << columnName
//                          }
//                    }
//                    first = false
//                }
//
//                def item = [
//                            'class': 'be.cytomine.ontology.ReviewedAnnotation',
//                            id: it.id,
//                            image: it.image,
//                            geometryCompression: it.geometryCompression,
//                            project: it.project,
//                            container: it.project,
//                            user: it.user,
//                            nbComments: it.nbComments,
//                            created: it.created,
//                            updated: it.updated,
//                            reviewed: (it.countReviewedAnnotations > 0),
//                            cropURL: UrlApi.getReviewedAnnotationCropWithAnnotationId(it.id),
//                            smallCropURL: UrlApi.getReviewedAnnotationCropWithAnnotationIdWithMaxWithOrHeight(it.id, 256),
//                            url: UrlApi.getReviewedAnnotationCropWithAnnotationId(it.id),
//                            imageURL: UrlApi.getAnnotationURL(it.project, it.image, it.id),
//                            term: (it.term ? [it.term] : []),
//                            userByTerm: (it.term ? [[id: it.annotationTerms, term: it.term, user: [it.userTerm]]] : []),
//                            location: it.location,
//                            parentIdent:it.parent
//                ]
//
//                println "realColumn=$realColumn"
//                realColumn.each { columnName ->
//                    item[columnName]=it[columnName]
//                }
//                data << item
//
//            } else {
//                if (it.term) {
//                    data.last().term.add(it.term)
//                    data.last().term.unique()
//                    if (it.term == lastTermId) {
//                        data.last().userByTerm.last().user.add(it.userTerm)
//                        data.last().userByTerm.last().user.unique()
//                    } else {
//                        data.last().userByTerm.add([id: it.annotationTerms, term: it.term, user: [it.userTerm]])
//                    }
//                }
//            }
//            lastTermId = it.term
//            lastAnnotationId = it.id
//        }
//        data
//    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        //read annotation (annotation or annotationIdent)
        securityACLService.check(json.project,Project,READ)
        securityACLService.checkisNotReadOnly(json.project,Project)
        SecUser currentUser = cytomineService.getCurrentUser()
        Transaction transaction = transactionService.start()
        //Synchronzed this part of code, prevent two annotation to be add at the same time
        synchronized (this.getClass()) {
            //Add Annotation
            log.debug this.toString()
            def result = executeCommand(new AddCommand(user: currentUser, transaction: transaction),null,json)
            return result
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(ReviewedAnnotation annotation, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsCreator(annotation,currentUser)
        def result = executeCommand(new EditCommand(user: currentUser),annotation,jsonNewData)
        return result
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ReviewedAnnotation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsCreator(domain,currentUser)
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def getStringParamsI18n(def domain) {
        return [domain.user.toString(), domain.image?.baseImage?.filename]
    }

    def deleteDependentAlgoAnnotationTerm(ReviewedAnnotation annotation, Transaction transaction, Task task = null) {
        AlgoAnnotationTerm.findAllByAnnotationIdentAndDeletedIsNull(annotation.id).each {
            algoAnnotationTermService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentHasManyTerm(ReviewedAnnotation annotation, Transaction transaction, Task task = null) {
        annotation.terms?.clear()
    }

}
