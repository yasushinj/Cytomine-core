package be.cytomine.api.security

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
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.api.RestController
import be.cytomine.security.SecRole
import be.cytomine.security.SecUser
import be.cytomine.security.SecUserSecRole
import be.cytomine.security.User
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Controller to manage user role
 */
@RestApi(name = "Security | sec user sec role services", description = "Methods for managing a user role")
class RestSecUserSecRoleController extends RestController {

    def secUserService
    def secRoleService
    def secUserSecRoleService
    def cytomineService
    def currentRoleServiceProxy

    /**
     * List all roles for a user
     */
    @RestApiMethod(description="List all roles for a user", listing = true)
    @RestApiParams(params=[
        @RestApiParam(name="user", type="string", paramType = RestApiParamType.PATH, description = "The user id")
    ])
    def list() {
        User user = secUserService.read(params.long('user'));
        if (params.highest){
            responseSuccess(secUserSecRoleService.getHighest(user))
        } else {
            responseSuccess(secUserSecRoleService.list(user))
        }
    }

    /**
     * Check a role for a user
     * If user has not this role, send 404
     */
    @RestApiMethod(description="Get a user role")
    @RestApiParams(params=[
        @RestApiParam(name="user", type="string", paramType = RestApiParamType.PATH, description = "The user id"),
        @RestApiParam(name="role", type="string", paramType = RestApiParamType.PATH, description = "The role id")
    ])
    def show() {
        User user = secUserService.read(params.long('user'));
        SecRole role = secRoleService.read(params.long('role'));
        SecUserSecRole secUserSecRole = secUserSecRoleService.get(user, role)
        if (secUserSecRole) {
            responseSuccess(secUserSecRole)
        } else {
            responseNotFound("SecUserSecRole", params.user)
        }
    }

    /**
     * Add a new role to a user
     */
    @RestApiMethod(description="Get a user role")
    @RestApiParams(params=[
        @RestApiParam(name="user", type="string", paramType = RestApiParamType.PATH, description = "The user id"),
        @RestApiParam(name="role", type="string", paramType = RestApiParamType.PATH, description = "The role id")
    ])
    def add() {
        add(secUserSecRoleService, request.JSON)
    }

    /**
     * Delete a role from a user
     */
    @RestApiMethod(description="Delete a user role")
    @RestApiParams(params=[
        @RestApiParam(name="user", type="string", paramType = RestApiParamType.PATH, description = "The user id"),
        @RestApiParam(name="role", type="string", paramType = RestApiParamType.PATH, description = "The role id")
    ])
    def delete() {
        delete(secUserSecRoleService, JSON.parse("{user : $params.user, role: $params.role}"),null)
    }

    @RestApiMethod(description="Define a role for a user. If admin is defined, user will have admin,user,guest. If user is defined, user will have user,guest, etc. Role may be create or remove")
    @RestApiParams(params=[
        @RestApiParam(name="user", type="string", paramType = RestApiParamType.PATH, description = "The user id"),
        @RestApiParam(name="role", type="string", paramType = RestApiParamType.PATH, description = "The role id")
    ])
    def define() {
        SecUser user = SecUser.read(params.long('user'))
        SecRole role = SecRole.read(params.long('role'))

        try {
            if(!user) {
                throw new ObjectNotFoundException("Cannot read user ${params.long('user')}")
            }
            if(!role) {
                throw new ObjectNotFoundException("Cannot read role${params.long('role')}")
            }
            secUserSecRoleService.define(user,role)

            responseSuccess(currentRoleServiceProxy.findCurrentRole(user))

        } catch (CytomineException e) {
            log.error("add error:" + e.msg)
            log.error(e)
            response([success: false, errors: e.msg], e.code)
        }
    }

}
