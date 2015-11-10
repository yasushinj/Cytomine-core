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
class UserPositionService extends ModelService {

    def securityACLService
    def dataSource
    def mongo
    def noSQLCollectionService

    def add(def json){

        SecUser user = cytomineService.getCurrentUser()
        ImageInstance image = ImageInstance.read(JSONUtils.getJSONAttrLong(json,"image",0))
        PersistentUserPosition position = new PersistentUserPosition()
        position.user = user
        position.image = image
        position.project = image.project
        def polygon = [
                [JSONUtils.getJSONAttrDouble(json,"topLeftX",-1),JSONUtils.getJSONAttrDouble(json,"topLeftY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"topRightX",-1),JSONUtils.getJSONAttrDouble(json,"topRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomRightX",-1),JSONUtils.getJSONAttrDouble(json,"bottomRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomLeftX",-1),JSONUtils.getJSONAttrDouble(json,"bottomLeftY",-1)]
        ]
        position.location = polygon
        position.zoom = JSONUtils.getJSONAttrInteger(json,"zoom",-1)
        position.created = new Date()
        position.updated = position.created
        position.imageName = image.getFileName()
        position.insert(flush:true) //don't use save (stateless collection)

        LastUserPosition lastUserPosition = new LastUserPosition()
        UserPosition.copyProperties(position,lastUserPosition)
        lastUserPosition.insert(flush:true)
        return lastUserPosition
    }

    def lastPositionByUser(ImageInstance image, SecUser user){
        def userPositions = LastUserPosition.createCriteria().list(sort: "created", order: "desc", max: 1) {
            eq("user", user)
            eq("image", image)
        }
        def result = (userPositions.size() > 0) ? userPositions[0] : []
        return result
    }

    def listOnlineUsersByImage(ImageInstance image){
        securityACLService.check(image,READ)
        DateTime thirtySecondsAgo = new DateTime().minusSeconds(30)
        def userPositions = LastUserPosition.createCriteria().list(sort: "created", order: "desc") {
            eq("image", image)
            or {
                gt("created", thirtySecondsAgo.toDate())
                gt("updated", thirtySecondsAgo.toDate())
            }
        }.collect { it.user.id }.unique()
        def result = ["users": userPositions.join(",")]
        return result
    }

    def listLastUserPositionsByProject(Project project){

        securityACLService.check(project,READ)
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        DateTime thirtySecondsAgo = new DateTime().minusSeconds(30)

        def result = db.lastUserPosition.aggregate(
                [$match : [ project : project.id, created:[$gt:thirtySecondsAgo.toDate()]]],
                [$project:[user:1,"image":1]],
                [$group : [_id : [ user: '$user', image: '$image']]]
        )
        return result.results().collect{it['_id']}.collect{[it["image"],it["user"]]}
    }
}
