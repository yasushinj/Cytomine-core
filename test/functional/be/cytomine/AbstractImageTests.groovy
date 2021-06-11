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

import be.cytomine.image.AbstractImage
import be.cytomine.image.server.Storage
import be.cytomine.meta.Property
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractImageAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class AbstractImageTests {

    void testListImages() {
        Storage storage = BasicInstanceBuilder.getStorage()
        BasicInstanceBuilder.getAbstractImage()
        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListImagesWithInProjectInfo() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        BasicInstanceBuilder.getAbstractImageNotExist(true)
        def result = AbstractImageAPI.list(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert json.collection.collect{it.inProject}.contains(true)
        assert json.collection.collect{it.inProject}.contains(false)
    }

    void testListImagesWithoutCredential() {
        BasicInstanceBuilder.getAbstractImage()
        def result = AbstractImageAPI.list(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testListByProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        def result = AbstractImageAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1
    }

    void testListByProjectNoExistWithCredential() {
        def result = AbstractImageAPI.listByProject(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testGetImageWithCredential() {
        AbstractImage image = BasicInstanceBuilder.getAbstractImage()
        def result = AbstractImageAPI.show(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }




    void testGetImageProperties() {
        AbstractImage image = BasicInstanceBuilder.getAbstractImage()
        if(!Property.findByDomainIdent(image.id)) {
            Property imageProperty = new Property(key: "key1", value: "value1",domainIdent:image.id, domainClassName:image.class.name)
            BasicInstanceBuilder.saveDomain(imageProperty)
        }
        def result = AbstractImageAPI.getProperty(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()>0
    }

    void testGetImageServers() {
        AbstractImage image = BasicInstanceBuilder.getAbstractImage()
        def result = AbstractImageAPI.getInfo(image.id,"imageservers",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddImageCorrect() {
        def imageToAdd = BasicInstanceBuilder.getAbstractImageNotExist()
        String json = imageToAdd.encodeAsJSON()
        println "encodeAsJSON="+json
        def result = AbstractImageAPI.create(json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int id = result.data.id
//      result = AbstractImageAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//      assert 200 == result.code
    }

    void testaddImageWithUnexistingScanner() {
        def imageToAdd = BasicInstanceBuilder.getAbstractImageNotExist()
        def json = JSON.parse((String)imageToAdd.encodeAsJSON())
        json.scanner = -99
        def result = AbstractImageAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testaddImageWithUnexistingSlide() {
        def imageToAdd = BasicInstanceBuilder.getAbstractImageNotExist()
        def json = JSON.parse((String)imageToAdd.encodeAsJSON())
        json.sample = -99
        def result = AbstractImageAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testaddImageWithUnexistingMime() {
        def imageToAdd = BasicInstanceBuilder.getAbstractImageNotExist()
        def json = JSON.parse((String)imageToAdd.encodeAsJSON())
        json.mime = -99
        def result = AbstractImageAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testaddImageWithUnexistingImageServer() {
        def mime = BasicInstanceBuilder.getMimeNotExist()
        def imageToAdd = BasicInstanceBuilder.getAbstractImageNotExist()
        def json = JSON.parse((String)imageToAdd.encodeAsJSON())
        json.mime = mime.id
        def result = AbstractImageAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testEditImage() {
        def image = BasicInstanceBuilder.getAbstractImageNotExist(true)
        def data = UpdateData.createUpdateSet(
                image,
                [height:[10000,900000],
                 width:[1000,9000],
                 path:["OLDPATH","NEWPATH"],
                 scanner:[BasicInstanceBuilder.getScanner(),BasicInstanceBuilder.getNewScannerNotExist(true)],
                 filename: ["OLDNAME","NEWNAME"]]
        )

        def result = AbstractImageAPI.update(image.id,data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int id = json.abstractimage.id
        def showResult = AbstractImageAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testEditMagnification() {

        def ai = BasicInstanceBuilder.getAbstractImageNotExist(true)
        def ii = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProject(), false)
        ii.baseImage = ai
        ii.save(true)

        assert ii.resolution == ai.resolution
        assert ii.magnification == ai.magnification

        def updatedImage = JSON.parse((String)ai.encodeAsJSON())
        updatedImage.resolution = 2.5d
        updatedImage.magnification = 20
        def result = AbstractImageAPI.update(ai.id, updatedImage.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.abstractimage.resolution == 2.5
        assert json.abstractimage.magnification == 20
        updatedImage = json.abstractimage

        result = ImageInstanceAPI.show(ii.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.resolution == 2.5
        assert json.magnification == 20

        json.magnification = 40

        result = ImageInstanceAPI.update(ii.id, json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.imageinstance.resolution == 2.5
        assert json.imageinstance.magnification == 40

        updatedImage.resolution = 6
        updatedImage.magnification = 10
        result = AbstractImageAPI.update(ai.id, updatedImage.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.abstractimage.resolution == 6
        assert json.abstractimage.magnification == 10


        result = ImageInstanceAPI.show(ii.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        assert json.resolution == 6
        assert json.magnification == 40
    }


    void testDeleteImage()  {
        def imageToDelete = BasicInstanceBuilder.getAbstractImage()
        Long id = imageToDelete.id
        def result = AbstractImageAPI.delete(id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        println "testDeleteImage=" +result
        assert 200 == result.code
        result = AbstractImageAPI.show(id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.deleted != null
        assert json.deleted != ""
    }

    void testDeleteImageWithData()  {
        def imageToDelete = BasicInstanceBuilder.getImageInstance()
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        annotation.image = imageToDelete
        annotation.save(flush:true)
        Long id = imageToDelete.baseImage.id
        def result = AbstractImageAPI.delete(id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 403 == result.code
    }

    void testDeleteImageNoExist()  {
        def result = AbstractImageAPI.delete(-99,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}