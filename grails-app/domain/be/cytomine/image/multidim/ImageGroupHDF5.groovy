package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.project.Project
import be.cytomine.utils.JSONUtils
import grails.converters.JSON
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Created by laurent on 06.02.17.
 */
@RestApiObject(name = "image hdf5 group", description = "A group of image from the same source with different dimension and hdf5 support")

class ImageGroupHDF5  extends CytomineDomain implements  Serializable {

   ImageGroup group

    @RestApiObjectField(description = "The HDF5 filenames for the whole multidim  image")
    String filenames

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['group'] = domain?.group?.id
        println "hey dude we're there"
        returnArray['filenames'] = domain?.filenames
        return returnArray
    }


    static mapping = {
        id generator: "assigned"
        sort "id"
    }

    static constraints = {
        filenames nullable: false
    }


    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        println "We are in check"
        ImageGroupHDF5.withNewSession {
            ImageGroupHDF5 imageAlreadyExist = ImageGroupHDF5.findByGroup(group)
            if (imageAlreadyExist != null && (imageAlreadyExist.id != id)) {
                throw new AlreadyExistException("I")
            }
        }
        println "Ok ckeck"
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ImageGroupHDF5 insertDataIntoDomain(def json, def domain = new ImageGroupHDF5()) {
        println "Found Insert"
        println "JSON INSERT " + json
        domain.group = JSONUtils.getJSONAttrDomain(json, "group", new ImageGroup(), true)
        domain.filenames = JSONUtils.getJSONAttrStr(json, "filenames")


        return domain;
    }



    public CytomineDomain container() {
        return group.container();
    }

}