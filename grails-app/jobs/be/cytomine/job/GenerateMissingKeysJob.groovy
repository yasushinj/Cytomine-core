package be.cytomine.job

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

import be.cytomine.security.SecUser

/**
 * Cytomine
 * User: stevben
 * Date: 6/01/12
 * Time: 15:56
 */
class GenerateMissingKeysJob {

    static triggers = {
        simple name: 'generateMissingKeysJob', startDelay: 1000, repeatInterval: 60000*60
    }

    def execute() {
        Collection<SecUser> secUsers = SecUser.findAllByPrivateKeyIsNullOrPublicKeyIsNull()
        secUsers?.each { user ->
            user.generateKeys()
            user.save()
        }
    }
}
