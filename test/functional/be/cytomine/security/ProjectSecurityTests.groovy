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
import be.cytomine.ontology.Property
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.JobData
import be.cytomine.processing.Software
import be.cytomine.project.Project
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.*
import be.cytomine.utils.Description
import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 2/03/11
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
class ProjectSecurityTests extends SecurityTestsAbstract {


    void testProjectSecurityForCytomineAdmin() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Get admin user
        User admin = BasicInstanceBuilder.getSuperAdmin(USERNAMEADMIN,PASSWORDADMIN)


        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        Infos.printUserRight(user1)
        Infos.printUserRight(admin)
        //check if admin user can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAMEADMIN,PASSWORDADMIN).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAMEADMIN,PASSWORDADMIN).data)))
        assert (200 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAMEADMIN,PASSWORDADMIN).code)
        assert (200 == ProjectAPI.delete(project.id,USERNAMEADMIN,PASSWORDADMIN).code)
    }

    void testProjectSecurityForProjectCreator() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        println "PROJECT="+project.deleted

        //check if user 1 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAME1,PASSWORD1).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME1,PASSWORD1).data)))
        assert (200 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME1,PASSWORD1).code)
        assert (200 == ProjectAPI.delete(project.id,USERNAME1,PASSWORD1).code)
    }

    void testProjectSecurityForProjectUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add right to user2
        def resAddUser = ProjectAPI.addUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(project)
        //check if user 2 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)


        //remove right to user2
        resAddUser = ProjectAPI.deleteUserProject(project.id,user2.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code

        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (403 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert (false == ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)
    }

    void testProjectSecurityForSimpleUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get user2
        User user2 = BasicInstanceBuilder.getUser(USERNAME2,PASSWORD2)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (403 == ProjectAPI.show(project.id,USERNAME2,PASSWORD2).code)
        assert(false==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list(USERNAME2,PASSWORD2).data)))
        Infos.printRight(project)
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAME2,PASSWORD2).code)
        assert (403 == ProjectAPI.delete(project.id,USERNAME2,PASSWORD2).code)

    }

    void testProjectSecurityForGhestUser() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)
        //Get ghest
        User ghest = BasicInstanceBuilder.getGhest("GHESTONTOLOGY","PASSWORD")

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data

        //Add right to user2
        def resAddUser = ProjectAPI.addUserProject(project.id,ghest.id,USERNAME1,PASSWORD1)
        assert 200 == resAddUser.code
        //log.info "AFTER:"+user2.getAuthorities().toString()

        Infos.printRight(project)
        //check if user 2 can access/update/delete
        assert (200 == ProjectAPI.show(project.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (true ==ProjectAPI.containsInJSONList(project.id,JSON.parse(ProjectAPI.list("GHESTONTOLOGY","PASSWORD").data)))
        assert (403 == ProjectAPI.update(project.id,project.encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == ProjectAPI.delete(project.id,"GHESTONTOLOGY","PASSWORD").code)
        assert (403 == ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),"GHESTONTOLOGY","PASSWORD").code)
    }




    void testProjectSecurityForAnonymous() {

        //Get user1
        User user1 = BasicInstanceBuilder.getUser(USERNAME1,PASSWORD1)

        //Create new project (user1)
        def result = ProjectAPI.create(BasicInstanceBuilder.getProjectNotExist().encodeAsJSON(),USERNAME1,PASSWORD1)
        assert 200 == result.code
        Project project = result.data
        Infos.printRight(project)
        //check if user 2 cannot access/update/delete
        assert (401 == ProjectAPI.show(project.id,USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.list(USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.update(project.id,project.encodeAsJSON(),USERNAMEBAD,PASSWORDBAD).code)
        assert (401 == ProjectAPI.delete(project.id,USERNAMEBAD,PASSWORDBAD).code)
    }

    void testAddProjectGrantAdminUndoRedo() {
        //not implemented (no undo/redo for project)
    }

    void testReadOnlyProject() {

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read Only
        project.isReadOnly = true
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Test as simple user

        //update project
        assert 403 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // TODO dowload empty image list & annotations list

        //Now run test as a project admin

        //update project
        assert 200 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // TODO dowload empty image list & annotations list

    }

    void testReadOnlyProjectWithImageData() {

        // Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Set project as Readonly
        project.isReadOnly = true
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //super admin data
        //Create an annotation (by superadmin)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project,true)
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project,image,true)
        //Create a description
        Description description = BasicInstanceBuilder.getDescriptionNotExist(annotation,true)
        //Create a property
        Property property = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,true)

        //admin data
        //Create an annotation (by admin)
        ImageInstance imageAdmin = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(imageAdmin)
        UserAnnotation annotationAdmin = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,false)
        annotationAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(annotationAdmin)
        //Create a description
        Description descriptionAdmin = BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin,true)
        //Create a property
        Property propertyAdmin = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,true)

        //simple user data
        //Create an annotation (by user)
        ImageInstance imageUser = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageUser.user = simpleUser;
        BasicInstanceBuilder.saveDomain(imageUser)
        UserAnnotation annotationUser = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,false)
        annotationUser.user = admin;
        BasicInstanceBuilder.saveDomain(annotationUser)
        //Create a description
        Description descriptionUser = BasicInstanceBuilder.getDescriptionNotExist(annotationUser,true)
        //Create a property
        Property propertyUser = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,true)


        /*
           Now Test as simple user
         */



        //add,update, delete property (simple user data)
        assert 403 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(propertyUser.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(propertyUser.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (admin data)
        assert 403 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(propertyAdmin.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(propertyAdmin.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (superadmin data)
        assert 403 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),simpleUsername,password).code
        assert 403 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 403 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add image instance
        assert 403 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete image instance (simple user data)
        assert 403 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(), simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageUser, simpleUsername, password).code

        //update, delete image instance (admin data)
        assert 403 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(), simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageAdmin, simpleUsername, password).code

        //update, delete image instance (superadmin data)
        assert 403 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(), simpleUsername, password).code
        assert 403 == ImageInstanceAPI.delete(image, simpleUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageUser.id,simpleUsername, password).code
        //start reviewing image (admin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,simpleUsername, password).code
        //start reviewing image (superadmin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(image.id,simpleUsername, password).code

        //add annotation
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete annotation (simple user data)
        def jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotationUser.id, jsonAnnotation, simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationUser,simpleUsername, password).code

        //update, delete annotation (admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotationAdmin.id, jsonAnnotation, simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationAdmin,simpleUsername, password).code

        //update, delete annotation (super admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotation.id, jsonAnnotation, simpleUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotation,simpleUsername, password).code


        //add, update, delete description (simple user data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,simpleUsername, password).code

        //add, update, delete description (admin data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,description.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),simpleUsername, password).code
        assert 403 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,simpleUsername, password).code


        /*
          Now run test as a project admin
         */

        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,annotationUser.encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, property.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (admin data)
        assert 403 == PropertyAPI.create(annotationAdmin.id, "annotation" ,annotationAdmin.encodeAsJSON(),adminUsername,password).code
        assert 403 == PropertyAPI.update(propertyAdmin.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 403 == PropertyAPI.delete(propertyAdmin.id, property.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (superadmin data)
        assert 403 == PropertyAPI.create(annotation.id, "annotation" ,annotation.encodeAsJSON(),adminUsername,password).code
        assert 403 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 403 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", adminUsername, password).code

        //add image instance
        assert 403 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),adminUsername, password).code

        //update, delete image instance (simple user data)
        assert 403 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),adminUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageUser, adminUsername, password).code

        //update, delete image instance (admin data)
        assert 403 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(),adminUsername, password).code
        assert 403 == ImageInstanceAPI.delete(imageAdmin, adminUsername, password).code

        //update, delete image instance (superadmin data)
        assert 403 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),adminUsername, password).code
        assert 403 == ImageInstanceAPI.delete(image, adminUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageUser.id,adminUsername, password).code
        //start reviewing image (admin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,adminUsername, password).code
        //start reviewing image (superadmin data)
        assert 403 == ReviewedAnnotationAPI.markStartReview(image.id,adminUsername, password).code

        //add annotation
        assert 403 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,false).encodeAsJSON(),adminUsername, password).code

        //update, delete annotation (simple user data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotationUser.id, jsonAnnotation, adminUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationUser,simpleUsername, password).code

        //update, delete annotation (admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotationAdmin.id, jsonAnnotation, adminUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotationAdmin,simpleUsername, password).code

        //update, delete annotation (super admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 403 == UserAnnotationAPI.update(annotation.id, jsonAnnotation, adminUsername, password).code
        assert 403 == UserAnnotationAPI.delete(annotation,simpleUsername, password).code


        //add, update, delete description (simple user data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,adminUsername, password).code

        //add, update, delete description (admin data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 403 == DescriptionAPI.create(project.id,project.class.name,description.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),adminUsername, password).code
        assert 403 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,adminUsername, password).code
    }


    void testReadOnlyProjectWithJobData() {
        //Init dataset

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.isReadOnly = false
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //data
        JobData jobData = BasicInstanceBuilder.getJobDataNotExist()
        Job job = jobData.job
        Software software = job.software;
        BasicInstanceBuilder.saveDomain(software)
        BasicInstanceBuilder.saveDomain(job)
        BasicInstanceBuilder.saveDomain(jobData)


        /*
           Now Test as simple user
         */

        assert 200 == JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist().encodeAsJSON(),simpleUser, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), simpleUser, password).code
        assert 200 == JobDataAPI.download(jobData.id, simpleUser, password).code
        assert 200 == JobDataAPI.delete(jobData.id, simpleUser, password).code

        assert 200 == JobAPI.create(BasicInstanceBuilder.getJobNotExist().encodeAsJSON(),simpleUser, password).code
        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), simpleUser, password).code
        assert 200 == JobAPI.delete(job.id, simpleUser, password).code

        assert 200 == SoftwareAPI.create(BasicInstanceBuilder.getSoftwareNotExist().encodeAsJSON(),simpleUser, password).code
        assert 200 == SoftwareAPI.update(software.id, software.encodeAsJSON(), simpleUser, password).code
        assert 200 == SoftwareAPI.delete(software.id, simpleUser, password).code


        /*
          Now run test as a project admin
         */

        assert 200 == JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, adminUsername, password).code
        assert 200 == JobDataAPI.delete(jobData.id, adminUsername, password).code

        assert 200 == JobAPI.create(BasicInstanceBuilder.getJobNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), adminUsername, password).code
        assert 200 == JobAPI.delete(job.id, adminUsername, password).code

        assert 200 == SoftwareAPI.create(BasicInstanceBuilder.getSoftwareNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == SoftwareAPI.update(software.id, software.encodeAsJSON(), adminUsername, password).code
        assert 200 == SoftwareAPI.delete(software.id, adminUsername, password).code

    }




    void testClassicProject() {
        /*
           Init dataset
         */

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.isReadOnly = false
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        /*
           Now Test as simple user
         */

        //update project
        assert 403 == ProjectAPI.update(project.id,project.encodeAsJSON(),simpleUsername, password).code
        // TODO dowload empty image list & annotations list

        /*
          Now run test as a project admin
         */

        //update project
        assert 200 == ProjectAPI.update(project.id,project.encodeAsJSON(),adminUsername, password).code
        // TODO dowload empty image list & annotations list

    }

    void testClassicProjectWithImageData() {
        /*
           Init dataset
         */

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.isReadOnly = false
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code


        /*super admin data*/
        //Create an annotation (by superadmin)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project,true)
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project,image,true)
        //Create a description
        Description description = BasicInstanceBuilder.getDescriptionNotExist(annotation,true)
        //Create a property
        Property property = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,true)

        /*admin data*/
        //Create an annotation (by admin)
        ImageInstance imageAdmin = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(imageAdmin)
        UserAnnotation annotationAdmin = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageAdmin,false)
        annotationAdmin.user = admin;
        BasicInstanceBuilder.saveDomain(annotationAdmin)
        //Create a description
        Description descriptionAdmin = BasicInstanceBuilder.getDescriptionNotExist(annotationAdmin,true)
        //Create a property
        Property propertyAdmin = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,true)

        /*simple user data*/
        //Create an annotation (by user)
        ImageInstance imageUser = BasicInstanceBuilder.getImageInstanceNotExist(project,false)
        imageUser.user = simpleUser;
        BasicInstanceBuilder.saveDomain(imageUser)
        UserAnnotation annotationUser = BasicInstanceBuilder.getUserAnnotationNotExist(project,imageUser,false)
        annotationUser.user = admin;
        BasicInstanceBuilder.saveDomain(annotationUser)
        //Create a description
        Description descriptionUser = BasicInstanceBuilder.getDescriptionNotExist(annotationUser,true)
        //Create a property
        Property propertyUser = BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,true)


        /*
           Now Test as simple user
         */


        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationUser,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotationAdmin,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,BasicInstanceBuilder.getAnnotationPropertyNotExist(annotation,false).encodeAsJSON(),simpleUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), simpleUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", simpleUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, simpleUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, simpleUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,image.encodeAsJSON(),simpleUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, simpleUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,simpleUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,simpleUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,simpleUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,simpleUsername, password).code

        //add annotation
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,false).encodeAsJSON(),simpleUsername, password).code

        //update, delete annotation (simple user data)
        def jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotationUser.id, jsonAnnotation, simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser,simpleUsername, password).code

        //update, delete annotation (admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, jsonAnnotation, simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin,simpleUsername, password).code

        //update, delete annotation (super admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotation.id, jsonAnnotation, simpleUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation,simpleUsername, password).code


        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,simpleUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,simpleUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,description.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),simpleUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,simpleUsername, password).code


        /*
          Now run test as a project admin
         */

        //add,update, delete property (simple user data)
        assert 200 == PropertyAPI.create(annotationUser.id, "annotation" ,annotationUser.encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyUser.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyUser.id, property.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (admin data)
        assert 200 == PropertyAPI.create(annotationAdmin.id, "annotation" ,annotationAdmin.encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(propertyAdmin.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(propertyAdmin.id, property.domainIdent, "annotation", adminUsername, password).code

        //add,update, delete property (superadmin data)
        assert 200 == PropertyAPI.create(annotation.id, "annotation" ,annotation.encodeAsJSON(),adminUsername,password).code
        assert 200 == PropertyAPI.update(property.id, property.domainIdent, "annotation" ,property.encodeAsJSON(), adminUsername,password).code
        assert 200 == PropertyAPI.delete(property.id, property.domainIdent, "annotation", adminUsername, password).code

        //add image instance
        assert 200 == ImageInstanceAPI.create(BasicInstanceBuilder.getImageInstanceNotExist(project,false).encodeAsJSON(),adminUsername, password).code

        //update, delete image instance (simple user data)
        assert 200 == ImageInstanceAPI.update(imageUser.id,imageUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageUser, adminUsername, password).code

        //update, delete image instance (admin data)
        assert 200 == ImageInstanceAPI.update(imageAdmin.id,imageUser.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(imageAdmin, adminUsername, password).code

        //update, delete image instance (superadmin data)
        assert 200 == ImageInstanceAPI.update(image.id,imageUser.encodeAsJSON(), adminUsername, password).code
        assert 200 == ImageInstanceAPI.delete(image, adminUsername, password).code

        println "###"+image.id
        //start reviewing image (simple user data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageUser.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageUser.id,adminUsername, password).code
        //start reviewing image (admin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(imageAdmin.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(imageAdmin.id,adminUsername, password).code
        //start reviewing image (superadmin data)
        assert 200 == ReviewedAnnotationAPI.markStartReview(image.id,adminUsername, password).code
        assert 200 == ReviewedAnnotationAPI.markStopReview(image.id,adminUsername, password).code



        //add annotation
        assert 200 == UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist(project,image,false).encodeAsJSON(),adminUsername, password).code

        //update, delete annotation (simple user data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotationUser.id, jsonAnnotation, adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationUser,simpleUsername, password).code

        //update, delete annotation (admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotationAdmin.id, jsonAnnotation, adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotationAdmin,simpleUsername, password).code

        //update, delete annotation (super admin data)
        jsonAnnotation = JSON.parse(annotation.encodeAsJSON())
        jsonAnnotation.id = "5"
        assert 200 == UserAnnotationAPI.update(annotation.id, jsonAnnotation, adminUsername, password).code
        assert 200 == UserAnnotationAPI.delete(annotation,simpleUsername, password).code


        //add, update, delete description (simple user data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.update(descriptionUser.domainIdent,descriptionUser.domainClassName,descriptionUser.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionUser.domainIdent,descriptionUser.domainClassName,adminUsername, password).code

        //add, update, delete description (admin data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.update(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,descriptionAdmin.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(descriptionAdmin.domainIdent,descriptionAdmin.domainClassName,adminUsername, password).code

        //add, update, delete description (super admin data)
        assert 200 == DescriptionAPI.create(project.id,project.class.name,description.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.update(description.domainIdent,description.domainClassName,description.encodeAsJSON(),adminUsername, password).code
        assert 200 == DescriptionAPI.delete(description.domainIdent,description.domainClassName,adminUsername, password).code

    }


    void testClassicProjectWithJobData() {
        /*
           Init dataset
         */

        def simpleUsername = "simpleUserRO"
        def adminUsername = "adminRO"
        def password = "password"

        //Create a project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //Force project to Read and write
        project.isReadOnly = false
        BasicInstanceBuilder.saveDomain(project)

        //Add a simple project user
        User simpleUser = BasicInstanceBuilder.getUser(simpleUsername,password)
        assert 200 == ProjectAPI.addUserProject(project.id,simpleUser.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        //Add a project admin
        User admin = BasicInstanceBuilder.getUser(adminUsername,password)
        assert 200 == ProjectAPI.addAdminProject(project.id,admin.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD).code

        /*data*/
        JobData jobData = BasicInstanceBuilder.getJobDataNotExist()
        Job job = jobData.job
        Software software = job.software;
        BasicInstanceBuilder.saveDomain(software)
        BasicInstanceBuilder.saveDomain(job)
        BasicInstanceBuilder.saveDomain(jobData)


        /*
           Now Test as simple user
         */

        assert 200 == JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist().encodeAsJSON(),simpleUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), simpleUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, simpleUsername, password).code
        assert 200 == JobDataAPI.delete(jobData.id, simpleUsername, password).code

        assert 200 == JobAPI.create(BasicInstanceBuilder.getJobNotExist().encodeAsJSON(),simpleUsername, password).code
        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), simpleUsername, password).code
        assert 200 == JobAPI.delete(job.id, simpleUsername, password).code

        assert 200 == SoftwareAPI.create(BasicInstanceBuilder.getSoftwareNotExist().encodeAsJSON(),simpleUsername, password).code
        assert 200 == SoftwareAPI.update(software.id, software.encodeAsJSON(), simpleUsername, password).code
        assert 200 == SoftwareAPI.delete(software.id, simpleUsername, password).code


        /*
          Now run test as a project admin
         */

        assert 200 == JobDataAPI.create(BasicInstanceBuilder.getJobDataNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == JobDataAPI.update(jobData.id, jobData.encodeAsJSON(), adminUsername, password).code
        assert 200 == JobDataAPI.download(jobData.id, adminUsername, password).code
        assert 200 == JobDataAPI.delete(jobData.id, adminUsername, password).code

        assert 200 == JobAPI.create(BasicInstanceBuilder.getJobNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == JobAPI.update(job.id, job.encodeAsJSON(), adminUsername, password).code
        assert 200 == JobAPI.delete(job.id, adminUsername, password).code

        assert 200 == SoftwareAPI.create(BasicInstanceBuilder.getSoftwareNotExist().encodeAsJSON(),adminUsername, password).code
        assert 200 == SoftwareAPI.update(software.id, software.encodeAsJSON(), adminUsername, password).code
        assert 200 == SoftwareAPI.delete(software.id, adminUsername, password).code

    }

}
