package be.cytomine.search

import be.cytomine.image.AbstractImage

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AbstractImageAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class AbstractImageSearchTests {


    //search
    void testGetSearch(){
        AbstractImage img = BasicInstanceBuilder.getAbstractImageNotExist(true)
        img.width = 499
        img.save(flush: true)
        img = img.refresh()
        AbstractImage img2 = BasicInstanceBuilder.getAbstractImageNotExist(true)

        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size >= 2


        def searchParameters = [[operator : "lte", field : "width", value:500]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByDeletedIsNullAndWidthLessThanEquals(500)

        searchParameters = [[operator : "gte", field : "width", value:600]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.findAllByDeletedIsNullAndWidthGreaterThanEquals(600).size()

        searchParameters = [[operator : "lte", field : "width", value:Integer.MAX_VALUE]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == AbstractImage.countByDeletedIsNull()

        searchParameters = [[operator : "lte", field : "width", value:100]]

        result = AbstractImageAPI.list(0,0, searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == 0

    }

    //pagination
    void testListImagesInstanceByProject() {
        BasicInstanceBuilder.getAbstractImage()
        BasicInstanceBuilder.getAbstractImageNotExist(true)
        def result = AbstractImageAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        long size = json.size
        assert size > 1
        Long id1 = json.collection[0].id
        Long id2 = json.collection[1].id

        result = AbstractImageAPI.list(1,0, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id1

        result = AbstractImageAPI.list(1,1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.size == size
        assert json.collection.size() == 1
        assert json.collection[0].id == id2
    }

}
