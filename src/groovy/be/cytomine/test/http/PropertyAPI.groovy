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

import be.cytomine.meta.Property
import be.cytomine.test.Infos
import grails.converters.JSON

class PropertyAPI extends DomainAPI {

    //SHOW
    static def show(Long id, Long idDomain, String type, String username, String password) {
        String typeCorrection = (type.contains(".")? "domain/$type" : type )
        String URL = Infos.CYTOMINEURL + "api/$typeCorrection/$idDomain/property/${id}.json"
        return doGET(URL, username, password)
    }

    //LISTBY...
    static def listByDomain(Long id, String type, String username, String password) {
        String typeCorrection = (type.contains(".")? "domain/$type" : type )
        String URL = Infos.CYTOMINEURL + "api/$typeCorrection/$id/property.json"
        return doGET(URL, username, password)
    }

    static def listKeywords(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/keywords.json"
        return doGET(URL, username, password)
    }

    //LISTKEYFORANNOTATION
    static def listKeyWithProject(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation/property/key.json?idProject=$idProject"
        return doGET(URL, username, password)
    }
    static def listKeyWithImage(Long idImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation/property/key.json?idImage=$idImage"
        return doGET(URL, username, password)
    }

    static def listKeyForImageInstanceWithProject(Long idProject, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/property/key.json?idProject=$idProject"
        return doGET(URL, username, password)
    }


    //ADD
    static def create(Long idDomain, String type, String json, String username, String password) {
        String typeCorrection = (type.contains(".")? "domain/$type" : type )
        String URL = Infos.CYTOMINEURL + "api/$typeCorrection/$idDomain/property.json"
        def result = doPOST(URL,json,username,password)
        result.data = Property.get(JSON.parse(result.data)?.property?.id)
        return result
    }

    //UPDATE
    static def update(def id, Long idDomain, String type, def jsonAnnotationProperty, String username, String password) {
        String typeCorrection = (type.contains(".")? "domain/$type" : type )
        String URL = Infos.CYTOMINEURL + "api/$typeCorrection/$idDomain/property/${id}.json"
        return doPUT(URL,jsonAnnotationProperty,username,password)
    }

    //DELETE
    static def delete(def id, Long idDomain, String type, String username, String password) {
        String typeCorrection = (type.contains(".")? "domain/$type" : type )
        String URL = Infos.CYTOMINEURL + "api/$typeCorrection/$idDomain/property/${id}.json"
        return doDELETE(URL,username,password)
    }

    //LISTANNOTATIONPOSITION
    static def listAnnotationCenterPosition(Long idUser, Long idImage, String bbox, String key, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$idUser/imageinstance/$idImage/annotationposition.json?bbox=$bbox,&key=$key"
        return doGET(URL, username, password)
    }
}
