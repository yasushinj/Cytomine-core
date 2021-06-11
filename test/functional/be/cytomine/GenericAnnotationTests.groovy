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
import be.cytomine.processing.RoiAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.test.http.AnnotationDomainAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.test.http.DomainAPI
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.UpdateData
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.MultiPolygon
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class GenericAnnotationTests  {

    void testGetAnnotationWithCredentialWithaAnnotationAlgo() {
        def annotation = BasicInstanceBuilder.getAlgoAnnotation()
        def result = AnnotationDomainAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testGetAnnotationNotExist() {
        def result = AnnotationDomainAPI.show(-99, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testGetAnnotationWithCredentialWidthAnnotationUser() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = AnnotationDomainAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testGetAnnotationWithCredentialWidthAnnotationRoi() {
        def annotation = BasicInstanceBuilder.getRoiAnnotation()
        def result = AnnotationDomainAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListAnnotationByProject() {
        AlgoAnnotation annotation = BasicInstanceBuilder.getAlgoAnnotation()
        def result = AnnotationDomainAPI.listByProject(annotation.project.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = AnnotationDomainAPI.listByProject(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

    }

    void testListAnnotationByProjecImageAndUsertWithCredentialWithAnnotationAlgo() {
        AlgoAnnotation annotation = BasicInstanceBuilder.getAlgoAnnotation()
        def result = AnnotationDomainAPI.listByProject(annotation.project.id, annotation.user.id, annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListAnnotationByProjecImageAndUsertWithCredentialWidthAnnotationUser() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = AnnotationDomainAPI.listByProject(annotation.project.id, annotation.user.id, annotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }


    void testListAnnotationByImageAndUserWithCredentialWithAnnotationAlgo() {
        AlgoAnnotation annotation = BasicInstanceBuilder.getAlgoAnnotation()
        def result = AnnotationDomainAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListAnnotationByImageAndUserWithCredentialWithAnnotationUser() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = AnnotationDomainAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }


    void testListAnnotationByProjectAndTermAndUserWithCredentialForAnnotationAlgo() {
        AlgoAnnotationTerm annotationTerm = BasicInstanceBuilder.getAlgoAnnotationTerm(true)
        def result = AnnotationDomainAPI.listByProjectAndTerm(annotationTerm.retrieveAnnotationDomain().project.id, annotationTerm.term.id, annotationTerm.retrieveAnnotationDomain().user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }

    void testListAnnotationByProjectAndTermAndUserWithCredentialForAnnotationUser() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()

        def result = AnnotationDomainAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }


    void testListAnnotationByProjectAndTermAndUserAndImageWithCredentialForAnnotationAlgo() {
        AlgoAnnotationTerm annotationTerm = BasicInstanceBuilder.getAlgoAnnotationTerm(true)

        def result = AnnotationDomainAPI.listByProjectAndTerm(annotationTerm.retrieveAnnotationDomain().project.id, annotationTerm.term.id,annotationTerm.retrieveAnnotationDomain().image.id, annotationTerm.retrieveAnnotationDomain().user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }

    void testListAnnotationByProjectAndTermAndUserAndImageWithCredentialForAnnotationUser() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        println "SecUser1=${SecUser.list().collect{it.id}.join(', ')}"
        def result = AnnotationDomainAPI.listByProjectAndTerm(annotationTerm.userAnnotation.project.id, annotationTerm.term.id,annotationTerm.userAnnotation.image.id, annotationTerm.userAnnotation.user.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        //assert json.collection instanceof JSONArray
    }

    void testListAnnotationByProjectAndDates() {
        UserAnnotation annot1 = BasicInstanceBuilder.getUserAnnotation()
        def project = annot1.project
        def creationTime = annot1.created.getTime()

        UserAnnotation annot2 = BasicInstanceBuilder.getUserAnnotationNotExist(project, annot1.image)
        annot2.created = new Date(creationTime - 10000)
        BasicInstanceBuilder.saveDomain(annot2)
        println annot1.created
        println annot2.created

        def result = AnnotationDomainAPI.listByProjectAndDates(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, project.id, creationTime - 5000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert DomainAPI.containsInJSONList(annot1.id, json)
        assert !DomainAPI.containsInJSONList(annot2.id, json)

        result = AnnotationDomainAPI.listByProjectAndDates(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, project.id, null, creationTime - 5000)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert !DomainAPI.containsInJSONList(annot1.id, json)
        assert DomainAPI.containsInJSONList(annot2.id, json)

        result = AnnotationDomainAPI.listByProjectAndDates(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, project.id, creationTime - 6000, creationTime - 4000)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert !DomainAPI.containsInJSONList(annot1.id, json)
        assert !DomainAPI.containsInJSONList(annot2.id, json)
    }

    void testDownloadAnnotationDocumentForAnnotationAlgo() {
        AlgoAnnotationTerm annotationTerm = BasicInstanceBuilder.getAlgoAnnotationTerm(true)
        def result = AnnotationDomainAPI.downloadDocumentByProject(annotationTerm.retrieveAnnotationDomain().project.id,annotationTerm.retrieveAnnotationDomain().user.id,annotationTerm.term.id, annotationTerm.retrieveAnnotationDomain().image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDownloadAnnotationDocumentForAnnotationUser() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        def result = AnnotationDomainAPI.downloadDocumentByProject(annotationTerm.userAnnotation.project.id,annotationTerm.userAnnotation.user.id,annotationTerm.term.id, annotationTerm.userAnnotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testDownloadAnnotationDocumentNewImpl() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        def result = AnnotationDomainAPI.downloadDocumentNewImplementation(annotationTerm.userAnnotation.project.id,annotationTerm.userAnnotation.user.id,annotationTerm.term.id, annotationTerm.userAnnotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AnnotationDomainAPI.downloadDocumentNewImplementation(annotationTerm.userAnnotation.project.id,BasicInstanceBuilder.getUser2().id,annotationTerm.term.id, annotationTerm.userAnnotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


    void testAddAnnotationCorrectForAlgo() {
        def annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def result = AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(),user.username, 'PasswordUserJob')
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = AnnotationDomainAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 404 == result.code

        result = AnnotationDomainAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.show(idAnnotation, user.username, 'PasswordUserJob')
        assert 200 == result.code
    }

    void testAddAnnotationCorrectForUser() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def result = AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.undo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = AnnotationDomainAPI.redo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddAnnotationCorrectForRoi() {
        def annotationToAdd = BasicInstanceBuilder.getRoiAnnotation()
        def result = AnnotationDomainAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,true)
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert RoiAnnotation.read(idAnnotation)
    }

    void testEditAnnotationWithGenericCallForAlgo() {
        AlgoAnnotation annotationToAdd = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotationToAdd.user

        def data = UpdateData.createUpdateSet(
                BasicInstanceBuilder.getAlgoAnnotation(),
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )
        def result = AnnotationDomainAPI.update(annotationToAdd.id,data.postData,user.username, Infos.GOODPASSWORDUSERJOB)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        def showResult = AnnotationDomainAPI.show(idAnnotation, user.username, Infos.GOODPASSWORDUSERJOB)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = AnnotationDomainAPI.undo(user.username, Infos.GOODPASSWORDUSERJOB)
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, user.username, Infos.GOODPASSWORDUSERJOB)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = AnnotationDomainAPI.redo(user.username, Infos.GOODPASSWORDUSERJOB)
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, user.username, Infos.GOODPASSWORDUSERJOB)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testEditAnnotationWithGenericCallForUser() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def data = UpdateData.createUpdateSet(
                BasicInstanceBuilder.getUserAnnotation(),
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )
        def result = AnnotationDomainAPI.update(annotationToAdd.id,data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        def showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = AnnotationDomainAPI.undo()
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = AnnotationDomainAPI.redo()
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testEditAnnotationWithGenericCallForRoi() {
        RoiAnnotation annotationToAdd = BasicInstanceBuilder.getRoiAnnotation()
        def data = UpdateData.createUpdateSet(
                BasicInstanceBuilder.getRoiAnnotation(),
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )
        def result = AnnotationDomainAPI.update(annotationToAdd.id,data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.roiannotation.id

        def showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        showResult = AnnotationDomainAPI.undo()
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(showResult.data))

        showResult = AnnotationDomainAPI.redo()
        assert 200 == result.code
        showResult = AnnotationDomainAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(showResult.data))
    }

    void testDeleteAnnotationForAlgo() {
        def annotationToDelete = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        UserJob user = annotationToDelete.user

        def id = annotationToDelete.id
        def result = AnnotationDomainAPI.delete(id, user.username, 'PasswordUserJob')
        assert 200 == result.code

        def showResult = AnnotationDomainAPI.show(id, user.username,'PasswordUserJob')
        assert 404 == showResult.code

        result = AnnotationDomainAPI.undo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, user.username,'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.redo(user.username, 'PasswordUserJob')
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, user.username,'PasswordUserJob')
        assert 404 == result.code
    }

    void testDeleteUserAnnotationForUser() {
        def annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        def id = annotationToDelete.id
        def result = AnnotationDomainAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = AnnotationDomainAPI.undo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.redo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteUserAnnotationForRoi() {
        def annotationToDelete = BasicInstanceBuilder.getRoiAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        def id = annotationToDelete.id
        def result = AnnotationDomainAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = AnnotationDomainAPI.undo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = AnnotationDomainAPI.redo()
        assert 200 == result.code

        result = AnnotationDomainAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    private void doFillAnnotationCall(def annotationToFill) {
        annotationToFill.location = new WKTReader().read("POLYGON ((4980 4980, 5516 4932, 5476 4188, 4956 4204, 4980 4980), (5100 4316, 5100 4804, 5404 4780, 5364 4316, 5100 4316))")
        assert annotationToFill.save(flush: true)  != null

        //do fill action
        def result = AnnotationDomainAPI.fill(annotationToFill.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //check if annotation is well filled
        annotationToFill.refresh()
        annotationToFill.location.toString().equals("POLYGON ((4980 4980, 5516 4932, 5476 4188, 4956 4204, 4980 4980))")
    }

    void testFillUserAnnotation() {
        def annotationToFill = BasicInstanceBuilder.getUserAnnotationNotExist()
        doFillAnnotationCall(annotationToFill)
    }

    void testFillAlgoAnnotation() {
        def annotationToFill = BasicInstanceBuilder.getAlgoAnnotationNotExist()
        doFillAnnotationCall(annotationToFill)
    }

    void testFillReviewedAnnotation() {
        def annotationToFill = BasicInstanceBuilder.getReviewedAnnotationNotExist()
        doFillAnnotationCall(annotationToFill)
    }

    void testFillAnnotationNotExist() {
        //do fill action
        def result = AnnotationDomainAPI.fill(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testFillAnnotationMultiPolygon() {

        String multiPolygon = "MULTIPOLYGON(((1 1,5 1,5 5,1 5,1 1),(2 2,2 3,3 3,3 2,2 2)),((6 3,9 2,9 4,6 3)))"

        String multiPolygonWithoutBlank = "ULTIPOLYGON(((1 1,5 1,5 5,1 5,1 1)),((6 3,9 2,9 4,6 3)))"

        //add annotation with empty space inside it
        def annotationToFill = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationToFill.location = new WKTReader().read(multiPolygon)
        assert annotationToFill.save(flush: true)  != null

        //do fill action
        def result = AnnotationDomainAPI.fill(annotationToFill.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //check if annotation is well filled
        annotationToFill.refresh()
        annotationToFill.location.toString().replaceAll(" ","").equals(multiPolygonWithoutBlank.replaceAll(" ",""))
    }

    void testFillAnnotationMultiPolygonComplex() {

        String multiPolygon = "MULTIPOLYGON (\n" +
                "         ((12099.256583509747 16750.418861169917, 12099.043823719125 16751.339457289127, 12099.008051747693 16752.283641756945, 12099.1505450325 16753.21769721943, 12099.466215058877 16754.10826803506, 12099.943789074123 16754.92355142332, 12100.566212643795 16755.634433159707, 12101.311258675076 16756.21552726093, 12102.15232115866 16756.64608253255, 12103.059365284169 16756.91072360568, 12104 16757, 12120 16757, 12121.033369789395 16756.89204935363, 12122.02211840411 16756.572858751348, 12122.923551423319 16756.05621092588, 12123.698744736195 16755.364414863907, 12124.31422529506 16754.52734249825, 12124.743416490253 16753.581138830083, 12132.743416490253 16729.581138830083, 12132.957505083801 16728.650494691818, 12132.990757289455 16727.696122265148, 12132.841960156618 16726.752834477018, 12132.516541402574 16725.855039916725, 12132.02637142403 16725.03548770356, 12131.389330297132 16724.324072887426, 12130.628655560595 16723.74674595824, 12129.772094573098 16723.324566242154, 12128.850892364628 16723.072933714288, 12127.898651902216 16723.001027249218, 12126.950108344521 16723.111469800468, 12126.039861997104 16723.400232722233, 12125.201116186798 16723.856782723513, 12124.464466094067 16724.46446609407, 12120 16728.928932188137, 12115.535533905933 16724.46446609407, 12114.748285726344 16723.82304829255, 12113.84767884134 16723.35391746745, 12112.870860636873 16723.07642388592, 12111.85812199173 16723.002013342277, 12110.851235397264 16723.133755052662, 12109.89173196494 16723.466215058877, 12109.019188390113 16723.985680363203, 12108.269594529698 16724.670724549225, 12107.673868926402 16725.493091558503, 12107.256583509747 16726.418861169917, 12099.256583509747 16750.418861169917)), \n" +
                "         ((12232 16821, 12344 16821, 12344.91739704001 16820.915117767763, 12345.803645796579 16820.663353068394, 12346.628655560595 16820.25325404176, 12347.364414863907 16819.698744736193, 12347.98594254969 16819.018652346756, 12348.472135954999 16818.2360679775, 12348.472135955 16818.2360679775, 12355.09016994375 16805, 12371 16805, 12371 16811, 12360 16811, 12360.97545161008 16811.096073597982, 12361.913417161826 16811.38060233744, 12362.777851165098 16811.84265193849, 12363.535533905933 16812.46446609407, 12364.157348061513 16813.222148834902, 12364.619397662556 16814.086582838176, 12364.903926402016 16815.02454838992, 12365 16816, 12364.903926402016 16816.97545161008, 12364.619397662556 16817.913417161824, 12364.157348061513 16818.777851165098, 12363.535533905933 16819.53553390593, 12362.777851165098 16820.15734806151, 12361.913417161826 16820.61939766256, 12360.97545161008 16820.903926402018, 12360 16821, 12376 16821, 12376.97545161008 16820.903926402018, 12377.913417161826 16820.61939766256, 12378.777851165098 16820.15734806151, 12379.535533905933 16819.53553390593, 12380.157348061513 16818.777851165098, 12380.619397662556 16817.913417161824, 12380.903926402016 16816.97545161008, 12381 16816, 12381 16802.071067811863, 12394.071067811865 16789, 12448 16789, 12449.033369789395 16788.89204935363, 12450.02211840411 16788.572858751348, 12450.923551423319 16788.05621092588, 12451.698744736195 16787.364414863907, 12452.31422529506 16786.52734249825, 12452.743416490253 16785.581138830083, 12456.926209682668 16773.032759252837, 12459.527864045 16778.2360679775, 12460.01405745031 16779.018652346756, 12460.635585136093 16779.698744736193, 12461.371344439405 16780.25325404176, 12462.196354203421 16780.663353068394, 12463.08260295999 16780.915117767763, 12464 16781, 12480 16781, 12480.97545161008 16780.903926402018, 12481.913417161826 16780.61939766256, 12482.777851165098 16780.15734806151, 12483.535533905933 16779.53553390593, 12484.157348061513 16778.777851165098, 12484.619397662556 16777.913417161824, 12484.903926402016 16776.97545161008, 12485 16776, 12485 16763.09016994375, 12514.2360679775 16748.472135955, 12515.018652346755 16747.98594254969, 12515.698744736193 16747.364414863907, 12516.25325404176 16746.628655560595, 12516.663353068392 16745.80364579658, 12516.915117767763 16744.91739704001, 12517 16744, 12517 16731.09016994375, 12530.2360679775 16724.472135955, 12531.085088605321 16723.934746281186, 12531.809518091726 16723.238452085305, 12532.380100389919 16722.411373171915, 12532.773792563736 16721.486911079526, 12532.974695375398 16720.502400161222, 12532.974695375398 16719.497599838778, 12532.773792563736 16718.513088920474, 12532.380100389919 16717.588626828085, 12531.809518091726 16716.761547914695, 12531.085088605321 16716.065253718814, 12530.2360679775 16715.527864045, 12517 16708.90983005625, 12517 16637, 12544 16637, 12544.91739704001 16636.915117767763, 12545.803645796579 16636.663353068394, 12546.628655560595 16636.25325404176, 12547.364414863907 16635.698744736193, 12547.98594254969 16635.018652346756, 12548.472135954999 16634.2360679775, 12548.472135955 16634.2360679775, 12552 16627.1803398875, 12555.527864045 16634.2360679775, 12555.177559661593 16633.32063211475, 12555.012578955937 16632.354445100453, 12555.039262044025 16631.37463695972, 12555.256583509747 16630.418861169917, 12555.656191811051 16629.523847658642, 12556.222730225021 16628.7239912932, 12556.934426998127 16628.050030105154, 12557.763932022499 16627.527864045, 12558.679367885246 16627.177559661595, 12559.645554899545 16627.012578955935, 12560.625363040279 16627.039262044025, 12561.581138830083 16627.25658350975, 12562.47615234136 16627.656191811053, 12563.2760087068 16628.22273022502, 12563.949969894844 16628.934426998127, 12564.472135954999 16629.7639320225, 12564.472135955 16629.7639320225, 12559.050343994764 16618.920348102027, 12577.581138830084 16612.74341649025, 12578.483606473977 16612.339550539218, 12579.2889595857 16611.765998518804, 12579.965707068937 16611.04518758755, 12580.487386593235 16610.205303054652, 12580.833599329926 16609.279186271717, 12580.990807591652 16608.303050462964, 12580.952864186038 16607.31506470768, 12580.721252784557 16606.353861444375, 12580.305029907693 16605.457025856627, 12579.720470794855 16604.659626208846, 12578.990433006356 16603.992842599237, 12578.143462642007 16603.48274774865, 12577.21267812518 16603.149287499273, 12553.77995987511 16597.291107936755, 12571.535533905933 16579.53553390593, 12572.176951707452 16578.748285726342, 12572.64608253255 16577.84767884134, 12572.92357611408 16576.87086063687, 12572.997986657721 16575.85812199173, 12572.866244947338 16574.851235397262, 12572.533784941123 16573.89173196494, 12572.014319636795 16573.019188390113, 12571.329275450777 16572.2695945297, 12570.506908441499 16571.673868926402, 12569.581138830084 16571.25658350975, 12549 16564.39620389972, 12549 16541, 12568 16541, 12569.033369789395 16540.89204935363, 12570.02211840411 16540.572858751348, 12570.923551423319 16540.05621092588, 12571.698744736195 16539.364414863907, 12572.31422529506 16538.52734249825, 12572.743416490253 16537.581138830083, 12580.743416490253 16513.581138830083, 12580.956176280875 16512.660542710873, 12580.991948252307 16511.716358243055, 12580.8494549675 16510.78230278057, 12580.533784941123 16509.89173196494, 12580.056210925877 16509.07644857668, 12579.433787356205 16508.365566840293, 12578.688741324924 16507.78447273907, 12577.84767884134 16507.35391746745, 12576.940634715831 16507.08927639432, 12576 16507, 12531.903882032024 16507, 12516.850712500727 16446.787321874817, 12516.554949339838 16445.93785633102, 12516.111775709485 16445.15512732888, 12515.535533905932 16444.464466094065, 12514.84487267112 16443.888224290513, 12514.062143668976 16443.44505066016, 12513.21267812518 16443.149287499273, 12485 16436.096117967976, 12485 16352, 12484.915117767763 16351.08260295999, 12484.663353068392 16350.196354203421, 12484.25325404176 16349.371344439403, 12483.698744736193 16348.635585136093, 12483.018652346755 16348.01405745031, 12482.2360679775 16347.527864045, 12418.2360679775 16315.527864045, 12417.30005314021 16315.17197122703, 12416.311891430776 16315.009737107586, 12293 16307.302743893162, 12293 16227.090169943749, 12322.2360679775 16212.472135955, 12323.018652346755 16211.98594254969, 12323.698744736193 16211.364414863907, 12324.25325404176 16210.628655560597, 12324.663353068392 16209.803645796579, 12324.915117767763 16208.91739704001, 12325 16208, 12325 16163.903882032022, 12353.21267812518 16156.850712500727, 12354.104245686496 16156.535653215455, 12354.92055236988 16156.058370837514, 12355.632401984956 16155.435935945225, 12356.214334374043 16154.69061067116, 12356.645536025304 16153.849052470216, 12356.910584493038 16152.94136068366, 12357 16152, 12357 16027.090169943749, 12370.2360679775 16020.472135955, 12371.03747162684 16019.971620087086, 12371.730405470302 16019.329275450777, 12372.290124453866 16018.568040531289, 12372.696640700458 16017.71509945216, 12372.935437288188 16016.800911215036, 12372.997986657721 16015.85812199173, 12372.882055136191 16014.92039931123, 12372.591782702886 16014.021229773449, 12372.137535148293 16013.192723224076, 12371.535533905933 16012.464466094067, 12361.779959875112 16002.708892063245, 12385.21267812518 15996.850712500727, 12386.104245686496 15996.535653215455, 12386.92055236988 15996.058370837514, 12387.632401984956 15995.435935945225, 12388.214334374043 15994.69061067116, 12388.645536025304 15993.849052470216, 12388.910584493038 15992.94136068366, 12389 15992, 12389 15859.090169943749, 12399.01273475459 15854.083802566456, 12408.928932188133 15864, 12396.464466094067 15876.464466094067, 12395.842651938487 15877.222148834902, 12395.380602337444 15878.086582838174, 12395.096073597984 15879.02454838992, 12395 15880, 12395.096073597984 15880.97545161008, 12395.380602337444 15881.913417161826, 12395.842651938487 15882.777851165098, 12396.464466094067 15883.535533905933, 12397.222148834902 15884.157348061513, 12398.086582838174 15884.619397662556, 12399.02454838992 15884.903926402016, 12400 15885, 12416 15885, 12417.064296372917 15884.885414335611, 12418.079811503034 15884.54690929224, 12419 15884, 12483 15836, 12483.686146021682 15835.378213656186, 12484.245869823684 15834.640566121181, 12484.659974791155 15833.812356186238, 12484.914258537958 15832.921988623635, 12485 15832, 12485 15808, 12484.903926402016 15807.02454838992, 12484.619397662556 15806.086582838174, 12484.157348061513 15805.222148834902, 12483.535533905933 15804.464466094067, 12482.777851165098 15803.842651938487, 12481.913417161826 15803.380602337444, 12480.97545161008 15803.096073597984, 12480 15803, 12456 15803, 12454.99806568721 15803.101415752193, 12454.036776143214 15803.401548946858, 12453.15512732888 15803.888224290515, 12452.388884475282 15804.541699164742, 12451.769131220562 15805.33546451118, 12451.321008661724 15806.237320205953, 12451.062695473316 15807.210681299604, 12451.004670465863 15808.216062133142, 12451.149287499273 15809.21267812518, 12457.187133075315 15833.364060429352, 12393.084652289093 15819.11906469908, 12391.95615103333 15819.000192276884, 12390.829908254967 15819.138839098712, 12389.7639320225 15819.527864045, 12357.7639320225 15835.527864045, 12356.959589891658 15836.03062897009, 12356.264670762566 15836.676249785562, 12355.704184464286 15837.441490886646, 12355.29830264834 15838.29881158792, 12355.061632819992 15839.21735730028, 12355.002692620652 15840.164069973825, 12355.123603281767 15841.104877842297, 12355.420013282648 15842.005921650734, 12355.881254959764 15842.834773234941, 12356.490728431174 15843.561602596617, 12357.226499018874 15844.160251471689, 12377.066247547185 15857.386750490563, 12365.527725654672 15874.694533329332, 12300.00750935606 15867.414509296152, 12292.850712500727 15838.78732187482, 12292.55494933984 15837.937856331024, 12292.111775709485 15837.15512732888, 12291.535533905933 15836.464466094067, 12290.84487267112 15835.888224290515, 12290.062143668976 15835.44505066016, 12289.21267812518 15835.149287499273, 12261 15828.096117967978, 12261 15808, 12260.903926402016 15807.02454838992, 12260.619397662556 15806.086582838174, 12260.157348061513 15805.222148834902, 12259.535533905933 15804.464466094067, 12258.777851165098 15803.842651938487, 12257.913417161826 15803.380602337444, 12256.97545161008 15803.096073597984, 12256 15803, 12203.903882032022 15803, 12196.850712500727 15774.78732187482, 12196.535653215455 15773.895754313504, 12196.058370837514 15773.07944763012, 12195.435935945225 15772.367598015044, 12194.69061067116 15771.785665625957, 12193.849052470216 15771.354463974696, 12192.94136068366 15771.089415506962, 12192 15771, 12170.5 15771, 12148 15741, 12147.350762877026 15740.28888316784, 12146.57725319782 15739.71540364161, 12145.708158765317 15739.300830538014, 12144.775712414461 15739.060539477832, 12143.814496565974 15739.003442337373, 12142.8601606391 15739.131656726222, 12141.948098891651 15739.440427449688, 12141.112137723452 15739.918302868697, 12140.383281128248 15740.547559616343, 12139.788560822557 15741.30485991928, 12139.350033697705 15742.16211714532, 12139.083963777372 15743.087537475942, 12139.000219020096 15744.046799070324, 12139.101905338002 15745.004324988293, 12139.385251405241 15745.924602662155, 12139.839748528311 15746.773500981126, 12153.56602159979 15767.362910588343, 12144 15776.928932188133, 12133 15765.928932188133, 12133 15720, 12132.915117767763 15719.08260295999, 12132.663353068392 15718.196354203421, 12132.25325404176 15717.371344439403, 12131.698744736193 15716.635585136093, 12131.018652346755 15716.01405745031, 12130.2360679775 15715.527864045, 12118.7082039325 15709.7639320225, 12132.472135955 15682.2360679775, 12132.82470059313 15681.312350634033, 12132.988608716681 15680.337317464488, 12132.957451153788 15679.349094432499, 12132.732446234397 15678.386323254632, 12132.32239215086 15677.486650423401, 12131.74332292906 15676.68525514575, 12131.017881462369 15676.013473757883, 12130.174434124201 15675.49757440489, 12129.24596157978 15675.157729897897, 12128.268769168828 15675.007228912329, 12127.281067286118 15675.051956371159, 12126.321477270125 15675.290163331358, 12125.427521222862 15675.712535371436, 12085.427521222862 15699.712535371436, 12084.620547611654 15700.314989612647, 12083.950665882832 15701.066931093967, 12083.44505066016 15701.937856331022, 12083.124212882893 15702.892435108606, 12083.001167752831 15703.891943696817, 12083.080906755744 15704.895835729534, 12083.360195175248 15705.86338702051, 12083.827703313285 15706.75534759296, 12084.464466094067 15707.535533905933, 12091.928932188133 15715, 12080 15715, 12079.02501472162 15715.09598086189, 12078.08746141418 15715.380238517224, 12077.223334947485 15715.841859648095, 12076.46581116276 15716.463121536903, 12075.843973174307 15717.220172483027, 12075.381694800564 15718.083947525545, 12075.096723992228 15719.021284315233, 12075.000001446817 15719.996196294911, 12075.095240569643 15720.971254307744, 12075.378784907463 15721.90902359035, 12075.839748528311 15722.773500981126, 12086.657414540894 15739, 11976 15739, 11975.118843195742 15739.078256133615, 11974.265268761324 15739.310574925477, 11973.46599567075 15739.689684227422, 11972.74604313272 15740.203716988173, 11920.261519415122 15785.190451603255, 11899.535533905933 15764.464466094067, 11898.7988838132 15763.856782723511, 11897.960138002894 15763.400232722233, 11897.049891655479 15763.111469800466, 11896.101348097782 15763.001027249216, 11895.14910763537 15763.072933714286, 11894.2279054269 15763.324566242156, 11893.371344439405 15763.74674595824, 11892.610669702866 15764.324072887428, 11891.973628575968 15765.03548770356, 11891.483458597426 15765.855039916729, 11891.15803984338 15766.75283447702, 11891.009242710543 15767.696122265148, 11891.042494916199 15768.65049469182, 11891.256583509747 15769.581138830084, 11898.279385971822 15790.649546216311, 11885.928932188133 15803, 11816 15803, 11815.02454838992 15803.096073597984, 11814.086582838174 15803.380602337444, 11813.222148834902 15803.842651938487, 11812.464466094067 15804.464466094067, 11811.842651938487 15805.222148834902, 11811.380602337444 15806.086582838174, 11811.096073597984 15807.02454838992, 11811 15808, 11811 15832, 11811.110013400457 15833.043087271653, 11811.435212422513 15834.040273112214, 11811.961286566075 15834.947675999603, 11812.665085748971 15835.725365342927, 11813.51563903466 15836.339118642523, 11814.475517519453 15836.761927463382, 11815.502481404896 15836.97518595105, 11891 15844.524937810562, 11891 15867, 11789 15867, 11789 15800, 11788.915117767763 15799.08260295999, 11788.663353068392 15798.196354203421, 11788.25325404176 15797.371344439403, 11787.698744736193 15796.635585136093, 11787.018652346755 15796.01405745031, 11786.2360679775 15795.527864045, 11773 15788.909830056251, 11773 15776, 11772.898584247807 15774.99806568721, 11772.598451053142 15774.036776143214, 11772.111775709485 15773.15512732888, 11771.458300835258 15772.388884475282, 11770.66453548882 15771.769131220562, 11769.762679794047 15771.321008661724, 11768.789318700396 15771.062695473316, 11767.783937866858 15771.004670465863, 11766.78732187482 15771.149287499273, 11737.533748623875 15778.462680812008, 11725 15765.928932188133, 11725 15704, 11724.908806756202 15703.049412691933, 11724.638553507897 15702.13350023992, 11724.199098363226 15701.285672654974, 11723.606471458248 15700.536856396157, 11722.882290220894 15699.914366256928, 11722.052970825616 15699.440908995297, 11721.148764602736 15699.13375505266, 11720.202654551662 15699.004108574769, 11719.249152210214 15699.056698714769, 11718.323038766985 15699.289607126473, 11717.458096337581 15699.694337940458, 11716.685875684334 15700.256127670404, 11716.034545329765 15700.95448374519, 11715.527864045 15701.7639320225, 11710.293851553548 15712.231957005404, 11675.123475237771 15684.095655952784, 11674.314339117074 15683.567863443983, 11673.418807217988 15683.205525463809, 11672.470310326396 15683.022168323769, 11671.504256445669 15683.024636864677, 11670.556708992952 15683.212838934298, 11669.663040528443 15683.579748827464, 11668.856612274813 15684.111669560192, 11668.167528721142 15684.788744186968, 11667.621513802858 15685.585697073388, 11667.238950610552 15686.472777451963, 11667.034120476 15687.41687003754, 11667.01466984081 15688.382731242347, 11667.181324809791 15689.334304841957, 11667.527864045 15690.2360679775, 11673.916197433544 15703.01273475459, 11667.070671626017 15709.858260562116, 11661.427694229973 15681.643373581894, 11667.535533905933 15675.535533905933, 11668.157348061513 15674.777851165098, 11668.619397662556 15673.913417161826, 11668.903926402016 15672.97545161008, 11669 15672, 11668.903926402016 15671.02454838992, 11668.619397662556 15670.086582838174, 11668.157348061513 15669.222148834902, 11667.535533905933 15668.464466094067, 11651.535533905933 15652.464466094067, 11650.777367961331 15651.842329112667, 11649.912343337313 15651.380157690977, 11648.973741684089 15651.095733782444, 11647.997675513416 15651.000000540324, 11647.0216987769 15651.09664128205, 11646.083361985191 15651.381937774328, 11645.21876745955 15651.844913291405, 11644.461180302236 15652.46775494243, 11643.839748528311 15653.226499018874, 11631.222875849284 15672.151808037417, 11616.440825170412 15657.369757358545, 11626.2360679775 15652.472135955, 11627.04365100412 15651.966886507718, 11627.740753474436 15651.317644261142, 11628.302071479226 15650.54797586088, 11628.707229908021 15649.68581926464, 11628.941522039011 15648.76246963085, 11628.996443373837 15647.811443345281, 11628.870000339775 15646.867261420015, 11628.566782653821 15645.964196425793, 11628.09779672196 15645.135028442464, 11627.480066121001 15644.40985518489, 11626.736013664973 15643.814999495211, 11625.892647486266 15643.372053858056, 11624.98058067569 15643.097096621545, 11585.95049845467 15635.29108017734, 11540.200915503603 15604.791358209963, 11532.743416490253 15582.418861169916, 11532.31422529506 15581.472657501748, 11531.698744736193 15580.635585136093, 11530.923551423319 15579.943789074123, 11530.022118404107 15579.427141248654, 11529.033369789395 15579.107950646368, 11528 15579, 11400 15579, 11399.023522491234 15579.096277773479, 11398.084650214627 15579.38140333005, 11397.219540183127 15579.844396168213, 11396.461508743794 15580.467425920131, 11395.839748528311 15581.226499018874, 11380.98857024995 15603.503266436417, 11343.01941932431 15611.097096621545, 11342.082441912606 15611.38231974023, 11341.219043421217 15611.844728588056, 11340.462353448263 15612.46658000303, 11339.84140706266 15613.224012827566, 11339.380030694978 15614.08796348867, 11339.095927889748 15615.025281200835, 11339 15616, 11339 15661.928932188133, 11328 15672.928932188133, 11299.535533905933 15644.464466094067, 11298.777851165098 15643.842651938487, 11297.913417161826 15643.380602337444, 11296.97545161008 15643.096073597984, 11296 15643, 11295.02454838992 15643.096073597984, 11294.086582838174 15643.380602337444, 11293.222148834902 15643.842651938487, 11292.464466094067 15644.464466094067, 11261.928932188133 15675, 11248 15675, 11247.05863931634 15675.089415506962, 11246.150947529784 15675.354463974696, 11245.30938932884 15675.785665625957, 11244.564064054775 15676.367598015044, 11243.941629162486 15677.07944763012, 11243.464346784545 15677.895754313504, 11243.149287499273 15678.78732187482, 11235.70704051876 15708.55630979687, 11224.98726524541 15713.916197433544, 11219.535533905933 15708.464466094067, 11218.777851165098 15707.842651938487, 11217.913417161826 15707.380602337444, 11216.97545161008 15707.096073597984, 11216 15707, 11215.02454838992 15707.096073597984, 11214.086582838174 15707.380602337444, 11213.222148834902 15707.842651938487, 11212.464466094067 15708.464466094067, 11211.842651938487 15709.222148834902, 11211.380602337444 15710.086582838174, 11211.096073597984 15711.02454838992, 11211 15712, 11211 15757.928932188133, 11197.928932188133 15771, 11184 15771, 11182.966630210605 15771.107950646368, 11181.977881595893 15771.427141248654, 11181.076448576681 15771.943789074123, 11180.301255263807 15772.635585136093, 11179.68577470494 15773.472657501748, 11179.256583509747 15774.418861169916, 11171.256583509747 15798.418861169916, 11171.039262044025 15799.37463695972, 11171.012578955937 15800.354445100453, 11171.177559661593 15801.320632114752, 11171.527864045 15802.2360679775, 11177.916197433544 15815.01273475459, 11148.464466094067 15844.464466094067, 11147.842651938487 15845.222148834902, 11147.380602337444 15846.086582838174, 11147.096073597984 15847.02454838992, 11147 15848, 11147 15928, 11147.084882232237 15928.91739704001, 11147.336646931608 15929.803645796579, 11147.74674595824 15930.628655560597, 11148.301255263807 15931.364414863907, 11148.981347653245 15931.98594254969, 11149.7639320225 15932.472135955, 11163 15939.090169943749, 11163 15952, 11163.096073597984 15952.97545161008, 11163.380602337444 15953.913417161826, 11163.842651938487 15954.777851165098, 11164.464466094067 15955.535533905933, 11165.222148834902 15956.157348061513, 11166.086582838174 15956.619397662556, 11167.02454838992 15956.903926402016, 11168 15957, 11168.97545161008 15956.903926402016, 11169.913417161826 15956.619397662556, 11170.777851165098 15956.157348061513, 11171.535533905933 15955.535533905933, 11176 15951.071067811867, 11203 15978.071067811867, 11203 15992, 11203.1004701447 15992.997299953387, 11203.39784287881 15993.954520358522, 11203.88016736968 15994.833192386362, 11204.528059932753 15995.598003914594, 11205.315483024204 15996.218218653254, 11206.210791639258 15996.668911376526, 11207.178005063473 15996.931969619161, 11248.097326455327 16003.751856517803, 11219.658784289377 16053.519305308217, 11219.258055694372 16054.414451450657, 11219.039772462535 16055.370601257893, 11219.012332997754 16056.350967056436, 11219.176793027977 16057.317829467353, 11219.526824986278 16058.233988651855, 11220.048961462746 16059.064195567702, 11220.72311336039 16059.776508169081, 11221.523342819153 16060.343520370456, 11222.418861169916 16060.743416490253, 11238.743852065889 16066.185080122244, 11236.464466094067 16068.464466094067, 11235.860067039754 16069.19626051769, 11235.404841576306 16070.0290816706, 11235.115192775396 16070.932920631607, 11235.001557509338 16071.875209505366, 11235.068030380839 16072.821994936527, 11243.068030380839 16120.821994936527, 11243.350353233658 16121.838691096473, 11243.839748528311 16122.773500981126, 11257.56602159979 16143.362910588343, 11252.464466094067 16148.464466094067, 11251.842651938487 16149.222148834902, 11251.380602337444 16150.086582838174, 11251.096073597984 16151.02454838992, 11251 16152, 11251.096073597984 16152.97545161008, 11251.380602337444 16153.913417161826, 11251.842651938487 16154.777851165098, 11252.464466094067 16155.535533905933, 11253.222148834902 16156.157348061513, 11254.086582838174 16156.619397662556, 11255.02454838992 16156.903926402016, 11256 16157, 11277.928932188133 16157, 11308.464466094067 16187.535533905933, 11309.222148834902 16188.157348061513, 11310.086582838174 16188.619397662556, 11311.02454838992 16188.903926402016, 11312 16189, 11406.48612181134 16189, 11429.226499018874 16204.160251471689, 11430.054508948755 16204.605981390489, 11430.951332889654 16204.888793030153, 11431.885249260267 16204.998683053338, 11432.82322448207 16204.931764537376, 11433.732081410813 16204.690404458714, 11434.579672846094 16204.283139970528, 11435.336018610757 16203.724376434879, 11435.974365980475 16203.033877890297, 11436.472135955 16202.2360679775, 11443.090169943749 16189, 11492.096117967978 16189, 11499.149287499273 16217.21267812518, 11499.488652884693 16218.155863447719, 11500.009332453727 16219.012403116303, 11500.690399731739 16219.747872204907, 11501.504482150325 16220.332711698458, 11502.418861169916 16220.743416490253, 11544.20854129694 16234.673309865928, 11532.91200307469 16251.618117199305, 11502.78732187482 16259.149287499273, 11501.895754313504 16259.464346784545, 11501.07944763012 16259.941629162486, 11500.367598015044 16260.564064054775, 11499.785665625957 16261.30938932884, 11499.354463974696 16262.150947529784, 11499.089415506962 16263.05863931634, 11499 16264, 11499 16280, 11499.096073597984 16280.97545161008, 11499.380602337444 16281.913417161826, 11499.842651938487 16282.777851165098, 11500.464466094067 16283.535533905933, 11501.222148834902 16284.157348061513, 11502.086582838174 16284.619397662556, 11503.02454838992 16284.903926402016, 11504 16285, 11556.096117967978 16285, 11563.149287499273 16313.21267812518, 11563.464346784545 16314.104245686496, 11563.941629162486 16314.92055236988, 11564.564064054775 16315.632401984956, 11565.30938932884 16316.214334374043, 11566.150947529784 16316.645536025304, 11567.05863931634 16316.910584493038, 11568 16317, 11752 16317, 11752.97545161008 16316.903926402016, 11753.913417161826 16316.619397662556, 11754.777851165098 16316.157348061513, 11755.535533905933 16315.535533905933, 11756.157348061513 16314.777851165098, 11756.619397662556 16313.913417161826, 11756.903926402016 16312.97545161008, 11757 16312, 11757 16298.071067811867, 11770.071067811867 16285, 11779 16285, 11779 16408, 11779.10334658309 16409.011328489993, 11779.409114125867 16409.980850042455, 11779.904662616025 16410.868485961517, 11780.569506756277 16411.63754259147, 11781.37616279855 16412.256228182356, 11782.291284684567 16412.69896711744, 11867.793817253614 16443.79079714255, 11875.149287499273 16473.212678125183, 11875.437354188085 16474.045058237563, 11875.867016031996 16474.813972906803, 11876.424939093213 16475.49555997127, 11877.093809031405 16476.068667356034, 11929.617157602752 16513.585344906995, 11923.527864045 16525.7639320225, 11923.186837269324 16526.645945153236, 11923.01797663615 16527.57639263219, 11923.027322266178 16528.5219924861, 11923.214539867555 16529.44892074344, 11923.572932693456 16530.324021313012, 11924.089681083758 16531.115991972598, 11924.746301022444 16531.796504044967, 11925.519305308217 16532.34121571062, 11981.519305308217 16564.34121571062, 11982.414892360963 16564.742091708587, 11983.371523667392 16564.96034449402, 11984.352358294618 16564.987568910023, 11985.319623213016 16564.822716514132, 11986.2360679775 16564.472135955, 11997.763932022499 16558.7082039325, 12001.2917960675 16565.763932022503, 11989.7639320225 16571.527864045, 11988.934426998127 16572.050030105154, 11988.222730225021 16572.7239912932, 11987.656191811051 16573.52384765864, 11987.256583509747 16574.418861169917, 11987.039262044025 16575.374636959717, 11987.012578955937 16576.354445100453, 11987.177559661593 16577.32063211475, 11987.527864045 16578.2360679775, 11988.050030105154 16579.065573001873, 11988.723991293198 16579.77726977498, 11989.523847658638 16580.343808188947, 11990.418861169916 16580.74341649025, 12035 16595.60379610028, 12035 16632, 12035.096073597984 16632.97545161008, 12035.380602337444 16633.913417161824, 12035.842651938487 16634.777851165098, 12036.464466094067 16635.53553390593, 12037.222148834902 16636.15734806151, 12038.086582838174 16636.61939766256, 12039.02454838992 16636.903926402018, 12040 16637, 12153 16637, 12133 16652, 12132.249115548368 16652.69381400546, 12131.655048874158 16653.525853740368, 12131.242636843635 16654.46133310997, 12131.029121666961 16655.461141393196, 12131.023430032195 16656.483478392023, 12131.225799896421 16657.485602023084, 12131.627770537176 16658.4256152878, 12132.212536280149 16659.264217911048, 12132.955649114396 16659.966349415434, 12133.826040819997 16660.50265493722, 12134.78732187482 16660.850712500727, 12160.740345813545 16667.33896848541, 12157.585343716513 16673.64897267947, 12121.85695338177 16659.357616545574, 12120.9215810472 16659.085665012084, 12119.951229955024 16659.000237857388, 12118.982729937787 16659.10457748294, 12118.052840570188 16659.394723661397, 12117.196855953474 16659.859663847652, 12116.44726512213 16660.48175116179, 12115.832518917316 16661.23737417925, 12115.375950130603 16662.097853106272, 12115.094887904084 16663.030528326093, 12115 16664, 12115 16704, 12115.091193243798 16704.950587308067, 12115.361446492103 16705.86649976008, 12115.800901636774 16706.714327345027, 12116.393528541752 16707.463143603843, 12117.117709779106 16708.085633743074, 12117.947029174382 16708.559091004703, 12118.851235397262 16708.866244947338, 12119.797345448336 16708.99589142523, 12120.750847789785 16708.94330128523, 12121.676961233014 16708.710392873527, 12122.541903662419 16708.30566205954, 12123.314124315664 16707.743872329596, 12123.965454670233 16707.04551625481, 12124.472135954999 16706.2360679775, 12124.472135955 16706.2360679775, 12130.764236533596 16693.65186682031, 12161.465497557989 16699.79211902519, 12156.837722339831 16713.675444679662, 12137.581138830084 16707.25658350975, 12136.607350658267 16707.037024564044, 12135.60935422935 16707.01528377118, 12134.626928464795 16707.19222769266, 12133.699231658366 16707.560803559412, 12132.863240674245 16708.106320386538, 12132.152277094296 16708.807034538095, 12131.59467906041 16709.635016401917, 12131.212671750713 16710.55726362991, 12131.021481511043 16711.537016571456, 12131.028728951362 16712.535223468254, 12131.234125197532 16713.51209700985, 12131.629483405482 16714.42870020732, 12132.19904507884 16715.24849837422, 12132.920108183393 16715.938815354646, 12133.7639320225 16716.472135955, 12163 16731.09016994375, 12163 16752, 12163.084882232237 16752.91739704001, 12163.336646931608 16753.80364579658, 12163.74674595824 16754.628655560595, 12164.301255263807 16755.364414863907, 12164.981347653245 16755.98594254969, 12165.7639320225 16756.472135955, 12177.2917960675 16762.236067977497, 12171.527864045 16773.7639320225, 12171.527864045001 16773.7639320225, 12171.16831142276 16774.713615340355, 12171.008051747693 16775.716358243055, 12171.053695260181 16776.73080053424, 12171.303359299542 16777.715099452158, 12171.74674595824 16778.628655560595, 12172.365566840295 16779.433787356207, 12173.134297403847 16780.097285519754, 12174.021229773449 16780.591782702886, 12174.989780596708 16780.896882350764, 12176 16781, 12188.39620389972 16781, 12195.256583509747 16801.581138830083, 12195.656191811051 16802.476152341358, 12196.222730225021 16803.2760087068, 12196.934426998127 16803.949969894846, 12197.7639320225 16804.472135955, 12229.7639320225 16820.472135955, 12230.851235397264 16820.866244947338, 12232 16821), \n" +
                "             (11789 16217.596117967978, 11789 16086.811388300845, 11795.256583509747 16105.581138830084, 11795.668796062157 16106.498133793217, 11796.256127670404 16107.314124315666, 11796.994831367554 16107.996118302874, 11797.855039916729 16108.516541402574, 11798.801973401967 16108.85435188984, 11799.797345448336 16108.995891425231, 11800.800911215036 16108.935437288188, 11801.7720945731 16108.675433757844, 11802.67162867776 16108.226393286028, 11803.463143603843 16107.606471458248, 11804.114636853083 16106.840732927829, 11804.599767277767 16105.960138002894, 11804.898920104923 16105.000290860493, 11805 16104, 11805 16035.090169943749, 11818.2360679775 16028.472135955, 11819.103337095343 16027.92036973673, 11819.839446565347 16027.202912748084, 11820.413285407338 16026.35008763951, 11820.800600857878 16025.39793826879, 11820.9850233907 16024.386706341003, 11820.958758559469 16023.359130630397, 11820.722916422777 16022.358640665954, 11820.287464628564 16021.427521222862, 11797.91540989897 15984.140763340205, 11810.67591879244 15965, 11868.096117967978 15965, 11883.149287499273 16025.21267812518, 11883.457988132439 16026.09048515779, 11883.923995218087 16026.895891057658, 11884.531169229589 16027.60100167818, 11885.25848151432 16028.181396464423, 11886.080742592663 16028.61697422609, 11886.969474561647 16028.892649315138, 11887.893897388698 16028.998874096822, 11888.821994936527 16028.931969619161, 11933.633054351954 16021.46345971659, 11944.697975933796 16038.060842089351, 11917.7639320225 16051.527864045, 11916.934426998127 16052.050030105154, 11916.222730225021 16052.723991293198, 11915.656191811053 16053.523847658638, 11915.256583509748 16054.418861169916, 11915.039262044025 16055.37463695972, 11915.012578955937 16056.354445100453, 11915.177559661593 16057.320632114752, 11915.527864045 16058.2360679775, 11916.050030105154 16059.065573001873, 11916.7239912932 16059.777269774979, 11917.52384765864 16060.343808188949, 11918.418861169916 16060.743416490253, 11938.026309793453 16067.2792326981, 11932.096117967978 16091, 11880 16091, 11879.02454838992 16091.096073597984, 11878.086582838174 16091.380602337444, 11877.222148834902 16091.842651938487, 11876.464466094067 16092.464466094067, 11875.842651938487 16093.222148834902, 11875.380602337444 16094.086582838174, 11875.096073597984 16095.02454838992, 11875 16096, 11875 16120, 11875.103117649238 16121.010219403292, 11875.408217297114 16121.978770226551, 11875.902714480244 16122.865702596153, 11876.566212643793 16123.634433159705, 11877.371344439403 16124.25325404176, 11878.28490054784 16124.696640700458, 11879.269199465762 16124.946304739819, 11880.283641756943 16124.991948252307, 11881.286384659645 16124.83168857724, 11882.2360679775 16124.472135955, 11911.01273475459 16110.083802566456, 11929.916197433544 16128.98726524541, 11924.621488681163 16139.576682750174, 11847.01941932431 16155.097096621545, 11846.082441912606 16155.38231974023, 11845.219043421217 16155.844728588056, 11844.462353448263 16156.46658000303, 11843.84140706266 16157.224012827566, 11843.380030694978 16158.08796348867, 11843.095927889748 16159.025281200835, 11843 16160, 11843 16204.096117967978, 11789 16217.596117967978))\n" +
                "     \n" +
                "     )"

        String multiPolygonWithoutBlank = "MULTIPOLYGON (\n" +
                "         ((12099.256583509747 16750.418861169917, 12099.043823719125 16751.339457289127, 12099.008051747693 16752.283641756945, 12099.1505450325 16753.21769721943, 12099.466215058877 16754.10826803506, 12099.943789074123 16754.92355142332, 12100.566212643795 16755.634433159707, 12101.311258675076 16756.21552726093, 12102.15232115866 16756.64608253255, 12103.059365284169 16756.91072360568, 12104 16757, 12120 16757, 12121.033369789395 16756.89204935363, 12122.02211840411 16756.572858751348, 12122.923551423319 16756.05621092588, 12123.698744736195 16755.364414863907, 12124.31422529506 16754.52734249825, 12124.743416490253 16753.581138830083, 12132.743416490253 16729.581138830083, 12132.957505083801 16728.650494691818, 12132.990757289455 16727.696122265148, 12132.841960156618 16726.752834477018, 12132.516541402574 16725.855039916725, 12132.02637142403 16725.03548770356, 12131.389330297132 16724.324072887426, 12130.628655560595 16723.74674595824, 12129.772094573098 16723.324566242154, 12128.850892364628 16723.072933714288, 12127.898651902216 16723.001027249218, 12126.950108344521 16723.111469800468, 12126.039861997104 16723.400232722233, 12125.201116186798 16723.856782723513, 12124.464466094067 16724.46446609407, 12120 16728.928932188137, 12115.535533905933 16724.46446609407, 12114.748285726344 16723.82304829255, 12113.84767884134 16723.35391746745, 12112.870860636873 16723.07642388592, 12111.85812199173 16723.002013342277, 12110.851235397264 16723.133755052662, 12109.89173196494 16723.466215058877, 12109.019188390113 16723.985680363203, 12108.269594529698 16724.670724549225, 12107.673868926402 16725.493091558503, 12107.256583509747 16726.418861169917, 12099.256583509747 16750.418861169917)), \n" +
                "         ((12232 16821, 12344 16821, 12344.91739704001 16820.915117767763, 12345.803645796579 16820.663353068394, 12346.628655560595 16820.25325404176, 12347.364414863907 16819.698744736193, 12347.98594254969 16819.018652346756, 12348.472135954999 16818.2360679775, 12348.472135955 16818.2360679775, 12355.09016994375 16805, 12371 16805, 12371 16811, 12360 16811, 12360.97545161008 16811.096073597982, 12361.913417161826 16811.38060233744, 12362.777851165098 16811.84265193849, 12363.535533905933 16812.46446609407, 12364.157348061513 16813.222148834902, 12364.619397662556 16814.086582838176, 12364.903926402016 16815.02454838992, 12365 16816, 12364.903926402016 16816.97545161008, 12364.619397662556 16817.913417161824, 12364.157348061513 16818.777851165098, 12363.535533905933 16819.53553390593, 12362.777851165098 16820.15734806151, 12361.913417161826 16820.61939766256, 12360.97545161008 16820.903926402018, 12360 16821, 12376 16821, 12376.97545161008 16820.903926402018, 12377.913417161826 16820.61939766256, 12378.777851165098 16820.15734806151, 12379.535533905933 16819.53553390593, 12380.157348061513 16818.777851165098, 12380.619397662556 16817.913417161824, 12380.903926402016 16816.97545161008, 12381 16816, 12381 16802.071067811863, 12394.071067811865 16789, 12448 16789, 12449.033369789395 16788.89204935363, 12450.02211840411 16788.572858751348, 12450.923551423319 16788.05621092588, 12451.698744736195 16787.364414863907, 12452.31422529506 16786.52734249825, 12452.743416490253 16785.581138830083, 12456.926209682668 16773.032759252837, 12459.527864045 16778.2360679775, 12460.01405745031 16779.018652346756, 12460.635585136093 16779.698744736193, 12461.371344439405 16780.25325404176, 12462.196354203421 16780.663353068394, 12463.08260295999 16780.915117767763, 12464 16781, 12480 16781, 12480.97545161008 16780.903926402018, 12481.913417161826 16780.61939766256, 12482.777851165098 16780.15734806151, 12483.535533905933 16779.53553390593, 12484.157348061513 16778.777851165098, 12484.619397662556 16777.913417161824, 12484.903926402016 16776.97545161008, 12485 16776, 12485 16763.09016994375, 12514.2360679775 16748.472135955, 12515.018652346755 16747.98594254969, 12515.698744736193 16747.364414863907, 12516.25325404176 16746.628655560595, 12516.663353068392 16745.80364579658, 12516.915117767763 16744.91739704001, 12517 16744, 12517 16731.09016994375, 12530.2360679775 16724.472135955, 12531.085088605321 16723.934746281186, 12531.809518091726 16723.238452085305, 12532.380100389919 16722.411373171915, 12532.773792563736 16721.486911079526, 12532.974695375398 16720.502400161222, 12532.974695375398 16719.497599838778, 12532.773792563736 16718.513088920474, 12532.380100389919 16717.588626828085, 12531.809518091726 16716.761547914695, 12531.085088605321 16716.065253718814, 12530.2360679775 16715.527864045, 12517 16708.90983005625, 12517 16637, 12544 16637, 12544.91739704001 16636.915117767763, 12545.803645796579 16636.663353068394, 12546.628655560595 16636.25325404176, 12547.364414863907 16635.698744736193, 12547.98594254969 16635.018652346756, 12548.472135954999 16634.2360679775, 12548.472135955 16634.2360679775, 12552 16627.1803398875, 12555.527864045 16634.2360679775, 12555.177559661593 16633.32063211475, 12555.012578955937 16632.354445100453, 12555.039262044025 16631.37463695972, 12555.256583509747 16630.418861169917, 12555.656191811051 16629.523847658642, 12556.222730225021 16628.7239912932, 12556.934426998127 16628.050030105154, 12557.763932022499 16627.527864045, 12558.679367885246 16627.177559661595, 12559.645554899545 16627.012578955935, 12560.625363040279 16627.039262044025, 12561.581138830083 16627.25658350975, 12562.47615234136 16627.656191811053, 12563.2760087068 16628.22273022502, 12563.949969894844 16628.934426998127, 12564.472135954999 16629.7639320225, 12564.472135955 16629.7639320225, 12559.050343994764 16618.920348102027, 12577.581138830084 16612.74341649025, 12578.483606473977 16612.339550539218, 12579.2889595857 16611.765998518804, 12579.965707068937 16611.04518758755, 12580.487386593235 16610.205303054652, 12580.833599329926 16609.279186271717, 12580.990807591652 16608.303050462964, 12580.952864186038 16607.31506470768, 12580.721252784557 16606.353861444375, 12580.305029907693 16605.457025856627, 12579.720470794855 16604.659626208846, 12578.990433006356 16603.992842599237, 12578.143462642007 16603.48274774865, 12577.21267812518 16603.149287499273, 12553.77995987511 16597.291107936755, 12571.535533905933 16579.53553390593, 12572.176951707452 16578.748285726342, 12572.64608253255 16577.84767884134, 12572.92357611408 16576.87086063687, 12572.997986657721 16575.85812199173, 12572.866244947338 16574.851235397262, 12572.533784941123 16573.89173196494, 12572.014319636795 16573.019188390113, 12571.329275450777 16572.2695945297, 12570.506908441499 16571.673868926402, 12569.581138830084 16571.25658350975, 12549 16564.39620389972, 12549 16541, 12568 16541, 12569.033369789395 16540.89204935363, 12570.02211840411 16540.572858751348, 12570.923551423319 16540.05621092588, 12571.698744736195 16539.364414863907, 12572.31422529506 16538.52734249825, 12572.743416490253 16537.581138830083, 12580.743416490253 16513.581138830083, 12580.956176280875 16512.660542710873, 12580.991948252307 16511.716358243055, 12580.8494549675 16510.78230278057, 12580.533784941123 16509.89173196494, 12580.056210925877 16509.07644857668, 12579.433787356205 16508.365566840293, 12578.688741324924 16507.78447273907, 12577.84767884134 16507.35391746745, 12576.940634715831 16507.08927639432, 12576 16507, 12531.903882032024 16507, 12516.850712500727 16446.787321874817, 12516.554949339838 16445.93785633102, 12516.111775709485 16445.15512732888, 12515.535533905932 16444.464466094065, 12514.84487267112 16443.888224290513, 12514.062143668976 16443.44505066016, 12513.21267812518 16443.149287499273, 12485 16436.096117967976, 12485 16352, 12484.915117767763 16351.08260295999, 12484.663353068392 16350.196354203421, 12484.25325404176 16349.371344439403, 12483.698744736193 16348.635585136093, 12483.018652346755 16348.01405745031, 12482.2360679775 16347.527864045, 12418.2360679775 16315.527864045, 12417.30005314021 16315.17197122703, 12416.311891430776 16315.009737107586, 12293 16307.302743893162, 12293 16227.090169943749, 12322.2360679775 16212.472135955, 12323.018652346755 16211.98594254969, 12323.698744736193 16211.364414863907, 12324.25325404176 16210.628655560597, 12324.663353068392 16209.803645796579, 12324.915117767763 16208.91739704001, 12325 16208, 12325 16163.903882032022, 12353.21267812518 16156.850712500727, 12354.104245686496 16156.535653215455, 12354.92055236988 16156.058370837514, 12355.632401984956 16155.435935945225, 12356.214334374043 16154.69061067116, 12356.645536025304 16153.849052470216, 12356.910584493038 16152.94136068366, 12357 16152, 12357 16027.090169943749, 12370.2360679775 16020.472135955, 12371.03747162684 16019.971620087086, 12371.730405470302 16019.329275450777, 12372.290124453866 16018.568040531289, 12372.696640700458 16017.71509945216, 12372.935437288188 16016.800911215036, 12372.997986657721 16015.85812199173, 12372.882055136191 16014.92039931123, 12372.591782702886 16014.021229773449, 12372.137535148293 16013.192723224076, 12371.535533905933 16012.464466094067, 12361.779959875112 16002.708892063245, 12385.21267812518 15996.850712500727, 12386.104245686496 15996.535653215455, 12386.92055236988 15996.058370837514, 12387.632401984956 15995.435935945225, 12388.214334374043 15994.69061067116, 12388.645536025304 15993.849052470216, 12388.910584493038 15992.94136068366, 12389 15992, 12389 15859.090169943749, 12399.01273475459 15854.083802566456, 12408.928932188133 15864, 12396.464466094067 15876.464466094067, 12395.842651938487 15877.222148834902, 12395.380602337444 15878.086582838174, 12395.096073597984 15879.02454838992, 12395 15880, 12395.096073597984 15880.97545161008, 12395.380602337444 15881.913417161826, 12395.842651938487 15882.777851165098, 12396.464466094067 15883.535533905933, 12397.222148834902 15884.157348061513, 12398.086582838174 15884.619397662556, 12399.02454838992 15884.903926402016, 12400 15885, 12416 15885, 12417.064296372917 15884.885414335611, 12418.079811503034 15884.54690929224, 12419 15884, 12483 15836, 12483.686146021682 15835.378213656186, 12484.245869823684 15834.640566121181, 12484.659974791155 15833.812356186238, 12484.914258537958 15832.921988623635, 12485 15832, 12485 15808, 12484.903926402016 15807.02454838992, 12484.619397662556 15806.086582838174, 12484.157348061513 15805.222148834902, 12483.535533905933 15804.464466094067, 12482.777851165098 15803.842651938487, 12481.913417161826 15803.380602337444, 12480.97545161008 15803.096073597984, 12480 15803, 12456 15803, 12454.99806568721 15803.101415752193, 12454.036776143214 15803.401548946858, 12453.15512732888 15803.888224290515, 12452.388884475282 15804.541699164742, 12451.769131220562 15805.33546451118, 12451.321008661724 15806.237320205953, 12451.062695473316 15807.210681299604, 12451.004670465863 15808.216062133142, 12451.149287499273 15809.21267812518, 12457.187133075315 15833.364060429352, 12393.084652289093 15819.11906469908, 12391.95615103333 15819.000192276884, 12390.829908254967 15819.138839098712, 12389.7639320225 15819.527864045, 12357.7639320225 15835.527864045, 12356.959589891658 15836.03062897009, 12356.264670762566 15836.676249785562, 12355.704184464286 15837.441490886646, 12355.29830264834 15838.29881158792, 12355.061632819992 15839.21735730028, 12355.002692620652 15840.164069973825, 12355.123603281767 15841.104877842297, 12355.420013282648 15842.005921650734, 12355.881254959764 15842.834773234941, 12356.490728431174 15843.561602596617, 12357.226499018874 15844.160251471689, 12377.066247547185 15857.386750490563, 12365.527725654672 15874.694533329332, 12300.00750935606 15867.414509296152, 12292.850712500727 15838.78732187482, 12292.55494933984 15837.937856331024, 12292.111775709485 15837.15512732888, 12291.535533905933 15836.464466094067, 12290.84487267112 15835.888224290515, 12290.062143668976 15835.44505066016, 12289.21267812518 15835.149287499273, 12261 15828.096117967978, 12261 15808, 12260.903926402016 15807.02454838992, 12260.619397662556 15806.086582838174, 12260.157348061513 15805.222148834902, 12259.535533905933 15804.464466094067, 12258.777851165098 15803.842651938487, 12257.913417161826 15803.380602337444, 12256.97545161008 15803.096073597984, 12256 15803, 12203.903882032022 15803, 12196.850712500727 15774.78732187482, 12196.535653215455 15773.895754313504, 12196.058370837514 15773.07944763012, 12195.435935945225 15772.367598015044, 12194.69061067116 15771.785665625957, 12193.849052470216 15771.354463974696, 12192.94136068366 15771.089415506962, 12192 15771, 12170.5 15771, 12148 15741, 12147.350762877026 15740.28888316784, 12146.57725319782 15739.71540364161, 12145.708158765317 15739.300830538014, 12144.775712414461 15739.060539477832, 12143.814496565974 15739.003442337373, 12142.8601606391 15739.131656726222, 12141.948098891651 15739.440427449688, 12141.112137723452 15739.918302868697, 12140.383281128248 15740.547559616343, 12139.788560822557 15741.30485991928, 12139.350033697705 15742.16211714532, 12139.083963777372 15743.087537475942, 12139.000219020096 15744.046799070324, 12139.101905338002 15745.004324988293, 12139.385251405241 15745.924602662155, 12139.839748528311 15746.773500981126, 12153.56602159979 15767.362910588343, 12144 15776.928932188133, 12133 15765.928932188133, 12133 15720, 12132.915117767763 15719.08260295999, 12132.663353068392 15718.196354203421, 12132.25325404176 15717.371344439403, 12131.698744736193 15716.635585136093, 12131.018652346755 15716.01405745031, 12130.2360679775 15715.527864045, 12118.7082039325 15709.7639320225, 12132.472135955 15682.2360679775, 12132.82470059313 15681.312350634033, 12132.988608716681 15680.337317464488, 12132.957451153788 15679.349094432499, 12132.732446234397 15678.386323254632, 12132.32239215086 15677.486650423401, 12131.74332292906 15676.68525514575, 12131.017881462369 15676.013473757883, 12130.174434124201 15675.49757440489, 12129.24596157978 15675.157729897897, 12128.268769168828 15675.007228912329, 12127.281067286118 15675.051956371159, 12126.321477270125 15675.290163331358, 12125.427521222862 15675.712535371436, 12085.427521222862 15699.712535371436, 12084.620547611654 15700.314989612647, 12083.950665882832 15701.066931093967, 12083.44505066016 15701.937856331022, 12083.124212882893 15702.892435108606, 12083.001167752831 15703.891943696817, 12083.080906755744 15704.895835729534, 12083.360195175248 15705.86338702051, 12083.827703313285 15706.75534759296, 12084.464466094067 15707.535533905933, 12091.928932188133 15715, 12080 15715, 12079.02501472162 15715.09598086189, 12078.08746141418 15715.380238517224, 12077.223334947485 15715.841859648095, 12076.46581116276 15716.463121536903, 12075.843973174307 15717.220172483027, 12075.381694800564 15718.083947525545, 12075.096723992228 15719.021284315233, 12075.000001446817 15719.996196294911, 12075.095240569643 15720.971254307744, 12075.378784907463 15721.90902359035, 12075.839748528311 15722.773500981126, 12086.657414540894 15739, 11976 15739, 11975.118843195742 15739.078256133615, 11974.265268761324 15739.310574925477, 11973.46599567075 15739.689684227422, 11972.74604313272 15740.203716988173, 11920.261519415122 15785.190451603255, 11899.535533905933 15764.464466094067, 11898.7988838132 15763.856782723511, 11897.960138002894 15763.400232722233, 11897.049891655479 15763.111469800466, 11896.101348097782 15763.001027249216, 11895.14910763537 15763.072933714286, 11894.2279054269 15763.324566242156, 11893.371344439405 15763.74674595824, 11892.610669702866 15764.324072887428, 11891.973628575968 15765.03548770356, 11891.483458597426 15765.855039916729, 11891.15803984338 15766.75283447702, 11891.009242710543 15767.696122265148, 11891.042494916199 15768.65049469182, 11891.256583509747 15769.581138830084, 11898.279385971822 15790.649546216311, 11885.928932188133 15803, 11816 15803, 11815.02454838992 15803.096073597984, 11814.086582838174 15803.380602337444, 11813.222148834902 15803.842651938487, 11812.464466094067 15804.464466094067, 11811.842651938487 15805.222148834902, 11811.380602337444 15806.086582838174, 11811.096073597984 15807.02454838992, 11811 15808, 11811 15832, 11811.110013400457 15833.043087271653, 11811.435212422513 15834.040273112214, 11811.961286566075 15834.947675999603, 11812.665085748971 15835.725365342927, 11813.51563903466 15836.339118642523, 11814.475517519453 15836.761927463382, 11815.502481404896 15836.97518595105, 11891 15844.524937810562, 11891 15867, 11789 15867, 11789 15800, 11788.915117767763 15799.08260295999, 11788.663353068392 15798.196354203421, 11788.25325404176 15797.371344439403, 11787.698744736193 15796.635585136093, 11787.018652346755 15796.01405745031, 11786.2360679775 15795.527864045, 11773 15788.909830056251, 11773 15776, 11772.898584247807 15774.99806568721, 11772.598451053142 15774.036776143214, 11772.111775709485 15773.15512732888, 11771.458300835258 15772.388884475282, 11770.66453548882 15771.769131220562, 11769.762679794047 15771.321008661724, 11768.789318700396 15771.062695473316, 11767.783937866858 15771.004670465863, 11766.78732187482 15771.149287499273, 11737.533748623875 15778.462680812008, 11725 15765.928932188133, 11725 15704, 11724.908806756202 15703.049412691933, 11724.638553507897 15702.13350023992, 11724.199098363226 15701.285672654974, 11723.606471458248 15700.536856396157, 11722.882290220894 15699.914366256928, 11722.052970825616 15699.440908995297, 11721.148764602736 15699.13375505266, 11720.202654551662 15699.004108574769, 11719.249152210214 15699.056698714769, 11718.323038766985 15699.289607126473, 11717.458096337581 15699.694337940458, 11716.685875684334 15700.256127670404, 11716.034545329765 15700.95448374519, 11715.527864045 15701.7639320225, 11710.293851553548 15712.231957005404, 11675.123475237771 15684.095655952784, 11674.314339117074 15683.567863443983, 11673.418807217988 15683.205525463809, 11672.470310326396 15683.022168323769, 11671.504256445669 15683.024636864677, 11670.556708992952 15683.212838934298, 11669.663040528443 15683.579748827464, 11668.856612274813 15684.111669560192, 11668.167528721142 15684.788744186968, 11667.621513802858 15685.585697073388, 11667.238950610552 15686.472777451963, 11667.034120476 15687.41687003754, 11667.01466984081 15688.382731242347, 11667.181324809791 15689.334304841957, 11667.527864045 15690.2360679775, 11673.916197433544 15703.01273475459, 11667.070671626017 15709.858260562116, 11661.427694229973 15681.643373581894, 11667.535533905933 15675.535533905933, 11668.157348061513 15674.777851165098, 11668.619397662556 15673.913417161826, 11668.903926402016 15672.97545161008, 11669 15672, 11668.903926402016 15671.02454838992, 11668.619397662556 15670.086582838174, 11668.157348061513 15669.222148834902, 11667.535533905933 15668.464466094067, 11651.535533905933 15652.464466094067, 11650.777367961331 15651.842329112667, 11649.912343337313 15651.380157690977, 11648.973741684089 15651.095733782444, 11647.997675513416 15651.000000540324, 11647.0216987769 15651.09664128205, 11646.083361985191 15651.381937774328, 11645.21876745955 15651.844913291405, 11644.461180302236 15652.46775494243, 11643.839748528311 15653.226499018874, 11631.222875849284 15672.151808037417, 11616.440825170412 15657.369757358545, 11626.2360679775 15652.472135955, 11627.04365100412 15651.966886507718, 11627.740753474436 15651.317644261142, 11628.302071479226 15650.54797586088, 11628.707229908021 15649.68581926464, 11628.941522039011 15648.76246963085, 11628.996443373837 15647.811443345281, 11628.870000339775 15646.867261420015, 11628.566782653821 15645.964196425793, 11628.09779672196 15645.135028442464, 11627.480066121001 15644.40985518489, 11626.736013664973 15643.814999495211, 11625.892647486266 15643.372053858056, 11624.98058067569 15643.097096621545, 11585.95049845467 15635.29108017734, 11540.200915503603 15604.791358209963, 11532.743416490253 15582.418861169916, 11532.31422529506 15581.472657501748, 11531.698744736193 15580.635585136093, 11530.923551423319 15579.943789074123, 11530.022118404107 15579.427141248654, 11529.033369789395 15579.107950646368, 11528 15579, 11400 15579, 11399.023522491234 15579.096277773479, 11398.084650214627 15579.38140333005, 11397.219540183127 15579.844396168213, 11396.461508743794 15580.467425920131, 11395.839748528311 15581.226499018874, 11380.98857024995 15603.503266436417, 11343.01941932431 15611.097096621545, 11342.082441912606 15611.38231974023, 11341.219043421217 15611.844728588056, 11340.462353448263 15612.46658000303, 11339.84140706266 15613.224012827566, 11339.380030694978 15614.08796348867, 11339.095927889748 15615.025281200835, 11339 15616, 11339 15661.928932188133, 11328 15672.928932188133, 11299.535533905933 15644.464466094067, 11298.777851165098 15643.842651938487, 11297.913417161826 15643.380602337444, 11296.97545161008 15643.096073597984, 11296 15643, 11295.02454838992 15643.096073597984, 11294.086582838174 15643.380602337444, 11293.222148834902 15643.842651938487, 11292.464466094067 15644.464466094067, 11261.928932188133 15675, 11248 15675, 11247.05863931634 15675.089415506962, 11246.150947529784 15675.354463974696, 11245.30938932884 15675.785665625957, 11244.564064054775 15676.367598015044, 11243.941629162486 15677.07944763012, 11243.464346784545 15677.895754313504, 11243.149287499273 15678.78732187482, 11235.70704051876 15708.55630979687, 11224.98726524541 15713.916197433544, 11219.535533905933 15708.464466094067, 11218.777851165098 15707.842651938487, 11217.913417161826 15707.380602337444, 11216.97545161008 15707.096073597984, 11216 15707, 11215.02454838992 15707.096073597984, 11214.086582838174 15707.380602337444, 11213.222148834902 15707.842651938487, 11212.464466094067 15708.464466094067, 11211.842651938487 15709.222148834902, 11211.380602337444 15710.086582838174, 11211.096073597984 15711.02454838992, 11211 15712, 11211 15757.928932188133, 11197.928932188133 15771, 11184 15771, 11182.966630210605 15771.107950646368, 11181.977881595893 15771.427141248654, 11181.076448576681 15771.943789074123, 11180.301255263807 15772.635585136093, 11179.68577470494 15773.472657501748, 11179.256583509747 15774.418861169916, 11171.256583509747 15798.418861169916, 11171.039262044025 15799.37463695972, 11171.012578955937 15800.354445100453, 11171.177559661593 15801.320632114752, 11171.527864045 15802.2360679775, 11177.916197433544 15815.01273475459, 11148.464466094067 15844.464466094067, 11147.842651938487 15845.222148834902, 11147.380602337444 15846.086582838174, 11147.096073597984 15847.02454838992, 11147 15848, 11147 15928, 11147.084882232237 15928.91739704001, 11147.336646931608 15929.803645796579, 11147.74674595824 15930.628655560597, 11148.301255263807 15931.364414863907, 11148.981347653245 15931.98594254969, 11149.7639320225 15932.472135955, 11163 15939.090169943749, 11163 15952, 11163.096073597984 15952.97545161008, 11163.380602337444 15953.913417161826, 11163.842651938487 15954.777851165098, 11164.464466094067 15955.535533905933, 11165.222148834902 15956.157348061513, 11166.086582838174 15956.619397662556, 11167.02454838992 15956.903926402016, 11168 15957, 11168.97545161008 15956.903926402016, 11169.913417161826 15956.619397662556, 11170.777851165098 15956.157348061513, 11171.535533905933 15955.535533905933, 11176 15951.071067811867, 11203 15978.071067811867, 11203 15992, 11203.1004701447 15992.997299953387, 11203.39784287881 15993.954520358522, 11203.88016736968 15994.833192386362, 11204.528059932753 15995.598003914594, 11205.315483024204 15996.218218653254, 11206.210791639258 15996.668911376526, 11207.178005063473 15996.931969619161, 11248.097326455327 16003.751856517803, 11219.658784289377 16053.519305308217, 11219.258055694372 16054.414451450657, 11219.039772462535 16055.370601257893, 11219.012332997754 16056.350967056436, 11219.176793027977 16057.317829467353, 11219.526824986278 16058.233988651855, 11220.048961462746 16059.064195567702, 11220.72311336039 16059.776508169081, 11221.523342819153 16060.343520370456, 11222.418861169916 16060.743416490253, 11238.743852065889 16066.185080122244, 11236.464466094067 16068.464466094067, 11235.860067039754 16069.19626051769, 11235.404841576306 16070.0290816706, 11235.115192775396 16070.932920631607, 11235.001557509338 16071.875209505366, 11235.068030380839 16072.821994936527, 11243.068030380839 16120.821994936527, 11243.350353233658 16121.838691096473, 11243.839748528311 16122.773500981126, 11257.56602159979 16143.362910588343, 11252.464466094067 16148.464466094067, 11251.842651938487 16149.222148834902, 11251.380602337444 16150.086582838174, 11251.096073597984 16151.02454838992, 11251 16152, 11251.096073597984 16152.97545161008, 11251.380602337444 16153.913417161826, 11251.842651938487 16154.777851165098, 11252.464466094067 16155.535533905933, 11253.222148834902 16156.157348061513, 11254.086582838174 16156.619397662556, 11255.02454838992 16156.903926402016, 11256 16157, 11277.928932188133 16157, 11308.464466094067 16187.535533905933, 11309.222148834902 16188.157348061513, 11310.086582838174 16188.619397662556, 11311.02454838992 16188.903926402016, 11312 16189, 11406.48612181134 16189, 11429.226499018874 16204.160251471689, 11430.054508948755 16204.605981390489, 11430.951332889654 16204.888793030153, 11431.885249260267 16204.998683053338, 11432.82322448207 16204.931764537376, 11433.732081410813 16204.690404458714, 11434.579672846094 16204.283139970528, 11435.336018610757 16203.724376434879, 11435.974365980475 16203.033877890297, 11436.472135955 16202.2360679775, 11443.090169943749 16189, 11492.096117967978 16189, 11499.149287499273 16217.21267812518, 11499.488652884693 16218.155863447719, 11500.009332453727 16219.012403116303, 11500.690399731739 16219.747872204907, 11501.504482150325 16220.332711698458, 11502.418861169916 16220.743416490253, 11544.20854129694 16234.673309865928, 11532.91200307469 16251.618117199305, 11502.78732187482 16259.149287499273, 11501.895754313504 16259.464346784545, 11501.07944763012 16259.941629162486, 11500.367598015044 16260.564064054775, 11499.785665625957 16261.30938932884, 11499.354463974696 16262.150947529784, 11499.089415506962 16263.05863931634, 11499 16264, 11499 16280, 11499.096073597984 16280.97545161008, 11499.380602337444 16281.913417161826, 11499.842651938487 16282.777851165098, 11500.464466094067 16283.535533905933, 11501.222148834902 16284.157348061513, 11502.086582838174 16284.619397662556, 11503.02454838992 16284.903926402016, 11504 16285, 11556.096117967978 16285, 11563.149287499273 16313.21267812518, 11563.464346784545 16314.104245686496, 11563.941629162486 16314.92055236988, 11564.564064054775 16315.632401984956, 11565.30938932884 16316.214334374043, 11566.150947529784 16316.645536025304, 11567.05863931634 16316.910584493038, 11568 16317, 11752 16317, 11752.97545161008 16316.903926402016, 11753.913417161826 16316.619397662556, 11754.777851165098 16316.157348061513, 11755.535533905933 16315.535533905933, 11756.157348061513 16314.777851165098, 11756.619397662556 16313.913417161826, 11756.903926402016 16312.97545161008, 11757 16312, 11757 16298.071067811867, 11770.071067811867 16285, 11779 16285, 11779 16408, 11779.10334658309 16409.011328489993, 11779.409114125867 16409.980850042455, 11779.904662616025 16410.868485961517, 11780.569506756277 16411.63754259147, 11781.37616279855 16412.256228182356, 11782.291284684567 16412.69896711744, 11867.793817253614 16443.79079714255, 11875.149287499273 16473.212678125183, 11875.437354188085 16474.045058237563, 11875.867016031996 16474.813972906803, 11876.424939093213 16475.49555997127, 11877.093809031405 16476.068667356034, 11929.617157602752 16513.585344906995, 11923.527864045 16525.7639320225, 11923.186837269324 16526.645945153236, 11923.01797663615 16527.57639263219, 11923.027322266178 16528.5219924861, 11923.214539867555 16529.44892074344, 11923.572932693456 16530.324021313012, 11924.089681083758 16531.115991972598, 11924.746301022444 16531.796504044967, 11925.519305308217 16532.34121571062, 11981.519305308217 16564.34121571062, 11982.414892360963 16564.742091708587, 11983.371523667392 16564.96034449402, 11984.352358294618 16564.987568910023, 11985.319623213016 16564.822716514132, 11986.2360679775 16564.472135955, 11997.763932022499 16558.7082039325, 12001.2917960675 16565.763932022503, 11989.7639320225 16571.527864045, 11988.934426998127 16572.050030105154, 11988.222730225021 16572.7239912932, 11987.656191811051 16573.52384765864, 11987.256583509747 16574.418861169917, 11987.039262044025 16575.374636959717, 11987.012578955937 16576.354445100453, 11987.177559661593 16577.32063211475, 11987.527864045 16578.2360679775, 11988.050030105154 16579.065573001873, 11988.723991293198 16579.77726977498, 11989.523847658638 16580.343808188947, 11990.418861169916 16580.74341649025, 12035 16595.60379610028, 12035 16632, 12035.096073597984 16632.97545161008, 12035.380602337444 16633.913417161824, 12035.842651938487 16634.777851165098, 12036.464466094067 16635.53553390593, 12037.222148834902 16636.15734806151, 12038.086582838174 16636.61939766256, 12039.02454838992 16636.903926402018, 12040 16637, 12153 16637, 12133 16652, 12132.249115548368 16652.69381400546, 12131.655048874158 16653.525853740368, 12131.242636843635 16654.46133310997, 12131.029121666961 16655.461141393196, 12131.023430032195 16656.483478392023, 12131.225799896421 16657.485602023084, 12131.627770537176 16658.4256152878, 12132.212536280149 16659.264217911048, 12132.955649114396 16659.966349415434, 12133.826040819997 16660.50265493722, 12134.78732187482 16660.850712500727, 12160.740345813545 16667.33896848541, 12157.585343716513 16673.64897267947, 12121.85695338177 16659.357616545574, 12120.9215810472 16659.085665012084, 12119.951229955024 16659.000237857388, 12118.982729937787 16659.10457748294, 12118.052840570188 16659.394723661397, 12117.196855953474 16659.859663847652, 12116.44726512213 16660.48175116179, 12115.832518917316 16661.23737417925, 12115.375950130603 16662.097853106272, 12115.094887904084 16663.030528326093, 12115 16664, 12115 16704, 12115.091193243798 16704.950587308067, 12115.361446492103 16705.86649976008, 12115.800901636774 16706.714327345027, 12116.393528541752 16707.463143603843, 12117.117709779106 16708.085633743074, 12117.947029174382 16708.559091004703, 12118.851235397262 16708.866244947338, 12119.797345448336 16708.99589142523, 12120.750847789785 16708.94330128523, 12121.676961233014 16708.710392873527, 12122.541903662419 16708.30566205954, 12123.314124315664 16707.743872329596, 12123.965454670233 16707.04551625481, 12124.472135954999 16706.2360679775, 12124.472135955 16706.2360679775, 12130.764236533596 16693.65186682031, 12161.465497557989 16699.79211902519, 12156.837722339831 16713.675444679662, 12137.581138830084 16707.25658350975, 12136.607350658267 16707.037024564044, 12135.60935422935 16707.01528377118, 12134.626928464795 16707.19222769266, 12133.699231658366 16707.560803559412, 12132.863240674245 16708.106320386538, 12132.152277094296 16708.807034538095, 12131.59467906041 16709.635016401917, 12131.212671750713 16710.55726362991, 12131.021481511043 16711.537016571456, 12131.028728951362 16712.535223468254, 12131.234125197532 16713.51209700985, 12131.629483405482 16714.42870020732, 12132.19904507884 16715.24849837422, 12132.920108183393 16715.938815354646, 12133.7639320225 16716.472135955, 12163 16731.09016994375, 12163 16752, 12163.084882232237 16752.91739704001, 12163.336646931608 16753.80364579658, 12163.74674595824 16754.628655560595, 12164.301255263807 16755.364414863907, 12164.981347653245 16755.98594254969, 12165.7639320225 16756.472135955, 12177.2917960675 16762.236067977497, 12171.527864045 16773.7639320225, 12171.527864045001 16773.7639320225, 12171.16831142276 16774.713615340355, 12171.008051747693 16775.716358243055, 12171.053695260181 16776.73080053424, 12171.303359299542 16777.715099452158, 12171.74674595824 16778.628655560595, 12172.365566840295 16779.433787356207, 12173.134297403847 16780.097285519754, 12174.021229773449 16780.591782702886, 12174.989780596708 16780.896882350764, 12176 16781, 12188.39620389972 16781, 12195.256583509747 16801.581138830083, 12195.656191811051 16802.476152341358, 12196.222730225021 16803.2760087068, 12196.934426998127 16803.949969894846, 12197.7639320225 16804.472135955, 12229.7639320225 16820.472135955, 12230.851235397264 16820.866244947338, 12232 16821) \n" +
                "             )\n" +
                "     \n" +
                "     )"

        //add annotation with empty space inside it
        def annotationToFill = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationToFill.location = new WKTReader().read(multiPolygon)
        assert annotationToFill.save(flush: true)  != null

        //do fill action
        def result = AnnotationDomainAPI.fill(annotationToFill.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //check if annotation is well filled
        annotationToFill.refresh()
        annotationToFill.location.toString().replaceAll(" ","").equals(multiPolygonWithoutBlank.replaceAll(" ",""))
    }

    void testFreehandAnnotationCorrectionUserAdd() {
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(BasicInstanceBuilder.getProject(),true)
        doFreeHandAnnotationAdd(annotation,false)
    }

    void testFreehandAnnotationCorrectionReviewedAdd() {
        def annotation = BasicInstanceBuilder.createReviewAnnotation(BasicInstanceBuilder.getImageInstance())
        doFreeHandAnnotationAdd(annotation,true)
    }

    void testFreehandAnnotationCorrectionReviewedAddBadGeom() {
        def annotation = BasicInstanceBuilder.createReviewAnnotation(BasicInstanceBuilder.getImageInstance())
        doFreeHandAnnotationAddWithSelfIntersectPolygon(annotation,true)
    }

    void testFreehandAnnotationCorrectionUserRem() {
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(BasicInstanceBuilder.getProject(),true)
        doFreeHandAnnotationRem(annotation,false)
    }

    void testFreehandAnnotationCorrectionReviewedRem() {
        def annotation = BasicInstanceBuilder.createReviewAnnotation(BasicInstanceBuilder.getImageInstance())
        doFreeHandAnnotationRem(annotation,true)
    }

    void testFreehandAnnotationCorrectionUserMerge() {
        def image = BasicInstanceBuilder.getImageInstance()
        def annot1 = BasicInstanceBuilder.getUserAnnotationNotExist(image.project, image, true)
        def annot2 = BasicInstanceBuilder.getUserAnnotationNotExist(image.project, image, true)
        doFreehandAnnotationMerge(annot1, annot2, false)
    }

    void testFreehandAnnotationCorrectionReviewedMerge() {
        def image = BasicInstanceBuilder.getImageInstance()
        def annot1 = BasicInstanceBuilder.createReviewAnnotation(image)
        def annot2 = BasicInstanceBuilder.createReviewAnnotation(image)
        doFreehandAnnotationMerge(annot1, annot2, true)
    }

    void testFreehandAnnotationCorrectionUserTargetted() {
        def image = BasicInstanceBuilder.getImageInstance()
        def annot1 = BasicInstanceBuilder.getUserAnnotationNotExist(image.project, image, true)
        def annot2 = BasicInstanceBuilder.getUserAnnotationNotExist(image.project, image, true)
        doFreehandAnnotationTargettedAdd(annot1, annot2, false)
    }

    void testFreehandAnnotationCorrectionReviewedTargetted() {
        def image = BasicInstanceBuilder.getImageInstance()
        def annot1 = BasicInstanceBuilder.createReviewAnnotation(image)
        def annot2 = BasicInstanceBuilder.createReviewAnnotation(image)
        doFreehandAnnotationTargettedAdd(annot1, annot2, true)
    }

    private void doFreeHandAnnotationAdd(def annotation, boolean reviewMode) {
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
        String expectedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"

        //add annotation with empty space inside it
        annotation.user = User.findByUsername(Infos.SUPERADMINLOGIN)
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correct add
        def json = [:]
        json.location = addedLocation
        json.image = annotation.image.id
        json.review = reviewMode
        json.remove = false
        json.layers = [annotation.user.id]
        def result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        annotation.refresh()
        assert new WKTReader().read(expectedLocation).equals(annotation.location)
        //assertEquals(expectedLocation,annotationToFill.location.toString())
    }

    private void doFreeHandAnnotationAddWithSelfIntersectPolygon(def annotation,boolean reviewMode) {
        String basedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String addedLocation = "POLYGON((0 0, 10 10, 0 10, 10 0, 0 0))"

        //add annotation with empty space inside it
        annotation.user = User.findByUsername(Infos.SUPERADMINLOGIN)
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correct remove
        def json = [:]
        json.location = addedLocation
        json.image = annotation.image.id
        json.review = reviewMode
        json.remove = false
        json.layers = [annotation.user.id]
        def result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    private void doFreeHandAnnotationRem(def annotation, boolean reviewMode) {
        String basedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"
        String removedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
        String expectedLocation = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"

        //add annotation with empty space inside it
        annotation.user = User.findByUsername(Infos.SUPERADMINLOGIN)
        annotation.location = new WKTReader().read(basedLocation)
        assert annotation.save(flush: true)  != null

        //correct remove
        def json = [:]
        json.location = removedLocation
        json.image = annotation.image.id
        json.review = reviewMode
        json.remove = true
        json.layers = [annotation.user.id]
        def result = AnnotationDomainAPI.correctAnnotation(annotation.id, JSONUtils.toJSONString(json),Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        annotation.refresh()
        assert new WKTReader().read(expectedLocation).equals(annotation.location)
    }

    private void doFreehandAnnotationMerge(def annot1, def annot2, boolean reviewMode) {
        String location1 = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String location2 = "POLYGON ((0 10000, 0 15000, 10000 15000, 10000 10000, 0 10000))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
        String expectedLocation = "POLYGON ((0 0, 0 15000, 10000 15000, 10000 0, 0 0))"

        annot1.location = new WKTReader().read(location1)
        assert annot1.save(flush: true) != null

        annot2.location = new WKTReader().read(location2)
        assert annot2.save(flush: true) != null

        def json = [:]
        json.location = addedLocation
        json.image = annot1.image.id
        json.review = reviewMode
        json.remove = false
        json.layers = [annot1.user.id]

        def result = AnnotationDomainAPI.correctAnnotation(annot1.id, JSONUtils.toJSONString(json), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def jsonResult = JSON.parse(result.data)
        assert jsonResult instanceof JSONObject
        println jsonResult
        def merge = reviewMode ? jsonResult.reviewedannotation : jsonResult.annotation
        assert new WKTReader().read(expectedLocation).equals(new WKTReader().read(merge.location))
    }

    private void doFreehandAnnotationTargettedAdd(def annot1, def annot2, boolean reviewMode) {
        String location1 = "POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))"
        String location2 = "POLYGON ((0 10000, 0 15000, 10000 15000, 10000 10000, 0 10000))"
        String addedLocation = "POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))"
        String expectedLocation = "POLYGON ((0 0, 0 10000, 10000 10000, 10000 0, 0 0))"

        annot1.location = new WKTReader().read(location1)
        assert annot1.save(flush: true) != null

        annot2.location = new WKTReader().read(location2)
        assert annot2.save(flush: true) != null

        def json = [:]
        json.location = addedLocation
        json.review = reviewMode
        json.remove = false
        json.annotation = annot1.id

        def result = AnnotationDomainAPI.correctAnnotation(annot1.id, JSONUtils.toJSONString(json), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        annot1.refresh()
        assert new WKTReader().read(expectedLocation).equals(annot1.location)

        Long commandId = JSON.parse(result.data).command
        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code

        annot1.refresh()
        assert !(new WKTReader().read(expectedLocation).equals(annot1.location))
    }


    static def polygones = [
                    a: "POLYGON ((1 1, 2 1, 2 2, 1 2, 1 1))",
                    b: "POLYGON ((1 3, 2 3, 2 5, 1 5, 1 3))",
                    c: "POLYGON ((3 1, 5 1,  5 3, 3 3, 3 1))",
                    d: "POLYGON ((4 4,8 4, 8 7,4 7,4 4))",
                    e: "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",
      ]



    def testAnnotationIncludeFilterUserAnnotation() {

        Project project  = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image =  BasicInstanceBuilder.getImageInstanceNotExist(project,true)

        User user1 = BasicInstanceBuilder.getUser1()
        User user2 = BasicInstanceBuilder.getUser2()

        Term term1 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)
        Term term2 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)

        UserAnnotation a1 = BasicInstanceBuilder.getUserAnnotationNotExist(image, polygones['a'],user1,term1)
        UserAnnotation a2 = BasicInstanceBuilder.getUserAnnotationNotExist(image, polygones['b'],user1,term2)
        UserAnnotation a3 = BasicInstanceBuilder.getUserAnnotationNotExist(image, polygones['c'],user2,term1)
        UserAnnotation a4 = BasicInstanceBuilder.getUserAnnotationNotExist(image, polygones['d'],user2,term2)

        checkIncluded(image,a1,a2,a3,a4,user1,user2,term1,term2)

        def result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "pdf",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "csv",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

    }


    def testAnnotationIncludeFilterAlgoAnnotation() {

        Project project  = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image =  BasicInstanceBuilder.getImageInstanceNotExist(project,true)

        UserJob user1 = BasicInstanceBuilder.getUserJob(project)
        UserJob user2 = BasicInstanceBuilder.getUserJob(project)

        Term term1 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)
        Term term2 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)

        AlgoAnnotation a1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, polygones['a'],user1,term1)
        AlgoAnnotation a2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, polygones['b'],user1,term2)
        AlgoAnnotation a3 = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, polygones['c'],user2,term1)
        AlgoAnnotation a4 = BasicInstanceBuilder.getAlgoAnnotationNotExist(image, polygones['d'],user2,term2)

        checkIncluded(image,a1,a2,a3,a4,user1,user2,term1,term2)

        def result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "pdf",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "csv",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


    public static def checkIncluded(
            ImageInstance image,
            AnnotationDomain a1,
            AnnotationDomain a2,
            AnnotationDomain a3,
            AnnotationDomain a4,
            SecUser user1,
            SecUser user2,
            Term term1,
            Term term2) {

        //tatic def listIncluded(String geometry, Long idImage, Long idUser,List<Long> terms,String username, String password) {
        def result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)

        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user2.id, [term1.id,term2.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)

        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user2.id, [term1.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert !AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)

        UserAnnotation a5 = BasicInstanceBuilder.getUserAnnotationNotExist(image, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",User.findByUsername(Infos.SUPERADMINLOGIN),term2)

        result = AnnotationDomainAPI.listIncluded(a5, image.id, user1.id, [term1.id,term2.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a5.id,result.data)
    }




    def testAnnotationIncludeFilterReviwedAnnotation() {

        Project project  = BasicInstanceBuilder.getProjectNotExist(true)
        ImageInstance image =  BasicInstanceBuilder.getImageInstanceNotExist(project,true)

        User user1 = BasicInstanceBuilder.getUser1()
        User user2 = BasicInstanceBuilder.getUser2()

        Term term1 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)
        Term term2 =  BasicInstanceBuilder.getTermNotExist(project.ontology,true)

        ReviewedAnnotation a1 = BasicInstanceBuilder.getReviewedAnnotationNotExist(image, polygones['a'],user1,term1)
        ReviewedAnnotation a2 = BasicInstanceBuilder.getReviewedAnnotationNotExist(image, polygones['b'],user1,term2)
        ReviewedAnnotation a3 = BasicInstanceBuilder.getReviewedAnnotationNotExist(image, polygones['c'],user2,term1)
        ReviewedAnnotation a4 = BasicInstanceBuilder.getReviewedAnnotationNotExist(image, polygones['d'],user2,term2)

        //tatic def listIncluded(String geometry, Long idImage, Long idUser,List<Long> terms,String username, String password) {
        def result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, 0, null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)

        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, 0, [term1.id,term2.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)

        result = AnnotationDomainAPI.listIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, 0, [term1.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)


        UserAnnotation a5 = BasicInstanceBuilder.getUserAnnotationNotExist(image, "POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))",user1,term2)

        result = AnnotationDomainAPI.listIncluded(a5, image.id, 0, [term1.id], Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert AnnotationDomainAPI.containsInJSONList(a1.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a2.id,result.data)
        assert AnnotationDomainAPI.containsInJSONList(a3.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a4.id,result.data)
        assert !AnnotationDomainAPI.containsInJSONList(a5.id,result.data)


        result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "pdf",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        result = AnnotationDomainAPI.downloadIncluded("POLYGON ((2 2, 3 2, 3 4, 2 4, 2 2))", image.id, user1.id, [term1.id,term2.id], "csv",Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }


    def testUserAnnotationSimplificationWithParameter() {

        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project,false)
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/very_big_annotation.txt').text)
        assert annotation.location.numPoints > 500

        int maxPoint
        int minPoint

        maxPoint = 50
        minPoint = 10

        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

        maxPoint = 150
        minPoint = 100
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation.txt').text)
        assert annotation.location.numPoints > 500
        result = UserAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

        maxPoint = 1000
        minPoint = 400
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation.txt').text)
        assert annotation.location.numPoints > 500
        result = UserAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

    }


    def testAlgoAnnotationSimplificationWithParameter() {
        def geometryVeryBig = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation.txt').text)
        def annotation = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotation.user
        annotation.location =  geometryVeryBig


        int maxPoint
        int minPoint

        maxPoint = 50
        minPoint = 10

        def result = AlgoAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint, user.username, 'PasswordUserJob')
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

        maxPoint = 150
        minPoint = 100
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation.txt').text)
        assert annotation.location.numPoints > 500
        result = AlgoAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint,user.username, 'PasswordUserJob')
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

        maxPoint = 1000
        minPoint = 400
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation.txt').text)
        assert annotation.location.numPoints > 500
        result = AlgoAnnotationAPI.create(annotation.encodeAsJSON(), minPoint,maxPoint,user.username, 'PasswordUserJob')
        assert 200 == result.code
        annotation = result.data
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

    }


    def testSimplifyService() {

        //create annotation
        def annotation = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotation.user

        //add very big geometry
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/very_big_annotation.txt').text)
        annotation.save(flush:true)
        assert annotation.location.numPoints > 500

        int maxPoint
        int minPoint

        //simplify
        maxPoint = 50
        minPoint = 10

        def result = AnnotationDomainAPI.simplifyAnnotation(annotation.id,minPoint,maxPoint,user.username, 'PasswordUserJob')
        assert 200==result.code

        //check if points are ok
        annotation.refresh()
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

    }


    def testSimplifyServiceWithEmptySpace() {

        //create annotation
        def annotation = BasicInstanceBuilder.getAlgoAnnotation()
        UserJob user = annotation.user

        //add very big geometry
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/annotationbig_emptyspace.txt').text)
        annotation.save(flush:true)
        assert annotation.location.numPoints > 500
        println "AAA NUMBER OF POINT: " + annotation.location.numPoints
        int maxPoint
        int minPoint

        //simplify
        maxPoint = 5000*10
        minPoint = 1000

        def result = AnnotationDomainAPI.simplifyAnnotation(annotation.id,minPoint,maxPoint,user.username, 'PasswordUserJob')
        assert 200==result.code

        //check if points are ok
        annotation.refresh()
        assert annotation.location.numPoints <= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, maxPoint)
        assert annotation.location.numPoints >= getPointMultiplyByGeometriesOrInteriorRings(annotation.location, minPoint)

        println "BBB NUMBER OF POINT: " + annotation.location.numPoints

    }

    private long getPointMultiplyByGeometriesOrInteriorRings(Geometry geometry, int numberOfPoints){
        long result = 0
        if (geometry instanceof MultiPolygon) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                Geometry geom = geometry.getGeometryN(i)
                int nbInteriorRing = 1
                if(geom instanceof Polygon)
                    nbInteriorRing = geom.getNumInteriorRing()
                result +=  geom.getNumGeometries() * nbInteriorRing
            }
        } else {
            int nbInteriorRing = 1
            if(geometry instanceof Polygon)
                nbInteriorRing = geometry.getNumInteriorRing()
            result = geometry.getNumGeometries() * nbInteriorRing
        }
        result = Math.max(1, result)

        if (result > 10) result/= 2
        result = Math.min(10, result)

        result*=numberOfPoints
        return result
    }
/*
    def testSimplifyServiceWithHole() {
        Project project = BasicInstanceBuilder.getProjectNotExist(true)
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotationNotExist(project,false)
        annotation.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/big_annotation_hole.txt').text)
        println "START NUMBER OF POINT:" + annotation.location.numPoints

        def result = UserAnnotationAPI.create(annotation.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        annotation = result.data
        println "END NUMBER OF POINT:" + annotation.location.numPoints

    }
*/
}
