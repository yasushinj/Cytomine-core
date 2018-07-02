package be.cytomine

import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.SoftwareUserRepositoryAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class SoftwareUserRepositoryTests {

    void testListSoftwareUserRepositoryWithCredential() {
        def result = SoftwareUserRepositoryAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testAddSoftwareUserRepositoryCorrect() {
        def repoToAdd = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist()
        def result = SoftwareUserRepositoryAPI.create(repoToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idSoftware = result.data.id

        result = SoftwareUserRepositoryAPI.show(idSoftware, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddSoftwareUserRepositoryAlreadyExist() {
        BasicInstanceBuilder.getSoftwareUserRepository()
        def repoToAdd = BasicInstanceBuilder.getSoftwareUserRepository()
        def result = SoftwareUserRepositoryAPI.create(repoToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 409 == result.code
    }

    void testUpdateSoftwareUserRepositoryCorrect() {
        def repo = BasicInstanceBuilder.getSoftwareUserRepositoryNotExist()
        repo.provider = "test"
        def data = repo.encodeAsJSON()
        def resultBase = SoftwareUserRepositoryAPI.update(repo.id, data, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == resultBase.code
        def json = JSON.parse(resultBase.data)
        assert json instanceof JSONObject
        int idSoftware = json.software.id

        def showResult = SoftwareUserRepositoryAPI.show(idSoftware, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        json = JSON.parse(showResult.data)
        BasicInstanceBuilder.compare(data.mapNew, json)
    }

    void testDeleteSoftwareUserRepository() {
        def repoToDelete = BasicInstanceBuilder.getSoftwareNotExist(true)
        def id = repoToDelete.id
        def result = SoftwareUserRepositoryAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = SoftwareUserRepositoryAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code
    }
}