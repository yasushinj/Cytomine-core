package be.cytomine

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

import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.AlgoAnnotationTerm
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.processing.Job
import be.cytomine.processing.JobData
import be.cytomine.project.Project
import be.cytomine.security.UserJob
import be.cytomine.test.BasicInstanceBuilder
import be.cytomine.test.Infos
import be.cytomine.test.http.JobAPI
import be.cytomine.test.http.JobDataAPI
import be.cytomine.test.http.TaskAPI
import be.cytomine.utils.UpdateData
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class JobTests  {

    void testListJobWithCredential() {
        def result = JobAPI.list(Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testListJobBySoftwareAndProjectWithCredential() {
        Job job = BasicInstanceBuilder.getJob()
        def result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray

        result = JobAPI.listBySoftwareAndProject(-99,job.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 0

        result = JobAPI.listBySoftwareAndProject(job.software.id,-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,false)
        json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
        assert json.collection.size() == 0
    }

    void testListJobBySoftwareAndProjectWithCredentialLight() {
        Job job = BasicInstanceBuilder.getJob()
        def result = JobAPI.listBySoftwareAndProject(job.software.id,job.project.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD,true)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json.collection instanceof JSONArray
    }

    void testShowJobWithCredential() {
        def result = JobAPI.show(BasicInstanceBuilder.getJob().id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddJobCorrect() {
        def jobToAdd = BasicInstanceBuilder.getJobNotExist()
        def result = JobAPI.create(jobToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idJob = result.data.id
  
        result = JobAPI.show(idJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddJobWithBadSoftware() {
        Job jobToAdd = BasicInstanceBuilder.getJob()
        Job jobToEdit = Job.get(jobToAdd.id)
        def jsonJob = jobToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonJob)
        jsonUpdate.software = -99
        jsonJob = jsonUpdate.toString()
        def result = JobAPI.update(jobToAdd.id, jsonJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testUpdateJobCorrect() {
        def job =  BasicInstanceBuilder.getJob()
        def data = UpdateData.createUpdateSet(job,[progress: [0,100]])
        def result = JobAPI.update(job.id, data.postData,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testUpdateJobNotExist() {
        Job jobWithNewName = BasicInstanceBuilder.getJobNotExist()
        jobWithNewName.save(flush: true)
        Job jobToEdit = Job.get(jobWithNewName.id)
        def jsonJob = jobToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonJob)
        jsonUpdate.id = -99
        jsonJob = jsonUpdate.toString()
        def result = JobAPI.update(-99, jsonJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testUpdateJobWithBadSoftware() {
        Job jobToAdd = BasicInstanceBuilder.getJob()
        Job jobToEdit = Job.get(jobToAdd.id)
        def jsonJob = jobToEdit.encodeAsJSON()
        def jsonUpdate = JSON.parse(jsonJob)
        jsonUpdate.software = -99
        jsonJob = jsonUpdate.toString()
        def result = JobAPI.update(jobToAdd.id, jsonJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testDeleteJob() {
        def jobToDelete = BasicInstanceBuilder.getJobNotExist()
        assert jobToDelete.save(flush: true)!= null
        def id = jobToDelete.id

        def jobData = BasicInstanceBuilder.getJobDataNotExist(jobToDelete)
        assert jobData.save(flush: true)!= null

        def result = JobDataAPI.show(jobData.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = JobAPI.delete(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        def showResult = JobAPI.show(id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == showResult.code

        result = JobDataAPI.show(jobData.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteJobNotExist() {
        def result = JobAPI.delete(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testGetLog() {
        def job =  BasicInstanceBuilder.getJob()
        def result = JobAPI.getLog(0L, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
        result = JobAPI.getLog(job.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
        def log = BasicInstanceBuilder.getAttachedFileNotExist()
        log.domainClassName = job.class.name
        log.domainIdent = job.id
        log.filename = "log.out"
        log.save(flush: true)
        result = JobAPI.getLog(job.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testListJobData() {
        Job job = BasicInstanceBuilder.getJob()
        def result = JobAPI.listAllJobData(job.id,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        result = JobAPI.listAllJobData(-99,Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }


    void testDeleteAllJobDataJobNotExist() {
        def result = JobAPI.deleteAllJobData(-99, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 404 == result.code
    }

    void testDeleteAllJobData() {
        //create a job
        Job job = BasicInstanceBuilder.getJobNotExist(true)
        BasicInstanceBuilder.getSoftwareProjectNotExist(job.software,job.project,true)

        UserJob userJob = BasicInstanceBuilder.getUserJobNotExist(true)
        userJob.job = job
        userJob.user = BasicInstanceBuilder.getUser()
        BasicInstanceBuilder.saveDomain(userJob)

        //add algo-annotation for this job
        AlgoAnnotation a1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(job,userJob,true)

        //add algo-annotation-term for this job
        AlgoAnnotationTerm at1 = BasicInstanceBuilder.getAlgoAnnotationTerm(job,a1,userJob)

        //add job data
        JobData data1 = BasicInstanceBuilder.getJobDataNotExist()
        data1.job = job
        BasicInstanceBuilder.saveDomain(data1)


        //count data = 1-1
        assert AlgoAnnotationTerm.findAllByUserJobInList(UserJob.findAllByJob(job)).size() == 1
        assert AlgoAnnotation.findAllByUserInList(UserJob.findAllByJob(job)).size() == 1
        assert JobData.findAllByJob(job).size() == 1

        //delete all job data
        def result = JobAPI.deleteAllJobData(job.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //count data = 0-0
        assert AlgoAnnotationTerm.findAllByUserJobInList(UserJob.findAllByJob(job)).size() == 0
        assert AlgoAnnotation.findAllByUserInList(UserJob.findAllByJob(job)).size() == 0
        assert JobData.findAllByJob(job).size() == 0
    }

    void testDeleteAllJobDataWithReviewedAnnotations() {
        //create a job

        UserJob userJob = BasicInstanceBuilder.getUserJobNotExist(true)
        Job job = userJob.job

        //add algo-annotation for this job
        AlgoAnnotation a1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(job,userJob,true,)

        //add algo-annotation-term for this job
        AlgoAnnotationTerm at1 = BasicInstanceBuilder.getAlgoAnnotationTerm(job,a1,userJob)

        //add reviewed annotation
        ReviewedAnnotation reviewed = BasicInstanceBuilder.getReviewedAnnotationNotExist()
        reviewed.project = job.project
        reviewed.image = a1.image
        reviewed.parentIdent = a1.id
        reviewed.parentClassName = a1.class.getName()
        BasicInstanceBuilder.saveDomain(reviewed)

        println "ReviewedAnnotation project=${reviewed.project.id} & parent=${reviewed.parentIdent}"
        println "ReviewedAnnotation job.project=${job.project.id}"
        //count data = 1-1
        assert AlgoAnnotationTerm.findAllByUserJobInList(UserJob.findAllByJob(job)).size() == 1
        assert AlgoAnnotation.findAllByUserInList(UserJob.findAllByJob(job)).size() == 1

        //delete all job data
        def result = JobAPI.deleteAllJobData(job.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }




    void testPurgeJobData() {

        //create job 1 & 2
        Job job1 = BasicInstanceBuilder.getJobNotExist(true)
        Job job2 = BasicInstanceBuilder.getJobNotExist(true)
        UserJob userJob1 = BasicInstanceBuilder.getUserJobNotExist(true)
        UserJob userJob2 = BasicInstanceBuilder.getUserJobNotExist(true)
        userJob1.job = job1
        userJob2.job = job2

        //create project
        Project project = BasicInstanceBuilder.getProjectNotExist(true)

        //update job 1 & 2 project
        job1.project = project
        job2.project = project
        BasicInstanceBuilder.saveDomain(job1)
        BasicInstanceBuilder.saveDomain(job2)
        BasicInstanceBuilder.saveDomain(userJob1)
        BasicInstanceBuilder.saveDomain(userJob2)

        //add two annotation with at + add jobdata
        println "Job1=${job1}"
        println "Job2=${job2}"
        println "UserJob.findByJob(job1)=${UserJob.findByJob(job1)}"
        AlgoAnnotation annotation1 = BasicInstanceBuilder.getAlgoAnnotationNotExist(job1,UserJob.findByJob(job1),true)
        AlgoAnnotation annotation2 = BasicInstanceBuilder.getAlgoAnnotationNotExist(job2, UserJob.findByJob(job2),true)

        //add a review for annotation from job 1
        ReviewedAnnotation reviewed1 = BasicInstanceBuilder.createReviewAnnotation(annotation1)

        def result = TaskAPI.create(project.id, Infos.SUPERADMINLOGIN,Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def jsonTask = JSON.parse(result.data)

        project.refresh()
        assert project.countJobAnnotations==2
        //purge
        result = JobAPI.purgeProjectData(project.id,jsonTask.task.id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code

        //check if annot job 1 is still there & job 2 deleted
        assert AlgoAnnotation.countByUser(UserJob.findByJob(job1))==1
        assert AlgoAnnotation.countByUser(UserJob.findByJob(job2))==0
        project.refresh()
        assert project.countJobAnnotations==1
    }

    void testShowUserJob() {
        def result = JobAPI.showUserJob(BasicInstanceBuilder.getUserJobNotExist(true).id, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        def json = JSON.parse(result.data)
        assert json instanceof JSONObject
    }

    void testAddUserJobCorrect() {
        def userJobToAdd = BasicInstanceBuilder.getUserJobNotExist()
        def result = JobAPI.createUserJob(userJobToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUserJob = result.data.id

        result = JobAPI.showUserJob(idUserJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

    void testAddUserJobIncorrect() {
        def userJobToAdd = BasicInstanceBuilder.getUserJobNotExist()
        userJobToAdd.job = null

        def result = JobAPI.createUserJob(userJobToAdd.encodeAsJSON(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 400 == result.code
    }

    void testAddUserJobWithProjectAndSoftwareCorrect() {
        def userJobToAdd = BasicInstanceBuilder.getUserJobNotExist()
        def json = JSON.parse(userJobToAdd.encodeAsJSON())
        json.software = userJobToAdd.job.software.id
        json.project = userJobToAdd.job.project.id
        json.job = "null"
        def result = JobAPI.createUserJob(json.toString(), Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
        int idUserJob = result.data.id

        result = JobAPI.showUserJob(idUserJob, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
        assert 200 == result.code
    }

}
