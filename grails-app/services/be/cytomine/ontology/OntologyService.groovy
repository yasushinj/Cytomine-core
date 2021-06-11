package be.cytomine.ontology

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

import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.CytomineException
import be.cytomine.annotations.DependencyOrder
import be.cytomine.command.*
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import org.springframework.security.acls.domain.BasePermission

import static org.springframework.security.acls.domain.BasePermission.*

class OntologyService extends ModelService {

    static transactional = true
    boolean saveOnUndoRedoStack = true

    def springSecurityService
    def transactionService
    def termService
    def securityACLService
    def projectService
    def secUserService
    def permissionService

    def currentDomain() {
        return Ontology
    }

    Ontology read(def id) {
        def ontology = Ontology.read(id)
        if (ontology) {
            securityACLService.check(ontology,READ)
            checkDeleted(ontology)
        }
        ontology
    }

    /**
     * List ontology with full tree structure (term, relation,...)
     * Security check is done inside method
     */
    def list() {
        def user = cytomineService.currentUser
        return securityACLService.getOntologyList(user)
    }

    /**
     * List ontology with just id/name
     * Security check is done inside method
     */
    def listLight() {
        def ontologies = list()
        def data = []
        ontologies.each { ontology ->
            def ontologymap = [:]
            ontologymap.id = ontology.id
            ontologymap.name = ontology.name
            data << ontologymap
        }
        return data
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser), null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(Ontology domain, def jsonNewData) throws CytomineException {
        securityACLService.check(domain,WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser),domain,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(Ontology domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time

        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain,DELETE)
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def determineRightsForUsers(Ontology ontology, def users) {
        users.each{determineRightsForUser(ontology, it)}
    }
    def determineRightsForUser(Ontology ontology, SecUser user) {
        List<Project> projects = projectService.listByOntology(ontology)
        if(projects.any{secUserService.listAdmins(it).contains(user)}) permissionService.addPermission(ontology, user.username, BasePermission.ADMINISTRATION)
        else permissionService.deletePermission(ontology, user.username, BasePermission.ADMINISTRATION)
        if(projects.any{secUserService.listUsers(it).contains(user)}) permissionService.addPermission(ontology, user.username, BasePermission.READ)
        else permissionService.deletePermission(ontology, user.username, BasePermission.READ)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name]
    }

    def afterAdd(def domain, def response) {
        aclUtilService.addPermission(domain, cytomineService.currentUser.username, BasePermission.ADMINISTRATION)
    }

    @DependencyOrder(order = 0)
    def deleteDependentTerm(Ontology ontology, Transaction transaction, Task task = null) {
        Term.findAllByOntology(ontology).each {
            termService.delete(it,transaction, null,false)
        }
    }

    @DependencyOrder(order = 1)
    def deleteDependentProject(Ontology ontology, Transaction transaction, Task task = null) {
        if(Project.findByOntologyAndDeletedIsNull(ontology)) {
            throw new ConstraintException("Ontology is linked with project. Cannot delete ontology!")
        }
    }
}
