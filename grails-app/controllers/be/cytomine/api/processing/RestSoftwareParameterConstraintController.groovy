package be.cytomine.api.processing

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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

import be.cytomine.api.RestController
import be.cytomine.processing.SoftwareParameterConstraint
import com.mongodb.util.JSON
import org.json.simple.JSONObject
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

@RestApiObject(name = "Software para", description = "")
class RestSoftwareParameterConstraintController extends RestController {

    def softwareParameterConstraintService

    @RestApiMethod(description = "")
    def list() {
        responseSuccess(softwareParameterConstraintService.list())
    }

    @RestApiMethod(description = "")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The constraint id")
    ])
    def show() {
        SoftwareParameterConstraint softwareParameterConstraint = SoftwareParameterConstraint.read(params.long('id'))
        if (softwareParameterConstraint) {
            responseSuccess(softwareParameterConstraint)
        } else {
            responseNotFound("SoftwareParameterConstraint", params.id)
        }
    }

    @RestApiMethod(description = "Add a new constraint to Cytomine")
    def add() {
        add(softwareParameterConstraintService, request.JSON)
    }

    @RestApiMethod(description = "")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The container id")
    ])
    def update() {
        update(softwareParameterConstraintService, request.JSON)
    }

    @RestApiMethod(description = "", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The container id")
    ])
    def delete() {
        delete(softwareParameterConstraintService, JSON.parse("{id : $params.id}"), null)
    }

    @RestApiMethod(description = "")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software constraint id"),
            @RestApiParam(name = "value", type = "string", paramType = RestApiParamType.PATH, description = "The constraint's value")
    ])
    def evaluate() {
        SoftwareParameterConstraint softwareParameterConstraint = SoftwareParameterConstraint.read(params.long('id'))
        def value = params.get('value')

        println "VALUE : ${value}"

        if (softwareParameterConstraint && value) {
            def result = [result: softwareParameterConstraintService.evaluate(softwareParameterConstraint, value)]

            responseSuccess(result)
        } else {
            responseNotFound("SoftwareParameterConstraint", params.id)
        }
    }

}
