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
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.security.User
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.AlgoAnnotationAPI
import be.cytomine.test.http.AnnotationTermAPI
import be.cytomine.test.http.UserAnnotationAPI
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.UpdateData
import com.vividsolutions.jts.io.WKTReader
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
class UserAnnotationTests  {

    void testGetUserAnnotationWithCredential() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testListUserAnnotationWithCredential() {
        BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testCountAnnotationWithCredential() {
        def result = UserAnnotationAPI.countByUser(BasicInstanceBuilder.getUser1().id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProject() {
        def result = UserAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testCountAnnotationByProjectWithDates() {
        Date startDate = new Date()
        def result = UserAnnotationAPI.countByProject(BasicInstanceBuilder.getProject().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD, startDate.getTime(), startDate.getTime() - 1000)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert json.total >= 0
    }

    void testDownloadUserAnnotationDocument() {
        AnnotationTerm annotationTerm = BasicInstanceBuilder.getAnnotationTerm()
        def result = UserAnnotationAPI.downloadDocumentByProject(annotationTerm.userAnnotation.project.id,annotationTerm.userAnnotation.user.id,annotationTerm.term.id, annotationTerm.userAnnotation.image.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListUserAnnotationTermsWithCredential() {
        def annotation = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def result = UserAnnotationAPI.show(annotation.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        assert annotation.terms().size() == 0

        def annotationTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotation, true)

        assert annotation.terms().size() == 1

        result = AnnotationTermAPI.deleteAnnotationTerm(annotationTerm.userAnnotation.id,annotationTerm.term.id,User.findByUsername(Infos.SUPERADMINLOGIN).id,Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert annotation.terms().size() == 0
    }

    void testAddBigUserAnnotationWithMaxNumberOfPoint() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotationNotExist()
        annotationToAdd.location = new WKTReader().read(new File('test/functional/be/cytomine/utils/very_big_annotation.txt').text)
        int maxPoints = 100
        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), 0, maxPoints, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id
        assert result.data.location.numPoints <= maxPoints

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
    void testAddTooLittleUserAnnotation() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "POLYGON ((225.73582220103702 306.89723126347087, 225.73582220103702 307.93556995227914, 226.08028300710947 307.93556995227914, 226.08028300710947 306.89723126347087, 225.73582220103702 306.89723126347087))"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }
    void testAddUserAnnotationMultiLine() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "LINESTRING(181.05636403199998 324.87936288,208.31216076799996 303.464094016)"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrect() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def result = UserAnnotationAPI.create(annotationToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        Long commandId = result.command

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


        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        //200 because the undoed annotation was not this one
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationMultipleCorrect() {
        def annotationToAdd1 = BasicInstanceBuilder.getUserAnnotation()
        def annotationToAdd2 = BasicInstanceBuilder.getUserAnnotation()
        def annotations = []
        annotations << JSON.parse(annotationToAdd1.encodeAsJSON())
        annotations << JSON.parse(annotationToAdd2.encodeAsJSON())
        def result = UserAnnotationAPI.create(JSONUtils.toJSONString(annotations), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrectWithoutProject() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.project = null
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationCorrectWithTerm() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        Term term1 = BasicInstanceBuilder.getTerm()
        Term term2 = BasicInstanceBuilder.getAnotherBasicTerm()
        term2.ontology = term1.ontology
        annotationToAdd.project.ontology = term1.ontology
        BasicInstanceBuilder.saveDomain(annotationToAdd.project)
        BasicInstanceBuilder.saveDomain(term2)


        def annotationWithTerm = JSON.parse((String)annotationToAdd.encodeAsJSON())
        annotationWithTerm.term = [term1.id, term2.id]

        log.info annotationToAdd.project.ontology.id

        def result = UserAnnotationAPI.create(annotationWithTerm.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idAnnotation = result.data.id

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)
        assert json.term.size() == 2


        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserAnnotationBadGeom() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POINT(BAD GEOMETRY)'

        Long idTerm1 = BasicInstanceBuilder.getTerm().id
        Long idTerm2 = BasicInstanceBuilder.getAnotherBasicTerm().id
        updateAnnotation.term = [idTerm1, idTerm2]

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationOutOfBoundsGeom() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        ImageInstance im = annotationToAdd.image

        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = "POLYGON((-1 -1,-1 $im.baseImage.height,${im.baseImage.width+5} $im.baseImage.height,$im.baseImage.width 0,-1 -1))"

        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        assert result.data.location.toString() == "POLYGON ((0 $im.baseImage.height, $im.baseImage.width $im.baseImage.height, $im.baseImage.width 0, 0 0, 0 $im.baseImage.height))"
    }

    void testAddUserAnnotationBadGeomEmpty() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = 'POLYGON EMPTY'
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationBadGeomNull() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.location = null
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserAnnotationImageNotExist() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def updateAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        updateAnnotation.image = -99
        def result = UserAnnotationAPI.create(updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testEditUserAnnotation() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def data = UpdateData.createUpdateSet(
                BasicInstanceBuilder.getUserAnnotation(),
                [location: [new WKTReader().read("POLYGON ((2107 2160, 2047 2074, 1983 2168, 1983 2168, 2107 2160))"),new WKTReader().read("POLYGON ((1983 2168, 2107 2160, 2047 2074, 1983 2168))")]]
        )

        def result = UserAnnotationAPI.update(annotationToAdd.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
        int idAnnotation = json.annotation.id

        Long commandId = JSON.parse(result.data).command

        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(result.data)
        BasicInstanceBuilder.compare(data.mapNew, json)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

        result = UserAnnotationAPI.redo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.redo()
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))

        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapOld, JSON.parse(result.data))

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code
        result = UserAnnotationAPI.show(idAnnotation, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        BasicInstanceBuilder.compare(data.mapNew, JSON.parse(result.data))
    }

    void testEditUserAnnotationOutOfBoundsGeom() {
        def annotation = BasicInstanceBuilder.getUserAnnotation()
        ImageInstance im = annotation.image

        def updateAnnotation = JSON.parse((String)annotation.encodeAsJSON())
        updateAnnotation.location = "POLYGON((-1 -1,-1 $im.baseImage.height,${im.baseImage.width+5} $im.baseImage.height,$im.baseImage.width 0,-1 -1))"

        def result = UserAnnotationAPI.update(annotation.id, updateAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def json = JSON.parse(result.data)

        assert json.annotation.location.toString() == "POLYGON ((0 $im.baseImage.height, $im.baseImage.width $im.baseImage.height, $im.baseImage.width 0, 0 0, 0 $im.baseImage.height))"
    }

    void testEditUserAnnotationNotExist() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        UserAnnotation annotationToEdit = UserAnnotation.get(annotationToAdd.id)
        def jsonAnnotation = JSON.parse((String)annotationToEdit.encodeAsJSON())
        jsonAnnotation.id = "-99"
        def result = UserAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testEditUserAnnotationWithBadGeometry() {
        UserAnnotation annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def jsonAnnotation = JSON.parse((String)annotationToAdd.encodeAsJSON())
        jsonAnnotation.location = "POINT (BAD GEOMETRY)"
        def result = UserAnnotationAPI.update(annotationToAdd.id, jsonAnnotation.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testDeleteUserAnnotation() {
        def annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist()
        assert annotationToDelete.save(flush: true)  != null
        def id = annotationToDelete.id
        def result = UserAnnotationAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        Long commandId = JSON.parse(result.data).command

        assert 200 == result.code

        def showResult = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.create(BasicInstanceBuilder.getUserAnnotationNotExist().encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)

        result = UserAnnotationAPI.undo()
        assert 200 == result.code

        //404 because the undoed annotation was not this one
        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code

        result = UserAnnotationAPI.redo()
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code


        result = UserAnnotationAPI.undo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.redo(commandId)
        assert 200 == result.code

        result = UserAnnotationAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteUserAnnotationNotExist() {
        def result = UserAnnotationAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteUserAnnotationWithTerm() {
        def annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def result = UserAnnotationAPI.delete(annotationToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def user = BasicInstanceBuilder.getUser(Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)

        annotationToDelete = BasicInstanceBuilder.getUserAnnotationNotExist(true)
        def annotTerm = BasicInstanceBuilder.getAnnotationTermNotExist(annotationToDelete)
        annotTerm.user = user

        result = AnnotationTermAPI.createAnnotationTerm(annotTerm.encodeAsJSON(), Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code

        result = UserAnnotationAPI.delete(annotationToDelete.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }




    void testListAlgoAnnotationByImageAndUser() {
        UserAnnotation annotation = BasicInstanceBuilder.getUserAnnotation()
        UserAnnotation annotationWith2Term = BasicInstanceBuilder.getUserAnnotation()
        AnnotationTerm aat = BasicInstanceBuilder.getAnnotationTermNotExist(annotationWith2Term,true)


        def result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        //very small bbox, hight annotation number
        String bbox = "1,1,100,100"
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,1,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,2,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, annotation.user.id, bbox, true,3,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray


        result = UserAnnotationAPI.listByImageAndUser(-99, annotation.user.id, bbox, false,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
        result = UserAnnotationAPI.listByImageAndUser(annotation.image.id, -99, bbox, false,null,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testUnionUserAnnotationByProjectWithCredential() {
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.save(flush: true)
        assert UserAnnotation.findAllByImage(image).size()==0

        def a1 = BasicInstanceBuilder.getUserAnnotationNotExist()

        a1.location = new WKTReader().read("POLYGON ((0 0, 0 5000, 10000 5000, 10000 0, 0 0))")
        a1.image = image
        a1.project = image.project
        assert a1.save(flush: true)  != null

        def a2 = BasicInstanceBuilder.getUserAnnotationNotExist()
        a2.location = new WKTReader().read("POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))")
        a2.image = image
        a2.project = image.project
        assert a2.save(flush: true)  != null

        def at1 = BasicInstanceBuilder.getAnnotationTermNotExist(a1,true)
        def at2 = BasicInstanceBuilder.getAnnotationTermNotExist(a2,true)
        at2.term = at1.term
        at2.save(flush:true)

        assert UserAnnotation.findAllByImage(a1.image).size()==2

        def result = AlgoAnnotationAPI.union(a1.image.id,a1.user.id,a1.terms().first().id,10,20, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert UserAnnotation.findAllByImage(a1.image).size()==1
    }

    void testUnionAlgoAnnotationByProjectWithCredentialBufferNull() {
        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
        image.save(flush: true)
        assert UserAnnotation.findAllByImage(image).size()==0

        def a1 = BasicInstanceBuilder.getUserAnnotationNotExist()

        a1.location = new WKTReader().read("POLYGON ((0 0, 0 6000, 10000 6000, 10000 0, 0 0))")
        a1.image = image
        a1.project = image.project
        assert a1.save(flush: true)  != null

        def a2 = BasicInstanceBuilder.getUserAnnotationNotExist()
        a2.location = new WKTReader().read("POLYGON ((0 5000, 10000 5000, 10000 10000, 0 10000, 0 5000))")
        a2.image = image
        a2.project = image.project
        assert a2.save(flush: true)  != null

        def at1 = BasicInstanceBuilder.getAnnotationTermNotExist(a1,true)
        def at2 = BasicInstanceBuilder.getAnnotationTermNotExist(a2,true)
        at2.term = at1.term
        at2.save(flush:true)

        assert UserAnnotation.findAllByImage(a1.image).size()==2

        def result = AlgoAnnotationAPI.union(a1.image.id,a1.user.id,a1.terms().first().id,10,null, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        assert UserAnnotation.findAllByImage(a1.image).size()==1
    }








//
//    def testUnionAnnotationRealCase() {
//
////        ImageInstance image = BasicInstanceBuilder.getImageInstanceNotExist()
//         image.save(flush: true)
//         assert UserAnnotation.findAllByImage(image).size()==0
//
//         def a1 = BasicInstanceBuilder.getUserAnnotationNotExist()
//
//         a1.location = new WKTReader().read("POLYGON ((83816 50999, 83822 50996, 83824 50997, 83828 50995, 83830 50993, 83830 50988, 83829 50986, 83827 50984, 83823 50982, 83821 50982, 83818 50980, 83816 50980, 83812 50982, 83807 50987, 83807 50991, 83812 50997, 83816 50999))")
//         a1.id = 18100264
//         a1.image = image
//         a1.project = image.project
//         assert a1.save(flush: true)  != null
//
//         def a2 = BasicInstanceBuilder.getUserAnnotationNotExist()
//         a2.location = new WKTReader().read("POLYGON ((83893 51230, 83899 51227, 83902 51224, 83905 51223, 83912 51216, 83905 51209, 83902 51208, 83900 51206, 83896 51204, 83895 51205, 83894 51204, 83890 51206, 83883 51213, 83883 51215, 83882 51216, 83882 51221, 83885 51224, 83885 51225, 83887 51227, 83893 51230))")
//         a2.id = 18100278
//         a2.image = image
//         a2.project = image.project
//         assert a2.save(flush: true)  != null
//
//
//        def a3 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a3.location = new WKTReader().read("POLYGON ((83586 51264, 83588 51264, 83592 51262, 83599 51257, 83599 51252, 83600 51251, 83600 51242, 83596 51238, 83595 51235, 83592 51232, 83584 51228, 83576 51232, 83574 51234, 83572 51238, 83568 51242, 83568 51247, 83571 51250, 83573 51254, 83580 51261, 83586 51264))")
//        a3.id = 18100291
//        a3.image = image
//        a3.project = image.project
//        assert a3.save(flush: true)  != null
//
//        def a4 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a4.location = new WKTReader().read("POLYGON ((84292 50836, 84292 50815, 84266 50815, 84270 50823, 84270 50826, 84274 50828, 84277 50828, 84292 50836))")
//        a4.id = 18100304
//        a4.image = image
//        a4.project = image.project
//        assert a4.save(flush: true)  != null
//
//        def a5 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a5.location = new WKTReader().read("POLYGON ((84470 51065, 84471 51065, 84471 50926, 84453 50935, 84451 50935, 84385 50902, 84373 50890, 84369 50888, 84360 50888, 84353 50891, 84338 50883, 84338 50903, 84340 50906, 84340 50946, 84326 50960, 84326 50966, 84327 50967, 84329 50981, 84338 50991, 84338 50997, 84340 50999, 84351 51004, 84373 51026, 84375 51029, 84378 51030, 84380 51032, 84384 51034, 84389 51034, 84402 51041, 84403 51040, 84408 51043, 84410 51043, 84413 51041, 84417 51043, 84420 51043, 84423 51041, 84470 51065))")
//        a5.id =  18100317
//        a5.image = image
//        a5.project = image.project
//        assert a5.save(flush: true)  != null
//
//
//        def a6 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a6.location = new WKTReader().read("POLYGON ((84470 51182, 84471 51182, 84471 51153, 84464 51156, 84455 51165, 84455 51172, 84456 51173, 84456 51175, 84459 51178, 84460 51178, 84463 51180, 84467 51180, 84470 51182))")
//        a6.id = 18100330
//        a6.image = image
//        a6.project = image.project
//        assert a6.save(flush: true)  != null
//
//        def a7 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a7.location = new WKTReader().read("POLYGON ((84311 51189, 84315 51185, 84315 51177, 84314 51175, 84307 51168, 84307 51124, 84296 51112, 84296 51110, 84294 51108, 84290 51106, 84289 51107, 84286 51105, 84284 51105, 84273 51111, 84272 51110, 84263 51114, 84261 51116, 84260 51123, 84257 51128, 84257 51134, 84258 51135, 84258 51137, 84262 51141, 84262 51147, 84266 51149, 84292 51175, 84294 51178, 84295 51178, 84301 51184, 84311 51189))")
//        a7.id = 18100343
//        a7.image = image
//        a7.project = image.project
//        assert a7.save(flush: true)  != null
//
//        def a8 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a8.location = new WKTReader().read("POLYGON ((84043 51324, 84067 51324, 84067 51303, 84064 51300, 84060 51298, 84058 51298, 84057 51297, 84049 51301, 84046 51304, 84046 51310, 84043 51313, 84043 51324))")
//        a8.id =  18100356
//        a8.image = image
//        a8.project = image.project
//        assert a8.save(flush: true)  != null
//
//
//        def a9 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a9.location = new WKTReader().read("POLYGON ((84555 51324, 84983 51324, 84982 50890, 84958 50878, 84954 50879, 84949 50876, 84942 50880, 84917 50867, 84906 50871, 84902 50875, 84902 50882, 84883 50901, 84883 50921, 84890 50928, 84890 50968, 84869 50989, 84829 51009, 84798 50993, 84792 50993, 84783 50988, 84778 50988, 84746 51004, 84701 50982, 84679 50960, 84675 50958, 84659 50963, 84648 50973, 84607 50993, 84567 50973, 84557 50963, 84550 50960, 84529 50939, 84529 50898, 84550 50877, 84573 50866, 84581 50856, 84578 50842, 84572 50834, 84572 50815, 84474 50815, 84474 51254, 84534 51284, 84555 51305, 84555 51324))")
//        a9.id = 18100369
//        a9.image = image
//        a9.project = image.project
//        assert a9.save(flush: true)  != null
//
//
//        def a10 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a10.location = new WKTReader().read("POLYGON ((85488 51324, 85495 51324, 85495 51318, 85494 51318, 85493 51319, 85490 51320, 85488 51322, 85488 51324))")
//        a10.id = 18100382
//        a10.image = image
//        a10.project = image.project
//        assert a10.save(flush: true)  != null
//
//
//        def a11 = BasicInstanceBuilder.getUserAnnotationNotExist()
//        a11.location = new WKTReader().read("POLYGON ((84986 51324, 85045 51324, 85045 51305, 85063 51286, 85063 51240, 85093 51210, 85093 51182, 85114 51161, 85154 51141, 85230 51181, 85251 51202, 85251 51242, 85241 51252, 85240 51263, 85246 51270, 85254 51275, 85263 51268, 85264 51263, 85264 51251, 85255 51242, 85255 51202, 85276 51181, 85316 51161, 85321 51162, 85346 51149, 85360 51156, 85367 51153, 85370 51150, 85372 51138, 85368 51133, 85347 51123, 85326 51102, 85325 51094, 85318 51089, 85304 51089, 85285 51099, 85269 51101, 85223 51078, 85201 51056, 85201 51029, 85198 51026, 85190 51022, 85170 51024, 85130 51004, 85104 50978, 85104 50958, 85102 50956, 85093 50952, 85076 50958, 85042 50941, 85034 50945, 85028 50951, 84986 50972, 84986 51324))")
//        a11.id = 18100395
//        a11.image = image
//        a11.project = image.project
//        assert a11.save(flush: true)  != null
//
//        def at1 = BasicInstanceBuilder.getAnnotationTermNotExist(a1,true)
//        def at2 = BasicInstanceBuilder.getAnnotationTermNotExist(a2,true)
//        def at3 = BasicInstanceBuilder.getAnnotationTermNotExist(a3,true)
//        def at4 = BasicInstanceBuilder.getAnnotationTermNotExist(a4,true)
//        def at5 = BasicInstanceBuilder.getAnnotationTermNotExist(a5,true)
//        def at6 = BasicInstanceBuilder.getAnnotationTermNotExist(a6,true)
//        def at7 = BasicInstanceBuilder.getAnnotationTermNotExist(a7,true)
//        def at8 = BasicInstanceBuilder.getAnnotationTermNotExist(a8,true)
//        def at9 = BasicInstanceBuilder.getAnnotationTermNotExist(a9,true)
//        def at10 = BasicInstanceBuilder.getAnnotationTermNotExist(a10,true)
//        def at11 = BasicInstanceBuilder.getAnnotationTermNotExist(a11,true)
//
//        at2.term = at1.term
//        at3.term = at1.term
//        at4.term = at1.term
//        at5.term = at1.term
//        at6.term = at1.term
//        at7.term = at1.term
//        at8.term = at1.term
//        at9.term = at1.term
//        at10.term = at1.term
//        at11.term = at1.term
//
//         at2.save(flush:true)
//        at3.save(flush:true)
//        at4.save(flush:true)
//        at5.save(flush:true)
//        at6.save(flush:true)
//        at7.save(flush:true)
//        at8.save(flush:true)
//        at9.save(flush:true)
//        at10.save(flush:true)
//        at11.save(flush:true)
//
////        println  a1.id + " = " + a1.location.area + " / " + a1.location.centroid
////        println  a2.id + " = " + a2.location.area + " / " + a2.location.centroid
////        println  a3.id + " = " + a3.location.area + " / " + a3.location.centroid
////        println  a4.id + " = " + a4.location.area + " / " + a4.location.centroid
////        println  a5.id + " = " + a5.location.area + " / " + a5.location.centroid
////        println  a6.id + " = " + a6.location.area + " / " + a6.location.centroid
////        println  a7.id + " = " + a7.location.area + " / " + a7.location.centroid
////        println  a8.id + " = " + a8.location.area + " / " + a8.location.centroid
////        println  a9.id + " = " + a9.location.area + " / " + a9.location.centroid
////        println  a10.id + " = " + a10.location.area + " / " + a10.location.centroid
////        println  a11.id + " = " + a11.location.area + " / " + a11.location.centroid
//
//        UserAnnotation.findAllByImage(a1.image).each {
//            println  it.id + " = " + it.location.area + " / " + it.location.centroid + " => " + it.location.toText()
//        }
//
//         assert UserAnnotation.findAllByImage(a1.image).size()==11
//
//         def result = AlgoAnnotationAPI.union(a1.image.id,a1.user.id,a1.terms().first().id,2,25, 9999999,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
//         assert 200 == result.code
//
//        println UserAnnotation.findAllByImage(a1.image).size()
//
//
//        UserAnnotation.findAllByImage(a1.image).each {
//            println  it.id + " = " + it.location.area + " / " + it.location.centroid + " => " + it.location.toText()
//        }
//
//         assert UserAnnotation.findAllByImage(a1.image).size()==5
//
//
//

//
//
//
//
//
//    }



}
