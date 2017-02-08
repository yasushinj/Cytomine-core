package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import net.sf.json.JSONObject

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class ImageGroupHDF5Service  extends  ModelService{

    def securityACLService


    def currentDomain() {
        return ImageGroupHDF5
    }

    def getStringParamsI18n(def domain) {
         return [domain.group.name, domain.filenames]
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
        //Add in db
        println "JSPN ADD " + json
       // securityACLService.check(json.project,Project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        json.user = currentUser.id
        synchronized (this.getClass()) {
            Command c = new AddCommand(user: currentUser)
            executeCommand(c,null,json)
        }

        //Convert the list in h5
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
