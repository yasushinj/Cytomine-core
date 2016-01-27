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
        boolean all = params.all
        responseSuccess(projectConnectionService.getConnectionByUserAndProject(user, project, all))
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

    def userProjectConnectionHistory = {
        SecUser user = secUserService.read(params.user)
        Project project = projectService.read(params.project)
        // Int limit
        // Int offset

        // si offset = 0 , j'en prend limit, sinon, j'en prends limit +1 pour avoir les infos du précédent.

        // TODO changer le ALL par un limit et si -1 alors c'est all.
        def connections = projectConnectionService.getConnectionByUserAndProject(user, project, true)

        def result = []

        //long before = 0; // Date.now si pas de précédent et le created du précédent sinon

        if(connections.size() >= 1) { // si on a passé le paramètre demandant la durée

            Date after = connections[connections.size()-1].created;

            def imagesConsultations = [] // here get image consultation for user, project and between the 2 dates.
            imagesConsultations = (imagesConsultations.size() > 0) ? imagesConsultations : []
            println imagesConsultations.size()


            //merging
            if(imagesConsultations.size()>=1) {
                int beginJ = imagesConsultations.size()-1;
                for(int i=connections.size()-1;i>=1;i--){
                    def nextConnection = connections[i-1];
                    int j = beginJ;
                    /*while(j>=0 && imagesConsultations[j].created < nextConnection.created){
                        get the list of images
                    }
                    beginJ = j;*/

                }
                // put the images by connexion in the results
            } else {
                result = connections;
            }
        }

        responseSuccess(result)
    }

}
