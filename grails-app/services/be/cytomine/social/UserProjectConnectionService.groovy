package be.cytomine.social

import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.MapReduceCommand
import grails.transaction.Transactional

@Transactional
class UserProjectConnectionService extends ModelService {

    def securityACLService
    def dataSource
    def mongo
    def noSQLCollectionService

    def add(def json){

        SecUser user = cytomineService.getCurrentUser()
        Project project = Project.read(JSONUtils.getJSONAttrLong(json,"project",0))
        PersistentProjectConnection connection = new PersistentProjectConnection()
        connection.user = user
        connection.project = project
        connection.created = new Date()
        connection.insert(flush:true) //don't use save (stateless collection)
        return connection
    }

    def lastConnectionInProject(Project project){
        def connection = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc"/*, max: 1*/) {
            distinct("user")
            eq("project", project)
        }
        def result = (connection.size() > 0) ? connection : []
        return result
    }

    def getConnectionByUserAndProject(User user, Project project, boolean all){
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
            // db.persistentProjectConnection.mapReduce(function(){emit(this.user, this.project);}, function(key,values){return values.length}, { query :{project:ID_PROJECT}, out : {inline:1}})

            def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

            String mapFunction = "function(){emit(this.user, this.project);}"
            String reduceFunction = "function(key,values){return values.length}"
            DBObject query = new BasicDBObject()
            query.put("project",new BasicDBObject('$eq',project.id))

            def out = db.persistentProjectConnection.mapReduce(
                    mapFunction,
                    reduceFunction,
                    "tmpCollectionName", // try null
                    MapReduceCommand.OutputType.INLINE,
                    query
            )

            def mResult = out.results()
            result = []
            out.drop()
            for(int i =0;i<mResult.size();i++){
                result << [user:mResult.get(i)._id,frequency:(int) mResult.get(i).value]
            }
        }
        return result
    }
}
