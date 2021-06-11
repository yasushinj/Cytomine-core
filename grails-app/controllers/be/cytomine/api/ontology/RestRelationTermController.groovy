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
import be.cytomine.ontology.Relation
import be.cytomine.ontology.Term
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for relation between terms in ontology (ex: parent)
 */
@RestApi(name = "Ontology | relation term services", description = "Methods for managing relation between terms in ontology (ex: t1 parent t2)")
class RestRelationTermController extends RestController {

    def relationService
    def termService
    def relationTermService


    /**
     * List all relation for a specific term and position
     */
    @RestApiMethod(description="List all relation for a specific term and position (1 or 2)", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The term id"),
        @RestApiParam(name="i", type="int", paramType = RestApiParamType.PATH,description = "The position index (1 or 2)")
    ])
    def listByTerm() {
        Term term = termService.read(params.long('id'))
        String position = params.i

        if (term && (position == "1" || position == "2")){
            responseSuccess(relationTermService.list(term, position))
        } else {
            responseNotFound("RelationTerm", "Term" + position, params.id)
        }
    }

    /**
     * List all relation for a specific term
     */
    @RestApiMethod(description="List all relation for a specific term", listing=true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH,description = "The term id"),
        @RestApiParam(name="i", type="int", paramType = RestApiParamType.PATH,description = "The position index (1 or 2)")
    ])
    def listByTermAll() {
        Term term = termService.read(params.long('id'))
        if (term) {
            responseSuccess(relationTermService.list(term))
        }
        else {
            responseNotFound("RelationTerm", "Term", params.id)
        }
    }

    /**
     * Check if a relation exist with term1 and term2
     */
    @RestApiMethod(description="Get a project property with its id or its key")
    @RestApiParams(params=[
        @RestApiParam(name="idrelation", type="long", paramType = RestApiParamType.PATH, description = "The relation id"),
        @RestApiParam(name="idterm1", type="long", paramType = RestApiParamType.PATH,description = "The term 1 id"),
        @RestApiParam(name="idterm2", type="long", paramType = RestApiParamType.PATH,description = "The term 2 id")
    ])
    def show() {
        Relation relation
        if (params.idrelation != null) {
            relation = relationService.read(params.long('idrelation'))
        } else {
            relation = relationService.getRelationParent()
        }

        Term term1 = termService.read(params.long('idterm1'))
        Term term2 = termService.read(params.long('idterm2'))

        if (relation && term1 && term2) {
            def relationTerm = relationTermService.get(relation, term1, term2)
            if(relationTerm) {
                responseSuccess(relationTerm)
            } else {
                responseNotFound("RelationTerm", "Relation", relation, "Term1", term1, "Term2", term2)
            }
        }
        else {
            responseNotFound("RelationTerm", "Relation", relation, "Term1", term1, "Term2", term2)
        }

    }

    /**
     * Add a new relation with two terms
     */
    @RestApiMethod(description="Add a relation between two terms. If not set, relation is PARENT")
    def add() {
        def json = request.JSON
        Relation relation
        if (json.relation != null && json.relation.toString()!="null") {
            String strRel = json.relation
            relation = relationService.read(Long.parseLong(strRel))
        } else {
            relation = relationService.getRelationParent()
        }

        json.relation = relation ? relation.id : -1

        add(relationTermService, json)
    }

    /**
     * Delete a relation between two terms
     */
    @RestApiMethod(description="Delete a relation between two terms")
    @RestApiParams(params=[
        @RestApiParam(name="idrelation", type="long", paramType = RestApiParamType.PATH,description = "The relation id"),
        @RestApiParam(name="idterm1", type="long", paramType = RestApiParamType.PATH,description = "The term 1"),
        @RestApiParam(name="idterm2", type="long", paramType = RestApiParamType.PATH,description = "The term 2")
    ])
    def delete() {

        Relation relation
        if (params.idrelation != null && params.idrelation!="null")
            relation = relationService.read(params.long('idrelation'))
        else
            relation = relationService.getRelationParent()

        def json = JSON.parse("{relation: ${relation ? relation.id : -1}, term1: $params.idterm1, term2: $params.idterm2}")
        delete(relationTermService, json,null)
    }
}
