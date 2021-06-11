package be.cytomine

import be.cytomine.image.ImageInstance

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

import be.cytomine.image.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.UploadedFileAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class UploadedFileTests {

    void testListUploadedFile() {
        def result = UploadedFileAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListUploadedFileRootOrFromParentOrHierarchical() {
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        def result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUploadedFile = result.data.id

        result = UploadedFileAPI.show(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        UploadedFile uploadedfileChildToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd.parent = UploadedFile.get(idUploadedFile)

        result = UploadedFileAPI.create(uploadedfileChildToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUploadedFileChild = result.data.id

        result = UploadedFileAPI.show(idUploadedFileChild, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        UploadedFile uploadedfile3 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfile3.parent = UploadedFile.get(idUploadedFileChild)

        result = UploadedFileAPI.create(uploadedfile3.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUploadedFile3 = result.data.id

        result = UploadedFileAPI.show(idUploadedFile3, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UploadedFileAPI.listOnlyRoots(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        def root = json.collection.find { it.id == idUploadedFile}
        def child = json.collection.find { it.id == idUploadedFileChild}
        def third = json.collection.find { it.id == idUploadedFile3}

        assert root != null
        assert child == null
        assert third == null


        result = UploadedFileAPI.listChilds(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        root = json.collection.find { it.id == idUploadedFile}
        child = json.collection.find { it.id == idUploadedFileChild}
        third = json.collection.find { it.id == idUploadedFile3}

        assert child != null
        assert root == null
        assert third == null


        result = UploadedFileAPI.hierarchicalList(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        root = json.collection.find { it.id == idUploadedFile}
        child = json.collection.find { it.id == idUploadedFileChild}
        third = json.collection.find { it.id == idUploadedFile3}

        assert child != null
        assert root != null
        assert third != null
    }

    void testSearchUploadedFile() {
        String name = "test"
        def result = UploadedFileAPI.searchWithName(name, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int previousSize = json.collection.size()

        UploadedFile uf = BasicInstanceBuilder.getUploadedFileNotExist()
        uf.originalFilename = "test"
        uf.save(true)
        uf = BasicInstanceBuilder.getUploadedFileNotExist()
        uf.originalFilename = "random"
        uf.save(true)

        result = UploadedFileAPI.searchWithName(name, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == previousSize + 1
    }

    void testShowUploadedFileWithCredential() {
        def result = UploadedFileAPI.show(BasicInstanceBuilder.getUploadedFile().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

  void testAddUploadedFileCorrect() {
      def uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
      def result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      int idUploadedFile = result.data.id

      result = UploadedFileAPI.show(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
  }


  void testUpdateUploadedFileCorrect() {
      def uploadedfile = BasicInstanceBuilder.getUploadedFile()
      def data = UpdateData.createUpdateSet(uploadedfile,[status: [0,4]])
      def result = UploadedFileAPI.update(uploadedfile.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json instanceof JSONObject
      int idUploadedFile = json.uploadedfile.id

      def showResult = UploadedFileAPI.show(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      json = JSON.parse(showResult.data)
      BasicInstanceBuilder.compare(data.mapNew, json)
  }

    void testDeleteUploadedFile() {
        def uploadedfileToDelete = BasicInstanceBuilder.getUploadedFileNotExist()
        assert uploadedfileToDelete.save(flush: true)!= null
        def id = uploadedfileToDelete.id
        def result = UploadedFileAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = UploadedFileAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }

    void testDeleteUploadedFileWithImageInProject() {
        def uploadedfileToDelete = BasicInstanceBuilder.getUploadedFileNotExist()
        ImageInstance img = BasicInstanceBuilder.getImageInstanceNotExist(BasicInstanceBuilder.getProjectNotExist(true),true)
        uploadedfileToDelete.image = img.baseImage;
        assert uploadedfileToDelete.save(flush: true)!= null
        def id = uploadedfileToDelete.id
        def result = UploadedFileAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 403 == result.code
    }

    void testDeleteUploadedFileNotExist() {
      def result = UploadedFileAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 404 == result.code
  }

    void testLTree() {
        UploadedFile uploadedfileToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        def result = UploadedFileAPI.create(uploadedfileToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFile = result.data.id

        UploadedFile uploadedfileChildToAdd = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfileChildToAdd.parent = UploadedFile.get(idUploadedFile)
        result = UploadedFileAPI.create(uploadedfileChildToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFileChild = result.data.id

        UploadedFile uploadedfile3 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfile3.parent = UploadedFile.get(idUploadedFileChild)
        result = UploadedFileAPI.create(uploadedfile3.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFile3 = result.data.id

        UploadedFile uploadedfile4 = BasicInstanceBuilder.getUploadedFileNotExist()
        uploadedfile4.parent = UploadedFile.get(idUploadedFile3)
        result = UploadedFileAPI.create(uploadedfile4.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        int idUploadedFile4 = result.data.id

        result = UploadedFileAPI.show(idUploadedFile, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        def json = JSON.parse(result.data)
        UploadedFile uf1 = UploadedFile.get(json.id)

        result = UploadedFileAPI.show(idUploadedFileChild, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        UploadedFile uf2 = UploadedFile.get(json.id)

        result = UploadedFileAPI.show(idUploadedFile3, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        UploadedFile uf3 = UploadedFile.get(json.id)

        result = UploadedFileAPI.show(idUploadedFile4, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        UploadedFile uf4 = UploadedFile.get(json.id)

        assert uf4.lTree.contains(uf3.lTree)
        assert uf3.lTree.contains(uf2.lTree)
        assert uf2.lTree.contains(uf1.lTree)
        assert uf4.parent.id == uf3.id

        //delete uf3
        result = UploadedFileAPI.delete(uf3.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UploadedFileAPI.show(idUploadedFile3, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UploadedFileAPI.show(idUploadedFile4, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)

        assert json.parent == uf2.id
    }

//   void testUploadFileWorkflow() {
//       def oneAnotherImage = BasicInstanceBuilder.initImage()
//       UploadedFile uploadedFile = new UploadedFile()
//       uploadedFile.originalFilename = "test.tif"
//       uploadedFile.filename = "/data/test.cytomine.be/1/1383567901007/test.tif"
//       uploadedFile.path = "/tmp/imageserver_buffer"
//       uploadedFile.size = 243464757l
//       uploadedFile.ext = "tif"
//       uploadedFile.contentType  = "image/tiff"
//       uploadedFile.storages = new Long[1]
//       uploadedFile.storages[0] = Storage.findByName("lrollus test storage").id
//       uploadedFile.projects = new Long[1]
//       uploadedFile.projects[0] = oneAnotherImage.project.id
//       uploadedFile.user = oneAnotherImage.user
//       uploadedFile.status = UploadedFile.TO_DEPLOY
//       uploadedFile.mimeType = "image/tiff"
//       BasicInstanceBuilder.saveDomain(uploadedFile)
//
//       def result = UploadedFileAPI.createImage(uploadedFile.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//       assert 200 == result.code
//       def image = AbstractImage.findByFilename("/data/test.cytomine.be/1/1383567901007/test.tif")
//       assert image
//
//
//       result = UploadedFileAPI.clearAbstractImageProperties(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//       assert 200 == result.code
//       result = UploadedFileAPI.populateAbstractImageProperties(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//       assert 200 == result.code
//       result = UploadedFileAPI.extractUsefulAbstractImageProperties(image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//       assert 200 == result.code
//
//       uploadedFile.refresh()
//
//       assert uploadedFile.image
//       assert uploadedFile.image.id == image.id
//
//   }

/*
    void testUploadFileWorkflowForGhest() {
        User ghest = BasicInstanceBuilder.getGhest("GHESTUPLOAD","PASSWORD")
        def oneAnotherImage = BasicInstanceBuilder.initImage()
        UploadedFile uploadedFile = new UploadedFile()
        uploadedFile.originalFilename = "test.tif"
        uploadedFile.filename = "/data/test.cytomine.be/1/1383567901007/test.tif"
        uploadedFile.path = "/tmp/imageserver_buffer"
        uploadedFile.size = 243464757l
        uploadedFile.ext = "tif"
        uploadedFile.contentType  = "image/tiff"
        uploadedFile.storages = new Long[1]
        uploadedFile.storages[0] = Storage.findByUser(User.findByUsername(Infos.SUPERADMINLOGIN)).id
        uploadedFile.projects = new Long[1]
        uploadedFile.projects[0] = oneAnotherImage.project.id
        uploadedFile.user = oneAnotherImage.user
        uploadedFile.status = UploadedFile.TO_DEPLOY
        BasicInstanceBuilder.saveDomain(uploadedFile)

        def result = UploadedFileAPI.createImage(uploadedFile.id,"GHESTUPLOAD", "PASSWORD")
        assert 403 == result.code

    }

*/


}
