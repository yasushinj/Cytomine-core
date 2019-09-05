package be.cytomine.meta

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class TagDomainAssociationService extends ModelService {

    static transactional = true
    def springSecurityService
    def transactionService
    def commandService
    def cytomineService
    def securityACLService

    def currentDomain() {
        return TagDomainAssociation
    }

    def read(Long id) {
        def association = TagDomainAssociation.read(id)
        if (association && !association.domainClassName.contains("AbstractImage")) {
            securityACLService.check(association.container(),READ)
        }
        association
    }

    /**
     * List all tags
     */
    def list(def searchParameters = [], Long max = 0, Long offset = 0) {
        def validSearchParameters = getDomainAssociatedSearchParameters(TagDomainAssociation, searchParameters)

        return criteriaRequestWithPagination(TagDomainAssociation, max, offset, {}, validSearchParameters, "created", "desc")
    }

    /**
     * List all tags
     */
    def listByTag(Tag tag) {
        return TagDomainAssociation.findAllByTag(tag)
    }

    /**
     * List all tags
     */
    def listByDomain(CytomineDomain domain) {
        return TagDomainAssociation.findAllByDomainClassNameAndDomainIdent(domain.getClass().name, domain.id)
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        def domainClass = json.domainClassName
        CytomineDomain domain

        if(domainClass.contains("AnnotationDomain")) {
            domain = AnnotationDomain.getAnnotationDomain(json.domainIdent)
        } else {
            domain = Class.forName(domainClass, false, Thread.currentThread().contextClassLoader).read(JSONUtils.getJSONAttrLong(json,'domainIdent',0))
        }

        if (domain != null && !domain.class.name.contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ)
            if (domain.hasProperty('user') && domain.user) {
                securityACLService.checkFullOrRestrictedForOwner(domain, domain.user)
            } else {
                securityACLService.checkisNotReadOnly(domain)
            }
        }

        SecUser currentUser = cytomineService.getCurrentUser()
        Command command = new AddCommand(user: currentUser)
        return executeCommand(command,null,json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(TagDomainAssociation domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()

        if(!domain.domainClassName.contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ)
            if (domain.retrieveCytomineDomain().hasProperty('user') && domain.retrieveCytomineDomain().user) {
                securityACLService.checkFullOrRestrictedForOwner(domain, domain.retrieveCytomineDomain().user)
            } else if (domain.domainClassName.contains("Project")){
                securityACLService.check(domain.domainIdent,domain.domainClassName, WRITE)
            } else {
                securityACLService.checkisNotReadOnly(domain)
            }
        }

        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(TagDomainAssociation domain) {
        return [domain.tag.name, domain.domainIdent, domain.domainClassName]
    }
}
