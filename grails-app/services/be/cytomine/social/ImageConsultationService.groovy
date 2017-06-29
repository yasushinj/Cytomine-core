package be.cytomine.social

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional

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

    def getImagesOfUsersByProjectBetween(User user, Project project, Date after = null, Date before = null){
        return getImagesOfUsersByProjectBetween(user.id, project.id, after, before)
    }

    def getImagesOfUsersByProjectBetween(Long userId, Long projectId, Date after = null, Date before = null){
        def results = [];
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def match;
        if(after && before){
            match = [$match:[$and : [[created: [$lt: before]], [created: [$gte: after]], [project : projectId], [user:userId]]]]
        } else if(after){
            match = [$match:[project : projectId, user:userId, created: [$gte: after]]]
        } else if(before){
            match = [$match:[project : projectId, user:userId, created: [$lt: before]]]
        } else {
            match = [$match:[project : projectId, user:userId]]
        }

        def images = db.persistentImageConsultation.aggregate(
                match,
                [$sort : [created:-1]]
        );
        images.results().each {
            results << [user: it["user"], project: it["project"], created : it["created"], image : it["image"], imageName: it["imageName"], mode: it["mode"]]
        }
        return results
    }
}
