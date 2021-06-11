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

import be.cytomine.image.ImageInstance
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.Ontology
import be.cytomine.ontology.UserAnnotation
import be.cytomine.processing.Software
import be.cytomine.processing.SoftwareProject
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.DomainAPI
import be.cytomine.test.http.ImageInstanceAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.TaskAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class ProjectTests  {

    void testListProjectWithCredential() {
        def result = ProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        int length = json.collection.length()

        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.length() == length+1
    }

    void testListProjectWithExtendedDataWithCredential() {
        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Long id = result.data.id

        result = ProjectAPI.list(true, true, true, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        def proj = json.collection.find{it.id == id}
        assert proj.lastActivity > proj.created
        assert proj.membersCount == 1
        assert proj.currentUserRoles != null
        assert proj.currentUserRoles.admin
    }

    void testListProjectWithoutCredential() {
        def result = ProjectAPI.list(Infos.BADLOGIN, Infos.BADPASSWORD)
        assert 401 == result.code
    }

    void testShowProjectWithCredential() {
        Project project = BasicInstanceBuilder.getProject()
        def result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.name == project.name
    }

    void testListProjectByUser() {
        Project project = BasicInstanceBuilder.getProject()
        User user = BasicInstanceBuilder.getUser()
        def result = ProjectAPI.listByUser(user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListProjectByUserLight() {

        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Project project = result.data
        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def user1 = User.findByUsername(Infos.SUPERADMINLOGIN)
        def user2 = BasicInstanceBuilder.getUser2()

        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'creator',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user2.id,'creator',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'admin',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user2.id,'admin',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'user',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user2.id,'user',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

    }






    void testListProjectByUserNotExist() {
        def result = ProjectAPI.listByUser(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testListProjectByOntology() {
        Ontology ontology = BasicInstanceBuilder.getOntology()
        def result = ProjectAPI.listByOntology(ontology.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListProjectByOntologyNotExist() {
        def result = ProjectAPI.listByOntology(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListProjectBySoftware() {
        Software software = BasicInstanceBuilder.getSoftware()
        User user = BasicInstanceBuilder.getUser()
        def result = ProjectAPI.listBySoftware(software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListProjectBySoftwareNotExist() {
        def result = ProjectAPI.listBySoftware(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testAddProjectCorrect() {
        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Project project = result.data
        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert ProjectAPI.containsInJSONList(User.findByUsername(Infos.SUPERADMINLOGIN).id,JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data))
    }

    void testAddProjectWithoutOntology() {
        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        projectToAdd.ontology = null
        def result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Project project = result.data
        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert ProjectAPI.containsInJSONList(User.findByUsername(Infos.SUPERADMINLOGIN).id,JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data))
    }

    void testAddProjectWithUser() {
        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        def user =  BasicInstanceBuilder.getUser()
        def json = JSON.parse(projectToAdd.encodeAsJSON())
        json.users = [user.id]
        json.admins = [user.id]
        def result = ProjectAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Project project = result.data
        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert ProjectAPI.containsInJSONList(User.findByUsername(Infos.SUPERADMINLOGIN).id,JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data))
        assert ProjectAPI.containsInJSONList(user.id,JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data))
        assert ProjectAPI.containsInJSONList(user.id,JSON.parse(ProjectAPI.listUser(project.id,"user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data))
    }



    void testAddProjectWithNameAlreadyExist() {
        def projectToAdd = BasicInstanceBuilder.getProject()
        String jsonProject = projectToAdd.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonProject)
        def result = ProjectAPI.create(jsonUpdate.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testEditProjectCorrect() {

        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def data = UpdateData.createUpdateSet(project,[name: ["OLDNAME","NEWNAME"]])

        def result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idProject = json.project.id
        def showResult = ProjectAPI.show(idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testEditProjectCorrectWithUser() {
        def creator = User.findByUsername(Infos.SUPERADMINLOGIN)
        def user1 =  BasicInstanceBuilder.getUserNotExist(true)
        def user2 =  BasicInstanceBuilder.getUserNotExist(true)
        def user3 =  BasicInstanceBuilder.getUserNotExist(true)
        def user4 =  BasicInstanceBuilder.getUserNotExist(true)
        println "creator=${creator.id}"
        println "user1=${user1.id}"
        println "user2=${user2.id}"
        println "user3=${user3.id}"
        println "user4=${user4.id}"

        def project = BasicInstanceBuilder.getProjectNotExist(false)

        def json = JSON.parse(project.encodeAsJSON())
        json.users = [creator.id,user2.id,user4.id]
        json.admins = [creator.id,user1.id]

        def result = ProjectAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        project = result.data
        Infos.printRight(project)
        def usersList = JSON.parse(ProjectAPI.listUser(project.id,"user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        def adminsList = JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        assert ProjectAPI.containsInJSONList(creator.id,usersList)
        assert ProjectAPI.containsInJSONList(creator.id,adminsList)
        assert ProjectAPI.containsInJSONList(user1.id,usersList)
        assert ProjectAPI.containsInJSONList(user1.id,adminsList)
        assert ProjectAPI.containsInJSONList(user2.id,usersList)
        assert ProjectAPI.containsInJSONList(user4.id,usersList)



        json = JSON.parse(project.encodeAsJSON())
        json.users = [creator.id,user3.id,user4.id,user1.id]
        json.admins = [creator.id,user3.id]

        result = ProjectAPI.update(project.id,json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        usersList = JSON.parse(ProjectAPI.listUser(project.id,"user",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        adminsList = JSON.parse(ProjectAPI.listUser(project.id,"admin",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        assert ProjectAPI.containsInJSONList(creator.id,usersList)
        assert ProjectAPI.containsInJSONList(creator.id,adminsList)
        Infos.printRight(project)
        assert ProjectAPI.containsInJSONList(user1.id,usersList)
        assert !ProjectAPI.containsInJSONList(user1.id,adminsList)
        assert !ProjectAPI.containsInJSONList(user2.id,usersList)
        assert ProjectAPI.containsInJSONList(user3.id,adminsList)
        assert ProjectAPI.containsInJSONList(user4.id,usersList)

    }

    void testEditProjectWithNameAlreadyExist() {
        Project projectWithOldName = BasicInstanceBuilder.getProject()
        Project projectWithNewName = BasicInstanceBuilder.getProjectNotExist(true)
//        projectWithNewName.save(flush: true)

        Project projectToEdit = Project.get(projectWithNewName.id)
        def jsonProject = projectToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonProject)
        jsonUpdate.name = projectWithOldName.name
        jsonProject = jsonUpdate.toString()
        def result = ProjectAPI.update(projectToEdit.id, jsonProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testEditProjectNotExist() {
        Project projectWithOldName = BasicInstanceBuilder.getProject()
        Project projectWithNewName = BasicInstanceBuilder.getProjectNotExist()
        projectWithNewName.save(flush: true)
        Project projectToEdit = Project.get(projectWithNewName.id)
        def jsonProject = projectToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonProject)
        jsonUpdate.name = projectWithOldName.name
        jsonUpdate.id = -99
        jsonProject = jsonUpdate.toString()
        def result = ProjectAPI.update(-99, jsonProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testEditProjectOntologyCorrect() {

        def project = BasicInstanceBuilder.getProjectNotExist(true)

        def data = UpdateData.createUpdateSet(project,[ontology: [project.ontology,BasicInstanceBuilder.getOntologyNotExist(true)]])

        def result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idProject = json.project.id
        def showResult = ProjectAPI.show(idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)


        BasicInstanceBuilder.compare(data.mapNew, json)


        def term = BasicInstanceBuilder.getTermNotExist(project.ontology, true)
        def at = BasicInstanceBuilder.getAnnotationTermNotExist(BasicInstanceBuilder.getUserAnnotationNotExist(project,true), term, true)
        def aat = BasicInstanceBuilder.getAlgoAnnotationTermNotExist(at.userAnnotation, term, true)
        def rat = BasicInstanceBuilder.getReviewedAnnotationNotExist(project)
        rat.terms = [term]
        rat.save()


        data = UpdateData.createUpdateSet(project,[ontology: [project.ontology,BasicInstanceBuilder.getOntologyNotExist(true)]])

        result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code

        json = JSON.parse(result.data)

        assert json.errors.contains("3 associated terms")

        data.postData = JSON.parse(data.postData)
        data.postData.forceOntologyUpdate = true
        data.postData = data.postData.toString()

        result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code

        json = JSON.parse(result.data)

        assert json.errors.contains("2 associated terms")

    }

    void testEditProjectOntologyAndDeletingTerms() {

        def project = BasicInstanceBuilder.getProjectNotExist(true)
        def term = BasicInstanceBuilder.getTermNotExist(project.ontology, true)
        def at = BasicInstanceBuilder.getAnnotationTermNotExist(BasicInstanceBuilder.getUserAnnotationNotExist(project,true), term, true)

        def data = UpdateData.createUpdateSet(project,[ontology: [project.ontology,BasicInstanceBuilder.getOntologyNotExist(true)]])

        data.postData = JSON.parse(data.postData)
        data.postData.forceOntologyUpdate = true
        data.postData = data.postData.toString()

        def result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idProject = json.project.id
        def showResult = ProjectAPI.show(idProject, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)


        BasicInstanceBuilder.compare(data.mapNew, json)


        def aat = BasicInstanceBuilder.getAlgoAnnotationTermNotExist(at.userAnnotation, term, true)
        def rat = BasicInstanceBuilder.getReviewedAnnotationNotExist(project)
        rat.terms = [term]
        rat.save()


        data = UpdateData.createUpdateSet(project,[ontology: [project.ontology,BasicInstanceBuilder.getOntologyNotExist(true)]])

        data.postData = JSON.parse(data.postData)
        data.postData.forceOntologyUpdate = true
        data.postData = data.postData.toString()

        result = ProjectAPI.update(project.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 403 == result.code

        json = JSON.parse(result.data)

        assert json.errors.contains("2 associated terms")
        assert !json.errors.contains("project members")

    }

    void testDeleteProject() {
        def projectToDelete = BasicInstanceBuilder.getProjectNotExist(true)

        def result = TaskAPI.create(projectToDelete.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def jsonTask = JSON.parse(result.data)

        //delete all job data
        result = ProjectAPI.delete(projectToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,jsonTask.task.id)
        assert 200 == result.code


    }

    void testDeleteProjectWithTask() {
        def projectToDelete = BasicInstanceBuilder.getProjectNotExist(true)

        def result = ProjectAPI.delete(projectToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def showResult = ProjectAPI.show(projectToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }

    void testDeleteProjectNotExist() {
        def result = ProjectAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testProjectCounterUserAnnotationCounter() {
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist()
        BasicInstanceBuilder.saveDomain(project)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //check if 0 algo annotation
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        //add algo annotation
        UserAnnotation a1 = BasicInstanceBuilder.getUserAnnotationNotExist()
        a1.image = image
        a1.project = project
        BasicInstanceBuilder.saveDomain(a1)

        project.refresh()
        image.refresh()

        //check if 1 algo annotation
        assert project.countAnnotations == 1
        assert image.countImageAnnotations == 1
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        //add algo annotation
        UserAnnotation a2 = BasicInstanceBuilder.getUserAnnotationNotExist()
        a2.image = image
        a2.project = project
        BasicInstanceBuilder.saveDomain(a2)

        project.refresh()
        image.refresh()

        //check if 2 algo annotation
        assert project.countAnnotations == 2
        assert image.countImageAnnotations == 2
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        //remove algo annotation
        a1.delete(flush: true)


        project.refresh()
        image.refresh()

        //check if 1 algo annotation
        assert project.countAnnotations == 1
        assert image.countImageAnnotations == 1
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        //remove algo annotation
        a2.delete(flush: true)

        project.refresh()
        image.refresh()

        //check if 1 algo annotation
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
    }



    void testProjectCounterAlgoAnnotationCounter() {
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist()
        BasicInstanceBuilder.saveDomain(project)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        //check if 0 algo annotation
        project.refresh()
        image.refresh()
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0

        //add algo annotation
        AlgoAnnotation a1 = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        a1.image = image
        a1.project = project
        BasicInstanceBuilder.saveDomain(a1)

        //check if 1 algo annotation
        project.refresh()
        image.refresh()
        assert project.countJobAnnotations == 1
        assert image.countImageJobAnnotations == 1
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0

        //add algo annotation
        AlgoAnnotation a2 = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        a2.image = image
        a2.project = project
        BasicInstanceBuilder.saveDomain(a2)

        //check if 2 algo annotation
        project.refresh()
        image.refresh()
        assert project.countJobAnnotations == 2
        assert image.countImageJobAnnotations == 2
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0

        //remove algo annotation
        a1.delete(flush: true)

        //check if 1 algo annotation
        project.refresh()
        image.refresh()
        assert project.countJobAnnotations == 1
        assert image.countImageJobAnnotations == 1
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0

        //remove algo annotation
        a2.delete(flush: true)

        //check if 1 algo annotation
        project.refresh()
        image.refresh()
        assert project.countJobAnnotations == 0
        assert image.countImageJobAnnotations == 0
        assert project.countAnnotations == 0
        assert image.countImageAnnotations == 0
    }


    void testProjectCounterImageInstanceCounter() {
        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        assert project.countImageInstance() == 0

        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        BasicInstanceBuilder.saveDomain(image)

        project.refresh()
        assert project.countImageInstance() == 1

        image.delete(flush: true)

        project.refresh()
        assert project.countImageInstance() == 0

    }

    void testLastOpened() {
        Project project1 = BasicInstanceBuilder.getProjectNotExist(true)
        Project project2 = BasicInstanceBuilder.getProjectNotExist(true)

        def result = ProjectAPI.doPing(project1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectAPI.doPing(project2.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectAPI.listLastOpened(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = ProjectAPI.listLastOpened(20,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

    }

    void testListCommandHistory() {
        def result = ProjectAPI.listCommandHistory(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testListCommandHistoryByProject() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        annotationToAdd.project = BasicInstanceBuilder.getProjectNotExist(true)
        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectAPI.listCommandHistory(annotationToAdd.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testListCommandHistoryByProjectWithDates() {
        Date startDate = new Date()
        def result = ProjectAPI.listCommandHistory(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONArray
    }

    void testDeleteProjectAndRestoreIt() {

        //Create a project
        def projectToAdd = BasicInstanceBuilder.getProjectNotExist()
        def result = ProjectAPI.create(projectToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Project project = result.data

        def user1 = User.findByUsername(Infos.SUPERADMINLOGIN)

        //check if project is there
       result = ProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectAPI.listByUser(user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'creator',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'admin',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'user',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        result = ProjectAPI.listByOntology(projectToAdd.getOntology().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        SoftwareProject softproj = BasicInstanceBuilder.getSoftwareProjectNotExist(BasicInstanceBuilder.getSoftware(),project, true)
        result = ProjectAPI.listBySoftware(softproj.software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))


        //delete project
        Date start = new Date()
        result = ProjectAPI.delete(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        Date stop = new Date()

        project.refresh()
        assert project.deleted > start
        assert project.deleted < stop

        //check if project is not there
        result = ProjectAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        result = ProjectAPI.show(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = ProjectAPI.listByUser(user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'creator',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'admin',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)
        assert !ProjectAPI.containsInJSONList(project.id,ProjectAPI.listByUserLight(user1.id,'user',Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).data)

        result = ProjectAPI.listByOntology(projectToAdd.getOntology().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))

        result = ProjectAPI.listBySoftware(softproj.software.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !DomainAPI.containsInJSONList(project.id,JSON.parse(result.data))
    }

    void testImageNamesOfBlindProject() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)

        def result = ImageInstanceAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].instanceFilename == image.instanceFilename
        assert json.collection[0].blindedName instanceof JSONObject.Null

        result = ImageInstanceAPI.show(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.instanceFilename == image.instanceFilename
        assert json.blindedName instanceof JSONObject.Null

        project.blindMode = true
        project.save(true)

        result = ImageInstanceAPI.listByProject(project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].instanceFilename == image.instanceFilename
        assert !(json.collection[0].blindedName instanceof JSONObject.Null)

        result = ImageInstanceAPI.show(image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.instanceFilename == image.instanceFilename
        assert !(json.blindedName instanceof JSONObject.Null)


        User user = BasicInstanceBuilder.getUser()

        assert (200 ==ProjectAPI.addUserProject(project.id, user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD).code)

        result = ImageInstanceAPI.listByProject(project.id, user.username, "password")
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection[0].instanceFilename instanceof JSONObject.Null
        assert !(json.collection[0].blindedName instanceof JSONObject.Null)

        result = ImageInstanceAPI.show(image.id, user.username, "password")
        assert 200 == result.code
        json = JSON.parse(result.data)

        assert json.instanceFilename instanceof JSONObject.Null
        assert !(json.blindedName instanceof JSONObject.Null)
    }

}
