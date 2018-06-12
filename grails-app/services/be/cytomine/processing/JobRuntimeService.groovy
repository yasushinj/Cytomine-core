package be.cytomine.processing

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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

import be.cytomine.Exception.MiddlewareException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.middleware.AmqpQueue
import be.cytomine.middleware.MessageBrokerServer
import be.cytomine.security.UserJob
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonBuilder
import org.json.JSONObject

class JobRuntimeService {

    static transactional = true

    def jobParameterService
    def amqpQueueService
    def softwareParameterService

    /**
     * Create an instance of a parameter for a given job
     * @param name : the name of the concerned parameter
     * @param job : the job implied
     * @param value : the value for the specified parameter
     * @return the newly created job parameter
     */
    JobParameter createJobParameter(String name, Job job, String value) {
        SoftwareParameter softwareParameter = SoftwareParameter.findBySoftwareAndName(job.software, name)
        JobParameter jobParameter = new JobParameter(value: value, job: job, softwareParameter: softwareParameter)
        return jobParameter
    }

    def retrieveParameters(Job job, def parameters) {
        def values = [:]

        parameters.each { parameter ->
            println parameter.name

            JobParameter jobParameter = JobParameter.findByJobAndSoftwareParameter(job, parameter as SoftwareParameter)

            println jobParameter

            String value = parameter.defaultValue
            if (jobParameter)
                value = jobParameter.value
            else if (parameter.required && value == null)
                throw new WrongArgumentException("Argument ${parameter.name} is required !")

            values.put(parameter, value)

            log.info("${parameter.name} = ${value}")
        }

        return values
    }

    /**
     * Returns the command to execute with the replaced parameters
     * @param job : the job to execute
     * @return the command with the given parameters
     * @throws be.cytomine.Exception.WrongArgumentException one required argument was not supplied
     */
    String[] getCommandJobWithArgs(Job job) {
        def parameters = SoftwareParameter.findAllBySoftwareAndServerParameter(job.software, false, [sort: "index", order: "asc"])

        def values = retrieveParameters(job, parameters)

        String command = job.software.executeCommand
        values.each {
            SoftwareParameter softwareParameter = it.key as SoftwareParameter

            String regex = "\\[${softwareParameter.name.toUpperCase()}\\]"
            String replacement = "--${softwareParameter.name} ${it.value as String}"

            if (softwareParameter.name.toUpperCase() == "HOST") {
                regex = "\\[CYTOMINE_HOST\\]"
                replacement = "--cytomine_host ${it.value as String}"
            } else if (softwareParameter.name.toUpperCase() == "PUBLICKEY") {
                regex = "\\[CYTOMINE_PUBLIC_KEY\\]"
                replacement = "--cytomine_public_key ${it.value as String}"
            } else if (softwareParameter.name.toUpperCase() == "PRIVATEKEY") {
                regex = "\\[CYTOMINE_PRIVATE_KEY\\]"
                replacement = "--cytomine_private_key ${it.value as String}"
            }

            command = command.replaceAll(regex, replacement)
        }

        return command.split(' ')
    }

    def getServerParameters(Job job) {
        def serverParameters = SoftwareParameter.findAllBySoftwareAndServerParameter(job.software, true, [sort: "index", order: "asc"])

        def returnedValues = [:]
        retrieveParameters(job, serverParameters).each { elem -> returnedValues.put(elem.key.name, elem.value) }

        return returnedValues
    }

    def initJob(Job job, UserJob userJob) {
        jobParameterService.add(JSON.parse(createJobParameter("host", job, Holders.getGrailsApplication().config.grails.serverURL).encodeAsJSON()))
        jobParameterService.add(JSON.parse(createJobParameter("publicKey", job, userJob.publicKey).encodeAsJSON()))
        jobParameterService.add(JSON.parse(createJobParameter("privateKey", job, userJob.privateKey).encodeAsJSON()))

        // Get all the parameters set by the server
        def softwareParameters = softwareParameterService.list(job.software, true)

        // Set all the parameters if they exist
        if (softwareParameters.find { it.name == "cytomine_id_software" })
            jobParameterService.add(JSON.parse(createJobParameter("cytomine_id_software", job, job.software.id.toString()).encodeAsJSON()))
        if (softwareParameters.find { it.name == "cytomine_id_project" })
            jobParameterService.add(JSON.parse(createJobParameter("cytomine_id_project", job, job.project.id.toString()).encodeAsJSON()))
    }

    def execute(Job job, UserJob userJob, ProcessingServer processingServer = null) {
        initJob(job, userJob)

        if (!job.software.executeCommand)
            throw new MiddlewareException("No command found for this job, the execution is aborted !")

        ProcessingServer currentProcessingServer = processingServer == null ? job.software.defaultProcessingServer : processingServer

        job.processingServer = currentProcessingServer
        job.save(failOnError: true)

        String queueName = amqpQueueService.queuePrefixProcessingServer + currentProcessingServer.name.capitalize()

        MessageBrokerServer messageBrokerServer = MessageBrokerServer.findByName("MessageBrokerServer")
        if (!amqpQueueService.checkRabbitQueueExists(queueName, messageBrokerServer))
            throw new MiddlewareException("The amqp queue doesn't exist, the execution is aborded !")

        JSONObject jsonObject = new JSONObject()
        jsonObject.put("requestType", "execute")
        jsonObject.put("jobId", job.id)
        jsonObject.put("command", getCommandJobWithArgs(job))
        jsonObject.put("pullingCommand", job.software.pullingCommand)
        jsonObject.put("serverParameters", getServerParameters(job))

        log.info("JOB REQUEST : ${jsonObject}")

        amqpQueueService.publishMessage(amqpQueueService.read(queueName), jsonObject.toString())
    }

    def killJob(Job job) {

        String queueName = amqpQueueService.queuePrefixProcessingServer + job.processingServer.name.capitalize()

        MessageBrokerServer messageBrokerServer = MessageBrokerServer.findByName("MessageBrokerServer")
        if (!amqpQueueService.checkRabbitQueueExists(queueName, messageBrokerServer)) {
            throw new MiddlewareException("The amqp queue doesn't exist, the execution is aborded !")
        }

        def message = [requestType: "kill", jobId: job.id]

        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder(message)

        log.info("KILL REQUEST : jobId - ${job.id} | processingServer - ${job.processingServer.id}")

        amqpQueueService.publishMessage(AmqpQueue.findByName(queueName), jsonBuilder.toString())
    }

    def getLogs(Job job) {
        def message = [requestType: "getLogs", jobId: job.id]

        JsonBuilder jsonBuilder = new JsonBuilder()
        jsonBuilder(message)

        amqpQueueService.publishMessage(AmqpQueue.findByName("queueCommunication"), jsonBuilder.toString())
    }

}
