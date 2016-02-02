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

    def getConnectionByUserAndProject(User user, Project project, Integer limit, Integer offset){
        securityACLService.check(project,WRITE)
        def result = []

        def connections = PersistentProjectConnection.createCriteria().list(sort: "created", order: "desc") {
            eq("user", user)
            eq("project", project)
            firstResult(offset)
            maxResults(limit)
        }

        if(connections.size() == 0) return result;

        Date after = connections[connections.size() - 1].created;

        // collect {it.created.getTime} is really slow. I just want the getTime of PersistentConnection
        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def continuousConnectionsResult = db.persistentConnection.aggregate(
                [$match: [project: project.id, user: user.id, created: [$gte: after]]],
                [$sort: [created: -1]],
                [$project: [dateInMillis: [$subtract: ['$created', new Date(0L)]]]]
        );
        def continuousConnections = continuousConnectionsResult.results().collect { it.dateInMillis }

        if(continuousConnections.size() == 0) {
            connections.each {
                result << [id: it.id, created: it.created, user: user.id, project: project.id, time: 0]
            }
            return result
        }

        def connectionsTime = connections.collect { it.created.getTime() }

        //merging
        int beginJ = continuousConnections.size() - 1;

        for (int i = connections.size() - 1; i >= 1; i--) {
            def nextConnectionDate = connectionsTime[i - 1];

            int j = beginJ;
            while (j >= 0 && continuousConnections[j] < nextConnectionDate) {
                j--;
            }

            // if j = beginJ, short time connection (<20sec). Avoid j+1 > size of array.
            long time = (j == beginJ) ? 0 : (continuousConnections[j + 1] - continuousConnections[beginJ]);
            if (time < 0) time = 0;
            beginJ = j >= 0 ? j : 0;

            result << [id: connections[i].id, created: connections[i].created, user: user.id,
                       project: project.id, time: time]
        }
        long time = continuousConnections[0] - continuousConnections[beginJ];
        result << [id: connections[0].id, created: connections[0].created, user: user.id,
                   project: project.id, time: time]
        result = result.reverse();

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
