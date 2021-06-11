package be.cytomine.middleware

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

import be.cytomine.image.server.ImageServer
import be.cytomine.test.HttpClient
import grails.converters.JSON
import grails.transaction.Transactional
import grails.util.Environment
import grails.util.Holders

@Transactional
class ImageServerService {
    def grailsApplication

    def list() {
        ImageServer.list()
    }

    def getStorageSpaces_bk() {
        def result = []
        String url;
        ImageServer.list().each {
            url = it.url+"/storage/size.json"

            HttpClient client = new HttpClient()

            client.connect(url,"","")

            client.get()

            String response = client.getResponseData()
            int code = client.getResponseCode()
            log.info "code=$code response=$response"
            if(code < 400){
                result << JSON.parse(response)
            }
        }
        // if dns sharding, multiple link are to the same IMS. We merge the same IMS.
        result = result.unique { it.hostname }
        return result
    }

    // [reveal-change] filter only image server storage found in config files
    def getStorageSpaces() {
        def result = []
        String url;
        ImageServer.list().each { server ->
            if(grailsApplication.config.grails.imageServerURL.contains(server.url)) { 
                url = it.url+"/storage/size.json"

                HttpClient client = new HttpClient()

                client.connect(url,"","")

                client.get()

                String response = client.getResponseData()
                int code = client.getResponseCode()
                log.info "code=$code response=$response"
                if(code < 400){
                    result << JSON.parse(response)
                }
            }
        }

        // if dns sharding, multiple link are to the same IMS. We merge the same IMS.
        result = result.unique { it.hostname }
        return result
    }
}
