package be.cytomine.api.social

import be.cytomine.Exception.CytomineException

/*
* Copyright (c) 2009-2015. Authors: see NOTICE file.
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
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
class RestUserPositionController extends RestController {

    def cytomineService
    def imageInstanceService
    def secUserService
    def dataSource
    def projectService
    def mongo
    def userPositionService

    @RestApiMethod(description="Record the position of the current user on an image.")
    @RestApiParams(params=[
            @RestApiParam(name="image", type="long", paramType = RestApiParamType.QUERY, description = "The image id (Mandatory)"),
            @RestApiParam(name="topLeftX", type="double", paramType = RestApiParamType.QUERY, description = "Top Left X coordinate of the user viewport"),
            @RestApiParam(name="topRightX", type="double", paramType = RestApiParamType.QUERY, description = "Top Right X coordinate of the user viewport"),
            @RestApiParam(name="bottomLeftX", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Left X coordinate of the user viewport"),
            @RestApiParam(name="bottomRightX", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Right X coordinate of the user viewport"),
            @RestApiParam(name="topLeftY", type="double", paramType = RestApiParamType.QUERY, description = "Top Left Y coordinate of the user viewport"),
            @RestApiParam(name="topRightY", type="double", paramType = RestApiParamType.QUERY, description = "Top Right Y coordinate of the user viewport"),
            @RestApiParam(name="bottomLeftY", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Left Y coordinate of the user viewport"),
            @RestApiParam(name="bottomRightY", type="double", paramType = RestApiParamType.QUERY, description = "Bottom Right Y coordinate of the user viewport"),
            @RestApiParam(name="zoom", type="integer", paramType = RestApiParamType.QUERY, description = "Zoom level in the user viewport")
    ])
    def add() {
        try {
            responseSuccess(userPositionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }


    @RestApiMethod(description="Get the last position for a user and an image.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id (Mandatory)"),
            @RestApiParam(name="user", type="long", paramType = RestApiParamType.PATH, description = "The user id (Mandatory)")
    ])
    def lastPositionByUser() {
        ImageInstance image = imageInstanceService.read(params.id)
        SecUser user = secUserService.read(params.user)
        responseSuccess(userPositionService.lastPositionByUser(image, user))
    }

    @RestApiMethod(description="Get users that have opened an image recently.")
    @RestApiParams(params=[
            @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The image id (Mandatory)"),
    ])
    def listOnlineUsersByImage() {
        ImageInstance image = imageInstanceService.read(params.id)
        responseSuccess(userPositionService.listOnlineUsersByImage(image))
    }
}
