package be.cytomine.social

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import org.joda.time.DateTime

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class ImageConsultationService extends ModelService {

    def securityACLService
    def dataSource
    def mongo
    def noSQLCollectionService

    def add(def json){

        SecUser user = cytomineService.getCurrentUser()
        ImageInstance image = ImageInstance.read(JSONUtils.getJSONAttrLong(json,"imageinstance",0))
        PersistentImageConsultation consultation = new PersistentImageConsultation()
        consultation.user = user
        consultation.image = image
        consultation.project = image.project
        consultation.mode = JSONUtils.getJSONAttrStr(json,"mode",true)
        consultation.created = new Date()
        consultation.imageName = image.getFileName()
        consultation.insert(flush:true) //don't use save (stateless collection)

        return consultation
    }

    def lastImageOfUsersByProject(Project project){

        securityACLService.check(project,READ)

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

        def results = []
        def images = db.persistentImageConsultation.aggregate(
                [$match:[project : project.id]],
                [$sort : [created:-1]],
                [$group : [_id : '$user', created : [$max :'$created'], image : [$first: '$image'], imageName : [$first: '$imageName'], user : [$first: '$user']]]);


        images.results().each {
            results << [user: it["_id"], created : it["created"], image : it["image"], imageName: it["imageName"]]
        }
        return results
    }
}
