package be.cytomine

import be.cytomine.test.Infos
import be.cytomine.test.http.RetrievalAPI

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

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 */
class RetrievalTests  {


    //Mock doesn't work with bamboo sometimes...

//    void testRetrievalResult() {
//        def project = BasicInstanceBuilder.getProject()
//        def annotation1 = BasicInstanceBuilder.getUserAnnotationNotExist(true,project)
//        def annotation2 = BasicInstanceBuilder.getUserAnnotationNotExist(true,project)
//        def annotation3 = BasicInstanceBuilder.getUserAnnotationNotExist(true,project)
//
//        //mock retrieval response with empty JSON
//        RetrievalHttpUtils.metaClass.'static'.getPostSearchResponse  = {String URL, String resource, AnnotationDomain annotation, String urlAnnotation, List<Long> projectsSearch-> // stuff }
//            println "getPostSearchResponse mocked"
//            def a1 = '{"id":'+annotation1.id+',"sim":0.10}'
//            def a2 = '{"id":'+annotation2.id+',"sim":0.05}'
//            def a3 = '{"id":'+annotation3.id+',"sim":0.02}'
//            return "[$a1,$a2,$a3]"
//        }
//
//        def result = RetrievalAPI.getResults(annotation1.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        println result.data
//        def json = JSON.parse(result.data)
//        assertEquals(3-1,json.annotation.size())
//    }
//
//    void testRetrievalNoResult() {
//        //mock retrieval response with empty JSON
//        RetrievalHttpUtils.metaClass.'static'.getPostSearchResponse  = {String URL, String resource, AnnotationDomain annotation, String urlAnnotation, List<Long> projectsSearch-> // stuff }
//            println "getPostSearchResponse mocked"
//            return "[]"
//        }
//        def annotation = BasicInstanceBuilder.getUserAnnotation()
//        def result = RetrievalAPI.getResults(annotation.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//        assert 200 == result.code
//        println result.data
//        def json = JSON.parse(result.data)
//        assertEquals(0,json.annotation.size())
//    }

    void testRetrievalWithAnnotationNotExist() {
        def result = RetrievalAPI.getResults(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }
}
