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
import be.cytomine.social.LastUserPosition
import be.cytomine.social.PersistentUserPosition
import be.cytomine.social.UserPosition
import be.cytomine.utils.JSONUtils
import org.joda.time.DateTime

import static org.springframework.security.acls.domain.BasePermission.READ

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
    def noSQLCollectionService
    def userPositionService

    /**
     * Add new position for user
     */
    def add = {

        try {
            responseSuccess(userPositionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }


    /**
     * Get the last position for a user and an image
     */
    def lastPositionByUser = {
        ImageInstance image = imageInstanceService.read(params.id)
        SecUser user = secUserService.read(params.user)
        responseSuccess(userPositionService.lastPositionByUser(image, user))
    }

    /**
     * Get users that have opened an image now
     */
    def listOnlineUsersByImage = {
        ImageInstance image = imageInstanceService.read(params.id)
        responseSuccess(userPositionService.listOnlineUsersByImage(image))
    }

    /**
     * Get online users
     */
    def listLastUserPositionsByProject = {
        Project project = projectService.read(params.project)
        responseSuccess(userPositionService.listLastUserPositionsByProject(project))
    }

    def lastImageOfUsersByProject = {
        Project project = projectService.read(params.project)
        responseSuccess(userPositionService.lastImageOfUsersByProject(project))
    }

}
