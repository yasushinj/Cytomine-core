package be.cytomine

import be.cytomine.test.Infos
import be.cytomine.test.http.SoftwareRepositoryAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

class SoftwareRepositoryTests {

    void testListSoftwareRepositoryWithCredential() {
        def result = SoftwareRepositoryAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

}
