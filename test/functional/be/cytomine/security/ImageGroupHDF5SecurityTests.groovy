package be.cytomine.security

import be.cytomine.image.AbstractImage

/*
* Copyright (c) 2009-2016. Authors: see NOTICE file.
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
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageGroupAPI
import be.cytomine.test.http.ImageGroupHDF5API
import be.cytomine.test.http.ProjectAPI

class ImageGroupHDF5SecurityTests extends SecurityTestsAbstract{


    void testImageGroupSecurityForProjectUser() {

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

        //Add image instance to project
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project

        //Init image group
        ImageGroup imageGroup = BasicInstanceBuilder.getImageGroupNotExist(project, false);
        ImageGroupAPI.create(imageGroup, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Init Imagegroup H5
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false);
        imageGroupHDF5.group = imageGroup
        imageGroupHDF5.filenames = "/data/28/hdf5_35398"

        //check if user 2 can access ImageGrouphdf5
        result = ImageGroupHDF5API.create(imageGroup, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        imageGroup = result.data
        assert (200 == ImageGroupHDF5API.show(image.id,SecurityTestsAbstract.USERNAME2,SecurityTestsAbstract.PASSWORD2).code)

        assert (403 == ImageGroupAPI.show(image,SecurityTestsAbstract.USERNAME3,SecurityTestsAbstract.PASSWORD3).code)
    }



}
