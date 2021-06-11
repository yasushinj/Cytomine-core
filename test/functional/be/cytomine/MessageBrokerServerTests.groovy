package be.cytomine

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

import be.cytomine.middleware.MessageBrokerServer
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.MessageBrokerServerAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by julien 
 * Date : 06/02/15
 * Time : 10:06
 */
class MessageBrokerServerTests {

    void testListMessageBrokerServerWithCredentials() {
        def result = MessageBrokerServerAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, null)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        MessageBrokerServer messageBrokerServer = BasicInstanceBuilder.getMessageBrokerServerNotExist(true)
        result = MessageBrokerServerAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, messageBrokerServer.name)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ((JSONArray)json.collection).size() >= 1
        assert MessageBrokerServerAPI.containsInJSONList(messageBrokerServer.id, json)

        //List from a substring of the name
        result = MessageBrokerServerAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, messageBrokerServer.name[2..messageBrokerServer.name.length()-2])
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert ((JSONArray)json.collection).size() >= 1
        assert MessageBrokerServerAPI.containsInJSONList(messageBrokerServer.id, json)
    }

    void testShowMessageBrokerServerWithCredentials() {
        MessageBrokerServer messageBrokerServer = BasicInstanceBuilder.getMessageBrokerServer()
        def result = MessageBrokerServerAPI.show(messageBrokerServer.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddMessageBrokerServerCorrect() {
        def messageBrokerServerToAdd = BasicInstanceBuilder.getMessageBrokerServerNotExist()
        def result = MessageBrokerServerAPI.create(messageBrokerServerToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idMessageBrokerServer = result.data.id

        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = MessageBrokerServerAPI.undo()
        assert 200 == result.code

        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = MessageBrokerServerAPI.redo()
        assert 200 == result.code

        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddMessageBrokerServerAlreadyExist() {
        def messageBrokerServerToAdd = BasicInstanceBuilder.getMessageBrokerServer()
        def result = MessageBrokerServerAPI.create(messageBrokerServerToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testUpdateMessageBrokerServerCorrect() {

        MessageBrokerServer messageBrokerServer = BasicInstanceBuilder.getMessageBrokerServerNotExist(true)
        def data = UpdateData.createUpdateSet(messageBrokerServer, [name: [messageBrokerServer.name,"NEWNAME"]])
        def result = MessageBrokerServerAPI.update(messageBrokerServer.id, data.postData, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idMessageBrokerServer = json.messagebrokerserver.id

        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        result = MessageBrokerServerAPI.undo()
        assert 200 == result.code
        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

        result = MessageBrokerServerAPI.redo()
        assert 200 == result.code
        result = MessageBrokerServerAPI.show(idMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))
    }

    void testUpdateMessageBrokerServerNotExist() {
        MessageBrokerServer messageBrokerServerWithOldName = BasicInstanceBuilder.getMessageBrokerServer()
        MessageBrokerServer messageBrokerServerWithNewName = BasicInstanceBuilder.getMessageBrokerServerNotExist()
        messageBrokerServerWithNewName.save(flush: true)
        MessageBrokerServer messageBrokerServerToEdit = MessageBrokerServer.get(messageBrokerServerWithNewName.id)
        def jsonMessageBrokerServer = messageBrokerServerToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonMessageBrokerServer)
        jsonUpdate.name = messageBrokerServerWithOldName.name
        jsonUpdate.id = -99
        jsonMessageBrokerServer = jsonUpdate.toString()
        def result = MessageBrokerServerAPI.update(-99, jsonMessageBrokerServer, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteMessageBrokerServer() {
        def messageBrokerServerToDelete = BasicInstanceBuilder.getMessageBrokerServerNotExist()
        assert messageBrokerServerToDelete.save(flush: true)!= null
        def id = messageBrokerServerToDelete.id
        def result = MessageBrokerServerAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = MessageBrokerServerAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = MessageBrokerServerAPI.undo()
        assert 200 == result.code

        result = MessageBrokerServerAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = MessageBrokerServerAPI.redo()
        assert 200 == result.code

        result = MessageBrokerServerAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteMessageBrokerServerNotExist() {
        def result = MessageBrokerServerAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
