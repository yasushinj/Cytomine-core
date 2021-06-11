package be.cytomine.middleware

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.MiddlewareException
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties

/**
 * Created by julien 
 * Date : 25/02/15
 * Time : 15:28
 */
class AmqpQueueService extends ModelService {

    static transactionService = true
    boolean saveOnUndoRedoStack = true

    final static queuePrefixProcessingServer = "queueProcessingServer"
    final static channelPrefixProcessingServer = "channelProcessingServer"
    final static exchangePrefixProcessingServer = "exchangeProcessingServer"

    def securityACLService
    def amqpQueueConfigInstanceService
    def amqpQueueConfigService
    def messageBrokerServerService

    // Avoid loading loop because rabbitConnectionService -> amqpService -> rabbitConnectionService
    private getRabbitConnectionService(){
        grailsApplication.mainContext.rabbitConnectionService
    }


    def currentDomain() {
        return AmqpQueue
    }

    AmqpQueue get(def id) {
        AmqpQueue.get(id)
    }

    AmqpQueue read(def id) {
        AmqpQueue amqpQueue = AmqpQueue.read(id)

        amqpQueue
    }

    AmqpQueue read(String name) {
        AmqpQueue amqpQueue = AmqpQueue.findByName(name)

        amqpQueue
    }

    def list() {
        AmqpQueue.list()
    }
    def list(String name) {
        AmqpQueue.findAllByNameIlike(name, [sort: "host", order: "desc"])
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain data
     * @return Response structure (new domain data, old domain data..)
     */
    def update(AmqpQueue domain, def json) throws CytomineException {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        return executeCommand(new EditCommand(user: currentUser), domain, json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AmqpQueue domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkAdmin(currentUser)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.host, domain.name, domain.exchange]
    }

    def afterAdd(def domain, def response) {
        createAmqpQueueDefault(domain)
    }

    def createAmqpQueue(AmqpQueue domain) {
        // Check if a connection already exists for the host. If not, it is created
        MessageBrokerServer mbs = messageBrokerServerService.getMessageBrokerServerByHost(domain.host)
        if(!mbs) {
            throw new MiddlewareException("Broker Server on $domain.host does not exist")
        }
        rabbitConnectionService.getRabbitConnection(mbs)

        Channel channel = rabbitConnectionService.getRabbitChannel(domain.name, mbs)

        // Declaring a durable exchange
        channel.exchangeDeclare(domain.exchange, "direct", true)

        // Get all configurations for the queue
        def parameters = amqpQueueConfigInstanceService.list(domain)
        parameters = parameters.sort { it.config.index }

        def propertiesList = []
        def propertiesMap = [:]

        for (AmqpQueueConfigInstance configInstance in parameters) {
            if (configInstance.config.isInMap) {
                if (configInstance.value) {
                    propertiesMap.put(configInstance.config.name, amqpQueueConfigService.getValueCasted(configInstance.value, configInstance.config.type))
                }
                else {
                    propertiesMap.put(configInstance.config.name, amqpQueueConfigService.getValueCasted(configInstance.config.defaultValue, configInstance.config.type))
                }
            } else {
                if (configInstance.value) {
                    propertiesList.add(amqpQueueConfigService.getValueCasted(configInstance.value, configInstance.config.type))
                }
                else {
                    propertiesList.add(amqpQueueConfigService.getValueCasted(configInstance.config.defaultValue, configInstance.config.type))
                }
            }
        }

        if (propertiesMap.isEmpty())
            propertiesMap = null

        try {
            channel.queueDeclare(domain.name, propertiesList[0], propertiesList[1], propertiesList[2], propertiesMap)
        } catch(IOException e) {
            throw new MiddlewareException("Queue :" + domain.name + " could not be created! : " + e.getMessage())
        }

        try {
            channel.queueBind(domain.name, domain.exchange, "")
        } catch(IOException e) {
            throw new MiddlewareException("Queue :" + domain.name + " could not be bound to the exchange : " + domain.exchange + ". " + e.getMessage())
        }
    }

    def createAmqpQueueDefault(AmqpQueue domain) {
        // Check if a connection already exists for the host. If not, it is created
        MessageBrokerServer mbs = messageBrokerServerService.getMessageBrokerServerByHost(domain.host)
        if(!mbs) {
            throw new MiddlewareException("Broker Server on $domain.host does not exist")
        }

        rabbitConnectionService.getRabbitConnection(mbs)

        Channel channel = rabbitConnectionService.getRabbitChannel(domain.name, mbs)

        // Declaring a durable exchange
        channel.exchangeDeclare(domain.exchange, "direct", true)

        // Get default parameters
        def defaultParams = amqpQueueConfigService.list()
        defaultParams = defaultParams.sort{ it.index }

        def listParams = defaultParams.collect{
            amqpQueueConfigService.getValueCasted(it.defaultValue, it.type)
        }


        try {
            channel.queueDeclare(domain.name, listParams[0], listParams[1], listParams[2], null)
        } catch(IOException e) {
            throw new MiddlewareException("Queue :" + domain.name + " could not be created! : " + e.getMessage())
        }

        try {
            channel.queueBind(domain.name, domain.exchange, "")
        } catch(IOException e) {
            throw new MiddlewareException("Queue :" + domain.name + " could not be bound to the exchange : " + domain.exchange + ". " + e.getMessage())
        }

    }


    def publishMessage(AmqpQueue domain, String messageBody) {

        MessageBrokerServer mbs = messageBrokerServerService.getMessageBrokerServerByHost(domain.host)
        if(!mbs) {
            throw new MiddlewareException("Broker Server on $domain.host does not exist")
        }

        Channel channel = rabbitConnectionService.getRabbitChannel(domain.name, mbs)

        try {
            log.info "Exchange : " + domain.exchange + " " + messageBody
            channel.basicPublish(domain.exchange, "", MessageProperties.PERSISTENT_TEXT_PLAIN, messageBody.getBytes())
        } catch(IOException e) {
            throw new MiddlewareException(("Cannot publish message : " + e.toString()))
        }
    }


    Boolean checkRabbitQueueExists(String softwareName, MessageBrokerServer mbs) {

        if(!mbs) {
            throw new MiddlewareException("Broker Server on $mbs.host does not exist")
        }

        Channel channel = rabbitConnectionService.getRabbitChannel(softwareName, mbs)

        try {
            channel.queueDeclarePassive(softwareName)
        } catch(IOException e) {
            return false
        }

        return true
    }

    Boolean checkAmqpQueueDomainExists(String queueName) {
        return read(queueName)
    }

}
