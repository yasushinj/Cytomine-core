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
import be.cytomine.meta.Property
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import grails.converters.JSON

class PropertySecurityTests extends SecurityTestsAbstract {

    void testProjectPropertySecurityForCytomineAdmin() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        Property projectPropertyToAdd = BasicInstanceBuilder.getProjectPropertyNotExist(project)
        def result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code

        Property projectProperty = result.data

        //check if admin user can access/update/delete
        assert (200 == PropertyAPI.show(projectProperty.id, projectProperty.domainIdent, "project" , USERNAMEADMIN, PASSWORDADMIN).code)
        assert (true == PropertyAPI.containsInJSONList(projectProperty.id, JSON.parse(PropertyAPI.listByDomain(projectProperty.domainIdent, "project", USERNAMEADMIN, PASSWORDADMIN).data)))
        assert (200 == PropertyAPI.update(projectProperty.id, projectProperty.domainIdent, "project", projectProperty.encodeAsJSON(), USERNAMEADMIN, PASSWORDADMIN).code)
        assert (200 == PropertyAPI.delete(projectProperty.id, projectProperty.domainIdent, "project", USERNAMEADMIN, PASSWORDADMIN).code)
    }

    void testProjectPropertySecurityForProjectManager() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addAdminProject(project.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)

        def projectPropertyToAdd = BasicInstanceBuilder.getProjectPropertyNotExist()
        projectPropertyToAdd.domain = project

        def result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),USERNAME1, PASSWORD1)
        assert 200 == result.code
        Property projectProperty = result.data

        assert (200 == PropertyAPI.show(projectProperty.id, projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).code)
        assert (true == PropertyAPI.containsInJSONList(projectProperty.id, JSON.parse(PropertyAPI.listByDomain(projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).data)))
        assert (200 == PropertyAPI.update(projectProperty.id, projectProperty.domainIdent, "project" ,projectProperty.encodeAsJSON(), USERNAME1, PASSWORD1).code)
        assert (200 == PropertyAPI.delete(projectProperty.id, projectProperty.domainIdent, "project", USERNAME1, PASSWORD1).code)
    }

    void testProjectPropertySecurityForProjectUser() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ProjectAPI.addUserProject(project.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)

        def projectPropertyToAdd = BasicInstanceBuilder.getProjectPropertyNotExist()
        projectPropertyToAdd.domain = project

        def result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),USERNAME1, PASSWORD1)
        assert 403 == result.code
        result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Property projectProperty = result.data

        assert (200 == PropertyAPI.show(projectProperty.id, projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).code)
        assert (true == PropertyAPI.containsInJSONList(projectProperty.id, JSON.parse(PropertyAPI.listByDomain(projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).data)))
        assert (403 == PropertyAPI.update(projectProperty.id, projectProperty.domainIdent, "project" ,projectProperty.encodeAsJSON(), USERNAME1, PASSWORD1).code)
        assert (403 == PropertyAPI.delete(projectProperty.id, projectProperty.domainIdent, "project", USERNAME1, PASSWORD1).code)
    }

    void testProjectPropertySecurityForNotContributor() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        def projectPropertyToAdd = BasicInstanceBuilder.getProjectPropertyNotExist()
        projectPropertyToAdd.domain = project

        def result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),USERNAME1, PASSWORD1)
        assert 403 == result.code
        result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Property projectProperty = result.data

        assert (403 == PropertyAPI.show(projectProperty.id, projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).code)
        assert (false == PropertyAPI.containsInJSONList(projectProperty.id, JSON.parse(PropertyAPI.listByDomain(projectProperty.domainIdent, "project" , USERNAME1, PASSWORD1).data)))
        assert (403 == PropertyAPI.update(projectProperty.id, projectProperty.domainIdent, "project" ,projectProperty.encodeAsJSON(), USERNAME1, PASSWORD1).code)
        assert (403 == PropertyAPI.delete(projectProperty.id, projectProperty.domainIdent, "project", USERNAME1, PASSWORD1).code)
    }

    void testProjectPropertySecurityForAnonymous() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        def projectPropertyToAdd = BasicInstanceBuilder.getProjectPropertyNotExist()
        projectPropertyToAdd.domain = project

        def result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),USERNAMEBAD, PASSWORDBAD)
        assert 401 == result.code
        result = PropertyAPI.create(projectPropertyToAdd.domainIdent, "project" ,projectPropertyToAdd.encodeAsJSON(),Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Property projectProperty = result.data

        assert (401 == PropertyAPI.show(projectProperty.id, projectProperty.domainIdent, "project" , USERNAMEBAD, PASSWORDBAD).code)
        assert (false == PropertyAPI.containsInJSONList(projectProperty.id, JSON.parse(PropertyAPI.listByDomain(projectProperty.domainIdent, "project" , USERNAMEBAD, PASSWORDBAD).data)))
        assert (401 == PropertyAPI.update(projectProperty.id, projectProperty.domainIdent, "project" ,projectProperty.encodeAsJSON(), USERNAMEBAD, PASSWORDBAD).code)
        assert (401 == PropertyAPI.delete(projectProperty.id, projectProperty.domainIdent, "project", USERNAMEBAD, PASSWORDBAD).code)
    }

    void testAnnotationPropertySecurityForCytomineAdmin() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)

        //Create project with user 1
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project

        //Add annotation with cytomine admin
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = project
        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAMEADMIN, SecurityTestsAbstract.PASSWORDADMIN)
        assert 200 == result.code

        Property annotationPropertyToAdd = BasicInstanceBuilder.getAnnotationPropertyNotExist(result.data)
        result = PropertyAPI.create(annotationPropertyToAdd.domainIdent, "annotation" ,annotationPropertyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code

        Property annotationProperty = result.data

        //check if admin user can access/update/delete
        assert (200 == PropertyAPI.show(annotationProperty.id, annotationProperty.domainIdent, "annotation" , USERNAMEADMIN, PASSWORDADMIN).code)
        assert (true == PropertyAPI.containsInJSONList(annotationProperty.id, JSON.parse(PropertyAPI.listByDomain(annotationProperty.domainIdent, "annotation", USERNAMEADMIN, PASSWORDADMIN).data)))
        assert (200 == PropertyAPI.update(annotationProperty.id, annotationProperty.domainIdent, "annotation", annotationProperty.encodeAsJSON(), USERNAMEADMIN, PASSWORDADMIN).code)
        assert (200 == PropertyAPI.delete(annotationProperty.id, annotationProperty.domainIdent, "annotation", USERNAMEADMIN, PASSWORDADMIN).code)
    }

    void testAnnotationPropertySecurityForProjectUser() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        def annotationToAdd = BasicInstanceBuilder.getUserAnnotationNotExist()
        Infos.addUserRight(USERNAME1,annotationToAdd.project)

        def result =  AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        def annotationPropertyToAdd = BasicInstanceBuilder.getAnnotationPropertyNotExist()
        annotationPropertyToAdd.domain = result.data
        result = PropertyAPI.create(annotationPropertyToAdd.domainIdent, "annotation" ,annotationPropertyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Property annotationProperty = result.data

        //Create image, project, annotation
        ImageInstance image = ImageInstanceAPI.buildBasicImage(SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        Project project = image.project
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotation.image = image
        annotation.project = image.project
        result = UserAnnotationAPI.create(annotation.encodeAsJSON(), SecurityTestsAbstract.USERNAME1, SecurityTestsAbstract.PASSWORD1)
        assert 200 == result.code
        annotation = result.data

        annotationProperty.domain = annotation;

        BasicInstanceBuilder.saveDomain(project)
        BasicInstanceBuilder.saveDomain(annotationProperty)

        //TODO: try with USERNAME1 & PASSWORD1
        def resAddUser = ProjectAPI.addAdminProject(project.id,user1.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        resAddUser = ProjectAPI.addUserProject(project.id,user2.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == resAddUser.code
        //check if user 2 can access/update/delete
        assert (200 == PropertyAPI.show(annotationProperty.id, annotationProperty.domainIdent, "annotation" , USERNAME2, PASSWORD2).code)
        assert (true == PropertyAPI.containsInJSONList(annotationProperty.id, JSON.parse(PropertyAPI.listByDomain(annotationProperty.domainIdent, "annotation" , USERNAME2, PASSWORD2).data)))
        assert (200 == PropertyAPI.update(annotationProperty.id, annotationProperty.domainIdent, "annotation" ,annotationProperty.encodeAsJSON(), USERNAME2, PASSWORD2).code)


        //remove right to user2
        resAddUser = ProjectAPI.deleteUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //check if user 2 cannot access/update/delete
        assert (403 == PropertyAPI.show(annotationProperty.id, annotationProperty.domainIdent, "annotation" ,USERNAME2,PASSWORD2).code)
        //assert (200 == PropertyAPI.listByDomain(annotationProperty.domainIdent, "annotation" , USERNAME2,PASSWORD2).code)
        assert (403 == PropertyAPI.update(annotationProperty.id, annotationProperty.domainIdent, "annotation", annotationProperty.encodeAsJSON(),USERNAME2,PASSWORD2).code)

//        //delete project because we will try to delete term
//        def resDelProj = ProjectAPI.delete(project.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
//        assert (403 == resDelProj.code)

//        assert (404 == PropertyAPI.delete(annotationProperty.id, annotationProperty.domainIdent, "annotation", USERNAME2,PASSWORD2).code)
    }

    void testAnnotationPropertySecurityForSimpleUser() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        def annotationToAdd = BasicInstanceBuilder.getUserAnnotationNotExist()
        Infos.addUserRight(USERNAME1,annotationToAdd.project)

        //Create new Term (user1)
        def result =  AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        def annotationPropertyToAdd = BasicInstanceBuilder.getAnnotationPropertyNotExist()
        annotationPropertyToAdd.domain = result.data
        result = PropertyAPI.create(annotationPropertyToAdd.domainIdent, "annotation" ,annotationPropertyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Property annotationProperty = result.data

        //check if user 2 cannot access/update/delete
        assert (403 == PropertyAPI.show(annotationProperty.id, annotationProperty.domainIdent, "annotation" , USERNAME2, PASSWORD2).code)
        assert (403 == PropertyAPI.update(annotationProperty.id, annotationProperty.domainIdent, "annotation", annotationProperty.encodeAsJSON(), USERNAME2, PASSWORD2).code)
        assert (403 == PropertyAPI.delete(annotationProperty.id, annotationProperty.domainIdent, "annotation", USERNAME2, PASSWORD2).code)
    }

    void testAnnotationPropertySecurityForAnonymous() {
        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        def annotationToAdd = BasicInstanceBuilder.getUserAnnotationNotExist()
        Infos.addUserRight(USERNAME1,annotationToAdd.project)

        //Create new Term (user1)
        def result =  AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        def annotationPropertyToAdd = BasicInstanceBuilder.getAnnotationPropertyNotExist()
        annotationPropertyToAdd.domain = result.data
        result = PropertyAPI.create(annotationPropertyToAdd.domainIdent, "annotation" ,annotationPropertyToAdd.encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Property annotationProperty = result.data

        //check if user 2 cannot access/update/delete
        assert (401 == PropertyAPI.show(annotationProperty.id, annotationProperty.domainIdent, "annotation" ,USERNAMEBAD, PASSWORDBAD).code)
        assert (401 == PropertyAPI.listByDomain(annotationProperty.domainIdent, "annotation", USERNAMEBAD, PASSWORDBAD).code)
        assert (401 == PropertyAPI.update(annotationProperty.id, annotationProperty.domainIdent, "annotation", annotationProperty.encodeAsJSON(), USERNAMEBAD, PASSWORDBAD).code)
        assert (401 == PropertyAPI.delete(annotationProperty.id, annotationProperty.domainIdent, "annotation", USERNAMEBAD, PASSWORDBAD).code)
    }
}
