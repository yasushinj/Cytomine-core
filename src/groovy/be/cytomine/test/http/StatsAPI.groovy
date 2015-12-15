package be.cytomine.test.http

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

import be.cytomine.image.server.Storage
import be.cytomine.test.Infos
import grails.converters.JSON

class StatsAPI  extends DomainAPI {

    static def statTerm(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/term.json"
        return doGET(URL, username, password)
    }

    static def statUser(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/user.json"
        return doGET(URL, username, password)
    }

    static def statTermSlide(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/termslide.json"
        return doGET(URL, username, password)
    }

    static def statUserSlide(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/userslide.json"
        return doGET(URL, username, password)
    }

    static def statUserAnnotations(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/userannotations.json"
        return doGET(URL, username, password)
    }

    static def statAnnotationEvolution(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/project/" + id + "/stats/annotationevolution.json"
        return doGET(URL, username, password)
    }

    static def statAnnotationTermedByProject(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/term/" + id + "/project/stat.json"
            return doGET(URL, username, password)
    }

    static def totalProjects(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/total/project.json"
        return doGET(URL, username, password)
    }

    static def totalUsers(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/total/user.json"
        return doGET(URL, username, password)
    }

}