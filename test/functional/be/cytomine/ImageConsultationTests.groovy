package be.cytomine

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageConsultationAPI
import be.cytomine.test.http.ImageInstanceAPI
import grails.converters.JSON

class ImageConsultationTests {


    void testAddConsultation() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{imageinstance:${image.id},mode:test}")

        def result = ImageConsultationAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert "test" == json.mode
        assert image.id == json.image

         //same re-opening image
        json = JSON.parse("{imageinstance:${image.id},mode:test}")
        result = ImageConsultationAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


    void testLastImageOfUsersByProject() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{imageinstance:${image.id},mode:test}}")

        def result = ImageConsultationAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        // project where image is located
        def project = BasicInstanceBuilder.getProject();
        result = ImageConsultationAPI.lastImageOfUsersByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

    }
    void testGetLastOpenedImage() {
        def image = BasicInstanceBuilder.getImageInstance()
        def json = JSON.parse("{imageinstance:${image.id},mode:test}}")

        def result = ImageConsultationAPI.create(image.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ImageInstanceAPI.listLastOpened(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert ImageInstanceAPI.containsInJSONList(image.id,json)

    }
}
