package be.cytomine.api.meta

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
import be.cytomine.CytomineDomain
import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import be.cytomine.image.ImageInstance
import be.cytomine.meta.Property
import be.cytomine.project.Project
import be.cytomine.utils.GeometryUtils
import com.vividsolutions.jts.geom.Geometry
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@RestApi(name = "property services", description = "Methods for managing properties")
class RestPropertyController extends RestController {

    def propertyService
    def cytomineService
    def projectService
    def imageInstanceService
    def secUserService
    def securityACLService

    /**
     * List all Property visible for the current user by Project, AnnotationDomain and ImageInstance
     */
    @RestApiMethod(description="Get all properties for a project", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH,description = "The project id")
    ])
    def listByProject() {
        def projectId = params.long('idProject')
        Project project = projectService.read(projectId)
        if(project) {
            responseSuccess(propertyService.list(project))
        } else {
            responseNotFound("Project",params.idProject)
        }
    }

    @RestApiMethod(description="Get all properties for an annotation (algo,user, or reviewed)", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="idAnnotation", type="long", paramType = RestApiParamType.PATH,description = "The annotation id")
    ])
    def listByAnnotation() {
        try {
            def annotationId = params.long('idAnnotation')
            AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(annotationId)
            if(annotation) {
                responseSuccess(propertyService.list(annotation))
            } else {
                responseNotFound("Annotation",params.idAnnotation)
            }
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    @RestApiMethod(description="Get all properties for an image instance", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="idImageInstance", type="long", paramType = RestApiParamType.PATH,description = "The image instance id")
    ])
    def listByImageInstance() {
        def imageInstanceId = params.long('idImageInstance')
        ImageInstance imageInstance = imageInstanceService.read(imageInstanceId)
        if(imageInstance) {
            responseSuccess(propertyService.list(imageInstance))
        } else {
            responseNotFound("ImageInstance",params.idImageInstance)
        }
    }

    @RestApiMethod(description="Get all properties for a cytomine domain", listing=true)
    @RestApiParams(params=[
    @RestApiParam(name="idDomain", type="long", paramType = RestApiParamType.PATH,description = "The abstract image id")
    ])
    def listByDomain() {
        CytomineDomain domain = cytomineService.getDomain(params.long('domainIdent'),params.get("domainClassName"))
        if(domain) {
            responseSuccess(propertyService.list(domain))
        } else {
            responseNotFound("Cytomine domain "+params.get("domainClassName"),params.long('domainIdent'))
        }
    }

    @RestApiMethod(description="Get all keys of annotation properties in a project or image", listing=true)
    @RestApiParams(params=[
            @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.QUERY,description = "(Optional, if null idImage must be set) The project id"),
            @RestApiParam(name="idImage", type="long", paramType = RestApiParamType.QUERY,description = "(Optional, if null idProject must be set) The image instance id"),
            @RestApiParam(name="user", type="boolean", paramType = RestApiParamType.QUERY,description = "(Optional) if true, we will return the id of the creator of each key")
    ])
    def listKeyForAnnotation() {
        Project project = projectService.read(params.long('idProject'))
        ImageInstance image = imageInstanceService.read(params.long('idImage'))
        boolean withUser = params.boolean('user');

        if(image) {
            responseSuccess(propertyService.listKeysForAnnotation(null, image, withUser))
        } else if(project) {
            responseSuccess(propertyService.listKeysForAnnotation(project, null, withUser))
        } else {
            responseNotFound("Property","Image/Project", params.idImage+"/"+params.idProject)
        }
    }

    @RestApiMethod(description="Get all keys of images properties in a project", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.QUERY,description = "(Optional, if null idImage must be set) The project id"),
    ])
    def listKeyForImageInstance() {
        Project project = projectService.read(params.long('idProject'))

        if(project) {
            responseSuccess(propertyService.listKeysForImageInstance(project))
        } else {
            responseNotFound("Property","Project", params.idProject)
        }
    }

    @RestApiMethod(description="For a specific key, Get all annotation centroid (x,y) and the corresponding value for an image and a layer (user)", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="idImage", type="long", paramType = RestApiParamType.PATH,description = "The image id"),
        @RestApiParam(name="idUser", type="long", paramType = RestApiParamType.PATH,description = "The layer id"),
        @RestApiParam(name="key", type="long", paramType = RestApiParamType.QUERY,description = "The properties key"),
        @RestApiParam(name="bbox", type="string", paramType = RestApiParamType.QUERY,description = "(Optional) Form of the restricted area"),
    ])
    def listAnnotationPosition() {
        def image = imageInstanceService.read(params.long('idImage'))
        def user = secUserService.read(params.idUser)
        if (image && user && params.key) {

            Geometry boundingbox = null
            if(params.bbox!=null) {
                boundingbox = GeometryUtils.createBoundingBox(params.bbox)
            }

            def data = propertyService.listAnnotationCenterPosition(user, image, boundingbox, params.key)
                responseSuccess(data)
        } else if (!user) {
            responseNotFound("User", params.idUser)
        } else if (!image) {
            responseNotFound("Image", params.idImage)
        }
    }

    @RestApiMethod(description="Get a project property with its id or its key")
    @RestApiParams(params=[
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null key must be set) The property id"),
        @RestApiParam(name="key", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null id must be set) The property key")
    ])
    def showProject() {
        def projectId = params.long('idProject')
        Project project = projectService.read(projectId)
        securityACLService.check(project,READ)

        Property property
        if(params.id != null) {
            property = propertyService.read(params.id)
        } else if (params.key != null) {
            property = propertyService.read(project, params.key)
        }

        if (property) {
            responseSuccess(property)
        } else {
            responseNotFound("Property", params.id)
        }
    }

    @RestApiMethod(description="Get an abstract image property with its id or its key")
    @RestApiParams(params=[
    @RestApiParam(name="domainIdent", type="string", paramType = RestApiParamType.PATH, description = "The domain id"),
    @RestApiParam(name="domainClassName", type="string", paramType = RestApiParamType.PATH, description = "The domain type"),
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null key must be set) The property id"),
    @RestApiParam(name="key", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null id must be set) The property key")
    ])
    def showDomain() {
        CytomineDomain domain = cytomineService.getDomain(params.long('domainIdent'),(String)params.get('domainClassName'))

        Property property
        if(params.id != null) {
            property = propertyService.read(params.id)
        } else if (params.key != null) {
            property = propertyService.read(domain, params.key)
        }

        if (property) {
            responseSuccess(property)
        } else {
            responseNotFound("Property", params.id)
        }
    }

    @RestApiMethod(description="Get a project property with its id or its key")
    @RestApiParams(params=[
        @RestApiParam(name="idAnnotation", type="long", paramType = RestApiParamType.PATH, description = "The annotation id"),
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null key must be set) The property id"),
        @RestApiParam(name="key", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null id must be set) The property key")
    ])
    def showAnnotation() {
        def annotationId = params.long('idAnnotation')
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(annotationId)

        Property property
        if(params.id != null) {
            property = propertyService.read(params.id)
        } else if (params.key != null) {
            property = propertyService.read(annotation, params.key)
        }

        if (property) {
            responseSuccess(property)
        } else {
            responseNotFound("Property", params.id)
        }
    }

    @RestApiMethod(description="Get an image instance property with its id or its key")
    @RestApiParams(params=[
        @RestApiParam(name="idImageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image instance id"),
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null key must be set) The property id"),
        @RestApiParam(name="key", type="long", paramType = RestApiParamType.PATH,description = "(Optional, if null id must be set) The property key")
    ])
    def showImageInstance() {
        def imageInstanceId = params.long('idImageInstance')
        ImageInstance imageInstance = imageInstanceService.read(imageInstanceId)

        Property property
        if(params.id != null) {
            property = propertyService.read(params.id)
        } else if (params.key != null) {
            property = propertyService.read(imageInstance, params.key)
        }

        if (property) {
            responseSuccess(property)
        } else {
            responseNotFound("Property", params.id)
        }
    }


    /**
     * Add a new Property (Method from RestController)
     */
    @RestApiMethod(description="Add a property to a project")
    @RestApiParams(params=[
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH, description = "The project id"),
    ])
    def addPropertyProject() {
        def json = request.JSON
        json.domainClassName = Project.getName()
        if(!json.domainIdent) json.domainIdent = params.idProject
        securityACLService.check(json.domainIdent,json.domainClassName,"container",WRITE)
        add(propertyService, request.JSON)
    }

    @RestApiMethod(description="Add a property to an annotation")
    @RestApiParams(params=[
        @RestApiParam(name="idAnnotation", type="long", paramType = RestApiParamType.PATH, description = "The annotation id"),
    ])
    def addPropertyAnnotation()  {
        def json = request.JSON
        if(!json.domainIdent) json.domainIdent = params.idAnnotation
        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(json.domainIdent)
        json.domainClassName = annotation.class.getName()
        add(propertyService, request.JSON)
    }

    @RestApiMethod(description="Add a property to a image instance")
    @RestApiParams(params=[
        @RestApiParam(name="idImageInstance", type="long", paramType = RestApiParamType.PATH, description = "The image instance id"),
    ])
    def addPropertyImageInstance()  {
        def json = request.JSON
        json.domainClassName = ImageInstance.getName()
        if(!json.domainIdent) json.domainIdent = params.idImageInstance
        add(propertyService, request.JSON)
    }

    @RestApiMethod(description="Add a property to an generic domain")
    @RestApiParams(params=[
    @RestApiParam(name="idDomain", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id"),
    ])
    def addPropertyDomain()  {
        def json = request.JSON
        if (json instanceof JSONArray) {
            responseResult(addMultiple(propertyService, json))
        } else {
            if (json.domainClassName == null || json.domainClassName == "")
                json.domainClassName = params.get("domainClassName")

            responseResult(addOne(propertyService, json))
        }
    }


    /**
     * Update a Property (Method from RestController)
     */
    @RestApiMethod(description="Edit a property")
    @RestApiParams(params=[
        @RestApiParam(name="idAnnotation", type="long", paramType = RestApiParamType.PATH,description = "(Optional) The annotation id"),
        @RestApiParam(name="idImageInstance", type="long", paramType = RestApiParamType.PATH,description = "(Optional) The image instance id"),
        @RestApiParam(name="idProject", type="long", paramType = RestApiParamType.PATH,description = "(Optional) The project id")
    ])
    def update() {
        def json = request.JSON
        if(Project.name == json.domainClassName) securityACLService.check(json.domainIdent,json.domainClassName,"container",WRITE)
        update(propertyService, request.JSON)
    }

    /**
     * Delete a Property (Method from RestController)
     */
    @RestApiMethod(description="Delete a property")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The property id")
    ])
    def delete()  {
        def json = JSON.parse("{id : $params.id}")
        delete(propertyService,json,null)
    }
}
