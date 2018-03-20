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
import be.cytomine.processing.SoftwareRepository
import grails.converters.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

class RestSoftwareRepositoryController extends RestController {

    def softwareRepositoryService

    @RestApiMethod(description = "Get all the software repositories available in Cytomine.", listing = true)
    def list() {
        responseSuccess(softwareRepositoryService.list())
    }

    @RestApiMethod(description = "Add a new software repository to cytomine.")
    def add() {
        add(softwareRepositoryService, request.JSON)
    }

    @RestApiMethod(description = "Get a specific software repository")
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def show() {
        SoftwareRepository softwareRepository = softwareRepositoryService.read(params.long('id'))
        if (softwareRepository) {
            responseSuccess(softwareRepository)
        } else {
            responseNotFound("SoftwareRepository", params.id)
        }
    }

    @RestApiMethod(description = "Update a software repository.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def update() {
        update(softwareRepositoryService, request.JSON)
    }

    @RestApiMethod(description = "Delete a software repository.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def delete() {
        println("RES : " + JSON.parse("{id : $params.id}"))
        delete(softwareRepositoryService, JSON.parse("{id : $params.id}"),null)
    }

    @RestApiMethod(description = "Check for newly added softwares in all repositories")
    def refreshRepositories() {
        responseSuccess(softwareRepositoryService.refreshRepositories())
    }

}
