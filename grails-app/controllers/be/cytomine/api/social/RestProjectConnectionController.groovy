package be.cytomine.api.social


/*
* Copyright (c) 2009-2016. Authors: see NOTICE file.
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
import be.cytomine.api.RestController
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.social.PersistentConnection

/**
 * Controller for an user connection to a project
 */
class RestProjectConnectionController extends RestController {

    def cytomineService
    def secUserService
    def projectService
    def projectConnectionService
    def imageConsultationService
    def securityACLService

    def add = {
        try {
            responseSuccess(projectConnectionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    def lastConnectionInProject = {
        Project project = projectService.read(params.project)
        responseSuccess(projectConnectionService.lastConnectionInProject(project));
    }

    def getConnectionByUserAndProject = {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer limit = params.limit != null ? params.getInt('limit') : -1
        def results = projectConnectionService.getConnectionByUserAndProject(user, project, limit, offset)
        // hack to avoid list to be cut. offset was already used in db request
        params.remove("offset")
        responseSuccess(results)
    }

    def numberOfConnectionsByProjectAndUser = {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        Long afterThan = params.long("afterThan");

        if(params.boolean('heatmap')) {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectOrderedByHourAndDays(project, afterThan, user))
        } else {
            responseSuccess(projectConnectionService.numberOfConnectionsByProjectAndUser(project, user))
        }
    }

    def numberOfProjectConnections = {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        Long afterThan = params.long("afterThan");
        String period = params.get("period").toString()

        responseSuccess(projectConnectionService.numberOfProjectConnections(afterThan,period))
    }

    def averageOfProjectConnections= {
        securityACLService.checkAdmin(cytomineService.getCurrentUser())
        Long afterThan = params.long("afterThan");
        String period = params.get("period").toString()

        responseSuccess(projectConnectionService.averageOfProjectConnections(afterThan,period))
    }

    def userProjectConnectionHistory = {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        Integer offset = params.offset != null ? params.getInt('offset') : 0
        Integer limit = params.limit != null ? params.getInt('limit') : -1

        // if offset > 0, we take limit +1 object to have the created of the previous object.
        boolean getPrevious;
        if(offset > 0 && limit >= 0) {
            limit++
            offset--
            getPrevious = true;
        };

        def connections = projectConnectionService.getConnectionByUserAndProject(user, project, limit, offset)

        Date before;
        def result = []
        if(connections.size() == 0) {
            return result
        }
        if(getPrevious) {
            before = connections.remove(0).created;
        } else {
            before = new Date();
        }

        if(connections.size() >= 1) {

            Date after = connections[connections.size()-1].created;

            def imagesConsultations = imageConsultationService.getImagesOfUsersByProjectBetween(user, project,after, before)

            imagesConsultations = (imagesConsultations.size() > 0) ? imagesConsultations : []

            //merging
            if(imagesConsultations.size()>=1) {
                def consultedImages;
                int beginJ = imagesConsultations.size()-1;
                for(int i=connections.size()-1;i>=1;i--){
                    consultedImages = [];

                    def nextConnection = connections[i-1];
                    int j = beginJ;
                    while(j>=0 && imagesConsultations[j].created < nextConnection.created){
                        consultedImages << imagesConsultations[j]
                        j--
                    }
                    beginJ = j;

                    result << [id : connections[i].id, created: connections[i].created, user: user.id,
                               project : project.id, time:connections[i].time, images:consultedImages]

                }
                consultedImages = [];
                for(int j=beginJ;j>=0;j--){
                    consultedImages << imagesConsultations[j]
                }

                result << [id : connections[0].id, created: connections[0].created, user:user.id,
                           project : project.id, time:connections[0].time, images:consultedImages]
                result = result.reverse();
            } else {
                result = connections;
            }
        }

        // hack to avoid list to be cut. offset was already used in db request
        params.remove("offset")

        responseSuccess(result)
    }
}
