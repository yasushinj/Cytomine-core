package be.cytomine

import be.cytomine.Exception.ServerException

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

import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass

@Secured(['ROLE_ADMIN','ROLE_SUPER_ADMIN'])
class DependencyController {


    def grailsApplication

    def index() { }

    def check() {
        try {
            checkDependence()

        } catch(Exception e) {
            log.error e
        }

    }

    boolean printAll = false

    public def checkDependence() {
        //domain to skip when checking dependence
        def domainToSkip = ["Command","AddCommand","EditCommand","DeleteCommand","CommandHistory","RedoStackItem","UndoStackItem"]

        //change domain name if necessary
        def fixDomainName =
            ["AnnotationDomain" : [[name:"UserAnnotation", type:"UserAnnotation"],[name:"AlgoAnnotation", type:"AlgoAnnotation"],[name:"ReviewedAnnotation", type:"ReviewedAnnotation"]],
             "User" : [[name:"SecUser", type:"SecUser"]],
             "user" : [[name:"secUser", type:"secUser"]],
             "UserJob" : [[name:"SecUser", type:"SecUser"]],
             "retrievalProject" : [[name:"project", type:"project"]],
             "receiver" : [[name:"secUser", type:"secUser"]]
        ]


        def allErrors = []
        grailsApplication.getDomainClasses().each { domain ->
            if(!domainToSkip.contains(domain.name)) {
                def columnDep = getDependencyColumn(domain,["be.cytomine."],["be.cytomine."])
                allErrors.addAll(checkServiceMethod(domain,columnDep,fixDomainName))

                def columnDepHasMany = getDependencyColumn(domain,["java.util.Set","java.util.SortedSet","java.util.List"],["be.cytomine."])
                allErrors.addAll(checkServiceMethodInverse(domain,columnDepHasMany,fixDomainName))
            }
        }

        log.error "There are ${allErrors.size()} dependency conflict!"

        allErrors.sort().each {
            log.error it
        }
        if (!allErrors.isEmpty()) {
            throw new ServerException(allErrors.join(","))
        }


    }

    private def getDependencyColumn(def domain, def typeFilter, def referenceFilter) {
        def columnDep = []

        def domainClass = grailsApplication.getDomainClasses().find {it.name == domain.name}

        def d = new DefaultGrailsDomainClass(domainClass.referenceInstance.class)
        d.persistentProperties.each { prop ->

            boolean hasTypeFilter = false
            typeFilter.each { filter ->
                if(prop.type.toString().contains(filter)) {
                    hasTypeFilter = true
                }
            }

            boolean hasReferenceFilter = false
            referenceFilter.each { filter ->
                if(prop.referencedPropertyType.toString().contains(filter)) {
                    hasReferenceFilter = true
                }
            }

            if(hasTypeFilter && hasReferenceFilter) {
                columnDep << [name:prop.name, type:getClassName(prop.referencedPropertyType.toString())]
            }
        }
        columnDep
    }

    private boolean deleteDependentMissingMethod(def serviceClass, def methodExpected) {
        def allServiceMethods = serviceClass.metaClass.methods*.name
        //if there is no delete method, don't throw error
        boolean deleteMethodExist = false

        boolean isFind = false
        allServiceMethods.each {
            if(it==methodExpected && !printAll) {
                isFind = true
            }

            if(it.startsWith("delete")) {
                deleteMethodExist = true
            }
        }
        return deleteMethodExist && !isFind


    }


    private def checkServiceMethod(def domain, def columns, def fixDomainName) {
        //special domain that we should delete manually (not with deleteDependentXXX method)
        def allErrors = []
        def fixColumn = []

        columns.each {
            if(fixDomainName.keySet().contains(it.type)) {
                fixDomainName.get(it.type).each {
                    fixColumn << it
                }
            } else {
                fixColumn << it
            }
        }

        def methodExpected = "deleteDependent"+domain.name

        fixColumn.each {
            String type = it.type

            def serviceClass = grailsApplication.getServiceClasses().find {it.name.toLowerCase() == type.toLowerCase()}

            if(!serviceClass) {
                allErrors << "Service ${type}Service must exist and must contains $methodExpected($type,transaction)!!!"
            } else {
                if(deleteDependentMissingMethod(serviceClass,methodExpected)) {
                    allErrors << "Service ${type}Service must implement $methodExpected($type,transaction)!!!"
                }
            }
        }
        allErrors
    }



    private def checkServiceMethodInverse(def domain, def columns,def fixDomainName) {
        def allErrors = []
        def fixColumn = []

        columns.each {
            if(fixDomainName.keySet().contains(it.type)) {
                fixDomainName.get(it.type).each {
                    fixColumn << it
                }
            } else {
                fixColumn << it
            }
        }

        fixColumn.each {
//            String fixName = it.name.substring(0,it.name.size()-1)
//            if(fixDomainName.keySet().contains(fixName)) {
//                fixName = fixDomainName.get(fixName).first().name
//            }


            def methodExpected2 = "deleteDependentHasMany" + domain.name

            String name = it.type

            String type = domain.name

            def serviceClass2 = grailsApplication.getServiceClasses().find{
                it.name.toLowerCase() == name.toLowerCase()
            }

            if(!serviceClass2) {
                allErrors << "Service ${name.substring(0,1).toUpperCase() + name.substring(1)}Service must exist and must contains $methodExpected2($type,transaction)!"
            } else {
                if(deleteDependentMissingMethod(serviceClass2,methodExpected2)) {
                    allErrors << "Service ${name.substring(0,1).toUpperCase() + name.substring(1)}Service must implement $methodExpected2($type,transaction)!"
                }
            }
        }
        allErrors
    }

    private String getClassName(String fullPath) {
    //be.cytomine.image.Image
        String[] array = fullPath.split("\\.")  //[be,cytomine,image,Image]
        //log.info array.length
        return array[array.length - 1] // Image
    }
}
