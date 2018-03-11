package be.cytomine.processing

import be.cytomine.Exception.CytomineException
import be.cytomine.command.*
import be.cytomine.middleware.AmqpQueue
import be.cytomine.middleware.MessageBrokerServer
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import groovy.json.JsonBuilder
import org.springframework.security.acls.domain.BasePermission

import static org.springframework.security.acls.domain.BasePermission.WRITE

class ProcessingServerService extends ModelService {

    static transactional = true

    def cytomineService
    def transactionService
    def aclUtilService
    def securityACLService
    def amqpQueueService

    @Override
    def currentDomain() {
        return ProcessingServer
    }

    ProcessingServer read(def id) {
        return ProcessingServer.read(id)
    }

    def list() {
        securityACLService.checkGuest(cytomineService.currentUser)
        return ProcessingServer.list()
    }

    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkGuest(currentUser)
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    def update(ProcessingServer processingServer, def jsonNewData) {
        securityACLService.check(processingServer.container(), WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), processingServer, jsonNewData)
    }

    def delete(ProcessingServer domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container())
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        return executeCommand(c, domain, null)
    }

    @Override
    def getStringParamsI18n(def domain) {
        return [domain.id, domain.name, domain.host, domain.type]
    }

    @Override
    def afterAdd(Object domain, Object response) {
        aclUtilService.addPermission(domain, cytomineService.currentUser.username, BasePermission.ADMINISTRATION)

        String queueName = amqpQueueService.queuePrefixProcessingServer + ((domain as ProcessingServer).name).capitalize()
        if (!amqpQueueService.checkAmqpQueueDomainExists(queueName)) {
            String exchangeName = amqpQueueService.exchangePrefixProcessingServer + ((domain as ProcessingServer).name).capitalize()
            String brokerServerURL = (MessageBrokerServer.findByName("MessageBrokerServer")).host
            AmqpQueue amqpQueue = new AmqpQueue(name: queueName, host: brokerServerURL, exchange: exchangeName)
            amqpQueue.save(failOnError: true)

            amqpQueueService.createAmqpQueueDefault(amqpQueue)

            def message = [requestType: 1, name: amqpQueue.name, host: amqpQueue.host, exchange: amqpQueue.exchange]
            JsonBuilder jsonBuilder = new JsonBuilder()
            jsonBuilder(message)

            amqpQueueService.publishMessage(AmqpQueue.findByName("queueCommunication"), jsonBuilder.toString())
        }
    }

}
