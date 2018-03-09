package be.cytomine.test.http

import be.cytomine.processing.SoftwareRepository
import be.cytomine.test.Infos
import grails.converters.JSON

class SoftwareRepositoryAPI extends DomainAPI {

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/software_repository/" + id + ".json"
        return doGET(URL, username, password)
    }

    static def list(String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/software_repository.json"
        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/software_repository.json"
        def result = doPOST(URL, json, username, password)
        result.data = SoftwareRepository.get(JSON.parse(result.data)?.softwarerepository?.id)
        return result
    }

    static def update(def id, def jsonSoftwareRepository, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/software_repository/" + id + ".json"
        return doPUT(URL, jsonSoftwareRepository, username, password)
    }

    static def delete(def id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/software_repository/" + id + ".json"
        return doDELETE(URL, username, password)
    }

}
