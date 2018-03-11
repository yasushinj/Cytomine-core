package be.cytomine.processing

import be.cytomine.Exception.CytomineException
import be.cytomine.command.*
import be.cytomine.middleware.AmqpQueue
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import groovy.json.JsonBuilder

import static org.springframework.security.acls.domain.BasePermission.DELETE
import static org.springframework.security.acls.domain.BasePermission.WRITE

class SoftwareRepositoryService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def aclUtilService
    def securityACLService
    def amqpQueueService

    def currentDomain() {
        return SoftwareRepository
    }

    SoftwareRepository read(def id) {
        return SoftwareRepository.read(id)
    }

    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        return SoftwareRepository.list()
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkGuest(currentUser)
        //json.user = currentUser.id
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    def update(SoftwareRepository domain, def jsonNewData) {
        securityACLService.check(domain.container(), WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), domain, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(SoftwareRepository domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        log.info "delete software"
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(), DELETE)
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.provider, domain.repositoryUser, domain.prefix, domain.installerName]
    }

    def refreshRepositories() {
        def message = [requestType: 2]
        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder(message)

        amqpQueueService.publishMessage(AmqpQueue.findByName("queueCommunication"), jsonBuilder.toString())
    }

}
