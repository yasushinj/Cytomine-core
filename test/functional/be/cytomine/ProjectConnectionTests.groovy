package be.cytomine

/*
* Copyright (c) 2009-2015. Authors: see NOTICE file.
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
import be.cytomine.test.http.ProjectAPI
import be.cytomine.test.http.UserAPI
import be.cytomine.test.http.ProjectConnectionAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class ProjectConnectionTests {


    void testAddConnection() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.getConnectionByUserAndProject(BasicInstanceBuilder.user1.id, project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectConnectionAPI.lastConnectionInProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectConnectionAPI.numberOfConnectionsByProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = ProjectConnectionAPI.numberOfConnectionsByProjectAndUser(project.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }



    /*void testPerf() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def user;
        String username = "user"
        for(int i=0;i<10000;i++){
            user = BasicInstanceBuilder.getUser(username+i, username+i)
            ProjectAPI.addUserProject(project.id,user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
            result = ProjectConnectionAPI.create(project.id, json.toString(),username+i, username+i)
            assert 200 == result.code
        }

        def begin = System.currentTimeMillis()
        result = UserAPI.listUsersWithLastActivity(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        assert 200 == result.code
        assert json.collection instanceof JSONArray
        println "collection.size()"
        println json.collection.size()
        def end = System.currentTimeMillis()
        println "ellapse time : "+(end-begin)
        assert false
    }*/

    void testGetConnectionByUserAndProject() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.getConnectionByUserAndProject(BasicInstanceBuilder.user1.id,project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testLastConnectionInProject() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.lastConnectionInProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testNumberOfConnectionsByProject() {
        def project = BasicInstanceBuilder.getProject()
        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.numberOfConnectionsByProject(project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 200 == result.code
    }



    void testNumberOfConnectionsByUserAndProject() {
        def project = BasicInstanceBuilder.getProject()

        def json = JSON.parse("{project:${project.id}}");

        def result = ProjectConnectionAPI.create(project.id, json.toString(),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = ProjectConnectionAPI.numberOfConnectionsByProjectAndUser(project.id, BasicInstanceBuilder.user1.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


}
