package be.cytomine.api.image

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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

import be.cytomine.api.RestController
import be.cytomine.image.AbstractImage
import be.cytomine.image.ImageInstance
import be.cytomine.image.Mime
import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.laboratory.Sample
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType

import javax.activation.MimetypesFileTypeMap

/**
 * Controller that handle request on file uploading (when a file is uploaded, list uploaded files...)
 */
@RestApi(name = "Image | uploaded file services", description = "Methods for managing an uploaded image file.")
class RestUploadedFileController extends RestController {

    def cytomineService
    def projectService
    def storageService
    def grailsApplication
    def uploadedFileService
    def imageInstanceService
    def abstractImageService
    def notificationService
    def securityACLService
    def secUserService
    def dataTablesService

    @RestApiMethod(description="Get all uploaded file made by the current user")
    def list() {

        Long root
        def uploadedFiles
        if(params.root) {
            root = Long.parseLong(params.root)
            uploadedFiles = uploadedFileService.listHierarchicalTree((User)cytomineService.getCurrentUser(), root)
            //if view is datatables, change way to store data
        } else if (params.datatables) {
            uploadedFiles = dataTablesService.process(params, UploadedFile, null, null, null)
        } else {
            Boolean onlyRoots
            if(params.onlyRoots) {
                onlyRoots = Boolean.parseBoolean(params.onlyRoots)
            }
            Long parent
            if(params.parent){
                parent = Long.parseLong(params.parent)
            }
            if(params.all){
                uploadedFiles = uploadedFileService.list()
            } else {
                uploadedFiles = uploadedFileService.list((User)secUserService.getUser(cytomineService.getCurrentUser().id), parent, onlyRoots)
            }
        }


        responseSuccess(uploadedFiles)
    }

    @RestApiMethod(description="Get an uploaded file")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def show () {
        UploadedFile up = uploadedFileService.get(params.long('id'))
        if (up) {
            securityACLService.checkIsSameUser(up.user, cytomineService.getCurrentUser())
            responseSuccess(up)
        } else {
            responseNotFound("UploadedFile", params.id)
        }
    }

    /**
     * Add a new image
     * TODO:: how to manage security here?
     *
     */
    @RestApiMethod(description="Add a new uploaded file. This DOES NOT upload the file, just create the domain.")
    def add () {
        add(uploadedFileService, request.JSON)
    }

    /**
     * Update a new image
     * TODO:: how to manage security here?
     */
    @RestApiMethod(description="Edit an uploaded file domain (mainly to edit status during upload)")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The uploaded file id")
    ])
    def update () {
        update(uploadedFileService, request.JSON)
    }

    /**
     * Delete a new image
     * TODO:: how to manage security here?
     */
    @RestApiMethod(description="Delete an uploaded file domain. This do not delete the file on disk.")
    @RestApiParams(params=[
    @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The uploaded file id")
    ])
    def delete () {
        delete(uploadedFileService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description="Download the uploaded file")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The uploaded file id")
    ])
    def downloadUploadedFile(){
        UploadedFile up = uploadedFileService.get(params.long('id'));
        if (up) {
            String url = uploadedFileService.downloadURI(up)
            log.info "redirect url"
            redirect (url : url)
        } else {
            responseNotFound("UploadedFile", params.id)
        }
    }
}
