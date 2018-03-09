package be.cytomine.api.processing

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

    @RestApiMethod(description = "Update a software.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def update() {
        update(softwareRepositoryService, request.JSON)
    }

    @RestApiMethod(description = "Delete a software.", listing = true)
    @RestApiParams(params = [
            @RestApiParam(name = "id", type = "long", paramType = RestApiParamType.PATH, description = "The software repository id")
    ])
    def delete() {
        delete(softwareRepositoryService, JSON.parse("{id : $params.id}"), null)
    }

}
