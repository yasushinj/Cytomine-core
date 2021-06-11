package be.cytomine.processing

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

import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.security.UserJob
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.security.acls.domain.BasePermission.READ

class JobParameterService extends ModelService {

    static transactional = true
    def cytomineService
    def commandService
    def modelService
    def securityACLService

    def currentDomain() {
        return JobParameter
    }

    def read(def id) {
        def jobParam = JobParameter.read(id)
        if(jobParam) {
            securityACLService.check(jobParam.container(),READ)
            if(jobParam.softwareParameter.name == "privateKey" && UserJob.findByJob(jobParam.job).user.privateKey != cytomineService.currentUser.privateKey){
                jobParam.value = "*****************************"
            }
        }
        jobParam
    }

    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        JobParameter.list()
    }

    def list(Job job) {
        securityACLService.check(job.container(),READ)
        JobParameter.findAllByJob(job)
    }

    def list(List<Long> jobIds) {

        def parameters = JobParameter.findAllByJobInList(Job.findAllByIdInList(jobIds),[sort: 'job'])
        def result = []
        parameters.each {
            def values = JobParameter.getDataFromDomain(it)
            if(values['name'].equals("privateKey")) values['value']= "*********************"
            result << values
        }
        return result
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        securityACLService.check(json.job,Job,"container", READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(JobParameter jp, def jsonNewData) {
        securityACLService.check(jp.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser),jp, jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(JobParameter domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),READ)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    /**
     * Add a job parameter for a job
     */
    def addJobParameter(def idJob, def idSoftwareParameter, def value,User currentUser,Transaction transaction) {
        JSONObject json = new JSONObject()
        json.put("softwareParameter", idSoftwareParameter)
        json.put("value", value)
        json.put("job", idJob)
        return executeCommand(new AddCommand(user: currentUser,transaction:transaction), null, json)
    }

    def getStringParamsI18n(def domain) {
        return [domain.value, domain.softwareParameter?.name]
    }
}
