package be.cytomine.image

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.CytomineMethodNotYetImplementedException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.api.UrlApi
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.multidim.ImageGroup
import be.cytomine.image.multidim.ImageSequence
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.meta.Property
import be.cytomine.ontology.ReviewedAnnotation
import be.cytomine.ontology.UserAnnotation
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.meta.Description
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import org.hibernate.FetchMode
import org.restapidoc.annotation.RestApiObjectField
import org.springframework.util.ReflectionUtils

import static org.springframework.security.acls.domain.BasePermission.READ

/**
 * TODO:: refactor + doc!!!!!!!
 */
class ImageInstanceService extends ModelService {

    static transactional = false

    def cytomineService
    def transactionService
    def userAnnotationService
    def algoAnnotationService
    def dataSource
    def reviewedAnnotationService
    def imageSequenceService
    def propertyService
    def annotationIndexService
    def securityACLService
    def mongo
    def noSQLCollectionService

    def currentDomain() {
        return ImageInstance
    }

    def read(def id) {
        def image = ImageInstance.read(id)
        if(image) {
            securityACLService.check(image.container(),READ)
            checkDeleted(image)
        }
        image
    }


    /**
     * Get all image id from project
     */
    public List<Long> getAllImageId(Project project) {
        securityACLService.check(project,READ)

        //better perf with sql request
        String request = "SELECT a.id FROM image_instance a WHERE project_id="+project.id  + " AND parent_id IS NULL AND deleted IS NULL"
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            data << it[0]
        }
        sql.close()
        return data
    }

    def list(User user, String sortColumn = "created", String sortDirection = "desc", def searchParameters = [], Long max = 0, Long offset = 0) {

        securityACLService.checkIsSameUser(user, cytomineService.currentUser)

        String imageInstanceAlias = "ii"
        String abstractImageAlias = "ai"

        if (!sortColumn) sortColumn = "created"
        if (!sortDirection) sortDirection = "asc"
        if (sortColumn.equals("numberOfAnnotations")) sortColumn = "countImageAnnotations"
        if (sortColumn.equals("numberOfJobAnnotations")) sortColumn = "countImageJobAnnotations"
        if (sortColumn.equals("numberOfReviewedAnnotations")) sortColumn = "countImageReviewedAnnotations"

        String sortedProperty = ReflectionUtils.findField(ImageInstance, sortColumn) ? "${imageInstanceAlias}." + sortColumn : null
        if (!sortedProperty) sortedProperty = ReflectionUtils.findField(AbstractImage, sortColumn) ? abstractImageAlias + "." + sortColumn : null
        if (!sortedProperty) throw new CytomineMethodNotYetImplementedException("ImageInstance list sorted by $sortDirection is not implemented")

        def validatedSearchParameters = getDomainAssociatedSearchParameters(searchParameters, false)

        validatedSearchParameters.findAll { !it.property.contains(".") }.each {
            it.property = "${imageInstanceAlias}." + it.property
        }
        validatedSearchParameters.findAll { it.property == "ii.instanceFilename" }.each { it.property = "name" }

        boolean joinAI = validatedSearchParameters.any {it.property.contains(abstractImageAlias + ".")} || sortedProperty.contains(abstractImageAlias + ".")

        def sqlSearchConditions = searchParametersToSQLConstraints(validatedSearchParameters)

        def nameSearch = sqlSearchConditions.data.find { it.property == "name" }

        sqlSearchConditions = [
                imageInstance: sqlSearchConditions.data.findAll {it.property.startsWith("$imageInstanceAlias.")}.collect { it.sql }.join(" AND "),
                abstractImage: sqlSearchConditions.data.findAll {it.property.startsWith("$abstractImageAlias.")}.collect { it.sql }.join(" AND "),
                tags : sqlSearchConditions.data.findAll{it.property.startsWith("tda.")}.collect{it.sql}.join(" AND "),
                parameters   : sqlSearchConditions.sqlParameters
        ]


        String select, from, where, search, sort
        String request

        select = "SELECT distinct $imageInstanceAlias.* "
        from = "FROM user_image $imageInstanceAlias "
        where = "WHERE user_image_id = ${user.id} "

        if (joinAI) {
            select += ", ${abstractImageAlias}.* "
            from += "JOIN abstract_image $abstractImageAlias ON ${abstractImageAlias}.id = ${imageInstanceAlias}.base_image_id "
        }
        search = ""

        if (sqlSearchConditions.imageInstance) {
            search += " AND "
            search += sqlSearchConditions.imageInstance
        }
        if (sqlSearchConditions.abstractImage) {
            search += " AND "
            search += sqlSearchConditions.abstractImage
        }
        if(sqlSearchConditions.tags){
            from += "LEFT OUTER JOIN tag_domain_association tda ON ii.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.image.ImageInstance' "
            search +=" AND "
            search += sqlSearchConditions.tags
        }

        if (nameSearch) {
            String operation
            if (nameSearch.operator == "ilike") {
                operation = "ILIKE"
            } else if (nameSearch.operator == "like") {
                operation = "LIKE"
            } else if (nameSearch.operator == "equals") {
                operation = "=="
            }
            where += "AND ( (NOT project_blind AND instance_filename $operation :name) OR (project_blind AND NOT user_project_manager AND base_image_id::text $operation :name) OR (project_blind AND user_project_manager AND (base_image_id::text $operation :name OR instance_filename $operation :name)))"
        }

        sort = " ORDER BY " + sortedProperty
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC "


        request = select + from + where + search + sort
        if (max > 0) request += " LIMIT $max"
        if (offset > 0) request += " OFFSET $offset"


        def sql = new Sql(dataSource)
        def data = []
        def mapParams = sqlSearchConditions.parameters

        if(nameSearch){
            mapParams.put("name", nameSearch.value)
        }

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1; i<=((GroovyResultSet) it).getMetaData().getColumnCount(); i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() } )


                map.putAt(objectKey, it[key])
            }

            map['created'] = map['created'].getTime()
            map['deleted'] = map['deleted']?.getTime()
            map['updated'] = map['updated']?.getTime()
            map['reviewStart'] = map['reviewStart']?.getTime()
            map['reviewStop'] = map['reviewStop']?.getTime()
            map['reviewUser'] = map['reviewUserId']
            map['baseImage'] = map['baseImageId']
            map['project'] = map['projectId']

            //TODO improve perf !
            def line = ImageInstance.getDataFromDomain(ImageInstance.insertDataIntoDomain(map))
            line.putAt('projectBlind', map.projectBlind)
            line.putAt('projectName', map.projectName)
            data << line
        }
        def size
        request = "SELECT COUNT(DISTINCT ${imageInstanceAlias}.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        return [data:data, total:size]
    }

    def listLight(User user) {
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        def data = []

        boolean isAdmin = securityACLService.isAdminByNow(user)

        // user_image already filter nested image
        def sql = new Sql(dataSource)
        sql.eachRow("select * from user_image where user_image_id = ? order by instance_filename", [user.id]) {
            def line = [id: it.id, projectName: it.project_name, project: it.project_id]
            if(it.project_blind) {
                line.blindedName = it.base_image_id
            }
            if(!it.project_blind || isAdmin || it.user_project_manager) {
                line.instanceFilename = it.instance_filename
            }
            data << line
        }
        sql.close()
        return data
    }

    def listLastOpened(User user, Long offset = null, Long max = null) {
        log.info "calling listLastOpened..."
        //get id of last open image
        securityACLService.checkIsSameUser(user,cytomineService.currentUser)
        def data = []

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

        def result = db.persistentImageConsultation.aggregate(
                [$match : [ user : user.id]],
                [$group : [_id : '$image', "date":[$max:'$created']]],
                [$sort : [ date : -1]],
                [$limit: (max==null? 5 : max)]
        )
        //result = result.results().collect{it['_id']}.collect{[it["image"],it["user"]]}
        def size = result.results().size()
        log.info "listLastOpened=${user}, size=${size}"
        result.results().each {
            try {
                ImageInstance image = read(it['_id'])
                String filename;
                filename = image.instanceFilename == null ? image.baseImage.originalFilename : image.instanceFilename;
                log.info "filename=${filename}"
                if(image.project.blindMode) filename = image.getBlindedName()
                data << [id:it['_id'],date:it['date'], thumb: UrlApi.getAbstractImageThumbURL(image.baseImage.id),instanceFilename:filename,project:image.project.id]
            } catch(CytomineException e) {
                //if user has data but has no access to picture,  ImageInstance.read will throw a forbiddenException
            }
        }
        data = data.sort{-it.date.getTime()}
        log.info "data : ${data}"
        return data
    }





    def listTree(Project project, Long max  = 0, Long offset = 0) {
        securityACLService.check(project,READ)

        def children = []
        def images = list(project, null, null, [], max, offset)
        images.data.each { image->
            children << [ id : image.id, key : image.id, title : image.instanceFilename, isFolder : false, children : []]
        }
        def tree = [:]
        tree.isFolder = true
        tree.hideCheckbox = true
        tree.name = project.getName()
        tree.title = project.getName();
        tree.key = project.getId()
        tree.id = project.getId()
        tree.children = children
        tree.size = images.total
        return tree
    }

    def list(Project project, String sortColumn = 'created', String sortDirection = "asc", def searchParameters = [], Long max  = 0, Long offset = 0, boolean light=false) {
        securityACLService.check(project,READ)

        String imageInstanceAlias = "ii"
        String abstractImageAlias = "ai"
        String mimeAlias = "mime"

        if(!sortColumn)  sortColumn = "created"
        if(!sortDirection)  sortDirection = "asc"
        if(sortColumn.equals("numberOfAnnotations")) sortColumn = "countImageAnnotations"
        if(sortColumn.equals("numberOfJobAnnotations")) sortColumn = "countImageJobAnnotations"
        if(sortColumn.equals("numberOfReviewedAnnotations")) sortColumn = "countImageReviewedAnnotations"

        String sortedProperty = ReflectionUtils.findField(ImageInstance, sortColumn) ? "${imageInstanceAlias}." + sortColumn : null
        if(sortColumn.equals("blindedName")) sortColumn = "id"
        if(!sortedProperty) sortedProperty = ReflectionUtils.findField(AbstractImage, sortColumn) ? abstractImageAlias + "." + sortColumn : null
        if(!sortedProperty) sortedProperty = ReflectionUtils.findField(Mime, sortColumn) ? mimeAlias + "." + sortColumn : null
        if(!sortedProperty) throw new CytomineMethodNotYetImplementedException("ImageInstance list sorted by $sortDirection is not implemented")
        sortedProperty = fieldNameToSQL(sortedProperty)

        def validatedSearchParameters = getDomainAssociatedSearchParameters(searchParameters, project.blindMode)

        validatedSearchParameters.findAll { !it.property.contains(".") }.each {
            it.property = "${imageInstanceAlias}." + it.property
        }

        def blindedNameSearch
        boolean manager = false
        for (def parameter : searchParameters){
            if(parameter.field.equals("blindedName")){
                parameter.field = "ai.id"
                blindedNameSearch = parameter
                break
            }
        }


        boolean joinAI = validatedSearchParameters.any {it.property.contains(abstractImageAlias+".")} || sortedProperty.contains(abstractImageAlias+".")
        boolean joinMime = validatedSearchParameters.any {it.property.contains(mimeAlias+".")} || sortedProperty.contains(mimeAlias+".")
        def sqlSearchConditions = searchParametersToSQLConstraints(validatedSearchParameters)

        sqlSearchConditions = [
                imageInstance: sqlSearchConditions.data.findAll {it.property.startsWith("$imageInstanceAlias.")}.collect { it.sql }.join(" AND "),
                abstractImage: sqlSearchConditions.data.findAll {it.property.startsWith("$abstractImageAlias.")}.collect { it.sql }.join(" AND "),
                mime: sqlSearchConditions.data.findAll {it.property.startsWith("$mimeAlias.")}.collect { it.sql }.join(" AND "),
                tags : sqlSearchConditions.data.findAll{it.property.startsWith("tda.")}.collect{it.sql}.join(" AND "),
                parameters   : sqlSearchConditions.sqlParameters
        ]

        if(blindedNameSearch) {
            joinAI = true
            blindedNameSearch = blindedNameSearch.values

            try{
                securityACLService.checkIsAdminContainer(project, cytomineService.currentUser)
                manager = true
            } catch(ForbiddenException e){}
        }


        String select, from, where, search, sort
        String request


        select = "SELECT distinct $imageInstanceAlias.* "
        from = "FROM image_instance $imageInstanceAlias "
        where = "WHERE ${imageInstanceAlias}.project_id = ${project.id} AND ${imageInstanceAlias}.parent_id IS NULL AND ${imageInstanceAlias}.deleted IS NULL "

        if (joinAI) {
            select += ", ${abstractImageAlias}.* "
            from += "JOIN abstract_image $abstractImageAlias ON ${abstractImageAlias}.id = ${imageInstanceAlias}.base_image_id "
        }
        if (joinMime) {
            select += ", ${mimeAlias}.* "
            from += "JOIN mime $mimeAlias ON ${mimeAlias}.id = ${abstractImageAlias}.mime_id "
        }
        search = ""

        if (sqlSearchConditions.imageInstance) {
            search += " AND "
            search += sqlSearchConditions.imageInstance
        }
        if (sqlSearchConditions.abstractImage) {
            search += " AND "
            search += sqlSearchConditions.abstractImage
        }
        if (sqlSearchConditions.mime) {
            search += " AND "
            search += sqlSearchConditions.mime
        }
        if(sqlSearchConditions.tags){
            from += "LEFT OUTER JOIN tag_domain_association tda ON ii.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.image.ImageInstance' "
            search +=" AND "
            search += sqlSearchConditions.tags
        }


        if(blindedNameSearch && manager) {
            search +=" AND "
            search += "("+abstractImageAlias+".id::text ILIKE :name OR  ${imageInstanceAlias}.instance_filename ILIKE :name ) "
        } else if(blindedNameSearch){
            search +=" AND "
            search += abstractImageAlias+".id::text ILIKE :name "
        }


        sort = " ORDER BY " + sortedProperty
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC "


        request = select + from + where + search + sort
        if (max > 0) request += " LIMIT $max"
        if (offset > 0) request += " OFFSET $offset"


        def sql = new Sql(dataSource)
        def data = []
        def mapParams = sqlSearchConditions.parameters

        if(blindedNameSearch){
            if(mapParams.isEmpty()) mapParams = [:]
            mapParams.put("name", blindedNameSearch)
        }

        sql.eachRow(request, mapParams) {
            def map = [:]

            for(int i =1; i<=((GroovyResultSet) it).getMetaData().getColumnCount(); i++){
                String key = ((GroovyResultSet) it).getMetaData().getColumnName(i)
                String objectKey = key.replaceAll( "(_)([A-Za-z0-9])", { Object[] test -> test[2].toUpperCase() } )


                map.putAt(objectKey, it[key])
            }

            map['created'] = map['created'].getTime()
            map['deleted'] = map['deleted']?.getTime()
            map['updated'] = map['updated']?.getTime()
            map['reviewUser'] = map['reviewUserId']
            map['reviewStart'] = map['reviewStart']?.getTime()
            map['reviewStop'] = map['reviewStop']?.getTime()
            map['baseImage'] = map['baseImageId']
            map['project'] = map['projectId']
            map['user'] = map['userId']

            //TODO improve perf !
            def line = ImageInstance.getDataFromDomain(ImageInstance.insertDataIntoDomain(map))
            line.putAt('numberOfAnnotations', map.countImageAnnotations)
            line.putAt('numberOfJobAnnotations', map.countImageJobAnnotations)
            line.putAt('numberOfReviewedAnnotations', map.countImageReviewedAnnotations)
            line.putAt('projectBlind', map.projectBlind)
            line.putAt('projectName', map.projectName)
            data << line
        }

        def size
        request = "SELECT COUNT(DISTINCT ${imageInstanceAlias}.id) " + from + where + search

        sql.eachRow(request, mapParams) {
            size = it.count
        }
        sql.close()

        if(light) {
            def result = []
            data.each { image ->
                result << [id: image.id, instanceFilename: image.instanceFilename, blindedName: image.blindedName]
            }
            data = result
        }
        return [data:data, total:size]
    }

    def listExtended(Project project, String sortColumn, String sortDirection, def searchParameters, Long max  = 0, Long offset = 0, def extended) {

        def data = []
        def images = list(project, sortColumn, sortDirection, searchParameters, max, offset)

        //get last activity grouped by images
        def user = cytomineService.currentUser

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def result = db.persistentImageConsultation.aggregate(
                [$match : [ user : user.id]],
                [$sort : [ created : -1]],
                [$group : [_id : '$image', created:[$max:'$created'], user:[$first: '$user']]],
                [$sort : [ _id : 1]]
        )

        def consultations = result.results().collect{[imageId : it['_id'],lastActivity:it['created'], user:it['user']]}

        // we sorted to apply binary search instead of a simple "find" method. => performance
        def binSearchI = { aList, property, target ->
            def a = aList
            def offSet = 0
            while (!a.empty) {
                def n = a.size()
                def m = n.intdiv(2)
                if(a[m]."$property" > target) {
                    a = a[0..<m]
                } else if (a[m]."$property" < target) {
                    a = a[(m + 1)..<n]
                    offSet += m + 1
                } else {
                    return (offSet + m)
                }
            }
            return -1
        }

        images.data.each { image ->
            def index
            def line = image
            if(extended.withLastActivity) {
                index = binSearchI(consultations, "imageId", image.id)
                if(index >= 0){
                    line.putAt("lastActivity", consultations[index].lastActivity)
                } else {
                    line.putAt("lastActivity", null)
                }
            }
            data << line
        }
        images.data = data
        return images
    }

    private long copyAnnotationLayer(ImageInstance image, User user, ImageInstance based, def usersProject,Task task, double total, double alreadyDone,SecUser currentUser, Boolean giveMe ) {
        log.info "copyAnnotationLayer=$image | $user "
        def alreadyDoneLocal = alreadyDone
        UserAnnotation.findAllByImageAndUser(image,user).each {
            copyAnnotation(it,based,usersProject,currentUser,giveMe)
            log.info "alreadyDone=$alreadyDone total=$total"
            taskService.updateTask(task,Math.min(100,((alreadyDoneLocal/total)*100d).intValue()),"Start to copy ${total.intValue()} annotations...")
            alreadyDoneLocal = alreadyDoneLocal +1
        }
        alreadyDoneLocal
    }


    private def copyAnnotation(UserAnnotation based, ImageInstance dest,def usersProject,SecUser currentUser,Boolean giveMe) {
        log.info "copyAnnotationLayer=${based.id}"

        //copy annotation
        UserAnnotation annotation = new UserAnnotation()
        annotation.created = based.created
        annotation.geometryCompression = based.geometryCompression
        annotation.image = dest
        annotation.location = based.location
        annotation.project = dest.project
        annotation.updated =  based.updated
        annotation.user = (giveMe? currentUser : based.user)
        annotation.wktLocation = based.wktLocation
        userAnnotationService.saveDomain(annotation)

        //copy term

        AnnotationTerm.findAllByUserAnnotation(based).each { basedAT ->
            if(usersProject.contains(basedAT.user.id) && basedAT.term.ontology==dest.project.ontology) {
                AnnotationTerm at = new AnnotationTerm()
                at.user = basedAT.user
                at.term = basedAT.term
                at.userAnnotation = annotation
                userAnnotationService.saveDomain(at)
            }
        }

        //copy description
        Description.findAllByDomainIdent(based.id).each {
            Description description = new Description()
            description.data = it.data
            description.domainClassName = it.domainClassName
            description.domainIdent = annotation.id
            userAnnotationService.saveDomain(description)
        }

        //copy properties
        Property.findAllByDomainIdent(based.id).each {
            Property property = new Property()
            property.key = it.key
            property.value = it.value
            property.domainClassName = it.domainClassName
            property.domainIdent = annotation.id
            userAnnotationService.saveDomain(property)
        }

    }

    public def copyLayers(ImageInstance image,def layers,def usersProject,Task task, SecUser currentUser,Boolean giveMe) {
        taskService.updateTask(task, 0, "Start to copy...")
        double total = 0
        if (task) {
            layers.each { couple ->
                def idImage = Long.parseLong(couple.split("_")[0])
                def idUser = Long.parseLong(couple.split("_")[1])
                def number = annotationIndexService.count(ImageInstance.read(idImage), SecUser.read(idUser))
                total = total + number
            }
        }
        taskService.updateTask(task, 0, "Start to copy $total annotations...")
        double alreadyDone = 0
        layers.each { couple ->
            def idImage = Long.parseLong(couple.split("_")[0])
            def idUser = Long.parseLong(couple.split("_")[1])
            alreadyDone = copyAnnotationLayer(ImageInstance.read(idImage), SecUser.read(idUser), image, usersProject,task, total, alreadyDone,currentUser,giveMe)
        }
        return []
    }


    def getLayersFromAbstractImage(AbstractImage image, ImageInstance exclude, def currentUsersProject,def layerFromNewImage, Project project = null) {
        //get id of last open image

        def layers = []
        def adminsMap = [:]

        def req1 = getLayersFromAbtrsactImageSQLRequestStr(true,project)
        def sql = new Sql(dataSource)
        sql.eachRow(req1,[image.id,exclude.id]) {
            if(currentUsersProject.contains(it.project) && layerFromNewImage.contains(it.user)) {
                layers << [image:it.image,user:it.user,projectName:it.projectName,project:it.project,lastname:it.lastname,firstname:it.firstname,username:it.username,admin:it.admin]
                adminsMap.put(it.image+"_"+it.user,true)
            }

        }
        sql.close()

        def req2 = getLayersFromAbtrsactImageSQLRequestStr(false,project)

        sql = new Sql(dataSource)
        sql.eachRow(req2,[image.id,exclude.id]) {
            if(!adminsMap.get(it.image+"_"+it.user) && currentUsersProject.contains(it.project) && layerFromNewImage.contains(it.user)) {
                layers << [image:it.image,user:it.user,projectName:it.projectName,project:it.project,lastname:it.lastname,firstname:it.firstname,username:it.username,admin:it.admin]
            }

        }
        sql.close()

        return layers

    }

    private String getLayersFromAbtrsactImageSQLRequestStr(boolean admin,Project project = null) {
        return """
            SELECT ii.id as image,su.id as user,p.name as projectName, p.id as project, su.lastname as lastname, su.firstname as firstname, su.username as username, '${admin}' as admin, count_annotation as annotations
            FROM image_instance ii, project p, ${admin? "admin_project" : "user_project" } up, sec_user su, annotation_index ai
            WHERE base_image_id = ?
            AND ii.id <> ?
            AND ii.deleted IS NULL
            AND ii.parent_id IS NULL
            AND ii.project_id = p.id
            AND up.id = p.id
            AND up.user_id = su.id
            AND ai.user_id = su.id
            AND ai.image_id = ii.id
            ${project? "AND p.id = " + project.id  : ""}
            ORDER BY p.name, su.lastname,su.firstname,su.username;
        """

    }



    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        securityACLService.check(json.project,Project,READ)
        securityACLService.checkisNotReadOnly(json.project,Project)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        log.info "json=$json"
        def project = Project.read(json.project)
        def baseImage = AbstractImage.read(json.baseImage)
        log.info "project=$project baseImage=$baseImage"
        def alreadyExist = ImageInstance.findByProjectAndBaseImage(project,baseImage)

        log.info "alreadyExist=${alreadyExist}"
        if(alreadyExist && alreadyExist.checkDeleted()) {
            //Image was previously deleted, restore it
            def jsonNewData = JSON.parse(alreadyExist.encodeAsJSON())
            jsonNewData.deleted = null
            Command c = new EditCommand(user: currentUser)
            return executeCommand(c,alreadyExist,jsonNewData)
        } else {
            synchronized (this.getClass()) {
                Command c = new AddCommand(user: currentUser)
                return executeCommand(c,null,json)
            }
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(ImageInstance domain, def jsonNewData) {
        securityACLService.check(domain.container(),READ)
        securityACLService.check(jsonNewData.project,Project,READ)
        securityACLService.checkFullOrRestrictedForOwner(domain.container(),domain.user)
        securityACLService.checkisNotReadOnly(domain.container())
        securityACLService.checkisNotReadOnly(jsonNewData.project,Project)
        def attributes = JSON.parse(domain.encodeAsJSON())
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)

        def res = executeCommand(c,domain,jsonNewData)
        ImageInstance imageInstance = res.object

        Double resolution = JSONUtils.getJSONAttrDouble(attributes,"resolution",null)

        boolean resolutionUpdated = resolution != imageInstance.resolution

        if(resolutionUpdated) {
            def annotations;
            annotations = UserAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                userAnnotationService.update(it, json)
            }

            annotations = AlgoAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                algoAnnotationService.update(it, json)
            }

            annotations = ReviewedAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                reviewedAnnotationService.update(it, json)
            }
        }
        return res
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ImageInstance domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
//        securityACLService.check(domain.container(),READ)
//        securityACLService.checkisNotReadOnly(domain.container())
//        SecUser currentUser = cytomineService.getCurrentUser()
//        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
//        return executeCommand(c,domain,null)

        //We don't delete domain, we juste change a flag
        securityACLService.checkFullOrRestrictedForOwner(domain.container(),domain.user)
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.instanceFilename == null ? domain.baseImage?.originalFilename : domain.instanceFilename, domain.project.name]
    }

    def deleteDependentAlgoAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        taskService.updateTask(task,task? "Delete ${AlgoAnnotation.countByImage(image)} annotations from algo":"")
        AlgoAnnotation.findAllByImage(image).each {
            algoAnnotationService.delete(it,transaction)
        }
    }

    def deleteDependentReviewedAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        taskService.updateTask(task,task? "Delete ${ReviewedAnnotation.countByImage(image)} reviewed annotations":"")
        ReviewedAnnotation.findAllByImage(image).each {
            reviewedAnnotationService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentUserAnnotation(ImageInstance image,Transaction transaction, Task task = null) {
        taskService.updateTask(task,task? "Delete ${UserAnnotation.countByImage(image)} annotations created by user":"")
        UserAnnotation.findAllByImage(image).each {
            userAnnotationService.delete(it,transaction,null,false)
        }
    }
//
//    def deleteDependentUserPosition(ImageInstance image,Transaction transaction, Task task = null) {
//        UserPosition.findAllByImage(image).each {
//            it.delete()
//        }
//    }
//
//    def deleteDependentAnnotationIndex(ImageInstance image,Transaction transaction, Task task = null) {
//        AnnotationIndex.findAllByImage(image).each {
//            it.delete()
//         }
//    }
//
//    def deleteDependentImageSequence(ImageInstance image, Transaction transaction, Task task = null) {
//        ImageSequence.findAllByImage(image).each {
//            imageSequenceService.delete(it,transaction,null,false)
//        }
//    }
//
//    def deleteDependentNestedImageInstance(ImageInstance image, Transaction transaction,Task task=null) {
//        NestedImageInstance.findAllByParent(image).each {
//            it.delete(flush: true)
//        }
//    }
    def listWithoutGroup(Project project, ImageGroup imageGroup, String sortColumn, String sortDirection, String search) {
        def listout = [];
        ImageSequence.findAllByImageGroup(imageGroup).each{
            listout << it.image.id
        }

        if(listout.size() == 0)
            return list(project, sortColumn, sortDirection, search);


        String abstractImageAlias = "ai"
        String _sortColumn = ImageInstance.hasProperty(sortColumn) ? sortColumn : "created"
        _sortColumn = AbstractImage.hasProperty(sortColumn) ? abstractImageAlias + "." + sortColumn : "created"
        String _search = (search != null && search != "") ? "%"+search+"%" : "%"

        return ImageInstance.createCriteria().list() {
            createAlias("baseImage", abstractImageAlias)
            eq("project", project)
            isNull("parent")
            isNull("deleted")
            fetchMode 'baseImage', FetchMode.JOIN
            not {'in'("id", listout)}
            ilike(abstractImageAlias + ".originalFilename", _search)
            order(_sortColumn, sortDirection)
        }

        // return ImageInstance.findAllByProjectAndIdNotInList(project, listout);
    }

    def listWithoutAnyGroup(Project project,String sortColumn, String sortDirection, String search ){
        def listImageOut = [];
        ImageGroup.findAllByProject(project).each { group ->
            ImageSequence.findAllByImageGroup(group).each{ imageSequence ->
                listImageOut << imageSequence.image.id
            }
        }

        if(listImageOut.size() == 0)
            return list(project, sortColumn, sortDirection, search);


        String abstractImageAlias = "ai"
        String _sortColumn = ImageInstance.hasProperty(sortColumn) ? sortColumn : "created"
        _sortColumn = AbstractImage.hasProperty(sortColumn) ? abstractImageAlias + "." + sortColumn : "created"
        String _search = (search != null && search != "") ? "%"+search+"%" : "%"

        return ImageInstance.createCriteria().list() {
            createAlias("baseImage", abstractImageAlias)
            eq("project", project)
            isNull("parent")
            isNull("deleted")
            fetchMode 'baseImage', FetchMode.JOIN
            not {'in'("id", listImageOut)}
            ilike(abstractImageAlias + ".originalFilename", _search)
            order(_sortColumn, sortDirection)
        }

    }

    private def getDomainAssociatedSearchParameters(ArrayList searchParameters, boolean blinded) {

        for (def parameter : searchParameters){
            if(parameter.field.equals("name") || parameter.field.equals("instanceFilename")){
                parameter.field = blinded ? "blindedName" : "instanceFilename"
            }
            if(parameter.field.equals("numberOfJobAnnotations")) parameter.field = "countImageJobAnnotations"
            if(parameter.field.equals("numberOfReviewedAnnotations")) parameter.field = "countImageReviewedAnnotations"
            if(parameter.field.equals("numberOfAnnotations")) parameter.field = "countImageAnnotations"
        }


        def validParameters = getDomainAssociatedSearchParameters(ImageInstance, searchParameters)

        String abstractImageAlias = "ai"
        String imageInstanceAlias = "ii"
        validParameters.addAll(getDomainAssociatedSearchParameters(AbstractImage, searchParameters).collect {[operator:it.operator, property:abstractImageAlias+"."+it.property, value:it.value]})
        validParameters.addAll(getDomainAssociatedSearchParameters(Mime, searchParameters).collect {[operator:it.operator, property:"mime."+it.property, value:it.value]})

        loop:for (def parameter : searchParameters){
            String property
            switch(parameter.field) {
                case "tag" :
                    property = "tda.tag_id"
                    parameter.values = convertSearchParameter(Long.class, parameter.values)
                    break
                default:
                    continue loop
            }
            validParameters << [operator: parameter.operator, property: property, value: parameter.values]
        }

        if(searchParameters.size() > 0){
            log.debug "The following search parameters have not been validated: "+searchParameters
        }

        validParameters.findAll { it.property.equals("baseImage") }.each {
            it.property = "base_image_id"
            it.value = it.value.id
        }
        return validParameters
    }


}
