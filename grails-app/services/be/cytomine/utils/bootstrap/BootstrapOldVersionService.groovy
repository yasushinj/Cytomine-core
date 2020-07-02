package be.cytomine.utils.bootstrap

/*
* Copyright (c) 2009-2020. Authors: see NOTICE file.
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

import be.cytomine.middleware.AmqpQueue
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.image.UploadedFile
import be.cytomine.processing.ImageFilter
import be.cytomine.meta.Property
import be.cytomine.project.Project
import be.cytomine.security.SecRole
import be.cytomine.security.SecUser
import be.cytomine.security.SecUserSecRole
import be.cytomine.security.User
import be.cytomine.meta.Configuration
import be.cytomine.utils.Version
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql
import org.apache.commons.io.FilenameUtils
import org.springframework.security.acls.domain.BasePermission

/**
 * Cytomine
 * User: lrollus
 * This class contains all code when you want to change the database dataset.
 * E.g.: add new rows for a specific version, drop a column, ...
 *
 * The main method ("execChangeForOldVersion") is called by the bootstrap.
 * This method automatically run all initYYYYMMDD() methods from this class where YYYYMMDD is lt version number
 *
 * E.g. init20150115() will be call if the current version is init20150201.
 * init20150101() won't be call because: 20150101 < 20150115 < 20150201.
 *
 * At the end of the execChangeForOldVersion, the current version will be set thanks to the grailsApplication.metadata.'app.version' config
 */
class BootstrapOldVersionService {

    def grailsApplication
    def bootstrapUtilsService
    def dataSource
    def permissionService
    def secUserService
    def storageService
    def tableService
    def mongo
    def noSQLCollectionService
    def executorService

    void execChangeForOldVersion() {
        def methods = this.metaClass.methods*.name.sort().unique()
        Version version = Version.getLastVersion()

        methods.findAll { it.startsWith("initv") }.each { method ->

            method = method.substring("initv".size())

            Short major = Short.parseShort(method.split("_")[0])
            Short minor = Short.parseShort(method.split("_")[1])
            Short patch = Short.parseShort(method.split("_")[2])

            if(major > version.major || (major == version.major && minor > version.minor)
                    || (major == version.major && minor == version.minor && patch > version.patch)) {
                log.info "Run code for v${method.replace("_",".")} update"
                this."initv$method"()
            } else {
                log.info "Skip code for initv$method"
            }

        }
        Version.setCurrentVersion(grailsApplication.metadata.'app.version')
    }

    void initv3_0_2() {
        log.info "3.0.2"
        log.info "ontology permission recalculation"
        def projects = Project.findAllByDeletedIsNullAndOntologyIsNotNull()
        short i = 0
        SpringSecurityUtils.doWithAuth("superadmin", {
            projects.each { project ->
                log.info "project $project"
                secUserService.listUsers(project).each { user ->
                    permissionService.addPermission(project.ontology, user.username, BasePermission.READ)
                }
                secUserService.listAdmins(project).each { admin ->
                    permissionService.addPermission(project.ontology, admin.username, BasePermission.ADMINISTRATION)
                }
                i++
                log.info "$i/${projects.size()}"
            }
        });
    }

    void initv3_0_0() {
        log.info "3.0.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE version DROP COLUMN IF EXISTS number;")

        boolean exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'image_filter' and COLUMN_NAME = 'processing_server_id';").size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("UPDATE image_filter SET processing_server_id = NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE image_filter DROP COLUMN IF EXISTS processing_server_id;")
        }
        def imagingServer = bootstrapUtilsService.createNewImagingServer()
        ImageFilter.findAll().each {
            it.imagingServer = imagingServer
            it.save(flush: true)
        }

        exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'processing_server' and COLUMN_NAME = 'url';").size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server DROP COLUMN IF EXISTS url;")
            new Sql(dataSource).executeUpdate("DELETE FROM processing_server;")
        }
        exists = new Sql(dataSource).rows("SELECT COLUMN_NAME " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'processing_server' and COLUMN_NAME = 'username';").size() > 0
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS name VARCHAR NOT NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD CONSTRAINT unique_name UNIQUE (name);")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS username VARCHAR NOT NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE processing_server ADD COLUMN IF NOT EXISTS index INTEGER NOT NULL;")
        }

        new Sql(dataSource).executeUpdate("ALTER TABLE software DROP COLUMN IF EXISTS service_name;")
        new Sql(dataSource).executeUpdate("ALTER TABLE software DROP COLUMN IF EXISTS result_sample;")
        def constraints = new Sql(dataSource).rows("SELECT CONSTRAINT_NAME " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'software' and CONSTRAINT_TYPE = 'UNIQUE';")
        exists = constraints.size() == 1
        if (exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE software DROP CONSTRAINT "+constraints[0].constraint_name+";")
        }

        new Sql(dataSource).executeUpdate("UPDATE software SET deprecated = false WHERE deprecated IS NULL;")
        new Sql(dataSource).executeUpdate("UPDATE software_parameter SET server_parameter = false WHERE server_parameter IS NULL;")

        if(SecUser.findByUsername("rabbitmq")) {
            def rabbitmqUser = SecUser.findByUsername("rabbitmq")
            def superAdmin = SecRole.findByAuthority("ROLE_SUPER_ADMIN")
            if(!SecUserSecRole.findBySecUserAndSecRole(rabbitmqUser,superAdmin)) {
                new SecUserSecRole(secUser: rabbitmqUser,secRole: superAdmin).save(flush:true)
            }
        }

        AmqpQueue.findAllByNameLike("queueSoftware%").each {it.delete(flush: true)}

        bootstrapUtilsService.addDefaultProcessingServer()
        bootstrapUtilsService.addDefaultConstraints()

    }

    void initv2_1_0() {
        log.info "2.1.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ADD COLUMN IF NOT EXISTS are_images_downloadable BOOLEAN DEFAULT FALSE;")
        noSQLCollectionService.dropIndex("lastConnection", "date_1")
    }

    void initv2_0_0() {
        log.info "2.0.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ALTER COLUMN ontology_id DROP NOT NULL;")

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET language = 'ENGLISH' WHERE language IS NULL;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ALTER COLUMN language SET DEFAULT 'ENGLISH';")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user ALTER COLUMN language SET NOT NULL;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user DROP COLUMN IF EXISTS skype_account;")
        new Sql(dataSource).executeUpdate("ALTER TABLE sec_user DROP COLUMN IF EXISTS sipAccount;")

        new Sql(dataSource).executeUpdate("DROP VIEW user_image;")
        tableService.initTable()

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        db.annotationAction.update([:], [$rename:[annotation:'annotationIdent']], false, true)
        db.annotationAction.update([:], [$set:[annotationClassName: 'be.cytomine.ontology.UserAnnotation']], false, true)
        db.annotationAction.update([:], [$unset:[annotation:'']], false, true)

        for(User systemUser :User.findAllByUsernameInList(['ImageServer1', 'superadmin', 'admin', 'rabbitmq', 'monitoring'])){
            systemUser.origin = "SYSTEM"
            systemUser.save();
        }

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET origin = 'BOOTSTRAP' WHERE origin IS NULL;")
    }

    void initv1_2_1() {
        log.info "1.2.1"
        List<Configuration> configurations = Configuration.findAllByKeyLike("%.%")

        for(int i = 0; i<configurations.size(); i++){
            configurations[i].key = configurations[i].key.replace(".","_")
            configurations[i].save()
        }

        bootstrapUtilsService.createConfigurations(true)
    }
}