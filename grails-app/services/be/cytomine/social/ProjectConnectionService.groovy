package be.cytomine.social

import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@Transactional
class ProjectConnectionService extends ModelService {

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
        securityACLService.check(project,WRITE)

        def results = []
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def connection = db.persistentProjectConnection.aggregate(
                [$match:[project : project.id]],
                [$group : [_id : '$user', created : [$max :'$created']]])

        connection.results().each {
            results << [user: it["_id"], created : it["created"]]
        }
        return results
    }

    def getConnectionByUserAndProject(User user, Project project, boolean all){
        securityACLService.check(project,WRITE)
        def result;
        Long test = System.currentTimeMillis();
        Long test2
        if(all){
            def connections = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc") {
                eq("user", user)
                eq("project", project)
            }

            result = []

            if(connections.size() >= 1) {

                Date after = connections[connections.size()-1].created;
                def continuousConnections = PersistentConnection.createCriteria().list(sort: "created", order: "desc") {
                    eq("user", user)
                    eq("project", project)
                    ge("created", after)
                }
                continuousConnections = (continuousConnections.size() > 0) ? continuousConnections : []
                 test = System.currentTimeMillis();

                //merging
                int beginJ = continuousConnections.size()-1;
                for(int i=connections.size()-1;i>=1;i--){
                    //def connectionDate = connections[i].created;
                    def nextConnectionDate = connections[i-1].created;
                    int j = beginJ;
                    while(j>=0 && continuousConnections[j].created < nextConnectionDate){
                        j--;
                    }
                    long time = continuousConnections[j+1].created.getTime() - continuousConnections[beginJ].created.getTime();
                    beginJ = j;

                    result << [id : connections[i].id, created: connections[i].created, user:connections[i].user.id,
                               project : connections[i].project.id, time:time]
                }
                // TODO prendre la lastConnexion pour user & project dans les persistent pour avoir une idée de la toute dernière connection.
                result << [id : connections[0].id, created: connections[0].created, user:connections[0].user.id,
                           project : connections[0].project.id, time:0]
                 test2 = System.currentTimeMillis();
                result.reverse();
            }
        }else {
            def connection = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc", max: 1) {
                eq("user", user)
                eq("project", project)
            }
            result = (connection.size() > 0) ? connection[0] : []
        }

        println "perf test"
        println test2-test

        return result
    }

    def numberOfConnectionsByProjectAndUser(Project project, User user = null){

        securityACLService.check(project,WRITE)
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

    def numberOfConnectionsByProjectOrderedByHourAndDays(Project project, Long afterThan = null, User user = null){

        securityACLService.check(project,WRITE)
        // what we want
        //db.persistentProjectConnection.aggregate( {"$match": {$and: [{project : ID_PROJECT}, {created : {$gte : new Date(AFTER) }}]}}, { "$project": { "created": {  "$subtract" : [  "$created",  {  "$add" : [  {"$millisecond" : "$created"}, { "$multiply" : [ {"$second" : "$created"}, 1000 ] }, { "$multiply" : [ {"$minute" : "$created"}, 60, 1000 ] } ] } ] } }  }, { "$project": { "y":{"$year":"$created"}, "m":{"$month":"$created"}, "d":{"$dayOfMonth":"$created"}, "h":{"$hour":"$created"}, "time":"$created" }  },  { "$group":{ "_id": { "year":"$y","month":"$m","day":"$d","hour":"$h"}, time:{"$first":"$time"},  "total":{ "$sum": 1}  }});
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())

        def result;
        def match
        def projection1 = [$project : [ created : [$subtract:['$created', [$add : [[$millisecond : '$created'], [$multiply : [[$second : '$created'], 1000]], [$multiply : [[$minute : '$created'], 60*1000]] ]]]]]]
        def projection2 = [$project : [ y : [$year:'$created'], m : [$month:'$created'], d : [$dayOfMonth:'$created'], h : [$hour:'$created'], time : '$created']]
        def group = [$group : [_id : [ year: '$y', month: '$m', day: '$d', hour: '$h'], "time":[$first:'$time'], "frequency":[$sum:1]]]
        if(afterThan) {
            match = [$match : [$and : [[ created : [$gte : new Date(afterThan)]],[ project : project.id]]]]
        } else {
            match = [$match : [ project : project.id]]
        }

        result = db.persistentProjectConnection.aggregate(
                match,
                projection1,
                projection2,
                group
        )


        def connections = []
        result.results().each {

            // TODO evolve when https://jira.mongodb.org/browse/SERVER-6310 is resolved
            // as we groupBy hours in UTC, the GMT + xh30 have problems.

            /*def year = it["_id"]["year"]
            def month = it["_id"]["month"]
            def day = it["_id"]["day"]
            def hour = it["_id"]["hour"]*/
            def time = it["time"]
            def frequency = it["frequency"]


            /*Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.MONTH, month);*/

            connections << [/*year: year, month: month, day: day, hour: hour, */time : time, frequency: frequency]
        }
        result = connections
        return result
    }

}
