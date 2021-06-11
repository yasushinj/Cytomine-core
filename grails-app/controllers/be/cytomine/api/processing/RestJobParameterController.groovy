package be.cytomine.api.processing

import be.cytomine.Exception.InvalidRequestException

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

import be.cytomine.api.RestController
import be.cytomine.processing.Job
import be.cytomine.processing.JobParameter
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller for job parameter
 * Each software may have some parameters (e.g.: cytomine project id, number of thread,...)
 * A job parameter is a software parameter instance with a specific value for this job
 */
@RestApi(name = "Processing | job parameter services", description = "Methods for managing job parameter. Each software may have some parameters (e.g.: cytomine project id, number of thread,...). A job parameter is a software parameter instance with a specific value for this job")
class RestJobParameterController extends RestController {

    def jobParameterService
    def jobService

    /**
     * List all job parameter
     */
    @RestApiMethod(description="Get all job parameter", listing = true)
    def list() {
        responseSuccess(jobParameterService.list())
    }

    /**
     * List all job parameter for a job
     */
    @RestApiMethod(description="Get all job parameter for a job", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The job id")
    ])
    def listByJob() {
        Job job = jobService.read(params.long('id'));
        if (job) {
            responseSuccess(jobParameterService.list(job))
        } else {
            responseNotFound("JobParameter", "Job", params.id)
        }
    }

    /**
     * Get a job parameter
     */
    @RestApiMethod(description="Get a job parameter")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The job parameter id")
    ])
    def show() {
        JobParameter jobParameter = jobParameterService.read(params.long('id'))
        if (jobParameter) {
            responseSuccess(jobParameter)
        } else {
            responseNotFound("JobParameter", params.id)
        }
    }

    /**
     * Add a new job parameter
     */
    @RestApiMethod(description="Add a new job parameter")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The job parameter id")
    ])
    def add() {
        if(!request.JSON.job) throw new InvalidRequestException("job not set")
        add(jobParameterService, request.JSON)
    }

    /**
     * Update job parameter
     */
    @RestApiMethod(description="Update a job parameter")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The job parameter id")
    ])
    def update() {
        update(jobParameterService, request.JSON)
    }

    /**
     * Delete job parameter
     */
    @RestApiMethod(description="Delete a job parameter")
    @RestApiParams(params=[
        @RestApiParam(name="id", type="long", paramType = RestApiParamType.PATH, description = "The job parameter id")
    ])
    def delete() {
        delete(jobParameterService, JSON.parse("{id : $params.id}"),null)
    }

}
