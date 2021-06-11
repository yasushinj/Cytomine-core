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

import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AttachedFileAPI
import be.cytomine.test.http.DomainAPI
import be.cytomine.meta.AttachedFile
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
class AttachedFileTests {

    void testListAttachedFile() {
        def result = AttachedFileAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testShowAttachedFile() {
        def domain = BasicInstanceBuilder.getAttachedFileNotExist(true)
        println domain.domainClassName
        println domain.domainIdent

        Project.list().each{println it.id}

        def result = AttachedFileAPI.show(domain.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testShowAttachedFileNotExist() {
        def result = AttachedFileAPI.show(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testListAttachedFileByDomain() {
        def domain = BasicInstanceBuilder.getAttachedFileNotExist(true)
        def result = AttachedFileAPI.listByDomain(domain.domainClassName,domain.domainIdent,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert !json.collection.isEmpty()
    }

    void testDownloadAttachedFile() {
        def result = AttachedFileAPI.download(BasicInstanceBuilder.getAttachedFileNotExist("test/functional/be/cytomine/utils/images/preview.png",true).id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddAttachedFileCorrect() {
        def attachedFileToAdd = BasicInstanceBuilder.getAttachedFileNotExist(false)
        def result = AttachedFileAPI.upload(attachedFileToAdd.domainClassName,attachedFileToAdd.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.filename == "simpleFile.txt"
    }

    void testAddAttachedFileCorrectWithOtherName() {
        def attachedFileToAdd = BasicInstanceBuilder.getAttachedFileNotExist(false)
        def result = AttachedFileAPI.upload("myTestFile", attachedFileToAdd.domainClassName,attachedFileToAdd.domainIdent,new File("test/functional/be/cytomine/utils/simpleFile.txt"),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.filename == "myTestFile"
    }

    void testAddAttachedFileIncorrect() {
        def attachedFileToAdd = BasicInstanceBuilder.getAttachedFileNotExist(false)
        String URL = Infos.CYTOMINEURL + "api/attachedfile.json?domainClassName=${attachedFileToAdd.domainClassName}&domainIdent=${attachedFileToAdd.domainIdent}"
        assert 400 == DomainAPI.doPOST(URL,"",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code
    }


    void testDownloadNoSecurity() {

        //TODO: TEMPORARY disable security (juste for access). There is an issue:
        //if we copy layers from image x - project 1 to image x - project 2, users may not have the right to download the file

        //create user guest
        User guest = BasicInstanceBuilder.getUser("testDownloadForGuest","password")

        //create a project in r/o
        Project project = BasicInstanceBuilder.getProjectNotExist()
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        //add an annotation to project
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project, true)

        //add attached file to annotation
        AttachedFile attachedFile = BasicInstanceBuilder.getAttachedFileNotExist(false)
        attachedFile.setDomain(annotation)
        BasicInstanceBuilder.saveDomain(attachedFile)

        //try to download attached file
        def result = AttachedFileAPI.show(attachedFile.id, "testDownloadForGuest", "password")
        assert 403 == result.code

    }

    void testDeleteAttachedFileCorrect() {
        def attachedFileToAdd = BasicInstanceBuilder.getAttachedFileNotExist(true)
        def result = AttachedFileAPI.show(attachedFileToAdd.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AttachedFileAPI.delete(attachedFileToAdd.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AttachedFileAPI.show(attachedFileToAdd.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

}
