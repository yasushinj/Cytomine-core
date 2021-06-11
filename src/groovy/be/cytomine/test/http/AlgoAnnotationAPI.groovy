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

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.test.Infos
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage Annotation to Cytomine with HTTP request during functional test
 */
class AlgoAnnotationAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation.json"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, List<Long> tags, boolean noTag = false, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?includeAlgo=true&project=$id&tags="+tags.join(",")
        if (noTag) URL+="&noTag=true"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Long idUser, Long idImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?project=$id&users="+idUser+"&images="+idImage
        return doGET(URL, username, password)
    }

    static def listByProjectAndTerm(Long idProject, Long idTerm, Long idUser,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?term=$idTerm&project=$idProject&user="+idUser
        return doGET(URL, username, password)
    }

    static def listByProjectAndTerm(Long idProject, Long idTerm,List<ImageInstance> images,Long idUser,String username, String password) {
         String URL = Infos.CYTOMINEURL + "api/annotation.json?term=$idTerm&project=$idProject&users="+idUser+"&offset=0&max=5&images=${images.collect{it.id}.join(",")}"
        return doGET(URL, username, password)
    }

    static def listByProjectAndTerm(Long idProject, Long idTerm,Long idImage, Long idUser,String username, String password) {
         String URL = Infos.CYTOMINEURL + "api/annotation.json?term=$idTerm&project=$idProject&users="+idUser+"&offset=0&max=5&images=${idImage}"
        return doGET(URL, username, password)
    }

    static def listByImageAndUser(Long idImage,Long idUser, String username, String password) {
         String URL = Infos.CYTOMINEURL+"api/annotation.json?user=$idUser&image=$idImage"
        return doGET(URL, username, password)
    }

    static def listByImageAndUser(Long idImage,Long idUser, String bbox, boolean netReviewedOnly,Integer force,String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?user=$idUser&image=$idImage&bbox=$bbox&notReviewedOnly=$netReviewedOnly" + (force? "&kmeansValue="+force:"")
        return doGET(URL, username, password)
    }

    static def listByProjectAndUsers(Long id,Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?project=$id&users=" +idUser
        return doGET(URL, username, password)
    }

    static def listByProjectAndUsersWithoutTerm(Long id,Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?project=$id&noTerm=true&users=" +idUser
        return doGET(URL, username, password)
    }

    static def listByProjectAndUsersSeveralTerm(Long id,Long idUser, String username, String password) {
         String URL = Infos.CYTOMINEURL+"api/annotation.json?project=$id&multipleTerm=true&users=" +idUser
        return doGET(URL, username, password)
    }

    static def downloadDocumentByProject(Long idProject,Long idUser, Long idTerm, Long idImageInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/project/"+ idProject +"/algoannotation/download?users=" +idUser + "&terms=" + idTerm +"&images=" + idImageInstance + "&format=pdf"
        return doGET(URL, username, password)
    }

    static def create(String jsonAnnotation,String username, String password) {
        create(jsonAnnotation,null,null,username,password)
    }

    static def create(String jsonAnnotation,def minPoint,def maxPoint, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation.json?"+(minPoint? "&minPoint=$minPoint": "")+(maxPoint? "&maxPoint=$maxPoint": "")
        def result = doPOST(URL, jsonAnnotation,username, password)
        if(JSON.parse(jsonAnnotation) instanceof JSONArray) return result
        result.data = AlgoAnnotation.read(JSON.parse(result.data)?.annotation?.id)
        return result
    }

    static def update(def id, def jsonAnnotation, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation/" + id + ".json"
        return doPUT(URL,jsonAnnotation,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation/" + id + ".json"
        return doDELETE(URL,username, password)
    }

    static def union(def idImage, def idUser, def idTerm, def minIntersectionLength, def bufferLength, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation/method/union.json?idImage=$idImage&idUser=$idUser&idTerm=$idTerm&minIntersectionLength=$minIntersectionLength&bufferLength=$bufferLength"
        return doPUT(URL,"",username,password)
    }

    static def union(def idImage, def idUser, def idTerm, def minIntersectionLength, def bufferLength, def area, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/algoannotation/method/union.json?idImage=$idImage&idUser=$idUser&idTerm=$idTerm&minIntersectionLength=$minIntersectionLength&bufferLength=$bufferLength&area=$area"
        return doPUT(URL,"",username,password)
    }

    static def countByProject(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "/api/project/$id/algoannotation/count.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

}
