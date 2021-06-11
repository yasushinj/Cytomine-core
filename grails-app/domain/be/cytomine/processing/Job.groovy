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

import be.cytomine.CytomineDomain
import be.cytomine.command.ResponseService
import be.cytomine.project.Project
import be.cytomine.security.UserJob
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * A job is a software instance
 * This is the execution of software with some parameters
 */
@RestApiObject(name = "Job", description = "A job is a software instance. This is the execution of software with some parameters")
class Job extends CytomineDomain  {

    /**
     * Job status (enum type are too heavy with GORM)
     */
    public static int NOTLAUNCH = 0
    public static int INQUEUE = 1
    public static int RUNNING = 2
    public static int SUCCESS = 3
    public static int FAILED = 4
    public static int INDETERMINATE = 5
    public static int WAIT = 6
    public static int PREVIEWED = 7
    public static int KILLED = 8

    /**
     * Job progression
     */
    @RestApiObjectField(description = "The algo progression (from 0 to 100)",mandatory = false)
    int progress = 0

    /**
     * Job status (see static int)
     */
    @RestApiObjectField(description = "The algo status (NOTLAUNCH = 0, INQUEUE = 1, RUNNING = 2,SUCCESS = 3,FAILED = 4,INDETERMINATE = 5,WAIT = 6,PREVIEWED = 7, KILLED=8)",mandatory = false)
    int status = 0

    /**
     * Job Indice for this software in this project
     */
    @RestApiObjectField(description = "Job Indice for this software in this project",useForCreation = false)
    int number

    /**
     * Text comment for the job status
     */
    @RestApiObjectField(description = "Text comment for the job status", mandatory = false)
    String statusComment

    /**
     * Job project
     */
    @RestApiObjectField(description = "The project of the job")
    Project project

    @RestApiObjectField(description = "The processing server in charge to run the job")
    ProcessingServer processingServer

    /**
     * Generic field for job rate info
     * The rate is a quality value about the job works
     */
    @RestApiObjectField(description = "Generic field for job rate info. The rate is a quality value about the job works",mandatory = false)
    Double rate = null

    /**
     * Flag to see if data generate by this job are deleted
     */
    @RestApiObjectField(description = "Flag to see if data generate by this job are deleted",mandatory = false)
    boolean dataDeleted = false

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "algoType", description = "The algo type based on the class name",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "softwareName", description = "The software name of the job",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "username", description = "The username of the job",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "userJob", description = "The user of the job",allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "jobParameters", description = "List of job parameters for this job",allowedType = "list",useForCreation = false)
    ])
    static transients = ["url"]

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "software", description = "The software of the job",allowedType = "long",useForCreation = true)
    ])
    static belongsTo = [software: Software]

    static constraints = {
        progress(min: 0, max: 100)
        project(nullable:true)
        statusComment(nullable:true)
        status(range: 0..8)
        rate(nullable: true)
        processingServer(nullable: true)
    }

    static mapping = {
        tablePerHierarchy(true)
        id(generator: 'assigned', unique: true)
        sort "id"
        software fetch: 'join'
    }

    public beforeInsert() {
        super.beforeInsert()
        //Get the last job with the same software and same project to incr job number
        List<Job> previousJob = Job.findAllBySoftwareAndProject(software,project,[sort: "number", order: "desc",max: 1])
        if(!previousJob.isEmpty()) {
            number = previousJob.get(0).number+1
        } else {
            number = 1
        };
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */    
    static def insertDataIntoDomain(def json,def domain = new Job()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.status = JSONUtils.getJSONAttrInteger(json, 'status', 0)
        domain.progress = JSONUtils.getJSONAttrInteger(json, 'progress', 0)
        domain.statusComment = JSONUtils.getJSONAttrStr(json, 'statusComment')
        domain.project = JSONUtils.getJSONAttrDomain(json, "project", new Project(), true)
        domain.processingServer = JSONUtils.getJSONAttrDomain(json, "processingServer", new ProcessingServer(), false)
        domain.software = JSONUtils.getJSONAttrDomain(json, "software", new Software(), true)
        domain.rate = JSONUtils.getJSONAttrDouble(json, 'rate', -1)
        domain.dataDeleted =  JSONUtils.getJSONAttrBoolean(json,'dataDeleted', false)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        return domain
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['algoType'] = ResponseService?.getClassName(domain)?.toLowerCase()
        returnArray['progress'] = domain?.progress
        returnArray['status'] = domain?.status
        returnArray['number'] = domain?.number
        returnArray['statusComment'] = domain?.statusComment
        returnArray['project'] = domain?.project?.id
        returnArray['processingServer'] = domain?.processingServer?.id
        returnArray['software'] = domain?.software?.id
        returnArray['softwareName'] = domain?.software?.fullName()
        returnArray['name'] = returnArray['softwareName']+" : "+returnArray['number']
        returnArray['rate'] = domain?.rate
        returnArray['dataDeleted'] = domain?.dataDeleted
        try {
            UserJob user = UserJob.findByJob(domain)
            returnArray['username'] = user?.humanUsername()
            returnArray['userJob'] = user?.id
            returnArray['jobParameters'] = domain?.parameters()
        } catch (Exception e) {
        }
        return returnArray
    }


    public def parameters() {
        if(this.version!=null) {
            def result = []
            List<JobParameter> parameters = JobParameter.findAllByJob(this,[sort: 'created'])
            parameters.each {
                def values = JobParameter.getDataFromDomain(it)
                if (["PUBLICKEY", "CYTOMINEPUBLICKEY", "PRIVATEKEY", "CYTOMINEPRIVATEKEY"].contains(values['name'].toUpperCase().replace('_', '')))
                    values['value']= "*********************"

                result << values
            }
            return result
        } else {
            return []
        }
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container()
    }

    public String toString() {
        software.getName() + "[" + software.getId() + "]"
    }

    public isNotLaunch () {
        return status == NOTLAUNCH
    }

    public isInQueue () {
        return status == INQUEUE
    }

    public isRunning () {
        return status == RUNNING
    }

    public isSuccess () {
        return status == SUCCESS
    }

    public isFailed () {
        return status == FAILED
    }

    public isIndeterminate () {
        return status == INDETERMINATE
    }

    public isWait () {
        return status == WAIT
    }

    public isPreviewed () {
        return status == PREVIEWED
    }

    public isKilled () {
        return status == KILLED
    }
}
