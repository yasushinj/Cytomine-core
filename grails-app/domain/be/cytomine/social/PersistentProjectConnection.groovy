package be.cytomine.social

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

import be.cytomine.CytomineDomain
import be.cytomine.project.Project
import be.cytomine.security.SecUser

/**
 * Info on last user connection for a project
 * User x connect to poject y the 2013/01/01 at xxhyymin
 */
class PersistentProjectConnection extends CytomineDomain{

    static mapWith = "mongo"

    static transients = ['id','updated','deleted','class']

    static belongsTo = [user : SecUser, project: Project]

    SecUser user
    Project project

    static constraints = {
        user (nullable:false)
        project (nullable: false)
    }

    static mapping = {
        version false
        stateless true //don't store data in memory after read&co. These data don't need to be update.
        project index:true
        compoundIndex project:1, created:-1
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray.created = domain?.created
        returnArray.user = domain?.user?.id
        returnArray.project = domain?.project?.id
        returnArray
    }
}
