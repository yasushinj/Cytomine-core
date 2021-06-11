package be.cytomine.image.server

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

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class ImageServerStorageService extends ModelService {

    def securityACLService

    def currentDomain() {
        return ImageServerStorage;
    }

    def add(def json) {
        securityACLService.check(json.storage,Storage,WRITE)
        Command c = new AddCommand(user: cytomineService.getCurrentUser())
        executeCommand(c,null,json)
    }

    def delete(ImageServerStorage iss, Transaction transaction = null, Task task = null) {
        securityACLService.check(iss.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,iss,null)

    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.imageServer.url, domain.storage.name]
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    def retrieve(Map json) {
        ImageServerStorage.read(json.id)
    }
}
