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
import be.cytomine.middleware.MessageBrokerServer
import be.cytomine.security.UserJob
import grails.converters.JSON
import grails.util.Holders
import org.json.JSONObject

class JobLauncherService {

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

    /**
     * Returns the command to execute with the replaced parameters
     * @param job : the job to execute
     * @return the command with the given parameters
     * @throws be.cytomine.Exception.WrongArgumentException one required argument was not supplied
     */
    String[] getCommandJobWithArgs(Job job) throws WrongArgumentException {
        Collection<SoftwareParameter> parameters = SoftwareParameter.findAllBySoftware(job.software, [sort: "index", order: "asc"])
        def paramsAndValue = [:]

        //Fill default value if no value specified, check if mandatory value are well defined
        parameters.eachWithIndex { softParam, i ->
            JobParameter jobParam = JobParameter.findByJobAndSoftwareParameter(job, softParam)

            String value = softParam.defaultValue
            if (jobParam) {
                value = jobParam.value
            } else if (softParam.required) {
                throw new WrongArgumentException("Argument " + softParam.name + " is required!")
            }
            paramsAndValue.put(softParam, value)
            log.info softParam.name + "=" + value
        }

        // Replace the arguments with actual parameters' value
        String command = job.software.executeCommand
        paramsAndValue.each {
            command = command.replaceAll('\\\$' + (it.key as SoftwareParameter).name, it.value as String)
        }

        return command.split(' ')
    }

    def initJob(Job job, UserJob userJob) {
        jobParameterService.add(JSON.parse(createJobParameter("host", job, Holders.getGrailsApplication().config.grails.serverURL).encodeAsJSON()))
        jobParameterService.add(JSON.parse(createJobParameter("publicKey", job, userJob.publicKey).encodeAsJSON()))
        jobParameterService.add(JSON.parse(createJobParameter("privateKey", job, userJob.privateKey).encodeAsJSON()))

        //get all parameters with set by server = true.
        def softwareParameters = softwareParameterService.list(job.software, true)

        // then set if these parameters exist
        if (softwareParameters.find { it.name == "cytomine_id_software" })
            jobParameterService.add(JSON.parse(createJobParameter("cytomine_id_software", job, job.software.id.toString()).encodeAsJSON()))
        if (softwareParameters.find { it.name == "cytomine_id_project" })
            jobParameterService.add(JSON.parse(createJobParameter("cytomine_id_project", job, job.project.id.toString()).encodeAsJSON()))
    }

    /**
     * Sends a job execution request to the software router
     * @param job : the job we want to execute
     * @param userJob : the userJob associated with this job
     * @param processingServer : the processing server on which we want to execute the job
     * @return /
     */
    def execute(Job job, UserJob userJob, ProcessingServer processingServer = null) {
        initJob(job, userJob)

        if (!job.software.executeCommand) {
            throw new MiddlewareException("No command found for this job, the execution is aborted !")
        }

        ProcessingServer currentProcessingServer = processingServer == null ? job.software.defaultProcessingServer : processingServer

        String queueName = amqpQueueService.queuePrefixProcessingServer + currentProcessingServer.name.capitalize()

        MessageBrokerServer messageBrokerServer = MessageBrokerServer.findByName("MessageBrokerServer")
        if (!amqpQueueService.checkRabbitQueueExists(queueName, messageBrokerServer)) {
            throw new MiddlewareException("The amqp queue doesn't exist, the execution is aborded !")
        }

        JSONObject jsonObject = new JSONObject()
        jsonObject.put("requestType", "execute")
        jsonObject.put("jobId", job.id)
        jsonObject.put("command", getCommandJobWithArgs(job))

        log.info("JOB REQUEST : ${jsonObject}")

        job.discard()

        amqpQueueService.publishMessage(amqpQueueService.read(queueName), jsonObject.toString())
    }

}
