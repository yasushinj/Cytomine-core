package be.cytomine.ontology

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.AnnotationDomain
import be.cytomine.Exception.ServerException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.processing.Job
import be.cytomine.processing.structure.ConfusionMatrix
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.UserJob
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task
import grails.converters.JSON

import static org.springframework.security.acls.domain.BasePermission.READ

class AlgoAnnotationTermService extends ModelService {

    static transactional = true
    def cytomineService
    def commandService
    def securityACLService

    def currentDomain() {
        return AlgoAnnotationTerm
    }

    def list(AnnotationDomain annotation) {
        securityACLService.check(annotation.container(),READ)
        AlgoAnnotationTerm.findAllByAnnotationIdentAndDeletedIsNull(annotation.id)
    }

    def list(Project project) {
        return AlgoAnnotationTerm.findAllByProjectAndDeletedIsNull(project)
    }

    def count(Job job) {
        securityACLService.check(job.container(),READ)
        long total = 0
        List<UserJob> users = UserJob.findAllByJob(job)
        users.each {
            total = total + AlgoAnnotationTerm.countByUserJobAndDeletedIsNull(it)
        }
        total
    }

    def read(AnnotationDomain annotation, Term term, UserJob userJob) {
        securityACLService.check(annotation.container(),READ)
        AlgoAnnotationTerm result
        if (userJob) {
            result = AlgoAnnotationTerm.findWhere(annotationIdent: annotation.id, term: term, userJob: userJob)
        } else {
            result = AlgoAnnotationTerm.findWhere(annotationIdent: annotation.id, term: term)
        }

        if(result) checkDeleted(result)
        result
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {
        AnnotationDomain annotation
        try {
            annotation = AnnotationDomain.getAnnotationDomain(json.annotation)
        } catch(Exception e) {
            annotation = AnnotationDomain.getAnnotationDomain(json.annotationIdent)
        }
        securityACLService.check(annotation.project,READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        SecUser creator = SecUser.read(json.user)
        if (!creator)
            json.user = currentUser.id

        Command command = new AddCommand(user: currentUser)
        return executeCommand(command,null,json)
    }

    def addAlgoAnnotationTerm(Long annotationID, Long idTerm, Long idUser, SecUser currentUser, Transaction transaction){
        def json = JSON.parse("{annotationIdent: $annotationID, term: $idTerm,user: $idUser}")
        return executeCommand(new AddCommand(user: currentUser, transaction: transaction), null,json)
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(AlgoAnnotationTerm domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser, transaction: transaction)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def afterAdd(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('algoannotation')
    }

    def afterDelete(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('algoannotation')
    }

    def afterUpdate(def domain, def response) {
        response.data['annotation'] = response.data.userannotation
        response.data.remove('algoannotation')
    }


    def getStringParamsI18n(def domain) {
        return [domain.term.name, domain.retrieveAnnotationDomain().id, domain.userJob]
    }

    /**
     * Compute Success rate AVG for all algo annotation term of userJob
     */
    double computeAVG(def userJob) {
        log.info "userJob=" + userJob?.id

        def nbTermTotal = AlgoAnnotationTerm.createCriteria().count {
            eq("userJob", userJob)
            isNotNull("expectedTerm")
            isNull("deleted")
        }
        if (nbTermTotal == 0) {
            throw new ServerException("UserJob has no algo-annotation-term!")
        }

        def nbTermCorrect = AlgoAnnotationTerm.createCriteria().count {
            eq("userJob", userJob)
            isNotNull("term")
            isNotNull("expectedTerm")
            eqProperty("term", "expectedTerm")
            isNull("deleted")
        }
        return (double) (nbTermCorrect / nbTermTotal)
    }

    /**
     * Compute Success rate AVG for all algo annotation term of userJob and a term
     */
    double computeAVG(def userJob, Term term) {

        def nbTermTotal = AlgoAnnotationTerm.createCriteria().count {
            eq("userJob", userJob)
            eq("expectedTerm", term)
            isNull("deleted")
        }
        if (nbTermTotal == 0) {
            throw new ServerException("UserJob has no algo-annotation-term!")
        }

        def nbTermCorrect = AlgoAnnotationTerm.createCriteria().count {
            eq("userJob", userJob)
            eq("expectedTerm", term)
            eqProperty("term", "expectedTerm")
            isNull("deleted")
        }
        return (double) (nbTermCorrect / nbTermTotal)
    }

    /**
     * Compute suceess rate AVG per term for a userjob
     * if AVG success for Term x = 90% && Term y = 20%,
     * Return will be ((90+20)/2)%
     */
    double computeAVGAveragePerClass(def userJob) {
        def terms = userJob.job.project.ontology.terms()
        double total = 0
        int nbTermNotEmpty = 0

        terms.each { term ->
            def nbTermCorrect = AlgoAnnotationTerm.createCriteria().count {
                eq("userJob", userJob)
                eq("expectedTerm", term)
                eqProperty("term", "expectedTerm")
                isNull("deleted")
            }
            def nbTermTotal = AlgoAnnotationTerm.createCriteria().count {
                eq("userJob", userJob)
                eq("expectedTerm", term)
                isNull("deleted")
            }

            if (nbTermTotal != 0) {
                total = total + (double) (nbTermCorrect / nbTermTotal)
                nbTermNotEmpty++
            }
        }
        double avg = 0
        if (nbTermNotEmpty != 0)
            avg = (double) (total / nbTermNotEmpty)
        return avg
    }

    /**
     * Compute full Confusion Matrix for all terms from projectTerms and all algo annotation term from userJob
     */
    ConfusionMatrix computeConfusionMatrix(List<Term> projectTerms, def userJob) {
        Collections.sort(projectTerms);
        def projectTermsId = projectTerms.collect {it.id + ""}
        ConfusionMatrix matrix = new ConfusionMatrix(projectTermsId);
        def algoAnnotationsTerm = AlgoAnnotationTerm.findAllByUserJobAndDeletedIsNull(userJob);
        algoAnnotationsTerm.each {
            if (it.term && it.expectedTerm) matrix.incrementEntry(it.expectedTerm?.id + "", it.term?.id + "")
        }
        return matrix
    }

    /**
     * Get AlgoAnnotationTerm prediction success AVG evolution for all userJobs and a project
     * For each userJobs, map its date with the success rate of its result
     */
    def listAVGEvolution(List<UserJob> userJobs, Project project) {
        listAVGEvolution(userJobs, project, null)
    }

    /**
     * Get AlgoAnnotationTerm prediction success AVG evolution for all userJobs for a specific term
     * For each userJobs, map its date with the success rate of its result
     * if term is null, compute success rate for all term
     */
    def listAVGEvolution(List<UserJob> userJobs, Project project, Term term) {

        if (userJobs.isEmpty()) {
            return null
        }

        def data = []
        int count = 0;
        def annotations = null;

        if (!term) {
            annotations = UserAnnotation.executeQuery("select a.created from UserAnnotation a where a.project = ?  order by a.created desc", [project])
        }
        else {
            annotations = UserAnnotation.executeQuery("select b.created from UserAnnotation b where b.project = ? and b.id in (select x.userAnnotation.id from AnnotationTerm x where x.term = ? and x.deleted is null)  order by b.created desc", [project, term])
        }
        userJobs.each {
            def userJobIt = it
            def item = [:]
            Date stopDate = userJobIt.created

            //we browse userjob (oreder desc creation).
            //For each userjob, we browse annotation (oreder desc creation) and we count the number of annotation
            //that are most recent than userjob, we subsitute this count from annotation.list()
            //=> not needed to browse n times annotations list, juste 1 time.
            while (count < annotations.size()) {
                if (annotations.get(count) < stopDate) break;
                count++;
            }
            item.size = annotations.size() - count;

            try {
                item.date = userJobIt.created.getTime()
                if (term)
                    item.avg = computeAVG(userJobIt, term)
                else {
                    if (userJobIt.rate == -1 && userJobIt.job.status == Job.SUCCESS) {
                        userJobIt.rate = computeAVG(userJobIt)
                        userJobIt.save(flush: true)
                    }
                    item.avg = userJobIt.rate
                }

                data << item
            } catch (Exception e) {
                log.info e
            }
        }
        return data
    }
}
