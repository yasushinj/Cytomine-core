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
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * User: lrollus
 * Date: 6/12/11
 * GIGA-ULg
 * This class implement all method to easily get/create/update/delete/manage ImageInstance to Cytomine with HTTP request during functional test
 */
class ImageInstanceAPI extends DomainAPI {

    static def listLightByUser(Long id, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$id/imageinstance/light.json?max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByUser(Long userId, def searchParameters = [], Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/user/$userId/imageinstance.json?${convertSearchParameters(searchParameters)}&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Integer max = 0, Integer offset = 0, def searchParameters = [], String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?${convertSearchParameters(searchParameters)}&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByProjectDatatables(Long id, int offset, int max, String sSearch, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?datatables=true&length=$max&start=$offset" + (sSearch? "&search%5Bvalue%5D=$sSearch":"")
        return doGET(URL, username, password)
    }

    static def listByProject(Long id, Long inf, Long sup,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?inf=$inf&sup=$sup"
        return doGET(URL, username, password)
    }

    static def listByProjectTree(Long id, Long max = 0 , Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?tree=true&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByProjectWithLastActivity(Long id, Long max = 0, Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?withLastActivity=true&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def listByProjectLight(Long id, Long max = 0 , Long offset = 0, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$id/imageinstance.json?light=true&max=$max&offset=$offset"
        return doGET(URL, username, password)
    }

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def listLastOpened(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/method/lastopened.json"
        return doGET(URL, username, password)
    }

    static def getBounds(Long projectId, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/$projectId/bounds/imageinstance.json"
        return doGET(URL, username, password)
    }


    static def create(String jsonImageInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance.json"
        def result = doPOST(URL,jsonImageInstance,username,password)
        result.data = ImageInstance.get(JSON.parse(result.data)?.imageinstance?.id)
        return result
    }

    static def update(def id, def jsonImageInstance, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + ".json"
        return doPUT(URL,jsonImageInstance,username,password)
    }

    static def delete(ImageInstance image, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + image.id + ".json"
        return doDELETE(URL,username,password)
    }

    static def next(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/next.json"
        return doGET(URL, username, password)
    }

    static def previous(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/previous.json"
        return doGET(URL, username, password)
    }

    static def sameImageData(Long id, String username, String password, Long idProject = null) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/sameimagedata.json" + (idProject? "?project=$idProject" : "")
        return doGET(URL, username, password)
    }

    static def copyImageData(Long id, def layers,def idTask,String username, String password) {
        copyImageData(id,false,layers,idTask,username,password)
    }

    static def copyImageData(Long id, boolean giveMe,def layers,def idTask,String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/copyimagedata.json?layers="+layers.collect{it.image.id+"_"+it.user.id}.join(",")  + (idTask? "&task=$idTask" : "")  + (giveMe? "&giveMe=$giveMe" : "")
        return doPOST(URL,"", username, password)
    }

    static def copyMetaData(Long id,Long based, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/copymetadata.json?based=$based"
        return doPOST(URL,"", username, password)
    }

//    "/api/imageinstance/$id/sameimagedata"(controller :"restImageInstance") {
//        action = [GET:"retrieveSameImageOtherProject"]
//    }
//    "/api/imageinstance/$id/copyimagedata"(controller :"restImageInstance") {
//        action = [POST:"copyAnnotationFromSameAbstractImage"]
//    }

    static def download(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imageinstance/" + id + "/download"
        return doGET(URL, username, password)
    }




    static ImageInstance buildBasicImage(String username, String password) {
        //Create project with user 1
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(), username, password)
        assert 200==result.code
        Project project = result.data
        //Add image with user 1
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        result = ImageInstanceAPI.create(image.encodeAsJSON(), username, password)
        assert 200==result.code
        image = result.data
        return image
    }

}
