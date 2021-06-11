package be.cytomine.image.server

import be.cytomine.CytomineDomain

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
import grails.util.Environment
import grails.util.Holders
/**
 * Cytomine
 * User: stevben
 * Date: 5/02/13
 * Time: 11:40
 */
class ImageServerStorage {
    ImageServer imageServer
    Storage storage
    
    def grailsApplication

    def getZoomifyUrl_bk() {
        log.info "calling getZoomifyUrl().."
        log.info "imageServer.url=${imageServer.url}"

        return imageServer.url + imageServer.service + "?zoomify=" + storage.getBasePath()
        
    }
    // [reveal-change] only return valid image server url from config file
    def getZoomifyUrl() {
        log.info "calling getZoomifyUrl().."
        log.info "imageServer.url=${imageServer.url}"
        try {
            def url = imageServer.url
            log.info "url=${url}"
            if(grailsApplication.config.grails.imageServerURL.contains(url)) {  
                return imageServer.url + imageServer.service + "?zoomify=" + storage.getBasePath()
            } else {
                def alternate_url = grailsApplication.config.grails.imageServerURL[0]
                log.info "alternate url=${alternate_url}"
                return alternate_url + imageServer.service + "?zoomify=" + storage.getBasePath()
            }
        } catch (Exception e) {
            e.printStackTrace()
            return imageServer.url + imageServer.service + "?zoomify=" + storage.getBasePath()
        }
    }

    /**
     * Define fields available for JSON response
     * This Method is called during application start
     */
    static def getDataFromDomain(def is) {
        def returnArray = [:]
        returnArray['imageServer'] = is?.imageServer
        returnArray['storage'] = is?.storage
        returnArray
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return storage.container();
    }
}
