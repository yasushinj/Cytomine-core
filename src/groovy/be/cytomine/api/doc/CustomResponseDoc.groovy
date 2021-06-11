package be.cytomine.api.doc

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

import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

class CustomResponseDoc {

    @RestApiObjectField(description = "Response for sequence possibilities")
    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "slice", description = "Image slice index",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "zStack", description = "Image zstack index",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "time", description = "Image time index",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "channel", description = "Image channel index",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "s", description = "Range of possible slice index for image group",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "z", description = "Range of possible zstack index for image group",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "t", description = "Range of possible time index for image group",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "c", description = "Range of possible channel index for image group",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "imageGroup", description = "Image group id",allowedType = "list",useForCreation = false)
     ])
    static def sequence_possibilties

    //If true, send an array with item {imageinstanceId,layerId,layerName,projectId, projectName, admin}
    @RestApiObjectField(description = "Response for project sharing the same image (list)")
    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "imageinstanceId", description = "Image id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "layerId", description = "User id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "layerName", description = "User name",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "projectId", description = "Project id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "projectName", description = "Project name",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "admin", description = "User is admin or not",allowedType = "boolean",useForCreation = false)
    ])
    static def project_sharing_same_image


    //If true, send an array with item {imageinstanceId,layerId,layerName,projectId, projectName, admin}
    @RestApiObjectField(description = "Response for annotation search")
    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "id", description = "Annotation id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "class", description = "Annotation class name",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "countReviewedAnnotations", description = "(If params showMeta=true and reviewed=false) the number of reviewed annotation from this annotation",allowedType = "int",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewed", description = "(If params showMeta=true) annotation is reviewed",allowedType = "int",useForCreation = false),
        @RestApiObjectField(apiFieldName = "image", description = "(If params showMeta=true), image annotation id)",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "project", description = "(If params showMeta=true) project annotation id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "container", description = "(If params showMeta=true) project annotation id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "created", description = "(If params showMeta=true) annotation create date",allowedType = "date",useForCreation = false),
        @RestApiObjectField(apiFieldName = "updated", description = "(If params showMeta=true) annotation update date",allowedType = "date",useForCreation = false),
        @RestApiObjectField(apiFieldName = "user", description = "(If params showMeta=true) user id that create annotation (if reveiwed annotation, user that create the annotation that has been validated)",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "countComments", description = "(If params showMeta=true) number of comments on this annotation",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewUser", description = "(If params showMeta=true, only for reviewed annotation) the user id that review",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "geometryCompression", description = "(If params showMeta=true) Geometry compression rate used to simplify",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "cropURL", description = "(If params showMeta=true) URL to get the crop annotation (image view that frame the annotation)",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "smallCropURL", description = "(If params showMeta=true)  URL to get the small crop annotation (image view that frame the annotation)",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "url", description = "(If params showMeta=true) URL to go to the annotation on the webapp",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "imageURL", description = "(If params showMeta=true) URL to go to the image on the webapp",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "parentIdent", description = "(If params showMeta=true, only for reviewed) the annotation parent of the reviewed annotation",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "wkt", description = "(If params showWKT=true) the full polygon form in WKT",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "area", description = "(If params showGis=true) the area size of the annotation",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "areaUnit", description = "(If params showGis=true) the area unit (pixels²=1,micron²=3)",allowedType = "int",useForCreation = false),
        @RestApiObjectField(apiFieldName = "perimeter", description = "(If params showGis=true) the perimeter size of the annotation",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "perimeterUnit", description = "(If params showGis=true) the perimeter unit (pixels=0,mm=2,)",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "x", description = "(If params showGis=true) the annotation centroid x",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "y", description = "(If params showGis=true) the annotation centroid y",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewUser", description = "(If params showGis=true) the user id thatreview",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewUser", description = "(If params showGis=true) the user id thatreview",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "reviewUser", description = "(If params showGis=true) the user id thatreview",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "term", description = "(If params showTerm=true) the term list id",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "annotationTerms", description = "(If params showTerm=true) the annotationterms list id",allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "userTerm", description = "(If params showTerm=true) the user id group by term id",allowedType = "map",useForCreation = false),
        @RestApiObjectField(apiFieldName = "rate", description = "(If params showTerm=true) the reliability of the prediction",allowedType = "double",useForCreation = false),
        @RestApiObjectField(apiFieldName = "originalfilename", description = "(If params showImage=true) the image filename",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "idTerm", description = "(If params showAlgo=true) the predicted term for the annotation",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "idExpectedTerm", description = "(If params showAlgo=true) the expected term (real term add by user previously)",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "creator", description = "(If params showUser=true) the username of the creator",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "lastname", description = "(If params showUser=true) the lastname of the creator",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "firstname", description = "(If params showUser=true) the firstname of the creator",allowedType = "string",useForCreation = false)
    ])
    static def annotation_listing

    @RestApiObjectField(description = "Response for search request")
    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "id", description = "Domain id",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "created", description = "Domain creation timestamp",allowedType = "date",useForCreation = false),
        @RestApiObjectField(apiFieldName = "class", description = "Domain class",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "name", description = "Domain name",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "description", description = "Domain description text",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "user", description = "Domain creator",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "userfullname", description = "Domain creator name (Lastname Firstname)",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "projectName", description = "Project (storing the domain) name",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "imageName", description = "Image (storing the domain) name. If domain is project, then null" ,allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "urlImage", description = "Domain thumb (if annotation: crop, if image: thumb, if project: null)" ,allowedType = "list",useForCreation = false),
        @RestApiObjectField(apiFieldName = "urlGoTo", description = "URL to go to the domain on the webapp (GUI)" ,allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "urlApi", description = "URL to get JSON data on the current domain" ,allowedType = "string",useForCreation = false)
    ])
    static def search


    @RestApiObjectField(description = "Response for search request v2 (STEP 1)")
    @RestApiObjectFields(params=[
    @RestApiObjectField(apiFieldName = "id", description = "Domain id",allowedType = "long",useForCreation = false),
    @RestApiObjectField(apiFieldName = "className", description = "Domain class name",allowedType = "string",useForCreation = false),
    ])
    static def search_engine_step1

    @RestApiObjectField(description = "Response for search request v2 (STEP 2)")
    @RestApiObjectFields(params=[
    @RestApiObjectField(apiFieldName = "id", description = "Domain id",allowedType = "long",useForCreation = false),
    @RestApiObjectField(apiFieldName = "className", description = "Domain class name",allowedType = "string",useForCreation = false),
    @RestApiObjectField(apiFieldName = "url", description = "URL to go to this resource on the webapp",allowedType = "string",useForCreation = false),
    @RestApiObjectField(apiFieldName = "name", description = "The name of the resource (could be: name, filename, term,...)",allowedType = "string",useForCreation = false),
    @RestApiObjectField(apiFieldName = "matching", description = "Domain data that match the request words (domain attribute, property, description,...)",allowedType = "List<Map>",useForCreation = false),
    ])
    static def search_engine_step2



//    "id": 93251967,
//    "className": "be.cytomine.ontology.UserAnnotation",
//    "url": "http://localhost:8080/#tabs-image-16623-92923499-93251967",
//    "name": "93251967",
//    "matching":
//    [
//    {
//        "value": "pwet: Bota",
//        "type": "property"
//    }
//    ]
}