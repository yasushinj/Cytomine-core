package be.cytomine

import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.ImageGroupHDF5API
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by laurent on 07.02.17.
 */
class ImageGroupHDF5Tests {
    void testAddCorrect() {
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5NotExist(false)
        println imageGroupHDF5.class
        println imageGroupHDF5
        def result = ImageGroupHDF5API.create(((ImageGroupHDF5)imageGroupHDF5).encodeAsJSON(), Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code


        int resID = result.data.id

        result = ImageGroupHDF5API.show(resID, Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code
    }

    void testShowImageGroup() {
        ImageGroupHDF5 imageGroupHDF5 = BasicInstanceBuilder.getImageGroupHDF5()
        println imageGroupHDF5
        def result = ImageGroupHDF5API.show(imageGroupHDF5.id, Infos.ANOTHERLOGIN, Infos.ANOTHERPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }
}
