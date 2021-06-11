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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * A ProjectDefaultLayer is a layer in the default list of the visible layer for all the image of the project
 */
@RestApiObject(name = "Project default layer", description = "A Project default layer is a layer in the default list of the visible layer for all the image of the project")
class ProjectDefaultLayer extends CytomineDomain {

    @RestApiObjectField(description = "The user layer")
    User user
    @RestApiObjectField(description = "The project")
    Project project
    @RestApiObjectField(description = "Hide the layer by default (but still in the layer list)")
    boolean hideByDefault

    static belongsTo = [project: Project, user:User]

    static constraints = {
        user(nullable: false)
        project(nullable: false)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        withNewSession {
            ProjectDefaultLayer layer = ProjectDefaultLayer.findByProjectAndUser(project, user)
            if(layer!=null && (layer.id!=id))  {
                throw new AlreadyExistException("ProjectDefaultLayer already exist!")
            }
        }
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {

        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['project'] = domain?.project?.id
        returnArray['user'] = domain?.user?.id
        returnArray['hideByDefault'] = domain?.hideByDefault

        return returnArray
    }

    /* Marshaller Helper fro user field */
    /*private static Integer userID(SearchEngineFilter filter) {
        return filter.getUser()?.id
    }*/

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ProjectDefaultLayer insertDataIntoDomain(def json,def domain = new ProjectDefaultLayer()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        domain.hideByDefault = JSONUtils.getJSONAttrBoolean(json, 'hideByDefault', false)
        return domain;
    }


    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container();
    }

}