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

import be.cytomine.image.multidim.ImageGroup
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage ImageFilter to Cytomine with HTTP request during functional test
 */
class ImageGroupAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imagegroup/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$idProject/imagegroup.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imagegroup.json"
        def result = doPOST(URL, json,username, password)
        Long idDiscipline = JSON.parse(result.data)?.imagegroup?.id
        return [data: ImageGroup.get(idDiscipline), code: result.code]
    }

    static def update(Long id, def jsonImageGroup, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imagegroup/" + id + ".json"
        return doPUT(URL,jsonImageGroup,username,password)
    }

    static def delete(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imagegroup/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    static def getInfos(Long id, String username, String password){
        String URL = Infos.CYTOMINEURL + "/api/imagegroup/$id/characteristics.json"
        return doGET(URL, username, password)
    }
}
