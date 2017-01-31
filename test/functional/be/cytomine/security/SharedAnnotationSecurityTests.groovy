package be.cytomine.security

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
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationCommentAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAnnotationAPI
import grails.converters.JSON

class SharedAnnotationSecurityTests extends SecurityTestsAbstract {

    // initialization block to record in DB the 3 users used in these tests
    {
        getUser1()
        getUser2()
        getUserAdmin()
    }


    void testSharedAnnotationSecurityForCytomineAdmin() {

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with cytomine admin
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, userAdmin)
        annotation1.image = image
        annotation1.project = project
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
        annotation1 = result.data

        //Add annotation 2 with user 1
        UserAnnotation annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user1)
        annotation2.image = image
        annotation2.project = project
        Infos.printRight(annotation2.project)
        result = UserAnnotationAPI.create(annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation2 = result.data

        //Admin can add comment on the two annotations
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code
    }

    void testSharedAnnotationSecurityForProjectAdmin() {

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with project admin
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user1)
        annotation1.image = image
        annotation1.project = project
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation1 = result.data

        //Add contributor to project
        ProjectAPI.addUserProject(project.id, getUser2().id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation 2 with a contributor
        UserAnnotation annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user2)
        annotation2.image = image
        annotation2.project = project
        Infos.printRight(annotation2.project)
        result = UserAnnotationAPI.create(annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotation2 = result.data

        //Project admin can add comment on the two annotations
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
    }


    void testSharedAnnotationSecurityForProjectUser() {

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with project admin
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user1)
        annotation1.image = image
        annotation1.project = project
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation1 = result.data

        //Add contributor to project
        ProjectAPI.addUserProject(project.id, getUser2().id, SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)

        //Add annotation 2 with a contributor
        UserAnnotation annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user2)
        annotation2.image = image
        annotation2.project = project
        Infos.printRight(annotation2.project)
        result = UserAnnotationAPI.create(annotation2.encodeAsJSON(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code
        annotation2 = result.data

        //When project is classic

        //Project user can add comment on the two annotations
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        //When project is restricted
        project.mode = Project.EditingMode.RESTRICTED
        BasicInstanceBuilder.saveDomain(project)

        //Project user can add comment only on its annotations
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 200 == result.code

        //When project is read-only
        project.mode = Project.EditingMode.READ_ONLY
        BasicInstanceBuilder.saveDomain(project)

        //Project user cannot add comment even on its annotations
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation2.class.name
        sharedAnnotation.annotationIdent = annotation2.id
        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
    }


    void testSharedAnnotationSecurityForNonProjectUser() {

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation 1 with project admin
        UserAnnotation annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(project, image, user1)
        annotation1.image = image
        annotation1.project = project
        def result = UserAnnotationAPI.create(annotation1.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation1 = result.data

        //User2 in not in the project si cannot add comment on the annotations
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        sharedAnnotation.annotationClassName = annotation1.class.name
        sharedAnnotation.annotationIdent = annotation1.id
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), SecurityTestsAbstract.USERNAME2, SecurityTestsAbstract.PASSWORD2)
        assert 403 == result.code
    }


}
