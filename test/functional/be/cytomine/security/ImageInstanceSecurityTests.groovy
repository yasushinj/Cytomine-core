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

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import grails.converters.JSON

class ImageInstanceSecurityTests extends SecurityTestsAbstract{


    void testImageInstanceSecurityForCytomineAdmin() {

        //Get user1
        User user1 = getUser1()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        //check if admin user can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        image = result.data
        assert (200 == ImageInstanceAPI.show(image.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
        result = ImageInstanceAPI.listByProject(project.id,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        assert (true ==ImageInstanceAPI.containsInJSONList(image.id,JSON.parse(result.data)))
        assert (200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert result.code == 200 || result.code == 500

        assert (200 == ImageInstanceAPI.delete(image,SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN).code)
    }

    void testImageInstanceSecurityForProjectAdmin() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addAdminProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        //check if user 2 can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        image = result.data
        assert (200 == ImageInstanceAPI.show(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        result = ImageInstanceAPI.listByProject(project.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true ==ImageInstanceAPI.containsInJSONList(image.id,JSON.parse(result.data)))
        //assert (200 == ImageInstanceAPI.update(image,USERNAME2,PASSWORD2).code)

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAMEADMIN,SecurityTestsAbstract.PASSWORDADMIN)
        assert result.code == 200 || result.code == 500

        assert (200 == ImageInstanceAPI.delete(image,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
    }

    void testImageInstanceSecurityForProjectUser() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()
        User user3 = getUser3()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project.id,user3.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        //check if user 2 can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        image = result.data
        assert (200 == ImageInstanceAPI.show(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        result = ImageInstanceAPI.listByProject(project.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true ==ImageInstanceAPI.containsInJSONList(image.id,JSON.parse(result.data)))
        //assert (200 == ImageInstanceAPI.update(image,USERNAME2,PASSWORD2).code)

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)
        assert (200 == ImageInstanceAPI.delete(image,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)
        assert (403 == ImageInstanceAPI.delete(image,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code)
    }

    void testImageInstanceSecurityForSimpleUser() {

        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        //check if simple  user can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert (403 == result.code)
        image = result.data

        image = BasicInstanceBuilder.getImageInstance()
        image.project = project
        image.save(flush:true)

        assert (403 == ImageInstanceAPI.show(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        assert (403 ==ImageInstanceAPI.listByProject(project.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
        //assert (403 == ImageInstanceAPI.update(image,USERNAME2,PASSWORD2).code)

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert result.code == 403

        assert (403 == ImageInstanceAPI.delete(image,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)
    }


    void testImageInstanceDownloadSecurityForProjectUser() {
        //Get user1
        User user1 = getUser1()
        User user2 = getUser2()
        User user3 = getUser3()

        //Get admin user
        User admin = getUserAdmin()

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project.id,user3.id,SecurityTestsAbstract.USERNAME1,SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        //check if user 2 can access/update/delete
        result = ImageInstanceAPI.create(image.encodeAsJSON(),SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        image = result.data

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert result.code == 403

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert result.code == 403

        project.areImagesDownloadable = true
        BasicInstanceBuilder.saveDomain(project)

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2)
        assert result.code == 200 || result.code == 500

        result = ImageInstanceAPI.download(image.id, SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3)
        assert result.code == 200 || result.code == 500
    }
}
