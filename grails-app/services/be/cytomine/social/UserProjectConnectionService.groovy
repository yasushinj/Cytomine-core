package be.cytomine.social

import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class UserProjectConnectionService extends ModelService {

    def securityACLService
    def dataSource
    def mongo
    def noSQLCollectionService

    def add(def json){

        SecUser user = cytomineService.getCurrentUser()
        Project project = Project.read(JSONUtils.getJSONAttrLong(json,"project",0))
        securityACLService.check(project,READ)
        PersistentProjectConnection connection = new PersistentProjectConnection()
        connection.user = user
        connection.project = project
        connection.created = new Date()
        connection.insert(flush:true) //don't use save (stateless collection)
        return connection
    }

    def lastConnectionInProject(Project project){
        securityACLService.check(project,READ)
        def connection = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc"/*, max: 1*/) {
            distinct("user")
            eq("project", project)
        }
        def result = (connection.size() > 0) ? connection : []
        return result
    }

    def getConnectionByUserAndProject(User user, Project project, boolean all){
        securityACLService.check(project,READ)
        def result;
        if(all){
            def connections = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc") {
                eq("user", user)
                eq("project", project)
            }
            result = (connections.size() > 0) ? connections : []
        }else {
            def connection = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc", max: 1) {
                eq("user", user)
                eq("project", project)
            }
            result = (connection.size() > 0) ? connection[0] : []
        }
        return result
    }

    def numberOfConnectionsByUserAndProject(User user ,Project project){

        securityACLService.check(project,READ)
        def result;
        if(user) {
            def mResult = PersistentProjectConnection.createCriteria().get/*(sort: "created", order: "desc")*/ {
                eq("user", user)
                eq("project", project)
                projections {
                    rowCount()
                }
            }
            result = [[user : user.id,frequency: (int) mResult]]
        } else{
            // what we want
            // db.persistentProjectConnection.aggregate([{$match: {project : ID_PROJECT}}, { $group : { _id : {user:"$user"} , number : { $sum : 1 }}}])

            def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

            result = db.persistentProjectConnection.aggregate(
                    [$match : [ project : project.id]],
                    [$group : [_id : [ user: '$user'], "frequency":[$sum:1]]]
            )

            def usersWithPosition = []
            result.results().each {
                def userId = it["_id"]["user"]
                def frequency = it["frequency"]
                usersWithPosition << [user: userId, frequency: frequency]
            }
            result = usersWithPosition
        }
        return result
    }
}
