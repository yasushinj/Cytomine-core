package be.cytomine.test.http

import be.cytomine.image.multidim.ImageGroupHDF5
import be.cytomine.test.Infos
import grails.converters.JSON

/**
 * Created by laurent on 07.02.17.
 */
class ImageGroupHDF5API  extends DomainAPI{

    static def show(Long id, String username, String password) {
        String URL = Infos.CYTOMINEURL + "/api/imagegrouph5/"+id+".json"

        return doGET(URL, username, password)
    }

    static def create(String json, String username, String password) {
        String URL = Infos.CYTOMINEURL + "api/imagegrouph5.json"
        def result = doPOST(URL,json, username, password)
        println result
        result.data = ImageGroupHDF5.get(JSON.parse(result.data)?.imagegrouphdf5?.id)
        return result
    }


}
