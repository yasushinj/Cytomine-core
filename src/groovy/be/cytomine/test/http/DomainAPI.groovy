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

import be.cytomine.test.HttpClient
import be.cytomine.test.Infos
import grails.converters.JSON
import groovy.util.logging.Log
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.codehaus.groovy.grails.web.json.JSONObject

import java.awt.image.BufferedImage

/**
 * User: lrollus
 * Date: 6/12/11
 * This class is a root class for all xxxAPI class. These class allow to manage (get/create/update/delete/...) each domain instance easily durint test.
 * It encapsulate all HTTP request to have clean test
 *
 */
@Log
class DomainAPI {

    /**
     * Check if json list contains number id
     * @param id Number
     * @param list JSON list
     * @return True if id is in list, otherwise, false
     */
    static boolean containsInJSONList(Long id, def responselist) {
        log.info "Search $id in ${responselist}"
        if (responselist instanceof String) {
            responselist = JSON.parse(responselist)
        }


        def list = responselist.collection

        if (list == null) {
            list = responselist.aaData
        }

        if (list == []) return false
        boolean find = false
        list.each { item ->
            Long idItem = item.id
            if ((idItem + "").equals(id + "")) {
                find = true
            }
        }
        return find
    }

    static boolean containsStringInJSONList(String key, def list) {
        log.info "Search $key in ${list}"
        list = list.collection
        if (list == []) return false
        boolean find = false
        list.each { item ->
            String strItem = item
            if (strItem.equals(key)) {
                find = true
            }
        }
        return find
    }

    /**
     * Make undo request to cytomine server
     */
    static def undo() {
        return undo(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
    }

    static def undo(Long commandId) {
        return undo(commandId, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
    }

    static def undo(String username, String password) {
        return undo(null, username, password)
    }

    static def undo(Long commandId, String username, String password) {
        log.info("test undo")
        HttpClient client = new HttpClient()
        String URL = Infos.CYTOMINEURL + "api/" + (commandId == null ? Infos.UNDOURL : "command/$commandId/undo.json")
        client.connect(URL, username, password)
        client.get()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }

    /**
     * Make redo request to cytomine server
     */
    static def redo() {
        return redo(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
    }

    static def redo(Long commandId) {
        return redo(commandId, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
    }

    static def redo(String username, String password) {
        return redo(null, username, password)
    }

    static def redo(Long commandId, String username, String password) {
        log.info("test redo")
        HttpClient client = new HttpClient()
        String URL = Infos.CYTOMINEURL + "api/" + (commandId == null ? Infos.REDOURL : "command/$commandId/redo.json")
        client.connect(URL, username, password)
        client.get()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }


    static def doGET(String URL,String username,String password, HttpClient clientParam =null) {
        log.info("GET:"+URL)
        HttpClient client
        if(clientParam) {
            client = clientParam;
        } else {
            client = new HttpClient()
            log.info("Connect")
            client.connect(URL, username, password);
        }
        log.info("Get")
        client.printCookies();
        client.get()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code,client:client]
    }

    static def doPOST(String URL,JSONObject json,String username,String password) {
        doPOST(URL,json.toString(),username,password)
    }

    static def doPOST(String URL,String data,String username,String password) {
        log.info("POST:"+URL)
        HttpClient client = new HttpClient();
        client.connect(URL, username, password);
        client.post(data)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }

    static def doPUT(String URL,String data,String username,String password) {
        log.info("PUT:"+URL)
        HttpClient client = new HttpClient();
        client.connect(URL, username, password);
        client.put(data)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }

    static def doPUT(String URL,byte[] data,String username,String password) {
        log.info("PUT:"+URL)
        HttpClient client = new HttpClient();
        client.connect(URL, username, password);
        client.put(data)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }

    static def doDELETE(String URL,String username,String password) {
        log.info("DELETE:"+URL)
        HttpClient client = new HttpClient();
        client.connect(URL, username, password);
        client.delete()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }



    static def doPOSTUpload(String url,File file, String username,String password) throws Exception {
        return doPOSTUpload(url,file, null, username, password)
    }
    static def doPOSTUpload(String url,File file,String filename, String username,String password) throws Exception {

        MultipartEntity entity = new MultipartEntity();
        entity.addPart("files[]",new FileBody(file)) ;
        if(filename) entity.addPart("filename",new StringBody(filename)) ;
        HttpClient client = new HttpClient();
        client.connect(url, username, password);
        client.post(entity)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        return [data: response, code: code]
    }


    static def downloadImage(String URL,String username,String password) throws Exception {

        log.info("DOWNLOAD:"+URL)
        HttpClient client = new HttpClient();
        //BufferedImage image = client.readBufferedImageFromURLWithoutKey(URL, username, password)
        BufferedImage image = client.readBufferedImageFromURLWithRedirect(URL, username, password)
        return [image: image]
    }

    static def downloadFile(String URL,String username,String password) throws Exception {

        log.info("DOWNLOAD:"+URL)
        HttpClient client = new HttpClient();
        def data = client.readFileFromURLWithoutKey(URL, username, password)
        return [data: data]
    }

    static String convertSearchParameters(def parameters){
        return parameters.collect{p->
            String value
            if(p.value instanceof Date) value = ((Date)p.value).time
            else if(p.value instanceof List) {
                value = p.value.collect{URLEncoder.encode(it.toString().replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8")}.join(",")
            }
            else value = URLEncoder.encode(p.value.toString().replaceAll("%(?![0-9a-fA-F]{2})", "%25"), "UTF-8")
            return p.field + "["+p.operator+"]=" + value
        }.join("&")
    }



}
