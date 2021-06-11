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

/**
 * Sequence service provide new id for domain
 */
class SequenceService {

    def sessionFactory
    public final static String SEQ_NAME = "hibernate_sequence"
    static transactional = true

    /**
     * Create database sequence
     */
    def initSequences() {
        sessionFactory.getCurrentSession().clear();
        def connection = sessionFactory.currentSession.connection()

        try {
            def statement = connection.createStatement()
            def dropSequenceQuery = ""//"DROP SEQUENCE IF EXISTS "+SEQ_NAME+";"
            def createSequenceQuery = "CREATE SEQUENCE " + SEQ_NAME + " START 1;"
            statement.execute(dropSequenceQuery + createSequenceQuery)
        } catch (org.postgresql.util.PSQLException e) {
            log.debug e
        }

    }

    /**
     * Get a new id number
     */
    def generateID() {
        def statement = sessionFactory.currentSession.connection().createStatement()
        def res = statement.executeQuery("select nextval('" + SEQ_NAME + "');")
        res.next()
        Long nextVal = res.getLong("nextval")
        return nextVal
    }
}
