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

        if(!version.major){
            methods.findAll { it =~ "init[0-9]" }.each { method ->
                Long methodDate = Long.parseLong(method.replace("init", ""))
                if (methodDate > version.number) {
                    log.info "Run code for version > $methodDate"
                    this."init$methodDate"()
                } else {
                    log.info "Skip code for $methodDate"
                }
            }

            version.major = 0
            version.minor = 0
            version.patch = 0
        }
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
        Version.setCurrentVersion(Long.parseLong(grailsApplication.metadata.'app.versionDate'), grailsApplication.metadata.'app.version')
    }

    void initv2_1_0() {
        log.info "2.1.0"
        new Sql(dataSource).executeUpdate("ALTER TABLE project ADD COLUMN IF NOT EXISTS are_images_downloadable BOOLEAN DEFAULT FALSE;")
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

    void init20180904() {
        log.info "20180904"

        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='version' and column_name='major';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN major integer;")
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN minor integer;")
            new Sql(dataSource).executeUpdate("ALTER TABLE version ADD COLUMN patch integer;")
        }
    }

    void init20180701() {
        log.info "20180701"

        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='configuration' and column_name='reading_role';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE configuration ADD COLUMN reading_role varchar(255) NOT NULL DEFAULT 'ADMIN';")

            String request = "SELECT id FROM configuration;"
            def sql = new Sql(dataSource)
            def data = []
            sql.eachRow(request) {
                data << it[0]
            }
            sql.close()
            if(data.size() > 0) new Sql(dataSource).executeUpdate("UPDATE configuration SET reading_role = 'ADMIN' WHERE id IN (" + data.join(",") + ");")

            request = "SELECT id FROM configuration WHERE reading_role_id = "+SecRole.findByAuthority("ROLE_USER").id+";"
            sql = new Sql(dataSource)
            data = []
            sql.eachRow(request) {
                data << it[0]
            }
            sql.close()
            if(data.size() > 0) new Sql(dataSource).executeUpdate("UPDATE configuration SET reading_role = 'USER' WHERE id IN (" + data.join(",") + ");")


            new Sql(dataSource).executeUpdate("ALTER TABLE configuration DROP COLUMN reading_role_id;")
        }
    }

    void init20180409() {
        log.info "20180409"
        //unused domain
        log.info "drop table"
        new Sql(dataSource).executeUpdate("DROP TABLE image_property;")

        // add ltree column
        log.info "add ltree"
        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='uploaded_file' and column_name='l_tree';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("CREATE EXTENSION ltree;")
            new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file ADD COLUMN l_tree ltree;")
        }

        // update ltree
        log.info "update ltree : step 1"
        log.info "record to update : "+UploadedFile.countByParentIsNullAndLTreeIsNull()
        UploadedFile.findAllByParentIsNullAndLTreeIsNull().each {
            it.save()
        }
        def ufs
        log.info "update ltree : step 2"
        log.info "record to update : "+UploadedFile.countByParentIsNotNullAndLTreeIsNull()
        UploadedFile.findAllByParentIsNotNullAndLTreeIsNull().each {
            if(it.lTree == null) {
                ufs = [it]
                def current = it
                while(current.parent != null && current.parent.lTree == null){
                    ufs << current
                    current = current.parent
                }
                ufs = ufs.reverse()
                for(int i = 0;i<ufs.size();i++) {
                    ufs[i].save()
                }
            }
        }

        executorService.execute({

            try {
                log.info "create new uploadedfile"
                // recreate uploadedFile from abstractimage
                // only for converted abstract_image
                ufs = UploadedFile.createCriteria().list {
                    join("image")
                    createAlias("image", "i")
                    neProperty("filename", "i.path")

                    isNotNull("image")
                }
                log.info "record to update : "+ufs.size()

                int i = 0;

                ufs.each {
                    def uf = new UploadedFile()

                    uf.contentType = "image/pyrtiff"
                    uf.image = it.image

                    String filename = it.image.originalFilename
                    int index = filename.lastIndexOf('.')
                    filename = filename.substring(0, index) + "_pyr" + filename.substring(index)

                    uf.originalFilename = filename
                    uf.filename = it.image.path
                    uf.parent = it
                    uf.path = it.path
                    uf.ext = FilenameUtils.getExtension(it.image.path)
                    uf.status = UploadedFile.DEPLOYED
                    uf.user = it.user
                    uf.storages = StorageAbstractImage.findAllByAbstractImage(it.image).collect { it.storage.id }
                    uf.size = 0L

                    uf.save(failOnError: true)

                    it.image = null
                    it.status = UploadedFile.CONVERTED
                    it.save()

                    if (i % 100 == 0) log.info("done : " + i + "/" + ufs.size())
                    i++
                }
            } catch (Exception e) {
                log.info "Error during migration. Exit application"
                e.printStackTrace()
                System.exit(1)
            }
        } as Runnable)
    }

    void init20170714() {
        bootstrapUtilsService.fillProjectConnections();
        bootstrapUtilsService.fillImageConsultations();
        log.info "generate missing storage !"
        for (user in User.findAll()) {
            if (!Storage.findByUser(user)) {
                log.info "generate missing storage for $user"
                SpringSecurityUtils.doWithAuth("admin", {
                    storageService.initUserStorage(user)
                });
            }
        }
    }

    void init20170201() {
        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='shared_annotation' and column_name='annotation_class_name';").size() == 1;
        if (!exists) {
            // add columns
            new Sql(dataSource).executeUpdate("ALTER TABLE shared_annotation ADD COLUMN annotation_class_name varchar(255);")
            new Sql(dataSource).executeUpdate("ALTER TABLE shared_annotation ADD COLUMN annotation_ident bigint;")

            //update all rows
            new Sql(dataSource).executeUpdate("UPDATE shared_annotation SET annotation_ident = user_annotation_id;")
            new Sql(dataSource).executeUpdate("UPDATE shared_annotation SET annotation_class_name = 'be.cytomine.ontology.UserAnnotation';")

            //add constraints
            new Sql(dataSource).executeUpdate("ALTER TABLE shared_annotation ALTER COLUMN annotation_ident SET NOT NULL;")
            new Sql(dataSource).executeUpdate("ALTER TABLE shared_annotation ALTER COLUMN annotation_class_name SET NOT NULL;")

            //delete
            new Sql(dataSource).executeUpdate("ALTER TABLE shared_annotation DROP COLUMN IF EXISTS user_annotation_id;")
        }
    }

    void init20160901() {

        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='project' and column_name='mode';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("ALTER TABLE project ADD COLUMN mode varchar(255) NOT NULL DEFAULT 'CLASSIC';")

            String request = "SELECT id FROM project WHERE is_read_only;"
            def sql = new Sql(dataSource)
            def data = []
            sql.eachRow(request) {
                data << it[0]
            }
            sql.close()
            new Sql(dataSource).executeUpdate("UPDATE project SET mode = 'READ_ONLY' WHERE id IN (" + data.join(",") + ");")


            exists = new Sql(dataSource).rows("SELECT column_name " +
                    "FROM information_schema.columns " +
                    "WHERE table_name='project' and column_name='is_read_only';").size() == 1;
            if (exists) {
                log.info "reinit table..."
                new Sql(dataSource).executeUpdate("DROP VIEW user_project;")
                new Sql(dataSource).executeUpdate("DROP VIEW admin_project;")
                new Sql(dataSource).executeUpdate("DROP VIEW creator_project;")
                new Sql(dataSource).executeUpdate("ALTER TABLE project DROP COLUMN is_read_only;")
                tableService.initTable()
            }
        }

        List<Property> properties = Property.findAllByDomainClassNameAndKey(Project.name, "@CUSTOM_UI_PROJECT")
        def configProject;
        properties.each { prop ->
            configProject = JSON.parse(prop.value)
            configProject.each {
                it.value["CONTRIBUTOR_PROJECT"] = it.value["GUEST_PROJECT"]
                it.value.remove("GUEST_PROJECT")
                it.value.remove("USER_PROJECT")
            }
            prop.value = configProject.toString()

            prop.save(true)
        }
    }

    void init20160503() {
        bootstrapUtilsService.convertMimeTypes();
    }

    void init20160324() {
        new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file DROP COLUMN IF EXISTS mime_type;")
        new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file DROP COLUMN IF EXISTS converted_filename;")
        new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file DROP COLUMN IF EXISTS converted_ext;")
        new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file DROP COLUMN IF EXISTS download_parent_id;")
    }

    void init20160224() {
        new Sql(dataSource).executeUpdate("DELETE FROM attached_file WHERE domain_class_name = 'be.cytomine.image.AbstractImage' AND (filename LIKE '%thumb%' OR filename LIKE '%nested%');")
    }
}