package be.cytomine.stats

import be.cytomine.CytomineDomain
import be.cytomine.Exception.ServerException
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.ontology.AlgoAnnotation
import be.cytomine.project.Project
import be.cytomine.social.AnnotationAction
import be.cytomine.social.PersistentImageConsultation
import be.cytomine.social.PersistentProjectConnection
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import groovy.sql.Sql


@Transactional
class StatsService extends ModelService {

    def secUserService
    def projectService
    def imageServerService
    def dataSource

    def total(def domain){
        return ["total" : CytomineDomain.isAssignableFrom(domain)? domain.countByDeletedIsNull() : domain.count];
    }

    def numberOfCurrentUsers(){
        return [total :secUserService.getAllOnlineUsers().size()];
    }

    def numberOfActiveProjects(){
        return [total :projectService.getActiveProjects().size()];
    }

    def mostActiveProjects(){
        return projectService.getActiveProjectsWithNumberOfUsers().max{it.users};
    }

    def statAnnotationTermedByProject(Term term){
        def projects = Project.findAllByOntology(term.ontology)
        def count = [:]
        def percentage = [:]

        //init list
        projects.each { project ->
            count[project.name] = 0
            percentage[project.name] = 0
        }

        projects.each { project ->
            def layers = secUserService.listLayers(project)
            if(!layers.isEmpty()) {
                def annotations = UserAnnotation.createCriteria().list {
                    eq("project", project)
                    inList("user", layers)
                }
                annotations.each { annotation ->
                    if (annotation.terms().contains(term)) {
                        count[project.name] = count[project.name] + 1;
                    }
                }
            }
        }

        def list = []
        count.each {
            list << ["key": it.key, "value": it.value]
        }
        return list
    }

    def statAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){

        String request = "SELECT created " +
                "FROM UserAnnotation " +
                "WHERE project = $project.id " +
                (term ? "AND id IN (SELECT userAnnotation.id FROM AnnotationTerm WHERE term = $term.id) " : "") +
                (startDate ? "AND created > '$startDate'" : "") +
                (endDate ? "AND created < '$endDate'" : "") +
                "ORDER BY created ASC"
        def annotations = UserAnnotation.executeQuery(request)

        def data = aggregateByPeriods(annotations, daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
        if(reverseOrder) {
            return data.reverse()
        }

        return data
    }

    def statAlgoAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){

        String request = "SELECT created " +
                "FROM AlgoAnnotation " +
                "WHERE project = $project.id " +
                (term ? "AND id IN (SELECT annotationIdent FROM AlgoAnnotationTerm WHERE term = $term.id) " : "") +
                (startDate ? "AND created > '$startDate' " : "") +
                (endDate ? "AND created < '$endDate' " : "") +
                "ORDER BY created ASC"
        def annotations = AlgoAnnotation.executeQuery(request)

        def data = aggregateByPeriods(annotations, daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
        if(reverseOrder) {
            return data.reverse()
        }

        return data
    }

    def statReviewedAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){
        // ReviewedAnnotationTerm not mapped => have to use SQL query
        def sql = new Sql(dataSource)
        String request = "SELECT created " +
                "FROM reviewed_annotation " +
                "WHERE project_id = $project.id " +
                (term ? "AND id IN (SELECT reviewed_annotation_terms_id FROM reviewed_annotation_term WHERE term_id = $term.id) " : "") +
                (startDate ? "AND created > '$startDate' " : "") +
                (endDate ? "AND created < '$endDate' " : "") +
                "ORDER BY created ASC"
        def annotations = []

        sql.eachRow(request) {
            annotations << it.created
        }

        def data = aggregateByPeriods(annotations, daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
        if(reverseOrder) {
            return data.reverse()
        }

        return data
    }

    def statUserSlide(Project project){
        def terms = Term.findAllByOntology(project.getOntology())
        if(terms.isEmpty()) {
            return []
        }
        Map<Long, Object> result = new HashMap<Long, Object>()

        //numberOfAnnotationsByUserAndImage[0] = id image, numberOfAnnotationsByUserAndImage[1] = user, numberOfAnnotationsByUserAndImage[2] = number of annotation
        def numberOfAnnotationsByUserAndImage = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.image.id")
                groupProperty("a.user")
                count("a.user")
            }
        }

        //build empty result table
        secUserService.listLayers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.value = 0
            result.put(item.id, item)
        }

        //Fill result table
        numberOfAnnotationsByUserAndImage.each { item ->
            def user = result.get(item[1].id)
            if(user) user.value++;
        }

        return result.values()
    }

    def statTermSlide(Project project){
        Map<Long, Object> result = new HashMap<Long, Object>()
        //Get project term
        def terms = Term.findAllByOntology(project.getOntology())

        //Check if there are user layers
        def userLayers = secUserService.listLayers(project)
        if(terms.isEmpty() || userLayers.isEmpty()) {
            return []
        }

        def annotationsNumber = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            inList("user", userLayers)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.image.id")
                groupProperty("term.id")
                count("term.id")
            }
        }

        //build empty result table
        terms.each { term ->
            def item = [:]
            item.id = term.id
            item.key = term.name
            item.value = 0
            item.color = term.color
            result.put(item.id, item)
        }

        //Fill result table
        annotationsNumber.each { item ->
            def term = item[1]
            result.get(term).value++;
        }
        return result.values()
    }

    def statTerm(Project project, Date startDate, Date endDate, boolean leafsOnly) {
        //Get leaf term (parent term cannot be map with annotation)
        def terms = leafsOnly ? project.ontology.leafTerms() : project.ontology.terms()

        //Get the number of annotation for each term
        String req = "SELECT at.term.id, count(at.id) " +
                "FROM AnnotationTerm at " +
                "INNER JOIN at.userAnnotation a " +
                "WHERE a.project = $project.id " +
                (startDate ? "AND at.created > '$startDate' " : "") +
                (endDate ? "AND at.created < '$endDate' " : "") +
                "GROUP BY at.term.id"
        def numberOfAnnotationForEachTerm = AnnotationTerm.executeQuery(req)

        def stats = [:]
        def color = [:]
        def ids = [:]
        def idsRevert = [:]
        def list = []

        //build empty result table
        terms.each { term ->
            stats[term.name] = 0
            color[term.name] = term.color
            ids[term.name] = term.id
            idsRevert[term.id] = term.name
        }

        //init result table with data
        numberOfAnnotationForEachTerm .each { result ->
            def name = idsRevert[result[0]]
            if(name) stats[name]=result[1]
        }

        //fill results stats tabble
        stats.each {
            list << ["id": ids.get(it.key), "key": it.key, "value": it.value, "color": color.get(it.key)]
        }
        return list
    }

    def statUserAnnotations(Project project){
        Map<Long, Object> result = new HashMap<Long, Object>()

        //Get project terms
        def terms = Term.findAllByOntology(project.getOntology())
        if(terms.isEmpty()) {
            responseSuccess([])
            return
        }

        //compute number of annotation for each user and each term
        def nbAnnotationsByUserAndTerms = AnnotationTerm.createCriteria().list {
            inList("term", terms)
            join("userAnnotation")
            createAlias("userAnnotation", "a")
            projections {
                eq("a.project", project)
                groupProperty("a.user.id")
                groupProperty("term.id")
                count("term")
            }
        }

        //build empty result table
        secUserService.listUsers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.terms = []
            terms.each { term ->
                def t = [:]
                t.id = term.id
                t.name = term.name
                t.color = term.color
                t.value = 0
                item.terms << t
            }
            result.put(user.id, item)
        }

        //complete stats for each user and term
        nbAnnotationsByUserAndTerms.each { stat ->
            def user = result.get(stat[0])
            if(user) {
                user.terms.each {
                    if (it.id == stat[1]) {
                        it.value = stat[2]
                    }
                }
            }
        }
        return result.values()

    }

    def statUser(Project project, Date startDate, Date endDate){
        Map<Long, Object> result = new HashMap<Long, Object>()
        //compute number of annotation for each user
        def userAnnotations = UserAnnotation.createCriteria().list {
            eq("project", project)
            if(startDate) {
                gt("created", startDate)
            }
            if(endDate) {
                lt("created", endDate)
            }
            join("user")  //right join possible ? it will be sufficient
            projections {
                countDistinct('id')
                groupProperty("user.id")
            }
        }

        //build empty result table
        secUserService.listLayers(project).each { user ->
            def item = [:]
            item.id = user.id
            item.key = user.firstname + " " + user.lastname
            item.username = user.username
            item.value = 0
            result.put(item.id, item)
        }

        //fill result table with number of annotation
        userAnnotations.each { item ->
            def user = result.get(item[1])
            if(user) user.value = item[0]
        }

        return result.values()
    }

    def statUsedStorage(){
        def spaces = imageServerService.list()
        if(spaces.isEmpty())
            throw new ServerException("No Image Server found!")

        Long used = 0
        Long available = 0
        spaces.each {
            def size = imageServerService.storageSpace(it)
            used += size.used
            available += size.available
        }

        Long total = used + available
        return [total : total, available : available, used : used, usedP:(double)(used/total)]
    }

    def statConnectionsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate=false) {
        def connections = PersistentProjectConnection.createCriteria().list(sort: "created", order: "asc") {
            eq("project", project)
            if(startDate) {
                gt("created", startDate)
            }
            if(endDate) {
                lt("created", endDate)
            }
            projections {
                property("created")
            }
        }

        return this.aggregateByPeriods(sortPageResult(connections), daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
    }

    def statImageConsultationsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate=false) {
        def consultations = PersistentImageConsultation.createCriteria().list(sort: "created", order: "asc") {
            eq("project", project)
            if(startDate) {
                gt("created", startDate)
            }
            if(endDate) {
                lt("created", endDate)
            }
            projections {
                property("created")
            }
        }

        return aggregateByPeriods(sortPageResult(consultations), daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
    }

    def statAnnotationActionsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate=false) {
        def actions = AnnotationAction.createCriteria().list(sort: "created", order: "asc") {
            eq("project", project)
            if(startDate) {
                gt("created", startDate)
            }
            if(endDate) {
                lt("created", endDate)
            }
            projections {
                property("created")
            }
        }

        return aggregateByPeriods(sortPageResult(actions), daysRange, startDate ?: project.created, endDate ?: new Date(), accumulate)
    }

    // Temporary HACK due to incorrect handling of sorting when properties are used - fixed in grails v3
    // (see https://stackoverflow.com/questions/20188249/grails-projections-ignoring-sort-order-with-mongodb )
    private def sortPageResult(result) {
        return result.toArray(new Date[0]).sort{it.getTime()}
    }

    private def aggregateByPeriods(def creationDates, int daysRange,  Date startDate, Date endDate, boolean accumulate) {
        def data = []
        int nbItems = creationDates.size()
        int count = 0
        int idx = 0

        Date current = startDate
        Long endTime = endDate.getTime()
        Calendar cal = Calendar.getInstance()

        //for each period (of duration daysRange), compute the number of items
        while(current.getTime() <= endTime) {
            def item = [:]
            item.date = current.getTime()

            //add a new step
            cal.setTime(current)
            cal.add(Calendar.DATE, daysRange)
            current = cal.getTime()

            if(!accumulate) {
                count = 0
            }

            while(idx < nbItems && creationDates[idx].getTime() < current.getTime()) {
                idx++
                count++
            }

            item.endDate = endTime ? Math.min(current.getTime(), endTime) : current.getTime()
            item.size = count
            data << item
        }

        return data
    }

}
