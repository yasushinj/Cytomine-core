package be.cytomine.project

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

import be.cytomine.Exception.CytomineException
import be.cytomine.command.*
import be.cytomine.security.User
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.transaction.Transactional

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@Transactional
class ProjectDefaultLayerService extends ModelService {

    static transactional = true
    boolean saveOnUndoRedoStack = true

    def springSecurityService
    def transactionService
    def securityACLService

    def currentDomain() {
        return ProjectDefaultLayer
    }

    ProjectDefaultLayer read(def id) {
        def layer = ProjectDefaultLayer.read(id)
        if (layer) {
            securityACLService.check(layer,READ)
        }
        layer
    }

    /**
     * Get all default layers of the current project
     * @return ProjectDefaultLayer list
     */
    def listByProject(Project project) {
        securityACLService.check(project,READ)
        return ProjectDefaultLayer.findAllByProject(project)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        securityACLService.check(json.project,Project,WRITE)
        User user = User.get(json.user)
        def result =  executeCommand(new AddCommand(user: user), null,json)
        return result
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(ProjectDefaultLayer domain, def jsonNewData) throws CytomineException {
        securityACLService.check(domain,WRITE)
        User user = domain.getUser()
        return executeCommand(new EditCommand(user: user),domain,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ProjectDefaultLayer domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.check(domain.getProject(),WRITE)
        User user = domain.getUser()
        Command c = new DeleteCommand(user: user,transaction:transaction)
        return executeCommand(c,domain,null, task)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.user.firstname+" "+domain.user.lastname]
    }
}
