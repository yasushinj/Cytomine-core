package be.cytomine.processing

import be.cytomine.CytomineDomain
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObjectField

class ProcessingServer extends CytomineDomain {

    @RestApiObjectField(description = "The name of the processing server")
    String name

    @RestApiObjectField(description = "The host of the processing server")
    String host

    @RestApiObjectField(description = "The type of the processing server")
    String type

    static constraints = {
        name(nullable: false, blank: false)
    }

    static mapping = {
        host(defaultValue: "'localhost'")
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ProcessingServer insertDataIntoDomain(def json, def domain = new ProcessingServer()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name')
        domain.host = JSONUtils.getJSONAttrStr(json, 'host')
        domain.type = JSONUtils.getJSONAttrStr(json, 'type')
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['host'] = domain?.host
        returnArray['type'] = domain?.type
        return returnArray
    }

}
