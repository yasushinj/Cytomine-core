package be.cytomine.security

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
import be.cytomine.image.ImageInstance
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractImageAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class AbstractImageSecurityTests extends SecurityTestsAbstract{

  void testAbstractImageSecurityForCytomineAdmin() {
      //Get user1
      User user1 = getUser1()

      //Get admin user
      User admin = getUserAdmin()

      Storage storage = BasicInstanceBuilder.getStorageNotExist(true)

      Infos.addUserRight(user1.username,storage)

      //Add an image
      AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(true)
      AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(true)

      //Add to storage
      StorageAbstractImage saa = new StorageAbstractImage(storage: storage,abstractImage: image1)
      BasicInstanceBuilder.saveDomain(saa)
      saa = new StorageAbstractImage(storage: storage,abstractImage: image2)
      BasicInstanceBuilder.saveDomain(saa)

      //Create project
      def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
      assert 200 == result.code
      Project project = result.data

      //Add image instance to project
      ImageInstance image = new ImageInstance(project:project,baseImage: image1, user: user1)
      println image
      //check if user 2 can access/update/delete
      result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code

      println "project=${project.id}"
      println "baseImage=${image1.id}"

      result = AbstractImageAPI.list(SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))

      assert 200 == AbstractImageAPI.show(image1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
      assert 200 == AbstractImageAPI.create(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
//      assert 200 == AbstractImageAPI.delete(image1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code
  }

    void testAbstractImageSecurityForCytomineUser() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser("testListACLUser",PASSWORD1)

        //Get admin user
        User admin = getUserAdmin()

        Storage storage = BasicInstanceBuilder.getStorageNotExist(true)
        Infos.addUserRight(user1,storage)
        Storage storageForbiden = BasicInstanceBuilder.getStorageNotExist(true)
        //don't add acl to this storage

        //Add an image
        AbstractImage image1 = BasicInstanceBuilder.getAbstractImageNotExist(true)
        AbstractImage image2 = BasicInstanceBuilder.getAbstractImageNotExist(true)
        // getAbstractImageNotExist create a storage & a storage_abstract_image
        // => Replace the storage in image2 by the storage2 (& the link storage_abstract_image)
        StorageAbstractImage.findByAbstractImage(image1).delete(flush: true)
        StorageAbstractImage.findByAbstractImage(image2).delete(flush: true)

        //Add to storage
        StorageAbstractImage saa = new StorageAbstractImage(storage: storage,abstractImage: image1)
        BasicInstanceBuilder.saveDomain(saa)
        //img 2 should not be available
        saa = new StorageAbstractImage(storage: storageForbiden,abstractImage: image2)
        BasicInstanceBuilder.saveDomain(saa)

        //Create project
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),user1.username,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        result = AbstractImageAPI.list(user1.username, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert (true ==AbstractImageAPI.containsInJSONList(image1.id,json))

        assert 200 == AbstractImageAPI.show(image1.id,user1.username,SecurityTestsAbstract.PASSWORD1).code
        json = JSON.parse(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON())
        json.storage = storage.id
        assert 200 == AbstractImageAPI.create(json.toString(), user1.username,SecurityTestsAbstract.PASSWORD1).code
        assert 200 == AbstractImageAPI.delete(image1.id,user1.username,SecurityTestsAbstract.PASSWORD1).code

        result = AbstractImageAPI.list(user1.username, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (false ==AbstractImageAPI.containsInJSONList(image2.id,json))
        assert 403 == AbstractImageAPI.show(image2.id,user1.username,SecurityTestsAbstract.PASSWORD1).code
        json = JSON.parse(BasicInstanceBuilder.getAbstractImageNotExist().encodeAsJSON())
        json.storage = storageForbiden.id
        assert 403 == AbstractImageAPI.create(json.toString(), user1.username,SecurityTestsAbstract.PASSWORD1).code
        assert 403 == AbstractImageAPI.delete(image2.id,user1.username,SecurityTestsAbstract.PASSWORD1).code
    }


}
