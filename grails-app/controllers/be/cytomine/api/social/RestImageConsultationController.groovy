package be.cytomine.api.social

import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController

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
import be.cytomine.project.Project

/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
class RestImageConsultationController extends RestController {

    def projectService
    def imageConsultationService

    def add = {
        try {
            responseSuccess(imageConsultationService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    def lastImageOfUsersByProject = {
        Project project = projectService.read(params.project)
        responseSuccess(imageConsultationService.lastImageOfUsersByProject(project))
    }

}
