package be.cytomine.image

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

import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.ForbiddenException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.AttachedFile
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class AbstractImageService extends ModelService {

    static transactional = false

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

    def currentDomain() {
        return AbstractImage
    }

    AbstractImage read(def id) {
        AbstractImage abstractImage = AbstractImage.read(id)
        if(abstractImage) {
            //securityACLService.checkAtLeastOne(abstractImage, READ)
            if(!hasRightToReadAbstractImageWithProject(abstractImage) && !hasRightToReadAbstractImageWithStorage(abstractImage)) {
                throw new ForbiddenException("You don't have the right to read or modity this resource! ${abstractImage} ${id}")
            }
        }
        abstractImage
    }

    AbstractImage get(def id) {
        AbstractImage abstractImage = AbstractImage.get(id)
        if(abstractImage) {
            //securityACLService.checkAtLeastOne(abstractImage, READ)
            if(!hasRightToReadAbstractImageWithProject(abstractImage) && !hasRightToReadAbstractImageWithStorage(abstractImage)) {
                throw new ForbiddenException("You don't have the right to read or modity this resource! ${abstractImage} ${id}")
            }
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

    def list(Project project) {
        securityACLService.check(project,READ)
        ImageInstance.createCriteria().list {
            eq("project", project)
            projections {
                groupProperty("baseImage")
            }
        }
    }

    def list(SecUser user) {
        if(currentRoleServiceProxy.isAdminByNow(user)) {
            return AbstractImage.list()
        } else {
            List<Storage> storages = securityACLService.getStorageList(cytomineService.currentUser)
            List<AbstractImage> images = StorageAbstractImage.findAllByStorageInList(storages).collect{it.abstractImage}
            return images.findAll{!it.deleted}
        }
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) throws CytomineException {
        transactionService.start()
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new AddCommand(user: currentUser)
        def res = executeCommand(c,null,json)
        //AbstractImage abstractImage = retrieve(res.data.abstractimage)
        AbstractImage abstractImage = res.object

        json.storage.each { storageID ->
            Storage storage = storageService.read(storageID)
            securityACLService.check(storage,WRITE)
            //CHECK WRITE ON STORAGE
            StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
            sai.save(flush:true,failOnError: true)
        }
        imagePropertiesService.extractUseful(abstractImage)
        abstractImage.save(flush : true)
        //Stop transaction

        return res
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    def update(AbstractImage image,def jsonNewData) throws CytomineException {
        securityACLService.checkAtLeastOne(image,WRITE)
        transactionService.start()
        SecUser currentUser = cytomineService.getCurrentUser()
        def res = executeCommand(new EditCommand(user: currentUser), image,jsonNewData)
        AbstractImage abstractImage = res.object

        if(jsonNewData.storage) {
            StorageAbstractImage.findAllByAbstractImage(abstractImage).each { storageAbstractImage ->
                securityACLService.check(storageAbstractImage.storage,WRITE)
                def sai = StorageAbstractImage.findByStorageAndAbstractImage(storageAbstractImage.storage, abstractImage)
                sai.delete(flush:true)
            }
            jsonNewData.storage.each { storageID ->
                Storage storage = storageService.read(storageID)
                securityACLService.check(storage,WRITE)
                StorageAbstractImage sai = new StorageAbstractImage(storage:storage,abstractImage:abstractImage)
                sai.save(flush:true,failOnError: true)
            }
        }
        return res
    }

    def getUploaderOfImage(long id){
        AbstractImage img = AbstractImage.get(id)
        if(!img){
            return null
        }
        return UploadedFile.findByImage(img).user
    }

    /**
     * Check if some instances of this image exists and are still active
     */
    def isUsed(def id) {
        AbstractImage domain = AbstractImage.read(id);
        boolean usedByImageInstance = ImageInstance.findAllByBaseImageAndDeletedIsNull(domain).size() != 0
        boolean usedByNestedFile = NestedFile.findAllByAbstractImage(domain).size() != 0

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
        UploadedFile uf = UploadedFile.findByImage(ai)
        uploadedFileService.delete(uf)

        while(uf.parent){
            if(UploadedFile.countByParentAndDeletedIsNull(uf.parent) == 0){
                uploadedFileService.delete(uf.parent)
                uf = uf.parent
            } else {
                break
            }
        }
    }

    /**
     * Get all image servers for an image id
     */
    def imageServers(def id) {
        AbstractImage image = read(id)
        def urls = []
        for (imageServerStorage in image.getImageServersStorage()) {
            urls << [imageServerStorage.getZoomifyUrl(), image.getPath()].join(File.separator) + "/" //+ "&mimeType=${uploadedFile.mimeType}"
        }


        return [imageServersURLs : urls]
    }

    def getMainUploadedFile(AbstractImage abstractImage) {
        List<UploadedFile> uploadedfiles = UploadedFile.findAllByImage(abstractImage)

        if(uploadedfiles.size()==1) {
            return uploadedfiles.first()
        } else {
            //get the first uploadedfile...
            return uploadedfiles.find{ main ->
                //...that is not present in parent (must be the 'last' child)
                uploadedfiles.find{ second -> second.parent?.id==main.id}==null;
            }
        }

//
//        if (uploadedfile?.parent && !uploadedfile?.parent?.ext?.equals("png") && !uploadedfile?.parent?.ext?.equals("jpg")) {
//            return uploadedfile.parent
//        }
//        else return uploadedfile

    }

    def downloadURI(AbstractImage abstractImage, boolean downloadParent) {
        List<UploadedFile> files = UploadedFile.findAllByImage(abstractImage)
        UploadedFile file = files.size() == 1 ? files[0] : files.find{it.parent!=null}

        if (downloadParent) {
            while(file.parent) {
                file = file.parent
            }
        }

        String fif = file?.absolutePath
        if (fif) {
            String imageServerURL = abstractImage.getRandomImageServerURL()
            return "$imageServerURL/image/download?fif=$fif&mimeType=${abstractImage.mimeType}"
        } else {
            return null
        }

    }

    /**
     * Get thumb image URL
     */
    def thumb(AbstractImage abstractImage, def parameters, boolean refresh = false) {
        return imageServerProxyService.thumb(abstractImage, parameters)

//        AttachedFile attachedFile = AttachedFile.findByDomainIdentAndFilename(id, url)
//        if (!attachedFile || refresh) {
//            String imageServerURL = abstractImage.getRandomImageServerURL()
//            log.info "$imageServerURL"+url
//            byte[] imageData = new URL("$imageServerURL"+url).getBytes()
//            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData))
//            attachedFileService.add(url, imageData, abstractImage.id, AbstractImage.class.getName())
//            return bufferedImage
//        } else {
//            return ImageIO.read(new ByteArrayInputStream(attachedFile.getData()))
//        }
    }

    def getAvailableAssociatedImages(AbstractImage abstractImage) {
        return imageServerProxyService.associated(abstractImage)
    }

    def getAssociatedImage(AbstractImage abstractImage, def parameters) {
        return imageServerProxyService.label(abstractImage, parameters)

//        AttachedFile attachedFile = AttachedFile.findByDomainIdentAndFilename(abstractImage.id, url)
//        if (attachedFile) {
//            return ImageIO.read(new ByteArrayInputStream(attachedFile.getData()))
//        } else {
//            String imageServerURL = abstractImage.getRandomImageServerURL()
//            byte[] imageData = new URL("$imageServerURL"+url).getBytes()
//            BufferedImage bufferedImage =  ImageIO.read(new ByteArrayInputStream(imageData))
//            attachedFileService.add(url, imageData, abstractImage.id, AbstractImage.class.getName())
//            return bufferedImage
//        }
    }

    def checkCropParameters(params) {
        def parameters = [:]
        parameters.format = params.format
        parameters.location = params.location
        parameters.geometry = params.geometry
        parameters.complete = params.boolean('complete')
        parameters.maxSize = params.int('maxSize')
        parameters.zoom = params.int('zoom')
        parameters.increaseArea = params.double('increaseArea')
        parameters.type = imageServerProxyService.checkType(params)
        parameters.colormap = params.colormap
        parameters.inverse = params.boolean('inverse')
        parameters.contrast = params.double('contrast')
        parameters.gamma = params.double('gamma')
        parameters.bits = (params.bits == "max") ? "max" : params.int('bits')
        parameters.alpha = params.int('alpha')
        parameters.strokeWidth = params.int('strokeWidth')
        parameters.strokeColor = params.strokeColor
        parameters.jpegQuality = params.int('jpegQuality')
        return parameters
    }

    def crop(AbstractImage abstractImage, def params) {
        def parameters = checkCropParameters(params)
        return imageServerProxyService.crop(abstractImage, parameters)
    }

    def window(AbstractImage abstractImage, def params, def urlOnly = false) {
        def parameters = checkCropParameters(params)
        parameters.x = params.double('x')
        parameters.y = params.double('y')
        parameters.w = params.double('w')
        parameters.h = params.double('h')
        parameters.drawScaleBar = params.boolean('drawScaleBar')
        parameters.resolution = params.double('resolution')
        parameters.magnification = params.double('magnification')
        parameters.withExterior = params.boolean('withExterior', false)
        return imageServerProxyService.window(abstractImage, parameters, urlOnly)
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
        NestedFile.findAllByAbstractImage(ai).each {
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
