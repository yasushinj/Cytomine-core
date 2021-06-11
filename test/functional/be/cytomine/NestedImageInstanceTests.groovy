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

import be.cytomine.image.NestedImageInstance
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.NestedImageInstanceAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 18/05/11
 * Time: 9:11
 * To change this template use File | Settings | File Templates.
 */
class NestedImageInstanceTests {

    void testGetNestedImageInstanceWithCredential() {
        def nested = BasicInstanceBuilder.getNestedImageInstance()
        def result = NestedImageInstanceAPI.show(nested.id,nested.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.id.toString()== nested.id.toString()

        result = ImageInstanceAPI.show(nested.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.id.toString()== nested.id.toString()
    }

    void testListNestedImagesInstanceByImage() {
        def nested = BasicInstanceBuilder.getNestedImageInstance()
        def result = NestedImageInstanceAPI.listByImageInstance(nested.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert NestedImageInstanceAPI.containsInJSONList(nested.id,json)

        result = NestedImageInstanceAPI.listByImageInstance(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testListImageCheckIfNoNestedImageInstance() {
        def nested = BasicInstanceBuilder.getNestedImageInstanceNotExist( BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true),true),true)

        def result = ImageInstanceAPI.listByProject(nested.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        assert NestedImageInstanceAPI.containsInJSONList(nested.parent.id,json)
        assert !NestedImageInstanceAPI.containsInJSONList(nested.id,json)

        result = ImageInstanceAPI.listByProject(nested.project.id,2,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert !NestedImageInstanceAPI.containsInJSONList(nested.parent.id,json)
        assert !NestedImageInstanceAPI.containsInJSONList(nested.id,json)

        result = ImageInstanceAPI.listByProject(nested.project.id, 1,0,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert NestedImageInstanceAPI.containsInJSONList(nested.parent.id,json)
        assert !NestedImageInstanceAPI.containsInJSONList(nested.id,json)

        result = ImageInstanceAPI.listLightByUser(nested.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert NestedImageInstanceAPI.containsInJSONList(nested.parent.id,json)
        assert !NestedImageInstanceAPI.containsInJSONList(nested.id,json)

    }


    void testAddNestedImageInstanceCorrect() {
        def nested = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        println nested.encodeAsJSON()
        def result = NestedImageInstanceAPI.create(nested.parent.id,nested.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        NestedImageInstance image = result.data
        Long idImage = image.id

        result = NestedImageInstanceAPI.show(image.id,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = NestedImageInstanceAPI.undo()
        assert 200 == result.code

        result = NestedImageInstanceAPI.show(idImage,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = NestedImageInstanceAPI.redo()
        assert 200 == result.code

        result = NestedImageInstanceAPI.show(idImage,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

    }


    void testAddNestedImageInstanceAlreadyExist() {
        def imageToAdd = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        def result = NestedImageInstanceAPI.create(imageToAdd.parent.id,imageToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        result = NestedImageInstanceAPI.create(imageToAdd.parent.id,imageToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testaddNestedImageInstanceWithUnexistingAbstractImage() {
        def imageToAdd = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        String jsonImage = imageToAdd.encodeAsJSON()
        def updateImage = JSON.parse(jsonImage)
        updateImage.baseImage = -99
        jsonImage = updateImage.toString()
        def result = NestedImageInstanceAPI.create(imageToAdd.parent.id,jsonImage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testaddNestedImageInstanceWithUnexistingImageInstance() {
        def imageToAdd = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        String jsonImage = imageToAdd.encodeAsJSON()
        def updateImage = JSON.parse(jsonImage)
        updateImage.parent = -99
        jsonImage = updateImage.toString()
        def result = NestedImageInstanceAPI.create(imageToAdd.parent.id,jsonImage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }



    void testaddNestedImageInstanceWithUnexistingProject() {
        def imageToAdd = BasicInstanceBuilder.getNestedImageInstance()
        String jsonImage = imageToAdd.encodeAsJSON()
        def updateImage = JSON.parse(jsonImage)
        updateImage.project = -99
        jsonImage = updateImage.toString()
        def result = NestedImageInstanceAPI.create(imageToAdd.parent.id,jsonImage, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testEditNestedImageInstance() {

        def image = BasicInstanceBuilder.getNestedImageInstance()
        def data = UpdateData.createUpdateSet(image,[project: [BasicInstanceBuilder.getProject(),BasicInstanceBuilder.getProjectNotExist(true)]])

        def result = NestedImageInstanceAPI.update(image.id,image.parent.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idNestedImageInstance = json.nestedimageinstance.id
        def showResult = NestedImageInstanceAPI.show(idNestedImageInstance,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = NestedImageInstanceAPI.undo()
        assert 200==showResult.code

        showResult = NestedImageInstanceAPI.show(idNestedImageInstance,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)

        BasicInstanceBuilder.compare(data.mapOld, json)

        showResult = NestedImageInstanceAPI.redo()
        assert 200==showResult.code

        showResult = NestedImageInstanceAPI.show(idNestedImageInstance,image.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testDeleteNestedImageInstance() {
        def NestedImageInstanceToDelete = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        assert NestedImageInstanceToDelete.save(flush: true) != null
        def idImage = NestedImageInstanceToDelete.id

        def result = NestedImageInstanceAPI.delete(NestedImageInstanceToDelete.id,NestedImageInstanceToDelete.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = NestedImageInstanceAPI.show(NestedImageInstanceToDelete.id,NestedImageInstanceToDelete.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = NestedImageInstanceAPI.undo()
        assert 200 == result.code

        result = NestedImageInstanceAPI.show(idImage,NestedImageInstanceToDelete.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = NestedImageInstanceAPI.redo()
        assert 200 == result.code

        result = NestedImageInstanceAPI.show(idImage,NestedImageInstanceToDelete.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteNestedImageInstanceNoExist() {
        def NestedImageInstanceToDelete = BasicInstanceBuilder.getNestedImageInstanceNotExist()
        def result = NestedImageInstanceAPI.delete(NestedImageInstanceToDelete.id,NestedImageInstanceToDelete.parent.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


}
