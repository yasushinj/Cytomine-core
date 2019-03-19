package be.cytomine.middleware


import be.cytomine.test.HttpClient
import be.cytomine.utils.ModelService
import grails.converters.JSON

class ImageServerService extends ModelService {

    static transactional = true

    def cytomineService
    def securityACLService

    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        ImageServer.list()
    }

    def read(def id) {
        securityACLService.checkGuest(cytomineService.currentUser)
        return ImageServer.read(id as Long)
    }

    def currentDomain() {
        ImageServer
    }

    def getStorageSpaces() {
        def result = []
        String url;
        ImageServer.list().each {
            url = it.url+"/storage/size.json"

            HttpClient client = new HttpClient()

            client.connect(url,"","")

            client.get()

            String response = client.getResponseData()
            int code = client.getResponseCode()
            log.info "code=$code response=$response"
            if(code < 400){
                result << JSON.parse(response)
            }
        }

        // if dns sharding, multiple link are to the same IMS. We merge the same IMS.
        result = result.unique { it.hostname }
        return result
    }
}
