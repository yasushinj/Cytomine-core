package be.cytomine.processing

import be.cytomine.CytomineDomain
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObjectField

class SoftwareRepository extends CytomineDomain {

    @RestApiObjectField(description = "The software repository provider's name")
    String provider

    @RestApiObjectField(description = "The software repository name")
    String repositoryUser

    @RestApiObjectField(description = "The prefix used to recognize a software in a repository")
    String prefix

    @RestApiObjectField(description = "The name of the installer")
    String installerName

    static constraints = {
        provider(nullable: false, blank: false)
        repositoryUser(nullable:false, blank: false)
    }

    static mapping = {
        id(generator: "assigned")
        sort("id")
        installerName(defaultValue: "'add_cytomine_software.py'")
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static SoftwareRepository insertDataIntoDomain(def json, def domain = new SoftwareRepository()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.provider = JSONUtils.getJSONAttrStr(json, 'provider')
        domain.repositoryUser = JSONUtils.getJSONAttrStr(json, 'repositoryUser')
        domain.prefix = JSONUtils.getJSONAttrStr(json, 'prefix')
        domain.installerName = JSONUtils.getJSONAttrStr(json, 'installerName')
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['provider'] = domain?.provider
        returnArray['repositoryUser'] = domain?.repositoryUser
        returnArray['prefix'] = domain?.prefix
        returnArray['installerName'] = domain?.installerName
        return returnArray
    }

}
