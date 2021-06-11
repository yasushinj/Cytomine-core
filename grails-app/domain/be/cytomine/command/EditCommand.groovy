package be.cytomine.command

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

import be.cytomine.CytomineDomain
import be.cytomine.project.Project
import grails.converters.JSON

/**
 * @author ULG-GIGA Cytomine Team
 * The EditCommand class is a command that edit a domain
 * It provide an execute method that edit domain from command, an undo method that undo edit on domain and an redo method edit domain
 */
class EditCommand extends Command {

    /**
     * Add command info for the new domain concerned by the command
     * @param newObject domain after update
     * @param oldObject domain before update
     * @param message Message build for the command
     */
    protected void fillCommandInfo(def newObject, def oldObject, String message) {
        HashMap<String, Object> paramsData = new HashMap<String, Object>()
        paramsData.put('previous' + responseService.getClassName(newObject), (JSON.parse(oldObject)))
        paramsData.put("new" + responseService.getClassName(newObject), newObject)
        data = (paramsData) as JSON
        actionMessage = message
    }

    /**
     * Get domain name
     * @return domaine name
     */
    String domainName() {
        String domain = serviceName.replace("Service", "")
        return domain.substring(0, 1).toUpperCase() + domain.substring(1);
    }

    /**
     * Process an Add operation for this command
     * @return Message
     */
    def execute() {
        initService()
        //Retrieve domain to update it
        def updatedDomain = this.domain
        def oldDomain = updatedDomain.encodeAsJSON()
        updatedDomain.insertDataIntoDomain(json,updatedDomain)
        //Init command info
        CytomineDomain container = updatedDomain?.container()
        if(container && container instanceof Project) {
            super.setProject(container)
        }
        def response = service.edit(updatedDomain, printMessage)
        fillCommandInfo(updatedDomain, oldDomain, response.data.message)
        return response
    }

    /**
     * Process an undo op
     * @return Message
     */
    def undo() {
        initService()
        return service.edit(JSON.parse(data).get("previous" + domainName()), printMessage)
    }

    /**
     * Process a redo op
     * @return Message
     */
    def redo() {
        initService()
        return service.edit(JSON.parse(data).get("new" + domainName()), printMessage)
    }
}
