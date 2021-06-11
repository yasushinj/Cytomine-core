package be.cytomine.processing

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

import be.cytomine.command.*
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.sql.RoiAnnotationListing
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter

import static org.springframework.security.acls.domain.BasePermission.READ

class RoiAnnotationService extends ModelService {

    static transactional = true
    def cytomineService
    def transactionService
    def annotationTermService
    def algoAnnotationTermService
    def modelService
    def simplifyGeometryService
    def dataSource
    def reviewedAnnotationService
    def propertyService
    def annotationListingService
    def securityACLService
    def imageInstanceService


    def currentDomain() {
        return RoiAnnotation
    }

    RoiAnnotation read(def id) {
        def annotation = RoiAnnotation.read(id)
        if (annotation) {
            securityACLService.check(annotation.container(),READ)
        }
        annotation
    }

    def list(Project project,def propertiesToShow = null) {
        securityACLService.check(project.container(),READ)
        annotationListingService.executeRequest(new RoiAnnotationListing(project: project.id, columnToPrint: propertiesToShow))
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json,def minPoint = null, def maxPoint = null) {
        securityACLService.check(json.project, Project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()

        Geometry annotationForm
        try {
            annotationForm = new WKTReader().read(json.location);
        } catch (ParseException e){
            throw new WrongArgumentException("Annotation location not valid")
        }
        if(!annotationForm.isValid()){
            throw new WrongArgumentException("Annotation location not valid")
        }
        ImageInstance im = imageInstanceService.read(json.image)
        if(!im){
            throw new WrongArgumentException("Annotation not associated with a valid image")
        }
        Geometry imageBounds = new WKTReader().read("POLYGON((0 0,0 $im.baseImage.height,$im.baseImage.width $im.baseImage.height,$im.baseImage.width 0,0 0))")

        annotationForm = annotationForm.intersection(imageBounds)

        //simplify annotation
        try {
            def data = simplifyGeometryService.simplifyPolygon(annotationForm,minPoint,maxPoint)
            json.location = data.geometry
            json.geometryCompression = data.rate
        } catch (Exception e) {
            log.error("Cannot simplify:" + e)
        }

        //Start transaction
        Transaction transaction = transactionService.start()
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser, transaction: transaction),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(RoiAnnotation annotation, def jsonNewData) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsSameUserOrAdminContainer(annotation,annotation.user,currentUser)
        Geometry annotationForm
        try {
            annotationForm = new WKTReader().read(jsonNewData.location);
        } catch (ParseException){
            throw new WrongArgumentException("Annotation location not valid")
        }
        if(!annotationForm.isValid()){
            throw new WrongArgumentException("Annotation location not valid")
        }
        ImageInstance im = imageInstanceService.read(jsonNewData.image)
        if(!im){
            throw new WrongArgumentException("Annotation not associated with a valid image")
        }
        Geometry imageBounds = new WKTReader().read("POLYGON((0 0,0 $im.baseImage.height,$im.baseImage.width $im.baseImage.height,$im.baseImage.width 0,0 0))")

        annotationForm = annotationForm.intersection(imageBounds)

        //simplify annotation
        try {
            def data = simplifyGeometryService.simplifyPolygon(annotationForm.toString(),annotation?.geometryCompression)
            jsonNewData.location = new WKTWriter().write(data.geometry)
        } catch (Exception e) {
            log.error("Cannot simplify:" + e)
        }

        return executeCommand(new EditCommand(user: currentUser),annotation,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(RoiAnnotation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkIsSameUserOrAdminContainer(domain,domain.user,currentUser)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [cytomineService.getCurrentUser().toString(), domain.image?.getFileName(), domain.user.toString()]
    }

    def afterAdd(def domain, def response) {
        response.data['annotation'] = response.data.roiannotation

    }

    def afterDelete(def domain, def response) {
        response.data['annotation'] = response.data.roiannotation
    }

    def afterUpdate(def domain, def response) {
        response.data['annotation'] = response.data.roiannotation
    }

}
