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
import be.cytomine.Exception.ConstraintException
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import static groovyx.net.http.ContentType.*

@Transactional
class ImageGroupHDF5Service  extends  ModelService{

    def securityACLService
    def imageGroupService
    def imageSequenceService
    def cytomineMailService
    def abstractImageService


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
        json.filenames = storage_base_path   + "/" + currentUser.id + "/" + JSONUtils.getJSONAttrStr(json, 'filenames')
        json.user = currentUser.id
        def email =  User.read(currentUser.id)
        def resultDB
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
        def imagesFilenames = imagesSequenceList.collect{
            def absoluteTiffPath =  it.image.baseImage.getAbsolutePath()
            def tiffPath = it.image.baseImage.path
            def basePath = absoluteTiffPath - tiffPath
            basePath + it.image.baseImage.filename
        }
        def filename = JSONUtils.getJSONAttrStr(json, 'filenames')

        if(imagesFilenames.size() > 0) {
            synchronized (this.getClass()) { //We add the group in db only if we have image to convert
                Command c = new AddCommand(user: currentUser)
                resultDB = executeCommand(c,null,json)
            }
            callIMSConversion(currentUser, imagesFilenames, filename)
        }
        else {
            throw new ConstraintException("You need to have at least one Image Sequence in your Image Group to convert it")
        }




        resultDB

    }

    private void callIMSConversion(SecUser currentUser, def imagesFilenames, String filename){
        String imageServerURL = grailsApplication.config.grails.imageServerURL[0]
        String url = "/multidim/convert.json"
        log.info "$imageServerURL" + url
        def http = new HTTPBuilder(imageServerURL)
        http.request(Method.POST) {
            uri.path = url
            requestContentType = URLENC
            body = [user: currentUser.id, files: imagesFilenames, dest: filename]
            response.success = { resp ->  log.info  "Imagegroup convert launch success ${resp.statusLine}" }
        }
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
