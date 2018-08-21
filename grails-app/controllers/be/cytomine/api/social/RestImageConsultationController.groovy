package be.cytomine.api.social

/*
* Copyright (c) 2009-2017. Authors: see NOTICE file.
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
import be.cytomine.project.Project
import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import org.restapidoc.annotation.RestApiMethod

import java.text.SimpleDateFormat


/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
class RestImageConsultationController extends RestController {

    def projectService
    def imageConsultationService
    def exportService

    @RestApiMethod(description="Add a new image consultation object")
    def add() {
        try {
            responseSuccess(imageConsultationService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    def lastImageOfUsersByProject = {
        Project project = projectService.read(params.project)
        responseSuccess(imageConsultationService.lastImageOfUsersByProject(project))
    }

    @RestApiMethod(description="Get a summary of the consultations on an image for an user and a project")
    def resumeByUserAndProject() {
        def result = imageConsultationService.resumeByUserAndProject(Long.parseLong(params.user), Long.parseLong(params.project))

        if(params.export.equals("csv")) {
            Long user = Long.parseLong(params.user)
            Long project = Long.parseLong(params.project)
            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
            String now = simpleFormat.format(new Date())
            response.contentType = grailsApplication.config.grails.mime.types[params.format]
            response.setHeader("Content-disposition", "attachment; filename=image_consultations_of_user_${user}_project_${project}_${now}.${params.export}")

            def exporterIdentifier = params.export;
            def exportResult = []
            List fields = ["time", "first", "last", "frequency", "imageId", "imageName", "imageThumb", "numberOfCreatedAnnotations"]
            Map labels = ["time": "Cumulated duration (ms)", "first" : "First consultation", "last" : "Last consultation", "frequency" :"Number of consultations","imageId": "Id of image", "imageName": "Name", "imageThumb": "Thumb", "numberOfCreatedAnnotations": "Number of created annotations"]
            result.each {
                def data = [:]
                data.time = it.time ?: 0;
                data.first = it.first
                data.last = it.last
                data.frequency = it.frequency
                data.imageId = it.image
                data.imageName = it.imageName
                data.imageThumb = it.imageThumb
                data.numberOfCreatedAnnotations = it.countCreatedAnnotations
                exportResult << data
            }

            String title = "Consultations of images into project ${project} by user ${user}"
            exportService.export(exporterIdentifier, response.outputStream, exportResult, fields, labels, null, ["column.widths": [0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12], "title": title, "csv.encoding": "UTF-8", "separator": ";"])
        } else {
            responseSuccess(result)
        }
    }

}
