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
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.middleware.AmqpQueue
import be.cytomine.utils.JSONUtils
import com.rabbitmq.tools.json.JSONUtil
import org.restapidoc.annotation.RestApiObjectField

import java.rmi.AlreadyBoundException

class ProcessingServer extends CytomineDomain {

    @RestApiObjectField(description = "The name of the processing server")
    String name

    @RestApiObjectField(description = "The host of the processing server")
    String host

    @RestApiObjectField(description = "The type of the processing server")
    String type

    @RestApiObjectField(description = "The processing method name of the processing server")
    String processingMethodName

    @RestApiObjectField(description = "The communication method name of the processing server")
    String communicationMethodName

    @RestApiObjectField(description = "The amqp queue associated to a given processing server")
    AmqpQueue amqpQueue

    static constraints = {
        name(nullable: false, blank: false, unique: true)
        host(blank: false)
        processingMethodName(blank: false)
        communicationMethodName(blank: false)
        amqpQueue(nullable: true)
    }

    static mapping = {
        id(generator: "assigned")
        sort("id")
        host(defaultValue: "'localhost'")
    }

    @Override
    void checkAlreadyExist() {
        ProcessingServer.withNewSession {
            if (name) {
                ProcessingServer processingServer = ProcessingServer.findByName(name)
                if (processingServer != null && processingServer.id != id) {
                    throw new AlreadyExistException("Processing server ${processingServer.name} + already exists !")
                }
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ProcessingServer insertDataIntoDomain(def json, def domain = new ProcessingServer()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name')
        domain.host = JSONUtils.getJSONAttrStr(json, 'host')
        domain.type = JSONUtils.getJSONAttrStr(json, 'type')
        domain.processingMethodName = JSONUtils.getJSONAttrStr(json, 'processingMethodName')
        domain.communicationMethodName = JSONUtils.getJSONAttrStr(json, 'communicationMethodName')
        domain.amqpQueue = JSONUtils.getJSONAttrDomain(json, 'amqpQueue', new AmqpQueue(), false)
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['host'] = domain?.host
        returnArray['type'] = domain?.type
        returnArray['processingMethodName'] = domain?.processingMethodName
        returnArray['communicationMethodName'] = domain?.communicationMethodName
        returnArray['amqpQueue'] = domain?.amqpQueue
        return returnArray
    }

}
