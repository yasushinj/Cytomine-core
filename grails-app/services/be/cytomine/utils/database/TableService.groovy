package be.cytomine.utils.database

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

import groovy.sql.Sql

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 7/07/11
 * Time: 15:16
 * Service used to create index at the application begining
 */
class TableService {

    def sessionFactory
    def dataSource
    def grailsApplication
    public final static String SEQ_NAME = "CYTOMINE_SEQ"
    static transactional = true

    /**
     * Create domain index
     */
    def initTable() {
        sessionFactory.getCurrentSession().clear();
        def connection = sessionFactory.currentSession.connection()

        try {

            if(executeSimpleRequest("select character_maximum_length from information_schema.columns where table_name = 'command' and column_name = 'data'")!=null) {
                log.debug "Change type..."
                new Sql(dataSource).executeUpdate("alter table command alter column data type character varying")
            }

            if(executeSimpleRequest("select character_maximum_length from information_schema.columns where table_name = 'shared_annotation' and column_name = 'comment'")!=null) {
                log.debug "Change type..."
                new Sql(dataSource).executeUpdate("alter table shared_annotation alter column comment type character varying")
            }

            if(executeSimpleRequest("select character_maximum_length from information_schema.columns where table_name = 'property' and column_name = 'value'")!=null) {
                log.debug "Change type property table..."
                new Sql(dataSource).executeUpdate("alter table property alter column value type character varying")
            }

            String reqcreate

            reqcreate = "CREATE VIEW user_project AS\n" +
                                "SELECT distinct project.*, sec_user.id as user_id\n" +
                                "FROM project, acl_object_identity, sec_user, acl_sid, acl_entry \n" +
                                "WHERE project.id = acl_object_identity.object_id_identity\n" +
                                "AND acl_sid.sid = sec_user.username\n" +
                                "AND acl_entry.sid = acl_sid.id\n" +
                                "AND acl_entry.acl_object_identity = acl_object_identity.id\n" +
                                "AND sec_user.user_id is null\n" +
                                "AND mask >= 1 AND project.deleted IS NULL"
            createRequest('user_project',reqcreate)

            reqcreate = "CREATE VIEW admin_project AS\n" +
                                "SELECT distinct project.*, sec_user.id as user_id\n" +
                                "FROM project, acl_object_identity, sec_user, acl_sid, acl_entry \n" +
                                "WHERE project.id = acl_object_identity.object_id_identity\n" +
                                "AND acl_sid.sid = sec_user.username\n" +
                                "AND acl_entry.sid = acl_sid.id\n" +
                                "AND acl_entry.acl_object_identity = acl_object_identity.id\n" +
                                "AND sec_user.user_id is null\n" +
                                "AND mask >= 16 AND project.deleted IS NULL"
            createRequest('admin_project',reqcreate)

            reqcreate = "CREATE VIEW creator_project AS\n" +
                                "SELECT distinct project.*, sec_user.id as user_id\n" +
                                "FROM project, acl_object_identity, sec_user, acl_sid\n" +
                                "WHERE project.id = acl_object_identity.object_id_identity\n" +
                                "AND acl_sid.sid = sec_user.username\n" +
                                "AND acl_object_identity.owner_sid = acl_sid.id\n" +
                                "AND sec_user.user_id is null AND project.deleted IS NULL"
            createRequest('creator_project',reqcreate)

            reqcreate = "CREATE VIEW user_image AS " +
                    "SELECT image_instance.*, " +
                        "project.name as project_name, " +
                        "project.blind_mode as project_blind, " +
                        "sec_user.id as user_image_id, " +
                        "case when MAX(mask) = 16 then true else false end as user_project_manager " +
                    "FROM image_instance " +
                    "INNER JOIN sec_user ON sec_user.user_id IS NULL " +
                    "INNER JOIN project ON project.id = image_instance.project_id AND project.deleted IS NULL " +
                    "INNER JOIN acl_object_identity ON project.id = acl_object_identity.object_id_identity " +
                    "INNER JOIN acl_sid ON acl_sid.sid = sec_user.username " +
                    "INNER JOIN acl_entry ON acl_entry.sid = acl_sid.id " +
                        "AND acl_entry.acl_object_identity = acl_object_identity.id AND acl_entry.mask >= 1 " +
                    "WHERE image_instance.deleted IS NULL " +
                        "AND image_instance.parent_id IS NULL " + // don't get nested images
                    "GROUP BY image_instance.id, project.id, sec_user.id"
            createRequest('user_image',reqcreate)

        } catch (org.postgresql.util.PSQLException e) {
            log.info e
        }
    }

    def executeSimpleRequest(String request) {
        def response = null
        log.debug "request = $request"
        new Sql(dataSource).eachRow(request) {
            log.debug it[0]
            response = it[0]
        }
        log.debug "response = $response"
        response
    }

    def createRequest(def name,def reqcreate) {
        try {

            boolean alreadyExist = false

            new Sql(dataSource).eachRow("select table_name from INFORMATION_SCHEMA.views where table_name like ?",[name]) {
                alreadyExist = true
            }

            if(alreadyExist) {
                def req =  "DROP VIEW " + name
                new Sql(dataSource).execute(req)

            }
            log.debug reqcreate
            new Sql(dataSource).execute(reqcreate)


        } catch(Exception e) {
            log.error e
        }
    }
}
