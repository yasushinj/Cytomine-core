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

import be.cytomine.ontology.AnnotationFilter
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage Discipline to Cytomine with HTTP request during functional test
 */
class AnnotationFilterAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationfilter/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationfilter.json?project=$id"
        return doGET(URL, username, password)
    }

    static def listByOntology(Long id,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/ontology/$id/annotationfilter.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationfilter.json"
        def result = doPOST(URL, json,username, password)
        Long idAnnotationFilter = JSON.parse(result.data)?.annotationfilter?.id
        return [data: AnnotationFilter.get(idAnnotationFilter), code: result.code]
    }

    static def update(def id, def json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationfilter/" + id + ".json"
        return doPUT(URL,json,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotationfilter/" + id + ".json"
        return doDELETE(URL,username,password)
    }
}
