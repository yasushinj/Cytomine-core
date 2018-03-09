package be.cytomine.api.processing

import be.cytomine.api.RestController
import be.cytomine.processing.ProcessingServer
import grails.converters.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

class RestProcessingServerController extends RestController {

    def processingServerService

    @RestApiMethod(description = "Get all the processing servers available in Cytomine")
    def list() {
        responseSuccess(processingServerService.list())
    }

    @RestApiMethod(description = "Get a specific processing server")
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def show() {
        ProcessingServer processingServer = processingServerService.read(params.long('id'))
        if (processingServer) {
            responseSuccess(processingServer)
        } else {
            responseNotFound("ProcessingServer", params.id)
        }
    }

    @RestApiMethod(description = "Add a new processing server to Cytomine")
    def add() {
        add(processingServerService, request.JSON)
    }

    @RestApiMethod(description = "Update a processing server available in Cytomine")
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def update() {
        update(processingServerService, request.JSON)
    }

    @RestApiMethod(description = "Delete a processing server", listing = true)
    @RestApiParams(params = [
        @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The processing server id")
    ])
    def delete() {
        delete(processingServerService, JSON.parse("{id : $params.id}"), null)
    }

}
