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
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationDomainAPI
import be.cytomine.test.http.AnnotationTermAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.utils.JSONUtils
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class UserAnnotationSecurityTests extends SecurityTestsAbstract {

    void testAnnotationSecurityForCytomineAdmin() {
        //Get User 1
        User user = getUser1()

        //Get cytomine admin
        User admin = getUserAdmin()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with cytomine admin
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation1.image = image
        annotation1.project = project
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        annotation1 = result.data

        //Add annotation 2 with user 1
        UserAnnotation annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation2.image = image
        annotation2.project = project
        Infos.printRight(annotation2.project)
        result = UserAnnotationAPI.create(annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation2 = result.data

        //add the two annotations
        def annotations = []
        annotations << JSON.parse(annotation1.encodeAsJSON())
        annotations << JSON.parse(annotation2.encodeAsJSON())
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code


        //Get/List annotation with cytomine admin
        assert (200 == UserAnnotationAPI.show(annotation2.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN).code)
        result = UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        log.info "JSON.parse(result.data)="+JSON.parse(result.data)
        assert (true ==UserAnnotationAPI.containsInJSONList(annotation2.id, JSON.parse(result.data)))

        //update annotation 2 with cytomine admin
        assert (200 == UserAnnotationAPI.update(annotation2.id,annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN).code)

        //Delete annotation 2 with cytomine admin
        assert (200 == UserAnnotationAPI.delete(annotation2.id, SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN).code)


    }

    //A project admin must be able to update/delete an annotation from another user
    void testAnnotationSecurityForProjectAdmin() {
        //Get User 1
        User user1 = getUser1()

        //Get User 2
        User user2 = getUser2()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add annotation 1 with user 2
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotation = result.data

        //add the two annotations
        def annotations = []
        annotations << JSON.parse(annotation.encodeAsJSON())
        annotations << JSON.parse(annotation.encodeAsJSON())
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        //Get/List annotation 1 with user 1
        assert (200 == UserAnnotationAPI.show(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
        result = UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (true ==UserAnnotationAPI.containsInJSONList(annotation.id, JSON.parse(result.data)))

        //update annotation 1 with user 1
        assert (200 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)

        //Delete annotation 1 with user 2
        assert (200 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
    }



    void testAnnotationSecurityForProjectUserAndAnnotationCreator() {
        //Get User 1
        User user = getUser1()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with user1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data

        //Get/List annotation 1 with user 1
        assert (200 == UserAnnotationAPI.show(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
        result = UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        assert (true ==UserAnnotationAPI.containsInJSONList(annotation.id, JSON.parse(result.data)))

        //update annotation 1 with user 1
        annotation.refresh()
        assert (200 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)

        //Delete annotation 1 with user 1
        assert (200 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1).code)
    }

    void testAnnotationSecurityForProjectUser() {
        //Get User 1
        User user1 = getUser1()

        //Get User 2
        User user2 = getUser2()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add annotation 1 with user 1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data

        //add the two annotations
        def annotations = []
        annotations << JSON.parse(annotation.encodeAsJSON())
        annotations << JSON.parse(annotation.encodeAsJSON())
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

        //Get/List annotation 1 with user 2
        assert (200 == UserAnnotationAPI.show(annotation.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        assert (true ==UserAnnotationAPI.containsInJSONList(annotation.id, JSON.parse(result.data)))

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        //update annotation 1 with user 2
        assert (403 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)

        //Delete annotation 1 with user 2
        assert (403 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)

        project.mode = Project.EditingMode.CLASSIC
        BasicInstanceBuilder.saveDomain(project)

        //update annotation 1 with user 2
        assert (200 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)

        //Delete annotation 1 with user 2
        assert (200 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)

    }


    void testAnnotationSecurityForUser() {
        //Get User 1
        User user1 = getUser1()

        //Get User 2
        User user2 = getUser2()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with user 1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        project.refresh()
        println "1**************"
        Infos.printRight(project)
        println "1**************"
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        //create annotation with a good user
        result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        annotation = result.data

        //Get/List annotation 1 with user 2
        assert (403 == UserAnnotationAPI.show(annotation.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
        result = UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert (403 == result.code)

        //update annotation 1 with user 2
        assert (403 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)

        //Delete annotation 1 with user 2
        assert (403 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2).code)
    }



    void testAnnotationSecurityForAnonymous() {
        //Get User 1
        User user1 = getUser1()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with user 1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data

        //Get/List annotation 1 with user 2
        assert (401 == UserAnnotationAPI.show(annotation.id, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
        assert (401 == UserAnnotationAPI.listByProject(project.id, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
        assert (401 == UserAnnotationAPI.update(annotation.id,annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
        assert (401 == UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAMEBAD, SecurityTestsAbstract.PASSWORDBAD).code)
    }


     void testFreeHandAnnotationWithProjectAdmin() {
         //project admin can correct annotation from another user
         String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
         String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
         String expectedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"

         User user1 = getUser1() //project admin
         User user2 = getUser2()

         //Create project with user 1
         ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
         Project project = image.project

         //Add project right for user 2
         def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
         Infos.printRight(project)
         assert 200 == resAddUser.code

         //Add annotation 1 with user1
         UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
         annotation.image = image
         annotation.project = image.project
         annotation.user = user2
         def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
         assert 200 == result.code
         annotation = result.data
         //add annotation with empty space inside it
         annotation.location = new WKTReader().read(basedLocation)
         assert annotation.save(flush: true)  != null

         //correct remove
         def json = [:]
         json.location = addedLocation
         json.image = annotation.image.id
         json.review = false
         json.remove = false
         json.layers = [user2.id,user1.id]
         result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
         assert 200 == result.code

         annotation.refresh()
         assert annotation.user.id == user2.id
         assert new WKTReader().read(expectedLocation).equals(annotation.location)
         //assertEquals(expectedLocation,annotationToFill.location.toString())
    }


     void testFreeHandAnnotationWithProjectUser() {
        //project user cannot correct annotation from another user
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"

        User user1 = getUser1() //project admin
        User user2 = getUser2()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add annotation 1 with user1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data
        //add annotation with empty space inside it
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correct remove
        def json = [:]
        json.location = addedLocation
        json.image = annotation.image.id
        json.review = false
        json.remove = false
        json.layers = [user2.id,user1.id]
        result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 400 == result.code
         annotation.refresh()
        assert new WKTReader().read(basedLocation).equals(annotation.location)
        //assertEquals(expectedLocation,annotationToFill.location.toString())
    }

    void testFreeHandAnnotationWithMultipleAnnotationUser() {
       //cannot correct annotation if addedlocation has annotations from multiple users
       String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
       String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"

       User user1 = getUser1() //project admin
       User user2 = getUser2()

       //Create project with user 1
       ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
       Project project = image.project

       //Add project right for user 2
       def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
       Infos.printRight(project)
       assert 200 == resAddUser.code

       //Add annotation 1 with user1
       UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
       annotation.image = image
       annotation.project = image.project
       def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
       assert 200 == result.code
       annotation = result.data
       //add annotation with empty space inside it
       annotation.location = new WKTReader().read(basedLocation)
       assert annotation.save(flush: true)  != null

       //Add annotation 1 with user2
       annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
       annotation.image = image
       annotation.project = image.project
       result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
       assert 200 == result.code
       annotation = result.data
       //add annotation with empty space inside it
       annotation.location = new WKTReader().read(basedLocation)
       assert annotation.save(flush: true)  != null

       //correct remove
       def json = [:]
       json.location = addedLocation
       json.image = annotation.image.id
       json.review = false
       json.remove = false
       json.layers = [user2.id,user1.id]
       result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
       assert 400 == result.code
   }

    void testTargettedFreeHandCorrectionWithNotProjectMember() {
        // user not member of project cannot correct annotation
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation 1 with user1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data
        //add annotation with empty space inside it
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correct remove
        def json = [:]
        json.location = addedLocation
        json.review = false
        json.remove = false
        json.annotation = annotation.id
        result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
        annotation.refresh()
        assert new WKTReader().read(basedLocation).equals(annotation.location)
    }

    void testTargettedFreeHandCorrectionWithProjectMember() {
        // project member can correct the annotation of another user iff project in classic mode
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
        String expectedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"

        User user2 = getUser2()

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code

        //Add annotation 1 with user1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data
        //add annotation with empty space inside it
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correction allowed in classic mode
        def json = [:]
        json.location = addedLocation
        json.review = false
        json.remove = false
        json.annotation = annotation.id
        result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotation.refresh()
        assert new WKTReader().read(expectedLocation).equals(annotation.location)

        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        //correction not allowed in restricted mode
        json.remove = true
        result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
        annotation.refresh()
        assert new WKTReader().read(expectedLocation).equals(annotation.location)
    }

    void testDeleteUserAnnotationWithTerm() {

        User user1 = getUser1()
        User user2 = getUser2()


        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project


        // DELETE AN ANNOT (USER1) WHEN USER1 HAD ASSOCIATED A TERM

        //Add annotation 1 with user1
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert result.code == 200
        annotation = result.data


        def annotTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotation)
        annotTerm.user = user1

        result = AnnotationTermAPI.createAnnotationTerm(annotTerm.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert result.code == 200

        result = UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert result.code == 200


        // DELETE AN ANNOT (USER1) WHEN USER2 HAD ASSOCIATED A TERM

        //Add annotation 1 with user1
        annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        annotation.user = user1
        result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert result.code == 200
        annotation = result.data


        //Add project right for user 2
        def resAddUser = ProjectAPI.addUserProject(project.id, user2.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Infos.printRight(project)
        assert 200 == resAddUser.code


        annotTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotation)
        annotTerm.user = user2

        result = AnnotationTermAPI.createAnnotationTerm(annotTerm.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert result.code == 200

        result = UserAnnotationAPI.delete(annotation.id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert result.code == 200
    }


}
