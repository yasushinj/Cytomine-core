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

import grails.converters.JSON

grails.config.locations = ["file:${userHome}/.grails/cytomineconfig.groovy"]
println "External config file: "+grails.config.locations
println "###########################################################################"
println "###########################################################################"

grails.plugin.springsecurity.useHttpSessionEventPublisher = true
grails.databinding.convertEmptyStringsToNull = false
JSON.use('default')
grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        json: ['application/json','text/json'],
        jsonp: 'application/javascript',
        html: ['text/html','application/xhtml+xml'],
        xml: ['text/xml', 'application/xml'],
        png : 'image/png',
        jpg : 'image/jpeg',
        text: 'text/plain',
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        css: 'text/css',
        csv: 'text/csv',
        all: '*/*',
        form: 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data'
]
cytomine.maxRequestSize = 10485760
storage_path="/data" //default path for image locations

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
grails.converters.json.default.deep = false

/**
 * Doc config
 */
grails.doc.title="Cytomine"
grails.doc.subtitle="Documentation"
grails.doc.authors="Hoyoux Renaud, Marée Raphaël, Rollus Loïc, Stévens Benjamin"
grails.doc.license="Apache2"
grails.doc.copyright="University of liège"
grails.doc.footer="www.cytomine.org"

// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []

cytomine.jobdata.filesystem = false
cytomine.jobdata.filesystemPath = "algo/data/"

// RabbitMQ server
grails.messageBrokerServerURL = "localhost:5672"

// set per-environment serverURL stem for creating absolute links
environments {
    scratch {
        grails.serverURL = "http://localhost:8080"
        grails.uploadURL = "http://localhost:9090"

        grails.imageServerURL = ["http://localhost:9080"]
        grails.retrievalServerURL = ["http://localhost:9097"]
        grails.converters.default.pretty.print = true
        grails.plugin.springsecurity.useBasicAuth = false
        grails.resources.adhoc.patterns = ['/images/*', '/js/*']
    }
    production {
        grails.UIURL = null
        grails.serverURL = "http://localhost-core"
        grails.uploadURL = "http://localhost:9090"
        grails.plugin.springsecurity.useBasicAuth = false
        grails.resources.adhoc.patterns = ['/images/*', '/js/*','/css/jsondoc/*']
        grails.retrievalServerURL = []
    }
    development {
        grails.UIURL = "http://localhost:8080"
        grails.serverURL = "http://localhost:8080"
        grails.uploadURL = "http://localhost-upload"
        grails.imageServerURL = ["http://localhost:9080"]
        grails.retrievalServerURL = ["http://localhost-retrieval"]
        grails.converters.default.pretty.print = true
        grails.plugin.springsecurity.useBasicAuth = false
        grails.resources.adhoc.patterns = ['/images/*', '/js/*','/css/jsondoc/*']
        grails.readOnlyProjectsByDefault = true
        grails.adminPassword="admin"
        grails.ImageServerPrivateKey=""
        grails.ImageServerPublicKey=""
        grails.adminPrivateKey="XXX"
        grails.adminPublicKey="XXX"
        grails.superAdminPrivateKey="X"
        grails.superAdminPublicKey="X"
    }
    test {
        grails.serverURL = "http://localhost:8090"
        grails.imageServerURL = ["http://localhost:9080"]
        grails.uploadURL = "http://localhost-upload"
        grails.retrievalServerURL = ["http://localhost-retrieval"]
        grails.plugin.springsecurity.useBasicAuth = true
        grails.plugin.springsecurity.basic.realmName = "Cytomine log"
        grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']
        grails.readOnlyProjectsByDefault = true

        grails.adminPassword = "password"
        grails.ImageServerPrivateKey=""
        grails.ImageServerPublicKey=""
        grails.adminPrivateKey="XXX"
        grails.adminPublicKey="XXX"
        grails.superAdminPrivateKey="X"
        grails.superAdminPublicKey="X"
    }
    testrun {
        grails.serverURL = "http://localhost:8090"
        grails.uploadURL = "http://localhost:9090"
        grails.imageServerURL = ["http://localhost:9085"]
        grails.converters.default.pretty.print = true
        grails.plugin.springsecurity.useBasicAuth = false
        grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']
    }
}
coverage {
    enableByDefault = false
    xml = true
}

environments {
    development {
        grails.resources.processing.enabled = false;
    }
}
// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
//   System.setProperty('mail.smtp.port', mail.error.port.toString())
//   System.setProperty('mail.smtp.starttls.enable',  mail.error.starttls.toString())

    println "Log4j consoleLevel"

    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d{dd-MM-yyyy HH:mm:ss,SSS} %5p %c{1} - %m%n')
        rollingFile  name:'infoLog', file:'/tmp/cytomine-info.log', threshold: org.apache.log4j.Level.INFO, maxFileSize:1024
        rollingFile  name:'warnLog', file:'/tmp/cytomine-warn.log', threshold: org.apache.log4j.Level.WARN, maxFileSize:1024
        rollingFile  name:'errorLog', file:'/tmp/cytomine-error.log', threshold: org.apache.log4j.Level.ERROR, maxFileSize:1024
        rollingFile  name:'custom', file:'/tmp/cytomine-custom.log', maxFileSize:1024
    }

    error  'org.codehaus.groovy.grails.domain',
            'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'net.sf.ehcache.hibernate',
            'org.hibernate.engine.StatefulPersistenceContext.ProxyWarnLog'

    error 'org.springframework.security.web.context', 'org.hibernate.engine','net.sf.hibernate.impl.SessionImpl'

    error 'com.granicus.grails.plugins.cookiesession'

    error 'grails.plugin.springsecurity'

    environments {
        production {
            root {
                info 'errorLog','warnLog', 'infoLog', 'stdout'
                additivity = true
            }
        }
        development {
            root {
                info 'errorLog','warnLog', 'infoLog', 'stdout'
                additivity = true
            }
        }
        cluster {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
        test {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
        perf {
            root {
                info 'appLog',"logfile", 'stdout'
                additivity = true
            }
        }
    }

    //UNCOMMENT THESE 2 LINES TO SEE SQL REQUEST AND THEIR PARAMETERS VALUES
//    debug 'org.hibernate.SQL'
//    trace 'org.hibernate.type'
}
grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"
grails.plugin.springsecurity.interceptUrlMap = [
        '/admin/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
        '/admincyto/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
        '/monitoring/**':    ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
        '/j_spring_security_switch_user': ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
        '/securityInfo/**': ['ROLE_ADMIN','ROLE_SUPER_ADMIN'],
        '/api/**':      ['IS_AUTHENTICATED_REMEMBERED'],
        '/lib/**':      ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/css/**':      ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/images/**':   ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/*':           ['IS_AUTHENTICATED_REMEMBERED'], //if cas authentication, active this      //beta comment
        '/login/**':    ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/logout/**':   ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/status/**':   ['IS_AUTHENTICATED_ANONYMOUSLY']
]

grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.fii.rejectPublicInvocations = false

/* Read CAS/LDAP config. A bad thing with Grails external config is that all config data from config properties file
   is set AFTER ldap/cas config. So we read config data from file directly and we force flag (active)
   def flag = readFromConfigFile()
   if(flag) grails.config.flag = true
 */

File configFile = new File("${userHome}/.grails/cytomineconfig.groovy")

boolean cas = false
println "${configFile.path}="+configFile.exists()
if(configFile.exists()) {
    config = new ConfigSlurper().parse(configFile.text)
    println config
    cas = config.grails.plugin.springsecurity.cas.active
}

println "cas.active="+cas
if(cas) {
    println("enable CAS")
    grails.plugin.springsecurity.cas.useSingleSignout = true
    grails.plugin.springsecurity.cas.active = true
    grails.plugin.springsecurity.ldap.active = true
    grails.plugin.springsecurity.logout.afterLogoutUrl =''

} else {
    println("disable CAS")
    grails.plugin.springsecurity.cas.useSingleSignout = false
    grails.plugin.springsecurity.cas.active = false
    grails.plugin.springsecurity.ldap.active = false
    grails.plugin.springsecurity.interceptUrlMap.remove('/*')
}
grails.plugin.springsecurity.cas.loginUri = '/login'
grails.plugin.springsecurity.cas.serverUrlPrefix = ''

//allow an admin to connect as a other user
grails.plugin.springsecurity.useSwitchUserFilter = true

grails.plugin.springsecurity.cas.serviceUrl = 'http://localhost:8080/j_spring_cas_security_check'

// LDAP Configuration
grails.plugin.springsecurity.auth.loginFormUrl = '/'
grails.plugin.springsecurity.ldap.search.base = ''
grails.plugin.springsecurity.ldap.context.managerDn = ''
grails.plugin.springsecurity.ldap.context.managerPassword = ''
grails.plugin.springsecurity.ldap.context.server = ''
grails.plugin.springsecurity.ldap.authorities.groupSearchBase = ''
grails.plugin.springsecurity.ldap.mapper.userDetailsClass= 'inetOrgPerson'// 'org.springframework.security.ldap.userdetails.InetOrgPerson'
grails.plugin.springsecurity.ldap.mapper.usePassword= true
grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
grails.plugin.springsecurity.ldap.context.anonymousReadOnly = true


// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'be.cytomine.security.SecUser'
grails.plugin.springsecurity.userLookup.passwordPropertyName = 'password'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'be.cytomine.security.SecUserSecRole'
grails.plugin.springsecurity.authority.className = 'be.cytomine.security.SecRole'
grails.plugin.springsecurity.authority.nameField = 'authority'
grails.plugin.springsecurity.projectClass = 'be.cytomine.project.Project'
grails.plugin.springsecurity.rememberMe.parameter = 'remember_me'

grails.plugins.dynamicController.mixins = [
        'com.burtbeckwith.grails.plugins.appinfo.IndexControllerMixin':       'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.HibernateControllerMixin':   'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.Log4jControllerMixin' :      'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.SpringControllerMixin' :     'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.MemoryControllerMixin' :     'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.PropertiesControllerMixin' : 'com.burtbeckwith.appinfo_test.AdminManageController',
        'com.burtbeckwith.grails.plugins.appinfo.ScopesControllerMixin' :     'com.burtbeckwith.appinfo_test.AdminManageController'
]

// Rest API Doc plugin
grails.plugins.restapidoc.docVersion = "0.1"
grails.plugins.restapidoc.basePath = "http://demo.cytomine.coop"
grails.plugins.restapidoc.customClassName = "be.cytomine.api.doc.CustomResponseDoc"
grails.plugins.restapidoc.controllerPrefix = "Rest"
grails.plugins.restapidoc.grailsDomainDefaultType = "long"

grails.plugins.restapidoc.defaultParamsQueryMultiple = [
        [name:"max",description:"Pagination: Number of record per page (default 0 = no pagination)",type:"int"],
        [name:"offset",description:"Pagination: Offset of first record (default 0 = first record)",type:"int"]
]

grails.plugins.restapidoc.defaultErrorAll = [
        "400": "Bad Request: missing parameters or bad message format",
        "401": "Unauthorized: must be auth",
        "403": "Forbidden: role error",
        "404": "Object not found"
]

grails.plugins.restapidoc.defaultErrorGet = [
        "400": "Bad Request: missing parameters or bad message format",
        "401": "Unauthorized: must be auth",
        "403": "Forbidden: role error",
        "404": "Object not found"
]

grails.plugins.restapidoc.defaultErrorPost = [
        "409": "Object already exist"
]

grails.plugins.restapidoc.defaultErrorPut = [
        "409": "Object already exist"
]

cytomine.customUI.global = [
        dashboard: ["ALL"],
        search : ["ROLE_ADMIN"],
        project: ["ALL"],
        ontology: ["ROLE_ADMIN"],
        storage : ["ROLE_USER","ROLE_ADMIN"],
        activity : ["ALL"],
        explore : ["ROLE_USER","ROLE_ADMIN"],
        admin : ["ROLE_ADMIN"],
        help : ["ALL"]
]

cytomine.customUI.project = [
        //tabs
        "project-images-tab":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotations-tab":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-jobs-tab":["ADMIN_PROJECT":false,"CONTRIBUTOR_PROJECT":false],
        "project-activities-tab":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":false],
        "project-information-tab":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-configuration-tab":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":false],

        //explore
        "project-explore-hide-tools":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-overview":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-info":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-digital-zoom":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-link":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-color-manipulation":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-image-layers":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-ontology":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-review":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-job":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-property":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-follow":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-guided-tour":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],

        //annotation details
        "project-explore-annotation-main":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-geometry-info":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-info":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-comments":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-preview":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-properties":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-description":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-panel":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-terms":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-attached-files":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-explore-annotation-creation-info":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],

        //annotation tools
        "project-tools-main":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-select":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-point":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-line":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-freehand-line":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-arrow":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-rectangle":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-diamond":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-circle":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-polygon":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-freehand-polygon":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-magic":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-freehand":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-union":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-diff":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-fill":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-rule":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-edit": ["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-resize":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-rotate":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-move":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-delete":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-screenshot":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-tools-undo-redo":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],

        //graphs
        "project-annotations-term-piegraph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotations-term-bargraph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotations-users-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotated-slides-term-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotated-slides-users-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-annotation-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-users-global-activities-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
        "project-users-heatmap-graph":["ADMIN_PROJECT":true,"CONTRIBUTOR_PROJECT":true],
]

environments {
    cluster {
        grails {
            cache {
                enabled = false
                ehcache {
                    ehcacheXmlLocation = 'classpath:ehcache.xml' // conf/ehcache.xml
                    reloadable = false
                }
            }
        }

    }
}


grails.client = "NO"

grails.plugin.springsecurity.password.algorithm = 'SHA-256'
grails.plugin.springsecurity.password.hash.iterations = 1

cytomine.middleware.rabbitmq.user = "router"
cytomine.middleware.rabbitmq.password = "router"


//limitations
cytomine.annotation.maxNumberOfPoint = 200


// instance hoster configurations
grails.admin.email = "info@cytomine.org"
grails.notification.email = ""
grails.notification.password = ""
grails.notification.smtp.host = "smtp.gmail.com"
grails.notification.smtp.port = "587"

grails.instanceHostWebsite = "https://www.cytomine.org"
grails.instanceHostSupportMail = "support@cytomine.coop"
grails.instanceHostPhoneNumber = null

grails.defaultLanguage = "ENGLISH"