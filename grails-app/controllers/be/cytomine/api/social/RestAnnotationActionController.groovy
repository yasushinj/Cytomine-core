package be.cytomine.api.social

import be.cytomine.Exception.CytomineException
import be.cytomine.api.RestController
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import be.cytomine.project.Project
import static org.springframework.security.acls.domain.BasePermission.READ

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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
/**
 * Controller for user position
 * Position of the user (x,y) on an image for a time
 */
class RestAnnotationActionController extends RestController {

    def annotationActionService
    def projectService
    def securityACLService

    def add = {
        try {
            responseSuccess(annotationActionService.add(request.JSON))
        } catch (CytomineException e) {
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

    @RestApiMethod(description="Get the number of annotation actions in the specified project")
    @RestApiParams(params=[
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.PATH, description = "The identifier of the project"),
            @RestApiParam(name="startDate", type="long", paramType = RestApiParamType.QUERY, description = "Only actions after this date will be counted (optional)"),
            @RestApiParam(name="endDate", type="long", paramType = RestApiParamType.QUERY, description = "Only actions before this date will be counted (optional)"),
            @RestApiParam(name="type", type="string", paramType = RestApiParamType.QUERY, description = "(Optional) If specified, only annotation action of this type will be taken into account"),
    ])
    def countByProject() {
        Project project = projectService.read(params.project)
        securityACLService.check(project, READ)

        Long startDate = params.long("startDate")
        Long endDate = params.long("endDate")

        responseSuccess(annotationActionService.countByProject(project, startDate, endDate, params.type))
    }


}
