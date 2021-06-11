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
import be.cytomine.ontology.*
import be.cytomine.processing.Job
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AnnotationDomainAPI
import be.cytomine.test.http.AnnotationTermAPI
import be.cytomine.test.http.DomainAPI
import be.cytomine.test.http.UserAnnotationAPI
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 16/03/11
 * Time: 16:12
 * To change this template use File | Settings | File Templates.
 */
class UserAnnotationListingTests {


    void testListAnnotationPropertyShow() {

        def dataSet = createAnnotationSet()

        def result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,null)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        println json
        println json.collection.get(0)
        checkForProperties(json.collection.get(0),['id','term','created','project','image'])

        def expectedProp = ['showBasic', 'showWKT']
        println "expectedProp=$expectedProp"
        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
        json = (JSON.parse(result.data))
        println "x=" + json
        println "x=" + json.collection

        println  json.collection
        checkForProperties(json.collection.get(0),['id',"location"],['term','created','area','project'])

        expectedProp = ['showDefault', 'hideMeta']
        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
        json = (JSON.parse(result.data))
        checkForProperties(json.collection.get(0),['id','term'],['location','created','project'])

        expectedProp = ['showBasic', 'showImage']
        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
        json = (JSON.parse(result.data))
        checkForProperties(json.collection.get(0),['id','originalfilename'],['term','location'])

        expectedProp = ['showWKT', 'hideWKT','hideBasic','hideMeta']
        result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,expectedProp)
        assert 404 == result.code
    }




  void testListAnnotationSearchByImage() {

      def dataSet = createAnnotationSet()

      def result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert 200 == result.code
      def json = JSON.parse(result.data)
      assert json.collection instanceof JSONArray
      assert json.collection.size()==dataSet.annotations.size()
      //generic way test
      checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size())

      dataSet.annotations[2].image = BasicInstanceBuilder.getImageInstanceNotExist( dataSet.project,true)
      BasicInstanceBuilder.saveDomain(dataSet.annotations[2])

      result = UserAnnotationAPI.listByImage(dataSet.image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      assert JSON.parse(result.data).collection instanceof JSONArray
      assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() -1
       //generic way test
      checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size()-1)

      UserAnnotationAPI.delete(dataSet.annotations[1].id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
      checkUserAnnotationResultNumber("image=${dataSet.image.id}",dataSet.annotations.size()-2)
  }

    void testListAnnotationSearchByMultipleTerm() {

        def dataSet = createAnnotationSet()

        def result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==0
        //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",0)

        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist(dataSet.annotations[2],true)
        BasicInstanceBuilder.saveDomain(at)

        result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection instanceof JSONArray
        assert JSON.parse(result.data).collection.size()==1
         //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",1)

        result = AnnotationTermAPI.deleteAnnotationTerm(at.userAnnotation.id,at.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection instanceof JSONArray
        assert JSON.parse(result.data).collection.size()==0
        //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&multipleTerm=true&project=${dataSet.project.id}",0)
    }


    void testListAnnotationSearchByNoTerm() {
        def dataSet = createAnnotationSet()
        def result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==1
         //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&noTerm=true&project=${dataSet.project.id}",1)

        result = AnnotationTermAPI.deleteAnnotationTerm(dataSet.annotations[0].id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==2
        //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&noTerm=true&project=${dataSet.project.id}",2)
    }

    void testListAnnotationSearchByProjectTerm() {
        def dataSet = createAnnotationSet()
        def result = UserAnnotationAPI.listByProjectAndUsers(dataSet.project.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==dataSet.annotations.size()
         //generic way test
        checkUserAnnotationResultNumber("user=${dataSet.user.id}&project=${dataSet.project.id}",dataSet.annotations.size())

    }



    void testListAnnotationSearchByMaxDistance() {
        def dataSet = createAnnotationSet()
        dataSet.annotations[0].location = new WKTReader().read("POINT(0 0)") //base point
        dataSet.annotations[1].location = new WKTReader().read("POINT(-10 0)") //should be < 11
        dataSet.annotations[2].location = new WKTReader().read("POLYGON((10 0,15 10,15 15,10 15,10 0))") //should be < 11
        dataSet.annotations[3].location = new WKTReader().read( "POINT(20 20)") //should be > 11

        dataSet.annotations.each {
            BasicInstanceBuilder.saveDomain(it)
        }

        checkUserAnnotationResults("project=${dataSet.project.id}&baseAnnotation="+dataSet.annotations[0].location.toText().replace(" ","%20")+"&maxDistanceBaseAnnotation=11",dataSet.annotations.subList(0,3),dataSet.annotations.subList(3,4))

        checkUserAnnotationResults("project=${dataSet.project.id}&baseAnnotation="+dataSet.annotations[0].id+"&maxDistanceBaseAnnotation=11",dataSet.annotations.subList(0,3),dataSet.annotations.subList(3,4))

    }


    void testListAnnotationSearchByImageAndUser() {

        def dataSet = createAnnotationSet()

        def result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==dataSet.annotations.size()

        //change image and user
        dataSet.annotations[2].image = BasicInstanceBuilder.getImageInstanceNotExist( dataSet.project,true)
        BasicInstanceBuilder.saveDomain(dataSet.annotations[2])
        dataSet.annotations[3].user = BasicInstanceBuilder.getUserNotExist(true)
        BasicInstanceBuilder.saveDomain(dataSet.annotations[3])

        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection instanceof JSONArray
        assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() - 2 //we change 1 for image and 1 for user

    }



    void testListAnnotationSearchByImageAndUserAndBBox() {

        def dataSet = createAnnotationSet()

        def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
        def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
        def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
        def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
         //e intersect a,b and c
        def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"

        dataSet.annotations[0].location = new WKTReader().read(a)
        dataSet.annotations[1].location = new WKTReader().read(b)
        dataSet.annotations[2].location = new WKTReader().read(c)
        dataSet.annotations[3].location = new WKTReader().read(d)

        dataSet.annotations.each {
            BasicInstanceBuilder.saveDomain(it)
        }

        def result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==3 //a,b,c
         //generic way test
        checkUserAnnotationResultNumber("notReviewedOnly=true&user=${dataSet.user.id}&image=${dataSet.image.id}&bbox=${e.replace(" ","%20")}",3)

        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,2,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(dataSet.image.id, dataSet.user.id, e, true,3,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

    }

    void testListAnnotationSearchByTermAndProjectAndUser() {

        def dataSet = createAnnotationSet()

        println "TERMS=${Term.list().collect{it}.join(", ")}"

        def result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size()==dataSet.annotations.size() - 1 //just 1 has no term
        //generic way test
        checkUserAnnotationResultNumber("project=${dataSet.project.id}&user=${dataSet.user.id}&term=${dataSet.term.id}",dataSet.annotations.size() - 1)

        //change image and user
        AnnotationTerm at = AnnotationTerm.findByUserAnnotation(dataSet.annotations[0])
        at.term = BasicInstanceBuilder.getTermNotExist(true)
        BasicInstanceBuilder.saveDomain(at)
        dataSet.annotations[1].user = BasicInstanceBuilder.getUserNotExist(true)
        BasicInstanceBuilder.saveDomain(dataSet.annotations[1])

        result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,dataSet.term.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert JSON.parse(result.data).collection instanceof JSONArray
        assert JSON.parse(result.data).collection.size()==dataSet.annotations.size() - 3 //we change 1 for term and 1 for user, and 1 has no term
        //generic way test
        checkUserAnnotationResultNumber("project=${dataSet.project.id}&user=${dataSet.user.id}&term=${dataSet.term.id}",dataSet.annotations.size() - 3)

    }

    void testListAnnotationSearchByTerm() {

        def dataSet = createAnnotationSet()
        Term term2 =  BasicInstanceBuilder.getTermNotExist(dataSet.project.ontology,true)
        term2 = BasicInstanceBuilder.saveDomain(term2)
        UserAnnotation a1 =  dataSet.annotations[0]
        def at = BasicInstanceBuilder.getAnnotationTermNotExist(a1,term2,true)
        def result = UserAnnotationAPI.listByProjectAndTerm(dataSet.project.id,term2.id,dataSet.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size()==1
        assert json.collection[0].term.size() == 2
        assert json.collection[0].term.findAll{term2.id == it}.size() == 1
    }

    def testAnnotationIncludeFilterUserAnnotation() {

        def dataSet = createAnnotationSet()

        def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
        def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
        def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
        def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
         //e intersect a,b and c
        def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"


        dataSet.annotations[0].location = new WKTReader().read(a)
        dataSet.annotations[1].location = new WKTReader().read(b)
        dataSet.annotations[2].location = new WKTReader().read(c)
        dataSet.annotations[3].location = new WKTReader().read(d)

        dataSet.annotations.each {
            BasicInstanceBuilder.saveDomain(it)
        }

        //tatic def listIncluded(String geometry, Long idImage, Long idUser,List<Long> terms,String username, String password) {
        def result = AnnotationDomainAPI.listIncluded(e, dataSet.image.id, dataSet.user.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection.size()==3 //d is not included!
        //generic way test
        checkUserAnnotationResultNumber("bbox=${e.replace(" ","%20")}&image=${dataSet.image.id}&user=${dataSet.user.id}",3)

        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(dataSet.image,e,dataSet.user,dataSet.term)

        checkUserAnnotationResultNumber("bboxAnnotation=${annotation.id}&image=${dataSet.image.id}&user=${dataSet.user.id}",4)

        checkUserAnnotationResultNumber("bboxAnnotation=${annotation.id}&image=${dataSet.image.id}&user=${dataSet.user.id}&excludedAnnotation=${annotation.id}",3)

    }







    void testAnnotationSearchWithSuggestedTerm() {
        //create annotation
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        annotationTerm.term = BasicInstanceBuilder.getTerm()
        BasicInstanceBuilder.saveDomain(annotationTerm)
        UserAnnotation annotation = annotationTerm.userAnnotation

        //create job
        UserJob userJob = BasicInstanceBuilder.getUserJob(annotation.project)
        Job job = userJob.job

        //create suggest with different term
        AlgoAnnotationTerm suggest = BasicInstanceBuilder.getAlgoAnnotationTerm(job,annotation,userJob)
        suggest.term = BasicInstanceBuilder.getAnotherBasicTerm()
        BasicInstanceBuilder.saveDomain(suggest)

        println "project=${annotation.project.id}"
        println "a.term=${annotation.terms().collect{it.id}.join(",")}"
        println "at.term=${suggest.term.id}"
        println "job=${job.id}"
        println "user=${UserJob.findByJob(job).id}"

        def result = AnnotationDomainAPI.listByProjectAndTermWithSuggest(annotation.project.id, annotationTerm.term.id, suggest.term.id, UserJob.findByJob(job).id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert AnnotationDomainAPI.containsInJSONList(annotation.id,json)

        //create annotation
        AnnotationTerm annotationTerm2 = BasicInstanceBuilder.getAnnotationTerm()
        annotationTerm2.userAnnotation = BasicInstanceBuilder.getUserAnnotationNotExist(annotationTerm2.container(),true)
        annotationTerm2.term = BasicInstanceBuilder.getTerm()
        BasicInstanceBuilder.saveDomain(annotationTerm2)
        UserAnnotation annotation2 = annotationTerm2.userAnnotation

        //create suggest with same term
        AlgoAnnotationTerm suggest2 = BasicInstanceBuilder.getAlgoAnnotationTerm(job,annotation2,userJob)
        suggest2.term = BasicInstanceBuilder.getTerm()
        BasicInstanceBuilder.saveDomain(suggest)

        //We are looking for a different term => annotation shouldn't be in result
        result = AnnotationDomainAPI.listByProjectAndTermWithSuggest(annotation2.project.id, annotationTerm2.term.id, suggest.term.id, job.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert !AnnotationDomainAPI.containsInJSONList(annotation2.id,json)
    }

    void testListAnnotationSearchGeneric() {

         def dataSet = createAnnotationSet()

         def a = "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))"
         def b = "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))"
         def c = "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))"
         def d = "POLYGON ((4 4,8 4, 8 7,4 7,4 4))"
          //e intersect a,b and c
         def e = "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))"

         dataSet.annotations[0].location = new WKTReader().read(a)
         dataSet.annotations[1].location = new WKTReader().read(b)
         dataSet.annotations[2].location = new WKTReader().read(c)
         dataSet.annotations[3].location = new WKTReader().read(d)

         dataSet.annotations.each {
             BasicInstanceBuilder.saveDomain(it)
         }

        checkUserAnnotationResultNumber("project=${dataSet.project.id}&hideMeta=true",4)

        dataSet.annotations[0].image = BasicInstanceBuilder.getImageInstanceNotExist(dataSet.project,true)
        BasicInstanceBuilder.saveDomain(dataSet.annotations[0])

        checkUserAnnotationResultNumber("image=${dataSet.image.id}",3)

        dataSet.annotations[1].user = BasicInstanceBuilder.getUserNotExist(true)
        BasicInstanceBuilder.saveDomain(dataSet.annotations[1])

        checkUserAnnotationResultNumber("user=${dataSet.user.id}&image=${dataSet.image.id}&project=${dataSet.project.id}",2)

        checkUserAnnotationResultNumber("user=${dataSet.user.id}&image=${dataSet.image.id}&project=${dataSet.project.id}&term=${dataSet.term.id}",1)


        checkUserAnnotationResultNumber("users=${dataSet.user.id}&images=${dataSet.image.id}&project=${dataSet.project.id}",2)

        checkUserAnnotationResultNumber("noTerm=true&project=${dataSet.project.id}",1)

     }



    void testListingUserAnnotationWithoutTerm() {
        //create annotation without term
        User user = BasicInstanceBuilder.getUser()
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        Infos.addUserRight(user.username,project)
        Ontology ontology = BasicInstanceBuilder.getOntology()
        project.ontology = ontology
        project.save(flush: true)

        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        image.save(flush: true)


        UserAnnotation annotationWithoutTerm = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationWithoutTerm.project = project
        annotationWithoutTerm.image = image
        annotationWithoutTerm.user = user
        assert annotationWithoutTerm.save(flush: true)

        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.user = user
        at.save(flush: true)
        UserAnnotation annotationWithTerm = at.userAnnotation
        annotationWithTerm.user = user
        annotationWithTerm.project = project
        annotationWithTerm.image = image
        assert annotationWithTerm.save(flush: true)

        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.user = BasicInstanceBuilder.getUser()
        at2.save(flush: true)
        UserAnnotation annotationWithTermFromOtherUser = at.userAnnotation
        annotationWithTermFromOtherUser.user = user
        annotationWithTermFromOtherUser.project = project
        annotationWithTermFromOtherUser.image = image
        assert annotationWithTermFromOtherUser.save(flush: true)

        //list annotation without term with this user
        def result = UserAnnotationAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)


        //list annotation without term with this user
        result = AnnotationDomainAPI.listByProjectAndUsersWithoutTerm(project.id, user.id, image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)

        //all images
        result = AnnotationDomainAPI.listByProjectAndUsersWithoutTerm(project.id, user.id,null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert DomainAPI.containsInJSONList(annotationWithoutTerm.id,json)
        assert !DomainAPI.containsInJSONList(annotationWithTerm.id,json)
    }



    void testListingUserAnnotationWithSeveralTerm() {
        //create annotation without term
        User user = BasicInstanceBuilder.getUser()
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        Infos.addUserRight(user.username,project)
        Ontology ontology = BasicInstanceBuilder.getOntology()
        project.ontology = ontology
        project.save(flush: true)

        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        image.save(flush: true)

        //annotation with no multiple term
        UserAnnotation annotationWithNoTerm = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationWithNoTerm.project = project
        annotationWithNoTerm.image = image
        annotationWithNoTerm.user = user
        assert annotationWithNoTerm.save(flush: true)

        //annotation with multiple term
        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.user = user
        at.save(flush: true)
        UserAnnotation annotationWithMultipleTerm = at.userAnnotation
        annotationWithMultipleTerm.user = user
        annotationWithMultipleTerm.project = project
        annotationWithMultipleTerm.image = image
        assert annotationWithMultipleTerm.save(flush: true)
        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.user = user
        at2.userAnnotation=annotationWithMultipleTerm
        at2.save(flush: true)
        AnnotationTerm at3 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at3.term.ontology = ontology
        at3.term.save(flush: true)
        at3.user = user
        at3.userAnnotation=annotationWithMultipleTerm
        at3.save(flush: true)


        //annotation with multiple term
        AnnotationTerm at4 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at4.term = at.term
        at4.user = user
        at4.save(flush: true)
        UserAnnotation annotationWithMultipleTerm2 = at4.userAnnotation
        annotationWithMultipleTerm2.user = user
        annotationWithMultipleTerm2.project = project
        annotationWithMultipleTerm2.image = image
        assert annotationWithMultipleTerm2.save(flush: true)
        AnnotationTerm at5 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at5.term = at2.term
        at5.user = user
        at5.userAnnotation=annotationWithMultipleTerm2
        at5.save(flush: true)


        //list annotation without term with this user
        def result = UserAnnotationAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2
        assert json.collection.collect{it.userByTerm.size()}.contains(2)
        assert json.collection.collect{it.userByTerm.size()}.contains(3)

        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)


        //list annotation without term with this user
        result = AnnotationDomainAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, image.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)

        //all images
        result = AnnotationDomainAPI.listByProjectAndUsersSeveralTerm(project.id, user.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        assert !DomainAPI.containsInJSONList(annotationWithNoTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
    }

    void testListingUserAnnotationWithSeveralIdenticalTerm() {
        //create annotation without term
        User user = BasicInstanceBuilder.getUser()
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        Infos.addUserRight(user.username,project)
        Ontology ontology = BasicInstanceBuilder.getOntology()
        project.ontology = ontology
        project.save(flush: true)

        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.project = project
        image.save(flush: true)

        //annotation with multiple term
        AnnotationTerm at = BasicInstanceBuilder.getAnnotationTermNotExist()
        at.term.ontology = ontology
        at.term.save(flush: true)
        at.user = user
        at.save(flush: true)
        UserAnnotation annotationWithMultipleTerm = at.userAnnotation
        annotationWithMultipleTerm.user = user
        annotationWithMultipleTerm.project = project
        annotationWithMultipleTerm.image = image
        assert annotationWithMultipleTerm.save(flush: true)
        AnnotationTerm at2 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at2.term.ontology = ontology
        at2.term.save(flush: true)
        at2.user = user
        at2.userAnnotation=annotationWithMultipleTerm
        at2.save(flush: true)

        AnnotationTerm at3 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at3.term = at.term
        //at3.user = user
        at3.userAnnotation=annotationWithMultipleTerm
        at3.save(flush: true)


        //annotation with multiple term
        AnnotationTerm at4 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at4.term = at.term
        at4.user = user
        at4.save(flush: true)
        UserAnnotation annotationWithMultipleTerm2 = at4.userAnnotation
        annotationWithMultipleTerm2.user = user
        annotationWithMultipleTerm2.project = project
        annotationWithMultipleTerm2.image = image
        assert annotationWithMultipleTerm2.save(flush: true)
        AnnotationTerm at5 = BasicInstanceBuilder.getAnnotationTermNotExist()
        at5.term = at2.term
        at5.user = user
        at5.userAnnotation=annotationWithMultipleTerm2
        at5.save(flush: true)


        //list annotation without term with this user
        def result = UserAnnotationAPI.listByProjectAndTerm(project.id, at.term.id, user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        assert 200 == result.code
        def json = JSON.parse(result.data)

        assert json.collection instanceof JSONArray
        assert json.collection.size() == 2
        assert json.collection.collect{it.userByTerm.size()}.contains(2)
        assert json.collection.collect{it.userByTerm.size()}.contains(2)

        def users = []
        json.collection.collect{it.userByTerm}.each {
            it.collect{it.user}.each{ u->
                users << u
            }
        }
        assert users.collect{it.size()}.contains(2)

        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm.id,json)
        assert DomainAPI.containsInJSONList(annotationWithMultipleTerm2.id,json)
    }

    void testListUserAnnotationByImageWithCredential() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.listByImage(annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListUserAnnotationByImageNotExistWithCredential() {
        def result = UserAnnotationAPI.listByImage(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListUserAnnotationByProjectWithCredential() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.listByProject(annotation.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = UserAnnotationAPI.listByProject(annotation.project.id, true,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
       json = JSON.parse(result.data)
    }

    void testListUserAnnotationByProjectNotExistWithCredential() {
        def result = UserAnnotationAPI.listByProject(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListUserAnnotationByProjecImageAndUsertWithCredential() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.listByProject(annotation.project.id, annotation.user.id, annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }




    void testListUserAnnotationByImageAndUserWithCredential() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = UserAnnotationAPI.listByImageAndUser(-99, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, -99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testListUserAnnotationByProjectAndTermAndUserWithCredential() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()

        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        result = UserAnnotationAPI.listByProjectAndTerm(-99, annotationTerm.term.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, -99, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListUserAnnotationByProjectAndTermWithUserNullWithCredential() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id, -1, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testListUserAnnotationByProjectAndTermAndUserAndImageWithCredential() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        println "SecUser=${SecUser.list().collect{it.id}.join(', ')}"
        def result = UserAnnotationAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id,annotationTerm.userAnnotation.image.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }

    void testListUserAnnotationyProjectAndUsersWithCredential() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.listByProjectAndUsers(annotation.project.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }


    private static void checkUserAnnotationResultNumber(String url,int expectedResult) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?$url"
        def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection.size()==expectedResult
    }

    private static void checkUserAnnotationResults(String url,List<AnnotationDomain> expected, List<AnnotationDomain> notExpected) {
        String URL = Infos.CYTOMINEURL+"api/annotation.json?$url"
        def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)

        expected.each { annotation ->
            assert DomainAPI.containsInJSONList(annotation.id,json)
        }
        notExpected.each { annotation ->
            assert !DomainAPI.containsInJSONList(annotation.id,json)
        }
    }


    static def checkForProperties(JSONObject jsonObject, def expectedProperties = null, def unexpectedProperties = null) {

        expectedProperties.each {
            assert jsonObject.containsKey(it)
        }
        if(unexpectedProperties) {
            unexpectedProperties.each {
                assert !jsonObject.containsKey(it)
            }
        }
    }

    def createAnnotationSet() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist(project, true)
        User me = User.findByUsername(Infos.SUPERADMINLOGIN)
        Term term =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)

        UserAnnotation a1 =  BasicInstanceBuilder.getUserAnnotationNotExist(image, me, term)
        UserAnnotation a2 =  BasicInstanceBuilder.getUserAnnotationNotExist(image, me, term)
        UserAnnotation a3 =  BasicInstanceBuilder.getUserAnnotationNotExist(image, me, term)

        UserAnnotation a4 =  BasicInstanceBuilder.getUserAnnotationNotExist(image, me, null)

        return [project:project,image:image,user:me,term:term,annotations:[a1,a2,a3,a4]]
    }

}
