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

import be.cytomine.meta.Tag
import be.cytomine.meta.TagDomainAssociation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import be.cytomine.utils.UpdateData
import grails.converters.JSON


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
        def json = JSON.parse(result.data)
        long size = json.size
        result = TagAPI.list(USERNAME1, PASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.size
        getGuest1()
        result = TagAPI.list(GUEST1,GPASSWORD1)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert size == json.size
    }
    void testAddTag() {
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        getUser1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        assert tag.user.id == getUser1().id

        getGuest1()
        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), GUEST1,GPASSWORD1)
        assert 403 == result.code
    }
    void testUpdateTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithNoAssociation() {
        getUser1()
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testUpdateTagByCreatorWithAssociation() {
        getUser1()
        Tag tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data

        def project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = project
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME1, PASSWORD1)

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)

        def data = UpdateData.createUpdateSet(tag,[name: [tag.name, BasicInstanceBuilder.getRandomString()]])
        result = TagAPI.update(tag.id, data.postData, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }

    void testDeleteTagByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        result = TagAPI.delete(tag.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithNoAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data
        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }

    void testDeleteTagByCreatorWithAssociation() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert 200 == result.code
        tag = result.data

        def project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        def association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = project
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME1, PASSWORD1)

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)

        result = TagAPI.delete(tag.id, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }

//tag association
    void testShowTagDomainAssociation() {

        def tag = BasicInstanceBuilder.getTagNotExist(true)

        getUser1()
        def project = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        association1 = result.data

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)

        TagDomainAssociation association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = tag
        association2.domain = annot
        result = TagDomainAssociationAPI.create(association2.encodeAsJSON(), association2.domainClassName, association2.domainIdent, USERNAME2, PASSWORD2)
        association2 = result.data

        result = TagDomainAssociationAPI.show(association1.id, USERNAME1, PASSWORD1)
        assert result.code == 200
        result = TagDomainAssociationAPI.show(association2.id, USERNAME1, PASSWORD1)
        assert result.code == 403
    }

    void testListTagDomainAssociationByDomain() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)

        getUser1()
        def project = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 200

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        TagDomainAssociation association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = annot
        result = TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200

        tag = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag.encodeAsJSON(), USERNAME2, PASSWORD2)
        tag = result.data
        Infos.addUserRight(getUser2(), project)
        association = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association.tag = tag
        association.domain = project
        result = TagDomainAssociationAPI.create(association.encodeAsJSON(), association.domainClassName, association.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200


        result = TagDomainAssociationAPI.listByDomain(project, USERNAME1, PASSWORD1)
        assert result.code == 200
        def json = JSON.parse(result.data)
        assert json.size == 2
        result = TagDomainAssociationAPI.listByDomain(annot, USERNAME1, PASSWORD1)
        assert result.code == 403
    }

    void testListTagDomainAssociationByTag() {
        def tag = BasicInstanceBuilder.getTagNotExist(true)

        getUser1()
        Project project = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 200
        association1 = result.data

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        TagDomainAssociation association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = tag
        association2.domain = annot
        result = TagDomainAssociationAPI.create(association2.encodeAsJSON(), association2.domainClassName, association2.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200
        association2 = result.data

        def algoAnnot = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        algoAnnot.project = project
        Infos.addUserRight(algoAnnot.user.user, project)
        result = AlgoAnnotationAPI.create(algoAnnot.encodeAsJSON(), algoAnnot.user.username, 'PasswordUserJob')
        algoAnnot = result.data
        TagDomainAssociation association3 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association3.tag = tag
        association3.domain = algoAnnot
        result = TagDomainAssociationAPI.create(association3.encodeAsJSON(), association3.domainClassName, association3.domainIdent, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert result.code == 200
        association3 = result.data

        def searchParameters = [[operator : "in", field : "tag", value:tag.id]]

        result = TagDomainAssociationAPI.search(searchParameters, USERNAME1, PASSWORD1)
        assert result.code == 200
        def json = JSON.parse(result.data)
        assert json.size == 2
        assert TagDomainAssociationAPI.containsInJSONList(association1.id,json)
        assert TagDomainAssociationAPI.containsInJSONList(association3.id,json)
    }

    void testListTagDomainAssociationByTagAndDomain() {
        getUser1()
        def tag1 = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag1.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert result.code == 200
        tag1 = result.data
        getUser2()
        def tag2 = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag2.encodeAsJSON(), USERNAME2, PASSWORD2)
        assert result.code == 200
        tag2 = result.data
        getUser3()
        def tag3 = BasicInstanceBuilder.getTagNotExist()
        result = TagAPI.create(tag3.encodeAsJSON(), USERNAME3, PASSWORD3)
        assert result.code == 200
        tag3 = result.data

        Project project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag3
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 200
        association1 = result.data


        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        TagDomainAssociation association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = tag2
        association2.domain = annot
        result = TagDomainAssociationAPI.create(association2.encodeAsJSON(), association2.domainClassName, association2.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200
        association2 = result.data

        def image = BasicInstanceBuilder.getImageInstanceNotExist()
        Infos.addUserRight(user3, image.project)
        result = ImageInstanceAPI.create(image.encodeAsJSON(), USERNAME3, PASSWORD3)
        assert result.code == 200
        image = result.data

        TagDomainAssociation association3 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association3.tag = tag1
        association3.domain = image
        result = TagDomainAssociationAPI.create(association3.encodeAsJSON(), association3.domainClassName, association3.domainIdent, USERNAME3, PASSWORD3)
        assert result.code == 200
        association3 = result.data

        TagDomainAssociation association4 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association4.tag = tag3
        association4.domain = image
        result = TagDomainAssociationAPI.create(association4.encodeAsJSON(), association4.domainClassName, association4.domainIdent, USERNAME3, PASSWORD3)
        assert result.code == 200
        association4 = result.data

        def searchParameters = [[operator : "in", field : "tag", value: tag1.id+","+tag2.id], [operator : "in", field : "domainIdent", value: project.id+","+image.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.size == 1
        assert TagDomainAssociationAPI.containsInJSONList(association3.id,json)

        searchParameters = [[operator : "in", field : "tag", value: tag1.id+","+tag2.id], [operator : "in", field : "domainIdent", value: image.id+","+annot.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == 2
        assert TagDomainAssociationAPI.containsInJSONList(association2.id,json)
        assert TagDomainAssociationAPI.containsInJSONList(association3.id,json)

        searchParameters = [[operator : "in", field : "tag", value: tag2.id+","+tag3.id], [operator : "in", field : "domainIdent", value: project.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == 1
        assert TagDomainAssociationAPI.containsInJSONList(association1.id,json)

        searchParameters = [[operator : "in", field : "tag", value: tag1.id+","+tag3.id], [operator : "in", field : "domainIdent", value: image.id+","+project.id]]
        result = TagDomainAssociationAPI.search(searchParameters, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.size == 3
    }


    void testAddTagDomainAssociation() {
        // check by domain permission

        def tag = BasicInstanceBuilder.getTagNotExist(true)

        getUser1()
        Project project = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data
        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 200

        getUser2()
        def annot = UserAnnotationAPI.buildBasicUserAnnotation(USERNAME2, PASSWORD2)
        TagDomainAssociation association2 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association2.tag = tag
        association2.domain = annot
        result = TagDomainAssociationAPI.create(association2.encodeAsJSON(), association2.domainClassName, association2.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 403
    }

    void testDeleteTagDomainAssociationByAdmin() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist(true)

        Project project = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(project.encodeAsJSON(), USERNAME1, PASSWORD1)
        project = result.data

        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME1, PASSWORD1)
        assert result.code == 200
        association1 = result.data

        result = TagDomainAssociationAPI.delete(association1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDeleteTagDomainAssociationByTagCreator() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert result.code == 200
        tag = result.data

        getUser2()
        Project project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME2, PASSWORD2)
        project = result.data

        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200
        association1 = result.data

        result = TagDomainAssociationAPI.delete(association1.id, USERNAME1, PASSWORD1)
        assert 403 == result.code
    }

    void testDeleteTagDomainAssociationByDomainGranted() {
        getUser1()
        def tag = BasicInstanceBuilder.getTagNotExist()
        def result = TagAPI.create(tag.encodeAsJSON(), USERNAME1, PASSWORD1)
        assert result.code == 200
        tag = result.data

        getUser2()
        Project project = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(project.encodeAsJSON(), USERNAME2, PASSWORD2)
        project = result.data
        Infos.addUserRight(user1, project)

        TagDomainAssociation association1 = BasicInstanceBuilder.getTagDomainAssociationNotExist()
        association1.tag = tag
        association1.domain = project
        result = TagDomainAssociationAPI.create(association1.encodeAsJSON(), association1.domainClassName, association1.domainIdent, USERNAME2, PASSWORD2)
        assert result.code == 200
        association1 = result.data

        result = TagDomainAssociationAPI.delete(association1.id, USERNAME1, PASSWORD1)
        assert 200 == result.code
    }
}
