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

import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.test.Infos
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * User: lrollus
 * Date: 6/12/11
 * This class implement all method to easily get/create/update/delete/manage Annotation to Cytomine with HTTP request during functional test
 */
class ReviewedAnnotationAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/reviewedannotation/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def countByUser(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/user/$id/reviewedannotation/count.json"
        return doGET(URL, username, password)
    }

    static def countByProject(Long id, String username, String password, Long startDate=null, Long endDate=null) {
        String URL = Infos.CYTOMINEURL + "/api/project/$id/reviewedannotation/count.json?" +
                (startDate ? "&startDate=$startDate" : "") +
                (endDate ? "&endDate=$endDate" : "")
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/reviewedannotation.json"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&project=$id"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&project=$id&user="+idUser
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, List<Long> tags, boolean noTag = false, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&project=$id&tags="+tags.join(",")
        if (noTag) URL+="&noTag=true"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Long idUser, Long idImage, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&project=$id&users="+idUser+"&images="+idImage
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Long idUser, Long idImage, Long idTerm, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&project=$id&users="+idUser+"&images="+idImage+"&term="+idTerm
        return doGET(URL, username, password)
    }

    static def listByImageAndTerm(Long idImage, Long idTerm, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&term=$idTerm&image=$idImage"
        return doGET(URL, username, password)
    }

    static def listByImage(Long id,String username, String password) {
        listByImage(id,null,username,password)
    }

    static def listByImage(Long id, String bbox, String username,String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation.json?reviewed=true&image=$id" + (bbox? "&bbox=$bbox" : "")
        return doGET(URL, username, password)
    }

    static def listByImageAndUser(Long idImage,Long idUser, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?reviewed=true&user=$idUser&image="+idImage+"&conflict=true"
        return doGET(URL, username, password)
    }

    static def listByImageAndReviewUser(Long idImage, Long idReviewUser, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?reviewed=true&reviewUsers=$idReviewUser&image="+idImage+"&conflict=true"
        return doGET(URL, username, password)
    }

    static def listByImageAndUserAndBBOX(Long idImage,Long idUser, String bbox, String username, String password) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?reviewed=true&user="+ idUser +"&image="+idImage+"&bbox=$bbox"
        return doGET(URL, username, password)
    }

    static def create(String jsonAnnotation, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/reviewedannotation.json"
        def result = doPOST(URL,jsonAnnotation,username,password)
        def json = JSON.parse(result.data)
        if(JSON.parse(jsonAnnotation) instanceof JSONArray) return [code: result.code]
        Long idAnnotation = json?.reviewedannotation?.id
        return [data: ReviewedAnnotation.get(idAnnotation), code: result.code]
    }

    static def update(def id, def jsonAnnotation, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/reviewedannotation/" + id + ".json"
        return doPUT(URL,jsonAnnotation,username,password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/reviewedannotation/" + id + ".json"
        return doDELETE(URL,username,password)
    }

    static def markStartReview(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/review.json"
        return doPOST(URL,"",username,password)
    }

    static def markStopReview(def id, String username, String password) {
        markStopReview(id,false,username,password)
    }

    static def markStopReview(def id, boolean cancel, String username,String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/review.json" + (cancel? "?cancel=true": "")
        return doDELETE(URL,username,password)
    }

    static def addReviewAnnotation(def id, String username, String password) {
        addReviewAnnotation(id,null,username,password)
    }

    static def addReviewAnnotation(def id, def terms, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation/" + id + "/review.json"
        return doPOST(URL,(terms? "{ \"terms\":[${terms.join(",")}]}" : ""),username,password)
    }

    static def addReviewAll(Long idImage, List<Long> users, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + idImage + "/annotation/review.json?users="+users.join(",")
        return doPOST(URL,"",username,password)
    }

    static def deleteReviewAll(Long idImage, List<Long> users, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + idImage + "/annotation/review.json?users="+users.join(",")
        return doDELETE(URL,username,password)
    }

    static def removeReviewAnnotation(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/annotation/" + id + "/review.json"
        return doDELETE(URL,username,password)
    }

}
