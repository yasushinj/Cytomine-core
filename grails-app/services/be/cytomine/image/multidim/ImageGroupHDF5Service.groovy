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

package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.hdf5.input.BuildHyperSpectralFile
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.transaction.Transactional
import groovy.json.JsonSlurper

@Transactional
class ImageGroupHDF5Service  extends  ModelService{

    def securityACLService
    def imageGroupService
    def imageSequenceService
    def cytomineMailService


    def currentDomain() {
        return ImageGroupHDF5
    }

    def getStringParamsI18n(def domain) {
         return [domain.id, domain.group.name]
    }

    ImageGroupHDF5 get(def id){
        ImageGroupHDF5.get(id)
    }

    ImageGroupHDF5 read(def id){
        ImageGroupHDF5.read(id)
    }

    def list(){
        ImageGroupHDF5.list();
    }

    def getByGroup(ImageGroup group){
        ImageGroupHDF5.findByGroup(group);
    }

    def add(def json){
        //Add in db (maybe this should come last)
       // securityACLService.check(json.project,Project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        String storage_base_path = grailsApplication.config.storage_path
        String root = storage_base_path   + "/" + currentUser.id;
        json.filenames = root + "/" + JSONUtils.getJSONAttrStr(json, 'filenames')
        json.user = currentUser.id
        def email =  User.read(currentUser.id)
        def resultDB
        synchronized (this.getClass()) {
            Command c = new AddCommand(user: currentUser)
            resultDB = executeCommand(c,null,json)
        }

        def group = JSONUtils.getJSONAttrInteger(json,'group',null)

        //Convert the list in h5
        //First get all the ImageSequence from the imageGroup
        ImageGroup imageGroup = imageGroupService.read(group)
        def imagesSequenceList = []
        if (imageGroup)  {
            imagesSequenceList = imageSequenceService.list(imageGroup)
        }
        else {
            return ; //Todo throw
        }


        imagesSequenceList.sort{a,b -> a.channel <=> b.channel}
        def imagesFilenames = imagesSequenceList.collect{ it.image.baseImage.filename}
        if(imagesFilenames.size() > 0){
            def filename = JSONUtils.getJSONAttrStr(json, 'filenames')

            Thread.start{
                BuildHyperSpectralFile h5builder = new BuildHyperSpectralFile(filename, root, imagesFilenames)
                h5builder.createFile(4)
                cytomineMailService.send(
                        cytomineMailService.NO_REPLY_EMAIL,
                        [email.getEmail()] as String[],
                        "",
                        "Your conversion into HDF5 is finished",
                        "The file has been created with success and can now be used")
            }

        }

        resultDB

    }

    def retrieve(def ids) {
        def id = Integer.parseInt(ids + "")
        CytomineDomain domain = currentDomain().get(id)
        if (!domain) {
            throw new ObjectNotFoundException("${currentDomain().class} " + id + " not found")
        }
        return domain
    }

    def delete(ImageGroupHDF5 domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
      //  securityACLService.check(domain.container(),READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser,transaction:transaction)
        return executeCommand(c,domain,null)
    }
}
