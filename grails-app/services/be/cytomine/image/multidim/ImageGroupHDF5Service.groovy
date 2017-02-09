package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.hdf5.input.BuildFile
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import net.sf.json.JSONObject

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class ImageGroupHDF5Service  extends  ModelService{

    def securityACLService
    def imageGroupService
    def imageSequenceService
    def mailService



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
        ImageGroupHDF5.list()
    }

    def add(def json){
        //Add in db (maybe this should come last)
        println "JSPN ADD " + json
       // securityACLService.check(json.project,Project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
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
            println "Not implemented"
            return ; //Todo throw
        }


        def imagesFilenames = imagesSequenceList.collect{ it.image.baseImage.filename}
        if(imagesFilenames.size() > 0){
            def filename = JSONUtils.getJSONAttrStr(json, 'filenames')
            def root = imagesSequenceList.first().image.baseImage.path

            Thread.start{
                BuildFile h5builder = new BuildFile(filename, root, imagesFilenames)
                h5builder.createParr(4)
                mailService.sendMail{
                    to email
                    from "noreply@cytomine.be"
                    subject "Your conversion into HDF5 is finished"
                    body "The file has been created with success and can now be used"
                }
            }

        }

        resultDB

    }

    def retrieve(def ids) {
        println 'found ret'
        def id = Integer.parseInt(ids + "")
        CytomineDomain domain = currentDomain().get(id)
        if (!domain) {
            throw new ObjectNotFoundException("${currentDomain().class} " + id + " not found")
        }
        return domain
    }
}
