package be.cytomine.utils.bootstrap

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

import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.image.UploadedFile
import be.cytomine.meta.Property
import be.cytomine.project.Project
import be.cytomine.security.SecRole
import be.cytomine.security.User
import be.cytomine.meta.Configuration
import be.cytomine.utils.Version
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.sql.Sql
import org.apache.commons.io.FilenameUtils

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
    def storageService
    def tableService
    def executorService
    def mongo
    def noSQLCollectionService

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

    void initv2_0_1() {
        log.info "2.0.1"
        new Sql(dataSource).executeUpdate("ALTER TABLE version DROP COLUMN IF EXISTS number;")
    }

    void initv1_9_9() {
        log.info "1.9.9"
        for(User systemUser :User.findAllByUsernameInList(['ImageServer1', 'superadmin', 'admin', 'rabbitmq', 'monitoring'])){
            systemUser.origin = "SYSTEM"
            systemUser.save();
        }

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET origin = 'BOOTSTRAP' WHERE origin IS NULL;")
    }

    void initv1_2_2() {
        log.info "1.2.2"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ALTER COLUMN ontology_id DROP NOT NULL;")

        new Sql(dataSource).executeUpdate("UPDATE sec_user SET language = 'ENGLISH';")
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