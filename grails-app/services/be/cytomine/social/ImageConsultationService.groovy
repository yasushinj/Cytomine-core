package be.cytomine.social

import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.sql.AnnotationListing
import be.cytomine.sql.UserAnnotationListing
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
        Long image = JSONUtils.getJSONAttrLong(json,"imageinstance")
        closeLastImageConsultation(user.id, image, new Date())
        PersistentImageConsultation consultation = new PersistentImageConsultation()
        consultation.user = user.id
        consultation.image = image.id
        consultation.project = image.project.id
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

    private void closeLastImageConsultation(Long user, Long image, Date before){
        PersistentImageConsultation consultation = PersistentImageConsultation.findByUserAndImageAndCreatedLessThan(user, image, before, [sort: 'created', order: 'desc', max: 1])

        //first consultation
        if(consultation == null) return;

        //last consultation already closed
        if(consultation.time) return;

        fillImageConsultation(consultation, before)

        consultation.save(flush : true, failOnError : true)
    }
    def annotationListingService
    private void fillImageConsultation(PersistentImageConsultation consultation, Date before = new Date()){
        Date after = consultation.created;

        // collect {it.created.getTime} is really slow. I just want the getTime of PersistentConnection
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def positions = db.persistentUserPosition.aggregate(
                [$match: [project: consultation.project, user: consultation.user, image: consultation.image, $and : [[created: [$gte: after]],[created: [$lte: before]]]]],
                [$sort: [created: 1]],
                [$project: [dateInMillis: [$subtract: ['$created', new Date(0L)]]]]
        );

        def continuousConnections = positions.results().collect { it.dateInMillis }

        //we calculated the gaps between connections to identify the period of non activity
        def continuousConnectionIntervals = []

        continuousConnections.inject(consultation.created.time) { result, i ->
            continuousConnectionIntervals << (i-result)
            i
        }

        consultation.time = continuousConnectionIntervals.split{it < 15000}[0].sum()

        AnnotationListing al = new UserAnnotationListing()
        al.project = consultation.project
        al.user = consultation.user
        al.image = consultation.image
        al.beforeThan = before
        al.afterThan = after

        // count created annotations
        consultation.countCreatedAnnotations = annotationListingService.listGeneric(al).size()
    }


}
