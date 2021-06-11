package be.cytomine.api.ontology

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

import be.cytomine.api.RestController
import be.cytomine.ontology.Ontology
import be.cytomine.utils.Task
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for ontology (terms tree)
 */
@RestApi(name = "Ontology | ontology services", description = "Methods for managing ontologies")
class RestOntologyController extends RestController {

    def springSecurityService
    def ontologyService
    def cytomineService
    def termService
    def taskService

    /**
     * List all ontology visible for the current user
     * For each ontology, print the terms tree
     */
    @RestApiMethod(description="Get all ontologies available", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="light", type="boolean", paramType = RestApiParamType.QUERY,description = "(Optional, default false) Only get a light list (with no term tree for each ontologies)")
    ])
    def list () {
        if(params.boolean("light")) {
            responseSuccess(ontologyService.listLight())
        } else {
            responseSuccess(ontologyService.list())
        }
    }

    @RestApiMethod(description="Get an ontology")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The ontology id")
    ])
    def show () {
        Ontology ontology = ontologyService.read(params.long('id'))
        if (ontology) {
            responseSuccess(ontology)
        } else {
            responseNotFound("Ontology", params.id)
        }
    }

    @RestApiMethod(description="Add an ontology")
    def add () {
        add(ontologyService, request.JSON)
    }

    @RestApiMethod(description="Update an ontology")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The ontology id")
    ])
    def update () {
        update(ontologyService, request.JSON)
    }

    @RestApiMethod(description="Delete an ontology")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The ontology id"),
        @RestApiParam(name="task", type="long", paramType = RestApiParamType.PATH,description = "(Optional, default:null) The id of the task to update during process"),
    ])
    def delete () {
        Task task = taskService.read(params.getLong("task"))
        delete(ontologyService, JSON.parse("{id : $params.id}"),task)
    }

}
