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

import be.cytomine.Exception.ServerException
import be.cytomine.command.*
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.util.GrailsUtil

import static org.springframework.security.acls.domain.BasePermission.READ

class JobDataService extends ModelService {

    static transactional = true

    def cytomineService
    def commandService
    def modelService
    def userGroupService
    def springSecurityService
    def transactionService
    def securityACLService

    def currentDomain() {
        return JobData
    }

    def read(def id) {
        def jobData = JobData.read(id)
        if(jobData) {
            securityACLService.check(jobData.container(),READ)
        }
        jobData
    }

    def list() {
        securityACLService.checkAdmin(cytomineService.currentUser)
        JobData.list(sort: "id")
    }

    def list(Job job) {
        securityACLService.check(job.container(),READ)
        JobData.findAllByJob(job)
    }

    def list(Job job, String key) {
        securityACLService.check(job.container(),READ)
        JobData.findAllByJobAndKey(job, key)
    }

    JobData save(JobData jobData, byte[] data) {
        if(!grailsApplication.config.cytomine.jobdata.filesystem) {
            return saveInDatabase(jobData,data)
        } else {
            return saveInFileSystem(jobData,data)
        }
    }

    byte[] read(JobData jobData) {
        if(!grailsApplication.config.cytomine.jobdata.filesystem) {
            return readFromDatabase(jobData)
        } else {
            return readFromFileSystem(jobData)
        }
    }


    /**
     * Return associated data of JobData instance domain attached to a Job
     * @param job a Job instance
     * @param key which identifies the JobData
     * @return a byte array, the data attached to JobData
     */
    byte[] getJobDataBinaryValue(Job job, String key) {
        Collection<JobData> jobDataCollection = list(job, key)
        if (jobDataCollection.isEmpty()) {
            return null //no preview available
        } else {
            JobData jobData = jobDataCollection.pop()
            return read(jobData)
        }

    }

    /**
      * Save a job data on database
      * @param jobData Job data description
      * @param data Data bytes
      * @return job data
      */
    private JobData saveInDatabase(JobData jobData, byte[] data) {
        JobDataBinaryValue value = new JobDataBinaryValue(jobData:jobData)
        value.data = data
        value.save(failOnError: true)
        jobData.value = value
        jobData.save(failOnError: true)

        jobData.size = data.length;
        return jobData
     }




     /**
      * Read job data files from database
      * @param jobData Job data description
      * @return Job data bytes
      */
    private byte[] readFromDatabase(JobData jobData) {
         return jobData.value.data
     }

     /**
      * Save a job data on disk file system
      * @param jobData Job data description
      * @param data data bytes
      */
    private void saveInFileSystem(JobData jobData, byte[] data) {
         File dir = new File(grailsApplication.config.cytomine.jobdata.filesystemPath + GrailsUtil.environment + "/"+jobData.job.id +"/" + jobData.key )
         File f = new File(dir.getAbsolutePath()+ "/"+jobData.filename)
         jobData.size = data.length;
         jobData.save(flush:true)
         try {
             dir.mkdirs()
             new FileOutputStream(f).withWriter { w ->
                 w << new BufferedInputStream( new ByteArrayInputStream(data) )
             }
         } catch(Exception e) {
             throw new ServerException("Cannot create file: " + e)
         }
     }

     /**
      * Read job data files from disk file system
      * @param jobData Job data description
      * @return Job data bytes
      */
    private byte[] readFromFileSystem(JobData jobData)  {
         File f = new File(grailsApplication.config.cytomine.jobdata.filesystemPath + GrailsUtil.environment + "/"+ jobData.job.id +"/" + jobData.key + "/"+ jobData.filename)
         try {
             InputStream inputStream = new FileInputStream(f);
             int offset = 0;
             int bytesRead;
             // Get the byte array
             byte[] bytes = new byte[(int) f.length()];
             // Iterate the byte array
             while (offset < bytes.length && (bytesRead = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                 offset += bytesRead;
             }
             // Close after use
             inputStream.close();
             return bytes
         } catch(Exception e) {
             throw new ServerException("Cannot read file: " + e)
         }
     }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        securityACLService.check(json.job, Job,"container",READ)
        securityACLService.checkisNotReadOnly(json.job, Job)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser),null,json)
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(JobData jd, def jsonNewData) {
        securityACLService.check(jd.container(),READ)
        securityACLService.checkisNotReadOnly(jd)
        SecUser currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser),jd,jsonNewData)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(JobData domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.check(domain.container(),READ)
        securityACLService.checkisNotReadOnly(domain)
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.key, domain.job?.id]
    }

    def deleteDependentJobDataBinaryValue(JobData jobData, Transaction transaction, Task task = null) {
        if(jobData.value) {
            jobData.value.delete()
        }
    }

}
