package be.cytomine.test.http

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

import be.cytomine.middleware.AmqpQueue
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * Created by julien 
 * Date : 02/03/15
 * Time : 09:18
 */
class AmqpQueueAPI extends DomainAPI{

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/amqp_queue/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password, String name) {
        String URL

        if(name) {
            URL = Infos.CYTOMINEURL + "api/amqp_queue.json?name=" + name
        }
        else
            URL = Infos.CYTOMINEURL + "api/amqp_queue.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/amqp_queue.json"
        def result = doPOST(URL,json,username,password)
        result.data = AmqpQueue.get(JSON.parse(result.data)?.amqpqueue?.id)
        return result
    }

    static def update(Long id, def jsonAmqpQueue, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/amqp_queue/" + id + ".json"
        return doPUT(URL,jsonAmqpQueue,username,password)
    }

    static def delete(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/amqp_queue/" + id + ".json"
        return doDELETE(URL,username,password)
    }

}