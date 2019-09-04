package be.cytomine

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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
import be.cytomine.test.http.TagAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TagTests {

    //Test Tags
    void testShowTag() {
        def tag = BasicInstanceBuilder.getTag()
        def result = TagAPI.show(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.id == tag.id
    }
    void testListTag() {
        def result = TagAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }
    void testAddTag() {
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.id == tag.id
        result = TagAPI.show(json.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testAddTagSameName() {
        def tagOrigin = BasicInstanceBuilder.getTag()
        def tag = BasicInstanceBuilder.getTagNotExist()
        tag.name = tagOrigin.name
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code
    }
    void testUpdateTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        tag.name = "NEW"
        def result = TagAPI.update(tag.id, tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        result = TagAPI.show(json.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.name == "NEW"
    }
    void testDeleteTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        //create associations then verify than > 0

        def result = TagAPI.delete(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.id == tag.id
        result = TagAPI.show(json.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 500 == result.code

        //check than 0 association to the associated object
    }



    //Test Tag Associations
    void testListTagByDomain() {
        //TagAPI.listByDomain()
        //...
        assert false
    }
    void testListTagDomainAssociationByTag() {
        //...
        //url is /api/tag_association.json ? SEARCH REST URL with domain class + tag id
        assert false
    }
    void testAddTagDomainAssociation() {
        //url is /api/domain/domainID/tag.json
        //...
        assert false
    }
    void testAddSameTagDomainAssociation() {
        //take 2 differents users o associate the same tag to same domain
        //...
        assert false
    }
    void testDeleteTagDomainAssociation() {
        //url is /api/domain/domainID/tag.json
        //...
        assert false
    }
}
