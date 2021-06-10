package be.cytomine.utils.database

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

import groovy.sql.Sql

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 7/07/11
 * Time: 15:16
 *
 * Cytomine Service that create all trigger in database
 * Most of trigger are used avoid request that count something
 * For example:
 * project has properties countUserAnnotation, countImages,...
 * A trigger automaticaly incr (or decr) these values.
 */
class TriggerService {

    def sessionFactory
    def grailsApplication
    public final static String SEQ_NAME = "CYTOMINE_SEQ"
    static transactional = true
    def dataSource

    def initTrigger() {
        sessionFactory.getCurrentSession().clear();
        def connection = sessionFactory.currentSession.connection()

        try {
            new Sql(dataSource).eachRow("SELECT 'DROP TRIGGER ' || trigger_name || ' ON ' || event_object_table || ';' FROM information_schema.triggers where trigger_name <> 'layer_integrity_checks'") {
                new Sql(dataSource).execute(it[0])
            }

            def statement = connection.createStatement()

            statement.execute(getUserAnnotationTriggerBeforeInsert())
            statement.execute(getUserAnnotationTriggerAfterInsert())
            statement.execute(getUserAnnotationTriggerAfterUpdate())
            statement.execute(getUserAnnotationTriggerBeforeDelete())
            statement.execute(getUserAnnotationTriggerAfterDelete())


            statement.execute(getAlgoAnnotationTriggerBeforeInsert())
            statement.execute(getAlgoAnnotationTriggerAfterInsert())
            statement.execute(getAlgoAnnotationTriggerAfterUpdate())
            statement.execute(getAlgoAnnotationTriggerBeforeDelete())
            statement.execute(getAlgoAnnotationTriggerAfterDelete())

            statement.execute(getImageTriggerBeforeInsert())
            statement.execute(getImageTriggerAfterInsert())
            statement.execute(getImageTriggerBeforeDelete())
            statement.execute(getImageTriggerAfterDelete())
            statement.execute(getImageTriggerBeforeUpdate())
            statement.execute(getImageTriggerAfterUpdate())

            statement.execute(getAnnotationCommentBeforeInsert())
            statement.execute(getAnnotationCommentAfterInsert())

            statement.execute(getAnnotationReviewBeforeInsert())
            statement.execute(getAnnotationReviewAfterInsert())
            statement.execute(getAnnotationReviewAfterUpdate())
            statement.execute(getAnnotationReviewBeforeDelete())
            statement.execute(getAnnotationReviewAfterDelete())

        } catch (org.postgresql.util.PSQLException e) {
            log.debug e
        }

    }

    String getUserAnnotationTriggerBeforeInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeInsertUserAnnotation() RETURNS TRIGGER AS \$incUserAnnBefore\$
        DECLARE
            currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentIndex  annotation_index%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = NEW.image_id FOR UPDATE;
            SELECT * INTO currentProject FROM project where id = NEW.project_id FOR UPDATE;
            SELECT * INTO currentIndex FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
            RETURN NEW;
        END ;
        \$incUserAnnBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeInsertUserAnnotationTrigger on user_annotation;"

        String createTrigger = "CREATE TRIGGER beforeInsertUserAnnotationTrigger BEFORE INSERT ON user_annotation FOR EACH ROW EXECUTE PROCEDURE beforeInsertUserAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getUserAnnotationTriggerAfterInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterInsertUserAnnotation() RETURNS TRIGGER AS \$incUserAnnAfter\$
        DECLARE
            alreadyExist INTEGER;
        BEGIN
                UPDATE image_instance
                SET count_image_annotations = count_image_annotations + 1
                WHERE image_instance.id = NEW.image_id;

                --UPDATE project
                --SET count_annotations = count_annotations + 1
                --WHERE project.id = NEW.project_id;

                SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
                IF (alreadyExist=0) THEN
                    INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
                END IF;
                UPDATE annotation_index SET count_annotation = count_annotation+1, version = version+1 WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
            RETURN NEW;
        END ;
        \$incUserAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterInsertUserAnnotationTrigger on user_annotation;"

        String createTrigger = "CREATE TRIGGER afterInsertUserAnnotationTrigger AFTER INSERT ON user_annotation FOR EACH ROW EXECUTE PROCEDURE afterInsertUserAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getUserAnnotationTriggerAfterUpdate() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterUpdateUserAnnotation() RETURNS TRIGGER AS \$incUserAnnAfter\$
        DECLARE
            alreadyExist INTEGER;
            current_ai_id annotation_index.id%TYPE;
            current_project_id image_instance.id%TYPE;
            current_image_id project.id%TYPE;
        BEGIN
                IF(NEW.user_id<>OLD.user_id) THEN
                    SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
                    IF (alreadyExist=0) THEN
                        INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
                    END IF;
                    UPDATE annotation_index SET count_annotation = count_annotation+1, version = version+1 WHERE user_id = NEW.user_id AND image_id = NEW.image_id;

                    UPDATE annotation_index SET count_annotation = count_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;

                END IF;
                IF NEW.deleted IS NULL AND OLD.deleted IS NOT NULL THEN
                    --UPDATE project
                    --SET count_annotations = count_annotations + 1
                    --WHERE project.id = OLD.project_id;

                    UPDATE image_instance
                    SET count_image_annotations = count_image_annotations + 1
                    WHERE image_instance.id = OLD.image_id;

                    UPDATE annotation_index SET count_annotation = count_annotation+1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
                ELSEIF NEW.deleted IS NOT NULL AND OLD.deleted IS NULL THEN
                    --UPDATE project
                    --SET count_annotations = count_annotations - 1
                    --WHERE project.id = OLD.project_id;

                    UPDATE image_instance
                    SET count_image_annotations = count_image_annotations - 1
                    WHERE image_instance.id = OLD.image_id;

                    UPDATE annotation_index SET count_annotation = count_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
                END IF;
            RETURN NEW;
        END ;
        \$incUserAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterUpdateUserAnnotationTrigger on user_annotation;"

        String createTrigger = "CREATE TRIGGER afterUpdateUserAnnotationTrigger AFTER UPDATE ON user_annotation FOR EACH ROW EXECUTE PROCEDURE afterUpdateUserAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }



    String getUserAnnotationTriggerBeforeDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeDeleteUserAnnotation() RETURNS TRIGGER AS \$incUserAnnBefore\$
        DECLARE
            currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentIndex  annotation_index%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = OLD.image_id FOR UPDATE;
            --SELECT * INTO currentProject FROM project where id = OLD.project_id FOR UPDATE;
            SELECT * INTO currentIndex FROM annotation_index WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
            RETURN OLD;
        END ;
        \$incUserAnnBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeDeleteUserAnnotationTrigger on user_annotation;"

        String createTrigger = "CREATE TRIGGER beforeDeleteUserAnnotationTrigger BEFORE DELETE ON user_annotation FOR EACH ROW EXECUTE PROCEDURE beforeDeleteUserAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getUserAnnotationTriggerAfterDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterDeleteUserAnnotation() RETURNS TRIGGER AS \$decUserAnnAfter\$
        DECLARE
            alreadyExist INTEGER;
            current_ai_id annotation_index.id%TYPE;
            current_project_id image_instance.id%TYPE;
            current_image_id project.id%TYPE;
        BEGIN
                --UPDATE project
                --SET count_annotations = count_annotations - 1
                --WHERE project.id = OLD.project_id;

                UPDATE image_instance
                SET count_image_annotations = count_image_annotations - 1
                WHERE image_instance.id = OLD.image_id;

                UPDATE annotation_index SET count_annotation = count_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
            RETURN OLD;
        END ;
         \$decUserAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterDeleteUserAnnotationTrigger on user_annotation;"

        String createTrigger = "CREATE TRIGGER afterDeleteUserAnnotationTrigger AFTER DELETE ON user_annotation FOR EACH ROW EXECUTE PROCEDURE afterDeleteUserAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }









    String getAlgoAnnotationTriggerBeforeInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeInsertAlgoAnnotation() RETURNS TRIGGER AS \$incAlgoAnnBefore\$
        DECLARE
            currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentIndex  annotation_index%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = NEW.image_id FOR UPDATE;
            SELECT * INTO currentProject FROM project where id = NEW.project_id FOR UPDATE;
            SELECT * INTO currentIndex FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
            RETURN NEW;
        END ;
        \$incAlgoAnnBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeInsertAlgoAnnotationTrigger on algo_annotation;"

        String createTrigger = "CREATE TRIGGER beforeInsertAlgoAnnotationTrigger BEFORE INSERT ON algo_annotation FOR EACH ROW EXECUTE PROCEDURE beforeInsertAlgoAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getAlgoAnnotationTriggerAfterInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterInsertAlgoAnnotation() RETURNS TRIGGER AS \$incAlgoAnnAfter\$
        DECLARE
            alreadyExist INTEGER;
        BEGIN
                UPDATE image_instance
                SET count_image_job_annotations = count_image_job_annotations + 1
                WHERE image_instance.id = NEW.image_id;

                --UPDATE project
                --SET count_job_annotations = count_job_annotations + 1
                --WHERE project.id = NEW.project_id;

                SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
                IF (alreadyExist=0) THEN
                    INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
                END IF;
                UPDATE annotation_index SET count_annotation = count_annotation+1, version = version+1 WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
            RETURN NEW;
        END ;
        \$incAlgoAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterInsertAlgoAnnotationTrigger on algo_annotation;"

        String createTrigger = "CREATE TRIGGER afterInsertAlgoAnnotationTrigger AFTER INSERT ON algo_annotation FOR EACH ROW EXECUTE PROCEDURE afterInsertAlgoAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getAlgoAnnotationTriggerAfterUpdate() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterUpdateAlgoAnnotation() RETURNS TRIGGER AS \$incUserAnnAfter\$
        DECLARE
            current_ai_id annotation_index.id%TYPE;
            current_project_id image_instance.id%TYPE;
            current_image_id project.id%TYPE;
        BEGIN
                IF NEW.deleted IS NULL AND OLD.deleted IS NOT NULL THEN
                    --UPDATE project
                    --SET count_job_annotations = count_job_annotations + 1
                    --WHERE project.id = OLD.project_id;

                    UPDATE image_instance
                    SET count_image_job_annotations = count_image_job_annotations + 1
                    WHERE image_instance.id = OLD.image_id;

                    UPDATE annotation_index SET count_annotation = count_annotation+1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
                ELSEIF NEW.deleted IS NOT NULL AND OLD.deleted IS NULL THEN
                    --UPDATE project
                    --SET count_job_annotations = count_job_annotations - 1
                    --WHERE project.id = OLD.project_id;

                    UPDATE image_instance
                    SET count_image_job_annotations = count_image_job_annotations - 1
                    WHERE image_instance.id = OLD.image_id;

                    UPDATE annotation_index SET count_annotation = count_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
                END IF;
            RETURN NEW;
        END ;
        \$incUserAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterUpdateAlgoAnnotationTrigger on algo_annotation;"

        String createTrigger = "CREATE TRIGGER afterUpdateAlgoAnnotationTrigger AFTER UPDATE ON algo_annotation FOR EACH ROW EXECUTE PROCEDURE afterUpdateAlgoAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getAlgoAnnotationTriggerBeforeDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeDeleteAlgoAnnotation() RETURNS TRIGGER AS \$incAlgoAnnBefore\$
        DECLARE
            currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentIndex  annotation_index%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = OLD.image_id FOR UPDATE;
            --SELECT * INTO currentProject FROM project where id = OLD.project_id FOR UPDATE;
            SELECT * INTO currentIndex FROM annotation_index WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
            RETURN OLD;
        END ;
        \$incAlgoAnnBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeDeleteAlgoAnnotation on algo_annotation;"

        String createTrigger = "CREATE TRIGGER beforeDeleteAlgoAnnotation BEFORE DELETE ON algo_annotation FOR EACH ROW EXECUTE PROCEDURE beforeDeleteAlgoAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getAlgoAnnotationTriggerAfterDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterDeleteAlgoAnnotation() RETURNS TRIGGER AS \$decAlgoAnnAfter\$
        DECLARE
            alreadyExist INTEGER;
            current_ai_id annotation_index.id%TYPE;
            current_project_id image_instance.id%TYPE;
            current_image_id project.id%TYPE;
        BEGIN
                --UPDATE project
                --SET count_job_annotations = count_job_annotations - 1
                --WHERE project.id = OLD.project_id;

                UPDATE image_instance
                SET count_image_job_annotations = count_image_job_annotations - 1
                WHERE image_instance.id = OLD.image_id;

                UPDATE annotation_index SET count_annotation = count_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
            RETURN OLD;
        END ;
         \$decAlgoAnnAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterDeleteAlgoAnnotationTrigger on algo_annotation;"

        String createTrigger = "CREATE TRIGGER afterDeleteAlgoAnnotationTrigger AFTER DELETE ON algo_annotation FOR EACH ROW EXECUTE PROCEDURE afterDeleteAlgoAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }




    String getImageTriggerBeforeInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeInsertImage() RETURNS TRIGGER AS \$incImageBefore\$
        DECLARE
            currentProject  project%ROWTYPE;
        BEGIN
            SELECT * INTO currentProject FROM project where id = NEW.project_id FOR UPDATE;
            RETURN NEW;
        END ;
        \$incImageBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeInsertImageTrigger on image_instance;"

        String createTrigger = "CREATE TRIGGER beforeInsertImageTrigger BEFORE INSERT ON image_instance FOR EACH ROW EXECUTE PROCEDURE beforeInsertImage(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }



    String getImageTriggerAfterInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION incrementProjectImage() RETURNS trigger as '
        BEGIN
            UPDATE project
            SET count_images = count_images + 1
            WHERE id = NEW.project_id;
            RETURN NEW;
        END ;'
        LANGUAGE plpgsql;"""

        String dropTrigger = "DROP TRIGGER IF EXISTS incrementProjectImageTrigger on image_instance;"

        String createTrigger = "CREATE TRIGGER incrementProjectImageTrigger AFTER INSERT ON image_instance FOR EACH ROW EXECUTE PROCEDURE incrementProjectImage();"


        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }



    String getImageTriggerBeforeDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeDeleteImage() RETURNS TRIGGER AS \$decImageBefore\$
        DECLARE
            currentProject  project%ROWTYPE;
        BEGIN
            SELECT * INTO currentProject FROM project where id = OLD.project_id FOR UPDATE;
            RETURN OLD;
        END ;
        \$decImageBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeDeleteImage on image_instance;"

        String createTrigger = "CREATE TRIGGER beforeDeleteImage BEFORE DELETE ON image_instance FOR EACH ROW EXECUTE PROCEDURE beforeDeleteImage(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getImageTriggerAfterDelete() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterDeleteImage() RETURNS TRIGGER AS \$decImageAfter\$
        BEGIN
                UPDATE project
                SET count_images = count_images - 1,
                count_annotations = count_annotations - (SELECT COUNT(*) FROM user_annotation WHERE image_id = OLD.id)
                WHERE id = OLD.project_id;

            RETURN OLD;
        END ;
         \$decImageAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterDeleteImageTrigger on image_instance;"

        String createTrigger = "CREATE TRIGGER afterDeleteImageTrigger AFTER DELETE ON image_instance FOR EACH ROW EXECUTE PROCEDURE afterDeleteImage(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }




    String getImageTriggerBeforeUpdate() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeUpdateImage() RETURNS TRIGGER AS \$decImageBefore\$
        DECLARE
            currentProject  project%ROWTYPE;
        BEGIN
            --SELECT * INTO currentProject FROM project where id = OLD.project_id FOR UPDATE;
            RETURN NEW;
        END ;
        \$decImageBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeUpdateImage on image_instance;"

        String createTrigger = "CREATE TRIGGER beforeUpdateImage BEFORE UPDATE ON image_instance FOR EACH ROW EXECUTE PROCEDURE beforeUpdateImage(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getImageTriggerAfterUpdate() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterUpdateImage() RETURNS TRIGGER AS \$decImageAfter\$
        DECLARE
            current_project_id image_instance.id%TYPE;
        BEGIN
            IF NEW.deleted IS NULL AND OLD.deleted IS NOT NULL THEN
                UPDATE project SET count_images = count_images + 1,
                count_annotations = count_annotations + (SELECT COUNT(*) FROM user_annotation WHERE image_id = OLD.id),
                count_job_annotations = count_job_annotations + (SELECT COUNT(*) FROM algo_annotation WHERE image_id = OLD.id),
                count_reviewed_annotations = count_reviewed_annotations + (SELECT COUNT(*) FROM reviewed_annotation WHERE image_id = OLD.id)
                WHERE project.id = OLD.project_id;
            ELSEIF NEW.deleted IS NOT NULL AND OLD.deleted IS NULL THEN
                UPDATE project
                SET count_images = count_images - 1,
                count_annotations = count_annotations - (SELECT COUNT(*) FROM user_annotation WHERE image_id = OLD.id),
                count_job_annotations = count_job_annotations - (SELECT COUNT(*) FROM algo_annotation WHERE image_id = OLD.id),
                count_reviewed_annotations = count_reviewed_annotations - (SELECT COUNT(*) FROM reviewed_annotation WHERE image_id = OLD.id)
                WHERE project.id = OLD.project_id;
            END IF;

            RETURN NEW;
        END ;
         \$decImageAfter\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterUpdateImageTrigger on image_instance;"

        String createTrigger = "CREATE TRIGGER afterUpdateImageTrigger AFTER UPDATE ON image_instance FOR EACH ROW EXECUTE PROCEDURE afterUpdateImage(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }




















    String getAnnotationCommentBeforeInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeInsertComment() RETURNS TRIGGER AS \$incImageBefore\$
        DECLARE
            currentUserAnnotation user_annotation%ROWTYPE;
            currentAlgoAnnotation algo_annotation%ROWTYPE;
        BEGIN
            IF NEW.annotation_class_name = 'be.cytomine.ontology.AlgoAnnotation' THEN
                SELECT * INTO currentAlgoAnnotation FROM algo_annotation where id = NEW.annotation_ident FOR UPDATE;
            ELSIF NEW.annotation_class_name = 'be.cytomine.ontology.UserAnnotation' THEN
                SELECT * INTO currentUserAnnotation FROM user_annotation where id = NEW.annotation_ident FOR UPDATE;
            END IF;
            RETURN NEW;
        END ;
        \$incImageBefore\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeInsertCommentTrigger on shared_annotation;"

        String createTrigger = "CREATE TRIGGER beforeInsertCommentTrigger BEFORE INSERT ON shared_annotation FOR EACH ROW EXECUTE PROCEDURE beforeInsertComment(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }

    String getAnnotationCommentAfterInsert() {
        String createFunction = """
        CREATE OR REPLACE FUNCTION afterInsertComment() RETURNS trigger AS \$incSharedAnnAfter\$
        BEGIN
            IF NEW.annotation_class_name = 'be.cytomine.ontology.AlgoAnnotation' THEN
                UPDATE algo_annotation
                SET count_comments = count_comments + 1
                WHERE id = NEW.annotation_ident;
            ELSIF NEW.annotation_class_name = 'be.cytomine.ontology.UserAnnotation' THEN
                UPDATE user_annotation
                SET count_comments = count_comments + 1
                WHERE id = NEW.annotation_ident;
            ELSE
                RAISE EXCEPTION 'TriggerService : Class not supported %', NEW.annotation_class_name;
            END IF;
            RETURN NEW;
        END ;
        \$incSharedAnnAfter\$ LANGUAGE plpgsql;"""

        String dropTrigger = "DROP TRIGGER IF EXISTS afterInsertCommentTrigger on shared_annotation;"

        String createTrigger = "CREATE TRIGGER afterInsertCommentTrigger AFTER INSERT ON shared_annotation FOR EACH ROW EXECUTE PROCEDURE afterInsertComment();"


        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }


    String getAnnotationReviewBeforeInsert() {

        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeInsertReviewedAnnotation() RETURNS trigger as \$incAnnRevAnn\$
        DECLARE
           currentImage  image_instance%ROWTYPE;
           currentProject  project%ROWTYPE;
           currentAnnotationIndex  annotation_index%ROWTYPE;
           current_class reviewed_annotation.parent_class_name%TYPE;
           algo_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.AlgoAnnotation';
           user_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.UserAnnotation';
           currentUserAnnotation user_annotation%ROWTYPE;
           currentAlgoAnnotation algo_annotation%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = NEW.image_id FOR UPDATE;
            SELECT * INTO currentProject FROM project where id = NEW.project_id FOR UPDATE;
            SELECT * INTO currentAnnotationIndex FROM annotation_index WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;

            SELECT parent_class_name INTO current_class from reviewed_annotation where id = NEW.id;
            IF current_class = user_class THEN
                SELECT * INTO currentUserAnnotation FROM user_annotation WHERE id = NEW.parent_ident FOR UPDATE;
            ELSEIF current_class = algo_class THEN
                SELECT * INTO currentAlgoAnnotation FROM algo_annotation WHERE id = NEW.parent_ident FOR UPDATE;
            END IF;
            RETURN NEW;
        END ;
        \$incAnnRevAnn\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeInsertReviewedAnnotationTrigger on reviewed_annotation;"

        String createTrigger = "CREATE TRIGGER beforeInsertReviewedAnnotationTrigger BEFORE INSERT ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE beforeInsertReviewedAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }

    String getAnnotationReviewAfterInsert() {

        String createFunction = """
        CREATE OR REPLACE FUNCTION incrementAnnotationReviewedAnnotation() RETURNS trigger as \$incAnnRevAnn\$
        DECLARE
           current_class reviewed_annotation.parent_class_name%TYPE;
           algo_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.AlgoAnnotation';
           user_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.UserAnnotation';
            alreadyExist INTEGER;
            current_id annotation_index.id%TYPE;
        BEGIN
            UPDATE image_instance
            SET count_image_reviewed_annotations = count_image_reviewed_annotations + 1
            WHERE image_instance.id = NEW.image_id;

            --UPDATE project
            --SET count_reviewed_annotations = count_reviewed_annotations + 1
            --WHERE project.id = NEW.project_id;


            SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;
            IF (alreadyExist=0) THEN
                INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.review_user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
            END IF;
            UPDATE annotation_index SET count_reviewed_annotation = count_reviewed_annotation+1, version = version+1 WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;


            SELECT parent_class_name INTO current_class from reviewed_annotation where id = NEW.id;
            IF current_class = user_class THEN
                UPDATE user_annotation
                SET count_reviewed_annotations = count_reviewed_annotations + 1
                WHERE user_annotation.id = NEW.parent_ident;
            ELSEIF current_class = algo_class THEN
                UPDATE algo_annotation
                SET count_reviewed_annotations = count_reviewed_annotations + 1
                WHERE algo_annotation.id = NEW.parent_ident;
            END IF;
            RETURN NEW;
        END ;
        \$incAnnRevAnn\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS incrementAnnotationReviewedAnnotationTrigger on reviewed_annotation;"

        String createTrigger = "CREATE TRIGGER incrementAnnotationReviewedAnnotationTrigger AFTER INSERT ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE incrementAnnotationReviewedAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }




    String getAnnotationReviewAfterUpdate() {

        String createFunction = """
        CREATE OR REPLACE FUNCTION updateAnnotationReviewedAnnotation() RETURNS trigger as \$incAnnRevAnn\$
        DECLARE
           current_class reviewed_annotation.parent_class_name%TYPE;
           algo_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.AlgoAnnotation';
           user_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.UserAnnotation';
            alreadyExist INTEGER;
            current_id annotation_index.id%TYPE;
        BEGIN

            IF NEW.deleted IS NULL AND OLD.deleted IS NOT NULL THEN
                --UPDATE project SET count_images = count_images + 1 WHERE project.id = OLD.project_id;

                UPDATE image_instance
                SET count_image_reviewed_annotations = count_image_reviewed_annotations + 1
                WHERE image_instance.id = NEW.image_id;

                --UPDATE project
                --SET count_reviewed_annotations = count_reviewed_annotations + 1
                --WHERE project.id = NEW.project_id;


                SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;
                IF (alreadyExist=0) THEN
                    INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.review_user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
                END IF;
                UPDATE annotation_index SET count_reviewed_annotation = count_reviewed_annotation+1, version = version+1 WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;


                SELECT parent_class_name INTO current_class from reviewed_annotation where id = NEW.id;
                IF current_class = user_class THEN
                    UPDATE user_annotation
                    SET count_reviewed_annotations = count_reviewed_annotations + 1
                    WHERE user_annotation.id = NEW.parent_ident;
                ELSEIF current_class = algo_class THEN
                    UPDATE algo_annotation
                    SET count_reviewed_annotations = count_reviewed_annotations + 1
                    WHERE algo_annotation.id = NEW.parent_ident;
                END IF;



            ELSEIF NEW.deleted IS NOT NULL AND OLD.deleted IS NULL THEN
                --UPDATE project SET count_images = count_images - 1 WHERE project.id = OLD.project_id;

                UPDATE image_instance
                SET count_image_reviewed_annotations = count_image_reviewed_annotations - 1
                WHERE image_instance.id = NEW.image_id;

                --UPDATE project
                --SET count_reviewed_annotations = count_reviewed_annotations - 1
                --WHERE project.id = NEW.project_id;


                SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;
                IF (alreadyExist=0) THEN
                    INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.review_user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
                END IF;
                UPDATE annotation_index SET count_reviewed_annotation = count_reviewed_annotation-1, version = version+1 WHERE user_id = NEW.review_user_id AND image_id = NEW.image_id;


                SELECT parent_class_name INTO current_class from reviewed_annotation where id = NEW.id;
                IF current_class = user_class THEN
                    UPDATE user_annotation
                    SET count_reviewed_annotations = count_reviewed_annotations - 1
                    WHERE user_annotation.id = NEW.parent_ident;
                ELSEIF current_class = algo_class THEN
                    UPDATE algo_annotation
                    SET count_reviewed_annotations = count_reviewed_annotations - 1
                    WHERE algo_annotation.id = NEW.parent_ident;
                END IF;
            END IF;

            RETURN NEW;

        END ;
        \$incAnnRevAnn\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS updateAnnotationReviewedAnnotationTrigger on reviewed_annotation;"

        String createTrigger = "CREATE TRIGGER updateAnnotationReviewedAnnotationTrigger AFTER UPDATE ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE updateAnnotationReviewedAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }




    String getAnnotationReviewBeforeDelete() {

        String createFunction = """
        CREATE OR REPLACE FUNCTION beforeDeleteReviewedAnnotation() RETURNS trigger as \$incAnnRevAnn\$
        DECLARE
            currentImage  image_instance%ROWTYPE;
            currentProject  project%ROWTYPE;
            currentAnnotationIndex  annotation_index%ROWTYPE;
           current_class reviewed_annotation.parent_class_name%TYPE;
           algo_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.AlgoAnnotation';
           user_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.UserAnnotation';
           currentUserAnnotation user_annotation%ROWTYPE;
           currentAlgoAnnotation algo_annotation%ROWTYPE;
        BEGIN
            SELECT * INTO currentImage FROM image_instance where id = OLD.image_id FOR UPDATE;
            --SELECT * INTO currentProject FROM project where id = OLD.project_id FOR UPDATE;
            SELECT * INTO currentAnnotationIndex FROM annotation_index WHERE user_id = OLD.review_user_id AND image_id = OLD.image_id;

            SELECT parent_class_name INTO current_class from reviewed_annotation where id = OLD.id;
            IF current_class = user_class THEN
                SELECT * INTO currentUserAnnotation FROM user_annotation WHERE id = OLD.parent_ident FOR UPDATE;
            ELSEIF current_class = algo_class THEN
                SELECT * INTO currentAlgoAnnotation FROM algo_annotation WHERE id = OLD.parent_ident FOR UPDATE;
            END IF;
            RETURN OLD;
        END ;
        \$incAnnRevAnn\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS beforeDeleteReviewedAnnotationTrigger on reviewed_annotation;"

        String createTrigger = "CREATE TRIGGER beforeDeleteReviewedAnnotationTrigger BEFORE DELETE ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE beforeDeleteReviewedAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }

    String getAnnotationReviewAfterDelete() {

        String createFunction = """
        CREATE OR REPLACE FUNCTION afterDeleteReviewedAnnotation() RETURNS trigger as \$incAnnRevAnn\$
        DECLARE
           current_class reviewed_annotation.parent_class_name%TYPE;
           algo_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.AlgoAnnotation';
           user_class reviewed_annotation.parent_class_name%TYPE := 'be.cytomine.ontology.UserAnnotation';
        BEGIN
            UPDATE image_instance
            SET count_image_reviewed_annotations = count_image_reviewed_annotations - 1
            WHERE image_instance.id = OLD.image_id;

            --UPDATE project
            --SET count_reviewed_annotations = count_reviewed_annotations - 1
            --WHERE project.id = OLD.project_id;

            UPDATE annotation_index
            SET count_reviewed_annotation = count_reviewed_annotation-1, version = version+1
            WHERE user_id = OLD.user_id
            AND image_id = OLD.image_id;

            IF OLD.parent_class_name = user_class THEN
                UPDATE user_annotation
                SET count_reviewed_annotations = count_reviewed_annotations - 1
                WHERE user_annotation.id = OLD.parent_ident;
            ELSEIF OLD.parent_class_name = algo_class THEN
                UPDATE algo_annotation
                SET count_reviewed_annotations = count_reviewed_annotations - 1
                WHERE algo_annotation.id = OLD.parent_ident;
            END IF;
            RETURN OLD;
        END ;
        \$incAnnRevAnn\$ LANGUAGE plpgsql; """

        String dropTrigger = "DROP TRIGGER IF EXISTS afterDeleteReviewedAnnotationTrigger on reviewed_annotation;"

        String createTrigger = "CREATE TRIGGER afterDeleteReviewedAnnotationTrigger AFTER DELETE ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE afterDeleteReviewedAnnotation(); "

        log.debug createFunction
        log.debug dropTrigger
        log.debug createTrigger
        return createFunction + dropTrigger + createTrigger
    }








//
//
//
//    String getReviewedAnnotationIndexTriggerIncr() {
//        String createFunction = """
//                           CREATE OR REPLACE FUNCTION incrementReviewedAnnotationIndex() RETURNS trigger as \$incAnnRevIndex\$
//    DECLARE
//            alreadyExist INTEGER;
//            current_id annotation_index.id%TYPE;
//    BEGIN
//            SELECT count(*) INTO alreadyExist FROM annotation_index WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
//            IF (alreadyExist=0) THEN
//                INSERT INTO annotation_index(user_id, image_id, count_annotation, count_reviewed_annotation, version, id) VALUES(NEW.user_id,NEW.image_id,0,0,0,nextval('hibernate_sequence'));
//            END IF;
//            UPDATE annotation_index SET count_reviewed_annotation = count_reviewed_annotation+1, version = version+1 WHERE user_id = NEW.user_id AND image_id = NEW.image_id;
//            RETURN NEW;
//    END;
//    \$incAnnRevIndex\$ LANGUAGE plpgsql; """
//
//        String dropTrigger = "DROP TRIGGER IF EXISTS incrementReviewedAnnotationIndexTrigger on reviewed_annotation;"
//
//        String createTrigger = "CREATE TRIGGER incrementReviewedAnnotationIndexTrigger AFTER INSERT ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE incrementReviewedAnnotationIndex(); "
//
//        log.debug createFunction
//        log.debug dropTrigger
//        log.debug createTrigger
//        return createFunction + dropTrigger + createTrigger
//    }
//
//    String getReviewedAnnotationIndexTriggerDecr() {
//        String createFunction = """
//                            CREATE OR REPLACE FUNCTION decrementReviewedAnnotationIndex() RETURNS trigger as \$decrAnnUserIndex\$
//            DECLARE
//                    alreadyExist INTEGER;
//                    current_id annotation_index.id%TYPE;
//            BEGIN
//                    UPDATE annotation_index SET count_reviewed_annotation = count_reviewed_annotation-1, version = version+1 WHERE user_id = OLD.user_id AND image_id = OLD.image_id;
//                    RETURN OLD;
//            END;
//            \$decrAnnUserIndex\$ LANGUAGE plpgsql; """
//
//        String dropTrigger = "DROP TRIGGER IF EXISTS decrementReviewedAnnotationIndexTrigger on reviewed_annotation;"
//
//        String createTrigger = "CREATE TRIGGER decrementReviewedAnnotationIndexTrigger AFTER DELETE ON reviewed_annotation FOR EACH ROW EXECUTE PROCEDURE decrementReviewedAnnotationIndex(); "
//
//        log.debug createFunction
//        log.debug dropTrigger
//        log.debug createTrigger
//        return createFunction + dropTrigger + createTrigger
//    }
//


}
