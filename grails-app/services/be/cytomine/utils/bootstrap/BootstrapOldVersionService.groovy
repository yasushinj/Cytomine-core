package be.cytomine.utils.bootstrap

import be.cytomine.image.UploadedFile

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

import be.cytomine.image.server.Storage
import be.cytomine.ontology.AlgoAnnotationTerm
import be.cytomine.ontology.Property
import be.cytomine.project.Project
import be.cytomine.security.User
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

    // TODO check que tout ici est bien fait comme il faut dans le bootstrap et pas des trucs qui manque genre le superadmin d'ims !!!

    void execChangeForOldVersion() {
        def methods = this.metaClass.methods*.name.sort().unique()
        Version version = Version.getLastVersion()

        println "version"
        println version.number

        methods.each { method ->
            if (method.startsWith("init")) {
                Long methodDate = Long.parseLong(method.replace("init", ""))
                if (methodDate > version.number) {
                    log.info "Run code for version > $methodDate"
                    this."init$methodDate"()
                } else {
                    log.info "Skip code for $methodDate"
                }
            }
        }

        Version.setCurrentVersion(Long.parseLong(grailsApplication.metadata.'app.version'))
    }

    void init20180409() {
        //unused domain
        println "drop table"
        new Sql(dataSource).executeUpdate("DROP TABLE image_property;")

        // add ltree column
        println "add ltree"
        boolean exists = new Sql(dataSource).rows("SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name='uploaded_file' and column_name='l_tree';").size() == 1;
        if (!exists) {
            new Sql(dataSource).executeUpdate("CREATE EXTENSION ltree;")
            new Sql(dataSource).executeUpdate("ALTER TABLE uploaded_file ADD COLUMN l_tree ltree;")
        }

        // update ltree
        println "update ltree"
        UploadedFile.findAllByParentIsNullAndLTreeIsNull().each {
            it.save()
        }
        def ufs
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

        println "create new uploadedfile"
        // recreate uploadedFile from abstractimage
        // only for converted abstract_image
        ufs = UploadedFile.createCriteria().list {
            ne("filename", image.path)
        }

        ufs.each {
            def uf = new UploadedFile()
            uf.contentType = "image/pyrtiff"
            uf.image = it.image
            uf.originalFilename = uf.image.originalFilename
            uf.filename = uf.image.path
            uf.parent = it
            uf.path= it.path
            uf.ext = FilenameUtils.getExtension(uf.filename)
            uf.status = UploadedFile.DEPLOYED
            uf.user = it.user

            uf.save(flush: true, failOnError: true)

            it.image = null
            it.save()
        }
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