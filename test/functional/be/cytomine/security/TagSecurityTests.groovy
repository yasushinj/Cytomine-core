package be.cytomine.security

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
import be.cytomine.test.http.*

class TagSecurityTests extends SecurityTestsAbstract {

    void testShowTag() {
        //Every one can see a tag
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist(true)
        def result = TagAPI.show(tag.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
        getGuest1()
        result = TagAPI.show(tag.id, GUEST1,GPASSWORD1)
        assert 200 == result.code
    }
    void testListTag() {
        getUser1()
        def result = TagAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        long size = result.data.size
        result = TagAPI.list(USERNAME1, PASSWORD1)
        assert 200 == result.code
        assert size == result.data.size
        getGuest1()
        result = TagAPI.list(GUEST1,GPASSWORD1)
        assert 200 == result.code
        assert size == result.data.size
    }
    void testAddTag() {
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        getUser1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code

        getGuest1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), GUEST1,GPASSWORD1)
        assert 401 == result.code
    }
    void testUpdateTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag.name = "NEW"
        result = TagAPI.update(tag.id, tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithNoAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag.name = "NEW"
        result = TagAPI.update(tag.id, tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code

        //add association by another than user1
        //user2 create project and associate a tag

        tag.name = "NEW"
        result = TagAPI.update(tag.id, tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 401 == result.code
    }

    void testDeleteTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        result = TagAPI.delete(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithNoAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code

        //add association by another than user1
        //user2 create project and associate a tag

        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 401 == result.code
    }

//tag association
    void testShowTagDomainAssociation() {
        // can see only mine
        //add one by me
        //one by another on another object
        //check i can see mine and not the other
        assert false
    }
    void testListTagDomainAssociation() {
        //list by domain that i have no authorization and check the 401
        assert false
    }
    void testListTagDomainAssociationByTag() {
        //...
        //url is /api/tag_association.json ? SEARCH REST URL with domain class + tag id
        //check i can see mine and not the other
        assert false
    }
    void testAddTagDomainAssociation() {
        // check by domain permission
        assert false
    }
    void testDeleteTagDomainAssociationByAdmin() {
        // works
        assert false
    }

    void testDeleteTagDomainAssociationByTagCreator() {
        // doesn't work
        assert false
    }

    void testDeleteTagDomainAssociationByDomainGranted() {
        // works
        assert false
    }
}
