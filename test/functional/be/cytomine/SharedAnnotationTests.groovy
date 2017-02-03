package be.cytomine

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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationCommentAPI
import be.cytomine.test.http.AnnotationDomainAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 8/02/11
 * Time: 9:01
 * To change this template use File | Settings | File Templates.
 */
class SharedAnnotationTests  {

    void testGetAnnotationCommentWithCredential() {
        //userAnnotation
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotation()
        def result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, sharedAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject

        //algoAnnotation
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        AnnotationDomain annotation = BasicInstanceBuilder.getAlgoAnnotation();
        sharedAnnotation.annotationClassName = annotation.class.name
        sharedAnnotation.annotationIdent = annotation.id
        BasicInstanceBuilder.saveDomain(sharedAnnotation)

        result = AnnotationCommentAPI.show(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, sharedAnnotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject

        // not existing annotation
        result = AnnotationCommentAPI.show(-99, sharedAnnotation.annotationClassName, -99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

    }

    void testListAnnotationCommentsByAnnotationWithCredential() {
        //userAnnotation
        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotation()
        def result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser(Infos.ADMINLOGIN, Infos.ADMINPASSWORD).id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2

        result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.ADMINLOGIN, Infos.ADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2

        //algoAnnotation
        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotation()
        AnnotationDomain annotation = BasicInstanceBuilder.getAlgoAnnotation();
        sharedAnnotation.annotationClassName = annotation.class.name
        sharedAnnotation.annotationIdent = annotation.id
        BasicInstanceBuilder.saveDomain(sharedAnnotation)

        result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        // not existing annotation
        result = AnnotationCommentAPI.list(-99, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

    }

    void testAddAnnotationComments() {


        def sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        def json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        def result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        result = AnnotationDomainAPI.show(sharedAnnotation.annotationIdent, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert(json.nbComments == 1)

        sharedAnnotation = BasicInstanceBuilder.getSharedAnnotationNotExist()
        AnnotationDomain annotation = BasicInstanceBuilder.getAlgoAnnotation();
        sharedAnnotation.annotationClassName = annotation.class.name
        sharedAnnotation.annotationIdent = annotation.id

        json = JSON.parse((String)sharedAnnotation.encodeAsJSON())
        json.subject = "subject for test mail"
        json.message = "message for test mail"
        json.users = [BasicInstanceBuilder.getUser1().id]
        result = AnnotationCommentAPI.create(sharedAnnotation.annotationIdent,sharedAnnotation.annotationClassName,json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationCommentAPI.list(sharedAnnotation.annotationIdent, sharedAnnotation.annotationClassName, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 1

        result = AnnotationDomainAPI.show(sharedAnnotation.annotationIdent, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert(json.nbComments == 1)
    }
}
