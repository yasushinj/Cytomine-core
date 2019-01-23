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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.RestController
import be.cytomine.image.AbstractImage
import be.cytomine.image.ImageInstance
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageSequence
import be.cytomine.image.server.ImageServer
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.test.HttpClient
import grails.converters.JSON
import groovyx.net.http.HTTPBuilder
import org.apache.commons.io.IOUtils
import org.restapidoc.annotation.*
import org.restapidoc.pojo.RestApiParamType
import java.awt.image.BufferedImage

/**
 * Controller for abstract image
 * An abstract image can be add in n projects
 */
@RestApi(name = "Image | abstract image services", description = "Methods for managing an image. See image instance service to manage an instance of image in a project.")
class RestAbstractImageController extends RestController {

    def imagePropertiesService
    def abstractImageService
    def cytomineService
    def projectService
    def imageSequenceService
    def dataTablesService
    def imageServerProxyService

    /**
     * List all abstract image available on cytomine
     */
    //TODO:APIDOC

    @RestApiMethod(description="Get all image available for the current user", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="project", type="long", paramType = RestApiParamType.QUERY, description = "If set, check if image is in project or not", required=false),
        @RestApiParam(name="sortColumn", type="string", paramType = RestApiParamType.QUERY, description = "Column sort (created by default)", required=false),
        @RestApiParam(name="sortDirection", type="string", paramType = RestApiParamType.QUERY, description = "Sort direction (desc by default)", required=false),
        @RestApiParam(name="search", type="string", paramType = RestApiParamType.QUERY, description = "Original filename search filter (all by default)", required=false),
        @RestApiParam(name="datatables", type="boolean", paramType=RestApiParamType.QUERY, description="", required=false),
    ])
    def list() {
        SecUser user = cytomineService.getCurrentUser()
        if (params.datatables) {
            Project project = projectService.read(params.long("project"))
            responseSuccess(dataTablesService.process(params, AbstractImage, null, [],project))
        }  else {
            responseSuccess(abstractImageService.list(user))
        }
    }

    /**
     * List all abstract images for a project
     */
    @RestApiMethod(description="Get all image having an instance in a project", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The project id")
    ])
    def listByProject() {
        Project project = Project.read(params.id)
        if (project) {
            responseSuccess(abstractImageService.list(project))
        } else {
            responseNotFound("Image", "Project", params.id)
        }
    }

    /**
     * Get a single image
     */
    @RestApiMethod(description="Get an image")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id")
    ])
    def show() {
        AbstractImage image = abstractImageService.read(params.long('id'))
        if (image) {
            responseSuccess(image)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    /**
     * Add a new image
     * TODO:: how to manage security here?
     */
    @RestApiMethod(description="Add a new image in the software. See 'upload file service' to upload an image")
    def add() {
        add(abstractImageService, request.JSON)
    }

    /**
     * Update a new image
     * TODO:: how to manage security here?
     */
    @RestApiMethod(description="Update an image in the software")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image sequence id")
    ])
    def update() {
        update(abstractImageService, request.JSON)
    }

    /**
     * Delete a new image
     * TODO:: how to manage security here?
     */
    @RestApiMethod(description="Delete an abstract image)")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image sequence id")
    ])
    def delete() {
        delete(abstractImageService, JSON.parse("{id : $params.id}"),null)
    }


    @RestApiMethod(description="Get all unused images available for the current user", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.QUERY, description = "The id of abstract image"),
    ])
    def listUnused() {
        SecUser user = cytomineService.getCurrentUser()
        def result = abstractImageService.listUnused(user);
        responseSuccess(result);
    }

    @RestApiMethod(description="Show user who uploaded an image")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The abstract image id")
    ])
    def showUploaderOfImage() {
        SecUser user = abstractImageService.getUploaderOfImage(params.long('id'))
        if (user) {
            responseSuccess(user)
        } else {
            responseNotFound("User", "Image", params.id)
        }
    }

    @RestApiMethod(description="Get a small image (thumb) for a specific image", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The image id"),
            @RestApiParam(name="refresh", type="boolean", paramType=RestApiParamType.QUERY, description="If true, don't take it from cache and regenerate it", required=false),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def thumb() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  512)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            parameters.refresh = params.boolean('refresh', false)
            responseBufferedImage(imageServerProxyService.thumb(abstractImage, parameters))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get an image (preview) for a specific image", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType=RestApiParamType.PATH, description="The image id"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file", required=false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed", required=false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast", required=false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction", required=false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel", required=false)
    ])
    @RestApiResponseObject(objectIdentifier ="image (bytes)")
    def preview() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def parameters = [:]
            parameters.format = params.format
            parameters.maxSize = params.int('maxSize',  1024)
            parameters.colormap = params.colormap
            parameters.inverse = params.boolean('inverse')
            parameters.contrast = params.double('contrast')
            parameters.gamma = params.double('gamma')
            parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
            responseBufferedImage(imageServerProxyService.thumb(abstractImage, parameters))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get available associated images", listing = true)
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id")
    ])
    @RestApiResponseObject(objectIdentifier ="associated image labels")
    def associated() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def associated = imageServerProxyService.associated(abstractImage)
            responseSuccess(associated)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    @RestApiMethod(description="Get an associated image of a abstract image (e.g. label, macro, thumbnail)", extensions=["png", "jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id"),
            @RestApiParam(name="label", type="string", paramType = RestApiParamType.PATH,description = "The associated image label"),
            @RestApiParam(name="maxSize", type="int", paramType=RestApiParamType.QUERY,description="The thumb max size", required = false),
    ])
    @RestApiResponseObject(objectIdentifier = "image (bytes)")
    def label() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            def parameters = [:]
            parameters.format = params.format
            parameters.label = params.label
            parameters.maxSize = params.int('maxSize', 256)
            def associatedImage = imageServerProxyService.label(abstractImage, parameters)
            responseBufferedImage(associatedImage)
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def crop() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            responseBufferedImage(imageServerProxyService.crop(abstractImage, params))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def windowUrl() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            String url = imageServerProxyService.window(abstractImage, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def window() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            responseBufferedImage(imageServerProxyService.window(abstractImage, params, false))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def cameraUrl() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            params.withExterior = false
            String url = imageServerProxyService.window(abstractImage, params, true)
            responseSuccess([url : url])
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def camera() {
        AbstractImage abstractImage = abstractImageService.read(params.long("id"))
        if (abstractImage) {
            params.withExterior = false
            responseBufferedImage(imageServerProxyService.window(abstractImage, params, false))
        } else {
            responseNotFound("Image", params.id)
        }
    }

    def download() {
        String url = abstractImageService.downloadURI(abstractImageService.read(params.long("id")), params.boolean("parent", false))
        log.info "redirect url"
        redirect (url : url)
    }

    /**
     * Get all image servers URL for an image
     */
    @RestApiMethod(description="Get all image servers URL for an image")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The image id"),
        @RestApiParam(name="merge", type="boolean", paramType = RestApiParamType.QUERY,description = "(Optional) If not null, return url representing the merge of multiple image. Value an be channel, zstack, slice or time."),
        @RestApiParam(name="channels", type="list", paramType = RestApiParamType.QUERY,description = "(Optional) If merge is not null, the list of the sequence index to merge."),
        @RestApiParam(name="colors", type="list", paramType = RestApiParamType.QUERY,description = "(Optional) If merge is not null, the list of the color for each sequence index (colors.size == channels.size)"),
    ])
    @RestApiResponseObject(objectIdentifier = "URL list")
    def imageServers() {

        try {
            def id = params.long('id')
            def merge = params.get('merge')

            if(!merge){
                responseSuccess(abstractImageService.imageServers(id))
                return
            }

            def idImageInstance = params.long('imageinstance')
            ImageInstance image = ImageInstance.read(idImageInstance)

            log.info "Ai=$id Ii=$idImageInstance"

            def sequences = imageSequenceService.get(image)
            ImageSequence sequence
            if(sequences.size() > 0) sequence = sequences[0]

            if(!sequence) {
                throw new WrongArgumentException("ImageInstance $idImageInstance is not in a sequence!")
            }

            ImageGroup group = sequence.imageGroup

            log.info "sequence=$sequence group=$group"

            def images = imageSequenceService.list(group)

            if(merge.equals("channel")){
                images = images.findAll{it.zStack == sequence.zStack && it.time == sequence.time}
            }
            log.info "all image for this group=$images"


            def servers = ImageServer.list()
            Random myRandomizer = new Random();


            def ids = params.get('channels').split(",").collect{Integer.parseInt(it)}
            def colors = params.get('colors').split(",").collect{it}
            def params = []

            ids.eachWithIndex {pos,index ->
                images.each { seq ->
                    def position = -1
                    if(merge=="channel") position = seq.channel
                    if(merge=="zstack") position = seq.zStack
                    if(merge=="slice") position = seq.slice
                    if(merge=="time") position = seq.time

                    if(position==pos && ids.contains(position)) {
                        def urls = abstractImageService.imageServers(seq.image.baseImage.id).imageServersURLs
                        def param = "url$index="+ URLEncoder.encode(urls.first(),"UTF-8") +"&color$index="+ URLEncoder.encode(colors.get(index),"UTF-8")
                        params << param
                    }

                }

            }


            String url = "vision/merge?" + params.join("&") +"&zoomify="
            log.info "url=$url"

            def urls = []

            servers.each {
                urls << it.url +"/"+ url
            }

            //retrieve all image instance (same sequence)


            //get url for each image

            responseSuccess([imageServersURLs : urls])
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

}



