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

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.UserAnnotationAPI
import grails.converters.JSON

class AnnotationValidatorTests {

   public static SELF_INTERSECT_CANNOT_MAKE_VALID = "POLYGON((13688 75041,13687 75040,13688 75041,13689 75041,13688 75041))"

    public static SELF_INTERSECT = "POLYGON((0 0, 10 10, 0 10, 10 0, 0 0))"


   public static GEOMETRY_COLLECTION = "GEOMETRYCOLLECTION(POLYGON((14512 10384,14480 10384,14464 10400,14472 10400,14480 10408,14488 10400,14496 10400,14512 10384)),LINESTRING(14512 10384,14520 10384))"

   public static LINE_STRING = "LINESTRING(885.55108264715 1319.0620040002,885.55108264715 1321.0620040002)"

   public static MULTI_LINE_STRING = "MULTILINESTRING((33064 25416,33056 25408),(33056 25408,33048 25408),(33064 25416,33080 25416))"

   public static EMPTY_COLLECTION = "GEOMETRYCOLLECTION EMPTY"

    public static EMPTY_POLYGON = "POLYGON EMPTY"

    public void testAnnotationValid() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = annotationToAdd.encodeAsJSON()
        def result = UserAnnotationAPI.create(json, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    public void testAnnotationNotValid() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = SELF_INTERSECT
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationNotValidBis() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = SELF_INTERSECT_CANNOT_MAKE_VALID
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationGeometry() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = GEOMETRY_COLLECTION
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationGeometryCollectionEmpty() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = EMPTY_COLLECTION
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationGeometryPolygonEmpty() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = EMPTY_POLYGON
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationGeometryLineString() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = LINE_STRING
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    public void testAnnotationGeometryMultiLineString() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = MULTI_LINE_STRING
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    public void testAnnotationGeometryPoint() {
        def annotationToAdd = BasicInstanceBuilder.getUserAnnotation()
        def json = JSON.parse(annotationToAdd.encodeAsJSON())
        json.location = "POINT(10 10)"
        def result = UserAnnotationAPI.create(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }
}