package be.cytomine.utils

/*
* Copyright (c) 2009-2016. Authors: see NOTICE file.
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

import be.cytomine.image.*
import be.cytomine.image.server.*
import be.cytomine.laboratory.*
import be.cytomine.ontology.*
import be.cytomine.processing.*
import be.cytomine.project.*
import be.cytomine.search.*
import be.cytomine.security.*
import be.cytomine.test.Infos
import be.cytomine.test.http.DomainAPI
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray

/*
* Will test the Resource Naming Convention for Restful app
* */
class RestUrlTests {

    //TODO : Goal, fix a lot of them
    def classes = [
            /*[clazz:AddCommand, filters : []],
            [clazz:Command, filters : []],
            [clazz:CommandHistory, filters : []],
            [clazz:DeleteCommand, filters : []],
            [clazz:EditCommand, filters : []],
            [clazz:RedoStackItem, filters : []],
            [clazz:UndoStackItem, filters : []],
            [clazz:Transaction, filters : []],*/

            //[clazz:ImageGroup, filters : []],
            //[clazz:ImageSequence, filters : []],
            //[clazz:ImageProperty, filters : []],
            //[clazz:ImageServer, filters : []],
            //[clazz:ImageServerStorage, filters : []],
            //[clazz:MimeImageServer, filters : []], now it is IMS that route on IIP by mimeType. So does it still have sense ?
            //[clazz:RetrievalServer, filters : []],
            [clazz:Storage, filters : []],
            //[clazz:StorageAbstractImage, filters : []],
            [clazz:AbstractImage, filters : []],
            //[clazz:ImageInstance, filters : []],
            //[clazz:Mime, filters : []],
            //[clazz:NestedFile, filters : []],
            //[clazz:NestedImageInstance, filters : []],
            [clazz:UploadedFile, filters : []],
            //[clazz:Source, filters : []],
            [clazz:Sample, filters : []],
            //[clazz:AmqpQueueConfigInstance, filters : []],
            //[clazz:AmqpQueueConfig, filters : []],
            //[clazz:AmqpQueue, filters : []],
            //[clazz:MessageBrokerServer, filters : []],
            [clazz:AlgoAnnotation, filters : []],
            //[clazz:AlgoAnnotationTerm, filters : []],
            //[clazz:AnnotationFilter, filters : []],
            //[clazz:AnnotationIndex, filters : []],
            //[clazz:AnnotationTerm, filters : []],
            [clazz:Ontology, filters : []],
            //[clazz:Property, filters : []],
            [clazz:Relation, filters : []],
            //[clazz:RelationTerm, filters : []],
            [clazz:ReviewedAnnotation, filters : []],
            //[clazz:SharedAnnotation, filters : []],
            [clazz:Term, filters : []],
            [clazz:UserAnnotation, filters : []],
            [clazz:ImageFilter, filters : []],
            [clazz:ImageFilterProject, filters : []],
            [clazz:Job, filters : []],
            [clazz:JobData, filters : []],
            //[clazz:JobDataBinaryValue, filters : []],
            [clazz:JobParameter, filters : []],
            //[clazz:JobTemplate, filters : []],
            //[clazz:JobTemplateAnnotation, filters : []],
            //[clazz:ProcessingServer, filters : []],
            //[clazz:RoiAnnotation, filters : []],
            [clazz:Software, filters : []],
            [clazz:SoftwareParameter, filters : []],
            [clazz:SoftwareProject, filters : []],
            [clazz:Discipline, filters : []],
            [clazz:Project, filters : []],
            //[clazz:ProjectDefaultLayer, filters : []],
            //[clazz:ProjectRepresentativeUser, filters : []],
            [clazz:SearchEngineFilter, filters : []],
            //[clazz:AuthWithToken, filters : []],
            //[clazz:ForgotPasswordToken, filters : []],
            [clazz:Group, filters : []],
            //[clazz:SecRole, filters : []],
            //[clazz:SecUser, filters : []],
            //[clazz:SecUserSecRole, filters : []],
            [clazz:User, filters : []],
            //[clazz:UserGroup, filters : []],
            //[clazz:UserJob, filters : []],
            //[clazz:AnnotationAction, filters : []],
            /*[clazz:LastConnection, filters : []],
            [clazz:LastUserPosition, filters : []],
            [clazz:PersistentConnection, filters : []],
            [clazz:PersistentImageConsultation, filters : []],
            [clazz:PersistentProjectConnection, filters : []],
            [clazz:PersistentUserPosition, filters : []],*/
            //[clazz:UserPosition, filters : []],
            [clazz:AttachedFile, filters : []],
            [clazz:Config, filters : []],
            [clazz:Description, filters : []],
            //[clazz:Keyword, filters : []],
            [clazz:News, filters : []]
            //[clazz:Version, filters : []]
    ]

    public void testUrl() {
        def problems = []
        classes.each {
            String URL = Infos.CYTOMINEURL + "api/"+it.clazz.simpleName.toLowerCase()+".json"
            println "URL is $URL"
            def result = DomainAPI.doGET(URL, Infos.SUPERADMINLOGIN, Infos.SUPERADMINPASSWORD)
            /*assert 200 == result.code
            def json = JSON.parse(result.data)
            assert json.collection instanceof JSONArray*/
            assert 200 == result.code || 404 == result.code
            if(404 == result.code) problems << it.clazz.simpleName
        }

        println problems
        println problems.size()
    }

}
