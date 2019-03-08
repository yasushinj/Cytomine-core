package be.cytomine.image

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.laboratory.Sample
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.AttachedFile
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class AbstractImageService extends ModelService {

    static transactional = true

    def commandService
    def cytomineService
    def imagePropertiesService
    def transactionService
    def storageService
    def groupService
    def imageInstanceService
    def attachedFileService
    def currentRoleServiceProxy
    def securityACLService
    def storageAbstractImageService
    def imageServerProxyService
    def projectService

    def currentDomain() {
        return AbstractImage
    }

    AbstractImage read(def id) {
        AbstractImage abstractImage = AbstractImage.read(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
//            if(!hasRightToReadAbstractImageWithProject(abstractImage) && !hasRightToReadAbstractImageWithStorage(abstractImage)) {
//                throw new ForbiddenException("You don't have the right to read or modity this resource! ${abstractImage} ${id}")
//            }
        }
        abstractImage
    }

    AbstractImage get(def id) {
        AbstractImage abstractImage = AbstractImage.get(id)
        if(abstractImage) {
            securityACLService.checkAtLeastOne(abstractImage, READ)
//            if(!hasRightToReadAbstractImageWithProject(abstractImage) && !hasRightToReadAbstractImageWithStorage(abstractImage)) {
//                throw new ForbiddenException("You don't have the right to read or modity this resource! ${abstractImage} ${id}")
//            }
        }
        abstractImage
    }

    boolean hasRightToReadAbstractImageWithProject(AbstractImage image) {
        if(currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)) return true
        List<ImageInstance> imageInstances = ImageInstance.findAllByBaseImage(image)
        List<Project> projects = imageInstances.collect{it.project}
        for(Project project : projects) {
            if(project.hasACLPermission(project,READ)) return true
        }
        return false
    }

    boolean hasRightToReadAbstractImageWithStorage(AbstractImage image) {
        if(currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)) return true
        List<Storage> storages = StorageAbstractImage.findAllByAbstractImage(image).collect{it.storage}
        for(Storage storage : storages) {
            if(storage.hasACLPermission(storage,READ)) return true
        }
        return false
    }

    // TODO: remove ? not meaningful
    def list(Project project) {
        securityACLService.check(project,READ)
        ImageInstance.createCriteria().list {
            eq("project", project)
            projections {
                groupProperty("baseImage")
            }
        }
    }

    def list(SecUser user, Project project = null) {
        List<AbstractImage> images
        if(currentRoleServiceProxy.isAdminByNow(user)) {
            images = AbstractImage.list()
        } else {
            List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser)
            return AbstractImage.createCriteria().list {
                createAlias("uploadedFile", "uf")
                inList("uf.storages", storages)
                isNull("deleted")
            }
//            List<AbstractImage> images = StorageAbstractImage.findAllByStorageInList(storages).collect{it.abstractImage}
//            return images.findAll{!it.deleted}
        }
        if(project) {
            TreeSet<Long> inProjectImagesId = new TreeSet<>(ImageInstance.findAllByProjectAndDeletedIsNull(project).collect{it.baseImage.id})

            def result = []

            for(AbstractImage image : images){
                def data = AbstractImage.getDataFromDomain(image)
                data.inProject = (inProjectImagesId.contains(image.id))
                result << data
            }
            return result
        }
        return images
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        def currentUser = cytomineService.currentUser
        securityACLService.checkUser(currentUser)

        if (json.uploadedFile) {
            UploadedFile uploadedFile = UploadedFile.read(json.uploadedFile as Long)
            if (uploadedFile.status != UploadedFile.TO_DEPLOY) {
                // throw new Error()
            }
        }

        return executeCommand(new AddCommand(user: currentUser), null, json)
    }

    def beforeAdd(def domain) {
        log.info "Create a new Sample"
        long timestamp = new Date().getTime()
        Sample sample = new Sample(name : timestamp.toString() + "-" + domain?.uploadedFile?.getOriginalFilename())
        domain?.sample = sample

        //TODO: mime type
//        def ext = uploadedFile.getExt()
//        Mime mime = Mime.findByMimeType(mimeType)
//        if (!mime) {
//            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
//            mimeType = mimeTypesMap.getContentType(uploadedFile.getAbsolutePath())
//            mime = new Mime(extension: ext, mimeType : mimeType)
//            mime.save(failOnError: true)
//        }
    }

    def afterAdd(def domain, def response) {
        log.info "Extract image properties"
        imagePropertiesService.clear(domain)
        imagePropertiesService.populate(domain)
        imagePropertiesService.extractUseful(domain)

        log.info "Add to projects, stored in uploaded file."
        //TODO: to improve to handle AbstractSlice -> SliceInstance
        def currentUser = cytomineService.currentUser
        domain.uploadedFile.projects?.each { projectId ->
            Project project = projectService.read(projectId)
            ImageInstance imageInstance = new ImageInstance( baseImage : domain, project:  project, user :currentUser)
            imageInstanceService.add(JSON.parse(imageInstance.encodeAsJSON()))
        }

//        Collection<Storage> storages = []
//        uploadedFile.getStorages()?.each {
//            storages << storageService.read(it)
//        }
//
//            storages.each { storage ->
//                storageAbstractImageService.add(JSON.parse(JSONUtils.toJSONString([storage: storage.id, abstractimage: abstractImage.id])))
//            }
//
//        json.storage.each { storageID ->
//            Storage storage = storageService.read(storageID)
//            securityACLService.check(storage,WRITE)
//            //CHECK WRITE ON STORAGE
//            StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
//            sai.save(flush:true,failOnError: true)
//        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(AbstractImage image,def jsonNewData) throws CytomineException {
        securityACLService.checkAtLeastOne(image,WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()
        def res = executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
//        AbstractImage abstractImage = res.object
//
//        if(jsonNewData.storage) {
//            StorageAbstractImage.findAllByAbstractImage(abstractImage).each { storageAbstractImage ->
//                securityACLService.check(storageAbstractImage.storage,WRITE)
//                def sai = StorageAbstractImage.findByStorageAndAbstractImage(storageAbstractImage.storage, abstractImage)
//                sai.delete(flush:true)
//            }
//            jsonNewData.storage.each { storageID ->
//                Storage storage = storageService.read(storageID)
//                securityACLService.check(storage,WRITE)
//                StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
//                sai.save(flush:true,failOnError: true)
//            }
//        }
        return res
    }

    def getUploaderOfImage(long id){
        AbstractImage img = read(id)
        return img?.uploadedFile?.user
    }

    /**
     * Check if some instances of this image exists and are still active
     */
    def isUsed(def id) {
        AbstractImage domain = AbstractImage.read(id);
        boolean usedByImageInstance = ImageInstance.findAllByBaseImageAndDeletedIsNull(domain).size() != 0
        boolean usedByNestedFile = CompanionFile.findAllByImage(domain).size() != 0

        return usedByImageInstance || usedByNestedFile
    }

    /**
     * Returns the list of all the unused abstract images
     */
    def listUnused(User user) {
        def result = []
        def abstractList = list(user);
        abstractList.each {
            image ->
                if(!isUsed(image.id)) result << image;
        }
        return result;
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AbstractImage domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        securityACLService.checkAtLeastOne(domain,WRITE)

        if (!isUsed(domain.id)) {
            def jsonNewData = JSON.parse(domain.encodeAsJSON())
            jsonNewData.deleted = new Date().time
            SecUser currentUser = cytomineService.getCurrentUser()
            Command c = new EditCommand(user: currentUser)
            c.delete = true
            return executeCommand(c,domain,jsonNewData)
        } else{
            def instances = ImageInstance.findAllByBaseImageAndDeletedIsNull(domain)
            throw new ForbiddenException("Abstract Image has instances in active projects : "+instances.collect{it.project.name}.join(",")
                    +" with the following names : "+instances.collect{it.instanceFilename}.unique().join(","));
        }
    }

    def uploadedFileService
    def deleteFile(AbstractImage ai){
//        UploadedFile uf = UploadedFile.findByImage(ai)
//        uploadedFileService.delete(uf)
//
//        while(uf.parent){
//            if(UploadedFile.countByParentAndDeletedIsNull(uf.parent) == 0){
//                uploadedFileService.delete(uf.parent)
//                uf = uf.parent
//            } else {
//                break
//            }
//        }
    }

    /**
     * Get all image servers for an image id
     */
    def imageServers(def id) {
        AbstractImage image = read(id)
        return [imageServersURLs : [image?.uploadedFile?.imageServer?.url]]
    }

    def getMainUploadedFile(AbstractImage abstractImage) {
        def file = abstractImage.uploadedFile
        while(file.parent) {
            file = file.parent
        }

        return file
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.originalFilename]
    }

    def deleteDependentImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        def images = ImageInstance.findAllByBaseImageAndDeletedIsNull(ai);
        if(!images.isEmpty()) {
            throw new WrongArgumentException("You cannot delete this image, it has already been insert in projects " + images.collect{it.project.name})
        }
    }

    def deleteDependentAttachedFile(AbstractImage ai, Transaction transaction,Task task=null) {
        AttachedFile.findAllByDomainIdentAndDomainClassName(ai.id, ai.class.getName()).each {
            attachedFileService.delete(it,transaction,null,false)
        }
    }

    def deleteDependentNestedFile(AbstractImage ai, Transaction transaction,Task task=null) {
        //TODO: implement this with command (nestedFileService should be create)
        CompanionFile.findAllByAbstractImage(ai).each {
            it.delete(flush: true)
        }
    }

    def deleteDependentStorageAbstractImage(AbstractImage ai, Transaction transaction,Task task=null) {
        //TODO: implement this with command (storage abst image should be create)
        StorageAbstractImage.findAllByAbstractImage(ai).each {
            storageAbstractImageService.delete(it,transaction,null)
        }
    }

    def deleteDependentNestedImageInstance(AbstractImage ai, Transaction transaction,Task task=null) {
        NestedImageInstance.findAllByBaseImage(ai).each {
            it.delete(flush: true)
        }
    }
}
