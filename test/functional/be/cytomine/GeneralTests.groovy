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

import be.cytomine.command.Command
import be.cytomine.command.CommandHistory
import be.cytomine.command.RedoStackItem
import be.cytomine.command.UndoStackItem
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.HttpClient
import be.cytomine.test.Infos
import be.cytomine.test.http.CommandAPI
import be.cytomine.test.http.DomainAPI
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UploadedFileAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.utils.News
import be.cytomine.utils.Version
import grails.converters.JSON
import grails.util.Environment
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.json.JSONArray

import java.text.SimpleDateFormat

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class GeneralTests  {

    def aclService
    def aclUtilService
    def objectIdentityRetrievalStrategy
    def sessionFactory
    def springSecurityService

    void testCommandMaxSizeOK() {
        log.info("create image")
        String jsonImage = "{\"text\" : \"*************************************************************************"
        String textAdded = "***************************************************************************************"
        jsonImage = jsonImage + "\"}"

        log.info("post with data size:" + jsonImage.size())
        String URL = Infos.CYTOMINEURL + "api/image.json"
        HttpClient client = new HttpClient()
        client.connect(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        client.post(jsonImage)
        int code = client.getResponseCode()
        String response = client.getResponseData()
        println response
        client.disconnect();

        log.info("check response")
        assert code != 413
        def json = JSON.parse(response)
    }

    void testLastAction() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()

        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        // Get the last 3 commands: it must be "REDO ADD ANNOTATION", "UNDO ADD ANNOTATION" and "ADD ANNOTATION"

        Long idProject = annotationToAdd.image.project.id
        Integer max = 3
        HttpClient client = new HttpClient();
        String url = Infos.CYTOMINEURL + "api/project/" + idProject + "/last/" + max + ".json"
        client.connect(url, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
        client.get()
        int code = client.getResponseCode()
        String response = client.getResponseData()
        client.disconnect();
        assert 200 == code
        def json = JSON.parse(response)
        assert json.collection instanceof JSONArray

    }

    void testLastActionProjectNotExist() {
        HttpClient client = new HttpClient();
        String url = Infos.CYTOMINEURL + "api/project/-99/last/10.json"
        client.connect(url, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
        client.get()
        int code = client.getResponseCode()
        client.disconnect();
        assert 404 == code
    }

    void testMultipleAuthConnexion() {
        BasicInstanceBuilder.getUserAnnotation()
        UserAnnotation annotation = UserAnnotation.findByDeletedIsNull()
        User user = BasicInstanceBuilder.getUser1()
        Infos.addUserRight(user.username,annotation.project)

        log.info "show userannotation " + annotation.id
        String URL = Infos.CYTOMINEURL + "api/userannotation/" + annotation.id + ".json"
        HttpClient client1 = new HttpClient();
        client1.connect(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
        client1.get()
        int code = client1.getResponseCode()
        String response = client1.getResponseData()
        assert code == 200

        HttpClient client2 = new HttpClient();
        client2.connect(URL, user.username, "password");
        client2.get()
        code = client2.getResponseCode()
        assert code == 200
        client1.disconnect()

        client1.connect(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        client1.get()
        code = client1.getResponseCode()
        assert code == 200

        client2.disconnect();
        client2.connect(URL, user.username,  "password");
        client2.get()
        code = client2.getResponseCode()
        assert code == 200

        client1.disconnect();
        client2.disconnect();
    }

    void testPing() {

        def result = ProjectAPI.doPing(BasicInstanceBuilder.getProject().id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListDeletedDomains() {
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(BasicInstanceBuilder.getProject(), true)
        def uploadedFile = BasicInstanceBuilder.getUploadedFileNotExist(true)

        def result = CommandAPI.listDeletedDomain(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, "uploadedFile")
        assert 200 == result.code
        int nbDomains = JSON.parse(result.data).collection.size()

        result = CommandAPI.listAllDeleted(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int total = JSON.parse(result.data).collection.size()

        UserAnnotationAPI.delete(annotation.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        UploadedFileAPI.delete(uploadedFile.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = CommandAPI.listDeletedDomain(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, "uploadedFile")
        assert 200 == result.code
        assert JSON.parse(result.data).collection.size() == nbDomains+1

        result = CommandAPI.listAllDeleted(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        //TODO fix later into the CommandController
        assert JSON.parse(result.data).collection.size() == total+1
    }



     void testNewsListing() {
         def data = [
                 [date:'12/08/2013', text:'A project has now a user and admin list. A project admin is able to edit annotations from other user. Furthermore, a project admin is not affected by the "private layer" options. Only project creator and project admin can raise a user as project admin.'],
                 [date:'27/07/2013',text:'Project can be locked. If a project is locked, you can delete all job data with no reviewed annotation.'],
                 [date:'14/06/2013',text:'Project, Image and Annotation can now have a description.'],
                 [date:'27/05/2013',text:'Review view is now available in project. This helps meet specific needs especially for Cytology review.'],
                 [date: '08/05/2013',text:'You can now use keyboard shortcuts to perform some actions. Look at the "Help" section on the top of this windows.']

         ]

         data.each {

             News news = new News(added:new SimpleDateFormat("dd/MM/yyyy").parse(it.date),text:it.text, user: User.findByEnabled(true))
             assert news.validate()
             println news.errors
             assert news.save(flush:true)
         }


         HttpClient client1 = new HttpClient();
         String URL = Infos.CYTOMINEURL + "api/news.json"
         client1.connect(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD);
         client1.get()
         int code = client1.getResponseCode()
         String response = client1.getResponseData()
         assert code == 200
         client1.disconnect()
         def json = JSON.parse(response).collection
         assert json.size()==5


     }

    void testArchiveCommand() {

         CommandHistory.list().each {it.delete()}
         Command.list().each {
             UndoStackItem.findAllByCommand(it).each {it.delete()}
             RedoStackItem.findAllByCommand(it).each {it.delete()}
             it.delete(flush: true)
         }

         def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
         def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
         result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

         assert Command.list().size()==3
         def histories = CommandHistory.list()
         assert histories.size()==3

         histories[0].created = new SimpleDateFormat("yyyy-MM-dd").parse("2012-12-05")
         histories[0].command.created = new SimpleDateFormat("yyyy-MM-dd").parse("2012-12-05")
         histories[1].created = new SimpleDateFormat("yyyy-MM-dd").parse("2013-04-12")
         histories[1].command.created = new SimpleDateFormat("yyyy-MM-dd").parse("2013-04-12")
         histories[2].created = new SimpleDateFormat("yyyy-MM-dd").parse("2013-04-12")
         histories[2].command.created = new SimpleDateFormat("yyyy-MM-dd").parse("2013-04-12")

         histories.each {
             BasicInstanceBuilder.saveDomain(it)
             BasicInstanceBuilder.saveDomain(it.command)
         }

         def ids = CommandHistory.list().collect{it.command.id+""}


         FileUtils.deleteDirectory(new File("oldcommand/${Environment.getCurrent()}"));

         assert !new File("oldcommand/${Environment.getCurrent()}").exists()

 //        ArchiveCommandService archive = new ArchiveCommandService()
 //        archive.archiveOldCommand()
         DomainAPI.doGET(Infos.CYTOMINEURL+"archive/archive.json",Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)



         assert new File("oldcommand/${Environment.getCurrent()}").exists()
         def today = new Date()
         def firstFile = new File("oldcommand/${Environment.getCurrent()}/${today.year}-${today.month+1}-${today.date}.log")

         assert firstFile.exists()

         def content1 = firstFile.text.split("\n")

         assert content1.size()==3

         assert ids.contains(content1[0].split(";")[0])
         assert ids.contains(content1[1].split(";")[0])
         assert ids.contains(content1[2].split(";")[0])
     }

    void testCharEncoding() {
        String name = "A PROJECT ./ SPECIAL CHAR"
        Project project = BasicInstanceBuilder.getProjectNotExist(false)
        project.name = name

        assert JSON.parse(project.encodeAsJSON()).name == name

        String URL = Infos.CYTOMINEURL + "api/project.json"
        def result = ProjectAPI.doPOST(URL,project.encodeAsJSON(),Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).project.name == name

        result = ProjectAPI.show(JSON.parse(result.data).project.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert JSON.parse(result.data).name == name

    }

    void testVersion() {
        Version.setCurrentVersion("1.0.0")
        Version.setCurrentVersion("1.1.0")
        Version.setCurrentVersion("27.3.0")
        assert 27==Version.getLastVersion().major
        assert 3==Version.getLastVersion().minor
        assert 0==Version.getLastVersion().patch
        assert Version.isOlderVersion(new Version(major: 27, minor: 3, patch: 1))
        assert !Version.isOlderVersion(new Version(major: 27, minor: 0, patch: 0))
        assert !Version.isOlderVersion(new Version(major: 2, minor: 185, patch: 200))
    }
}
