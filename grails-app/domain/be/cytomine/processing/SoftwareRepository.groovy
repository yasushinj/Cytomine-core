package be.cytomine.processing

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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
import be.cytomine.utils.JSONUtils
import com.rabbitmq.tools.json.JSONUtil
import org.restapidoc.annotation.RestApiObjectField

class SoftwareRepository extends CytomineDomain {

    @RestApiObjectField(description = "The software repository provider's name")
    String provider

    @RestApiObjectField(description = "The software user name")
    String userName

    @RestApiObjectField(description = "The software repository name")
    String repositoryName

//    @RestApiObjectField(description = "The prefix used to recognize a software in a repository")
//    String prefix
//
//    @RestApiObjectField(description = "The name of the installer")
//    String installerName

    static constraints = {
        provider(nullable: false, blank: false)
        //repositoryUser(nullable:false, blank: false)
    }

    static mapping = {
        id(generator: "assigned")
        sort("id")
        //installerName(defaultValue: "'add_cytomine_software.py'")
    }

    @Override
    void checkAlreadyExist() {
        SoftwareRepository.withNewSession {
            if (repositoryName) {
                SoftwareRepository softwareRepository = SoftwareRepository.findByRepositoryName(repositoryName)
                if (softwareRepository != null && softwareRepository.id != id) {
                    throw new AlreadyExistException("The software repository ${repositoryName} already exists !")
                }
            }
        }
//        SoftwareRepository.withNewSession {
//            if (repositoryUser) {
//                SoftwareRepository softwareRepository = SoftwareRepository.findByRepositoryUser(repositoryUser)
//                if (softwareRepository != null && softwareRepository.id != id) {
//                    throw new AlreadyExistException("Software repository " + softwareRepository.repositoryUser + " already exists !");
//                }
//            }
//        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static SoftwareRepository insertDataIntoDomain(def json, def domain = new SoftwareRepository()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.provider = JSONUtils.getJSONAttrStr(json, 'provider')
        domain.userName = JSONUtils.getJSONAttrStr(json, 'userName')
        domain.repositoryName = JSONUtils.getJSONAttrStr(json, 'repositoryName')
        return domain

//        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
//        domain.provider = JSONUtils.getJSONAttrStr(json, 'provider')
//        domain.repositoryUser = JSONUtils.getJSONAttrStr(json, 'repositoryUser')
//        domain.prefix = JSONUtils.getJSONAttrStr(json, 'prefix')
//        domain.installerName = JSONUtils.getJSONAttrStr(json, 'installerName')
//        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['provider'] = domain?.provider
        returnArray['userName'] = domain?.userName
        returnArray['repositoryName'] = domain?.repositoryName
        return returnArray
//        def returnArray = CytomineDomain.getDataFromDomain(domain)
//        returnArray['provider'] = domain?.provider
//        returnArray['repositoryUser'] = domain?.repositoryUser
//        returnArray['prefix'] = domain?.prefix
//        returnArray['installerName'] = domain?.installerName
//        return returnArray
    }

}
