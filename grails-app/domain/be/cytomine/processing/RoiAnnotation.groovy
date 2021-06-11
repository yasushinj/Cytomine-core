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

import be.cytomine.AnnotationDomain
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.image.ImageInstance
import be.cytomine.ontology.Term
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * A ROI on the image, usefull to run a job on this area
 */
@RestApiObject(name = "ROI annotation", description = "A region of interest on a picture")
class RoiAnnotation extends AnnotationDomain implements Serializable {

    @RestApiObjectField(description = "User id that created this annotation")
    User user

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "cropURL", description = "URL to get the annotation crop",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "smallCropURL", description = "URL to get a small annotation crop (<256px)",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "url", description = "URL to go to the annotation on the image",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "imageURL", description = "URL to go to the image",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewed", description = "True if annotation has at least one review",allowedType = "boolean",useForCreation = false)
    ])
    static constraints = {
    }

    static mapping = {
          id generator: "assigned"
          columns {
              location type: org.hibernate.spatial.GeometryType
          }
         wktLocation(type: 'text')
        sort "id"
      }


    def beforeInsert() {
        super.beforeInsert()
    }

    def beforeUpdate() {
        super.beforeUpdate()
    }

    /**
     * Check if annotation is reviewed
     * @return True if annotation is linked with at least one review annotation
     */
    boolean hasReviewedAnnotation() {
        return false
    }

    /**
     * Get all terms map with the annotation
     * @return Terms list
     */
    def terms() {
        return []
    }

    /**
     * Get all annotation terms id
     * @return Terms id list
     */
    def termsId() {
        return []
    }

    /**
     * Get all terms for automatic review
     * If review is done "for all" (without manual user control), we add these term to the new review annotation
     * @return
     */
    List<Term> termsForReview() {
        terms().unique()
    }

    /**
     * Check if its an algo annotation
     */
    boolean isAlgoAnnotation() {
        return false
    }

    /**
     * Check if its a review annotation
     */
    boolean isReviewedAnnotation() {
        return false
    }

    /**
     * Get CROP (annotation image area) URL for this annotation
     * @param cytomineUrl Cytomine base URL
     * @return Full CROP Url
     */
    def getCropUrl() {
        UrlApi.getUserAnnotationCropWithAnnotationId(id)
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static RoiAnnotation insertDataIntoDomain(def json, def domain = new RoiAnnotation()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')

        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new ImageInstance(), true)
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)

        domain.geometryCompression = JSONUtils.getJSONAttrDouble(json, 'geometryCompression', 0)
        if (json.location && json.location instanceof Geometry) {
            domain.location = json.location
        }
        else {
            try {
                domain.location = new WKTReader().read(json.location)
            }
            catch (com.vividsolutions.jts.io.ParseException ex) {
                throw new WrongArgumentException(ex.toString())
            }
        }
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = AnnotationDomain.getDataFromDomain(domain)
        ImageInstance imageinstance = domain?.image
        returnArray['cropURL'] = UrlApi.getROIAnnotationCropWithAnnotationId(domain?.id)
        returnArray['smallCropURL'] = UrlApi.getROIAnnotationCropWithAnnotationIdWithMaxWithOrHeight(domain?.id, 256)
        returnArray['url'] = UrlApi.getROIAnnotationCropWithAnnotationId(domain?.id)
        returnArray['imageURL'] = UrlApi.getAnnotationURL(imageinstance?.project?.id, imageinstance?.id, domain?.id)
        returnArray['reviewed'] = domain?.hasReviewedAnnotation()
        return returnArray
    }

    /**
     * Return domain user (annotation user, image user...)
     * By default, a domain has no user.
     * You need to override userDomainCreator() in domain class
     * @return Domain user
     */
    public SecUser userDomainCreator() {
        return user;
    }
}
