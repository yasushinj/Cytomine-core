package be.cytomine.processing

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
import be.cytomine.image.server.RetrievalServer
import be.cytomine.ontology.Ontology
import be.cytomine.ontology.Term
import be.cytomine.ontology.UserAnnotation
import be.cytomine.ontology.UserAnnotationService
import be.cytomine.project.Project
import be.cytomine.test.HttpClient
import be.cytomine.utils.ValueComparator
import grails.converters.JSON
import groovy.sql.Sql
import org.apache.http.NoHttpResponseException
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.model.NotFoundException

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static grails.async.Promises.task
import static org.springframework.security.acls.domain.BasePermission.READ

/**
 * Retrieval is a server that can provide similar pictures of a request picture
 * It can suggest term for an annotation thx to similar picture
 */
class ImageRetrievalService {

    static transactional = false

    def currentRoleServiceProxy
    def cytomineService
    def dataSource
    def abstractImageService


    public void indexImageAsync(URL url,String id, String storage, Map<String,String> properties) {
        log.info "Index image async:$id $storage"

        if(!RetrievalServer.list().isEmpty()) {
            RetrievalServer server = RetrievalServer.list().get(0)
            def process = task {
                println "index - starting"
                indexImage(ImageIO.read(url),id,storage,properties,server.url,server.username,server.password)
                println "index - ending"
            }
        } else {
            log.info "No retrieval server found"
        }
    }

    /**
     * Search similar annotation and best term for an annotation
     * @param project project which will provide annotation learning set
     * @param annotation annotation to search
     * @return [annotation: #list of similar annotation#, term: #map with best term#]
     */
    def listSimilarAnnotationAndBestTerm(Project project, AnnotationDomain annotation) throws Exception {
        if(RetrievalServer.list().isEmpty()) {
            throw new ServerException("No retrieval found!")
        }
        log.info "Search similarities for annotation ${annotation.id}"
        def data = [:]

        if(annotation.location.numPoints<3) {
            data.term = []
            return data
        }

        //find project used for retrieval
        List<Long> projectSearch = []
        if(project.retrievalDisable) {
            //retrieval not available for this project, just return empty result
            return data
        } else if(project.retrievalAllOntology) {
            //retrieval available, look in index for all user annotation for the project with same ontologies
            projectSearch=getAllProjectId(annotation.project.ontology)
        } else {
            //retrieval avaliable, but only looks on a restricted project list
            projectSearch=project.retrievalProjects.collect {it.id}
        }

        //Only keep projects available for the current user
        boolean isAdmin = currentRoleServiceProxy.isAdminByNow(cytomineService.currentUser)
        projectSearch = projectSearch.findAll{ Project.read(it).checkPermission(READ,isAdmin)}

        log.info "search ${annotation.id} on projects ${projectSearch}"
        log.info "log.addannotation2"
        //Get similar annotation
        def similarAnnotations = loadAnnotationSimilarities(annotation,projectSearch)
        data.annotation = similarAnnotations

        log.info "log.addannotation3"
        //Get all term from project
        def projectTerms = project.ontology.terms()
        def bestTermNotOrdered = getTermMap(projectTerms)
        ValueComparator bvc = new ValueComparator(bestTermNotOrdered);

        log.info "log.addannotation4"
        //browse annotation
        similarAnnotations.each { similarAnnotation ->
            //for each annotation, browse annotation terms
            def terms = similarAnnotation.terms()
            terms.each { term ->
                if (projectTerms.contains(term)) {
                    Double oldValue = bestTermNotOrdered.get(term)
                    //for each term, add similarity value
                    bestTermNotOrdered.put(term, oldValue + similarAnnotation.similarity)
                }
            }
        }
        log.info "log.addannotation5"

        //Sort [term:rate] by rate (desc)
        TreeMap<Term, Double> bestTerm = new TreeMap(bvc);
        bestTerm.putAll(bestTermNotOrdered)
        def bestTermList = []
        log.info "log.addannotation6"
        //Put them in a list
        for (Map.Entry<Term, Double> entry: bestTerm.entrySet()) {
            Term term = entry.getKey()
            term.rate = entry.getValue()
            bestTermList << term
        }
        data.term = bestTermList
        return data
    }

    private def getTermMap(List<Term> termList) {
        def map = [:]
        termList.each {
            map.put(it, 0d)
        }
        map
    }
    /**
     * Get all project id for all project with this ontology
     * @param ontology Ontology filter
     * @return Project id list
     */
    public List<Long> getAllProjectId(Ontology ontology) {
        //better for perf than Project.findByOntology(ontology).collect {it.id}
        String request = "SELECT p.id FROM project p WHERE ontology_id="+ontology.id
        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request) {
            data << it[0]
        }
        try {
            sql.close()
        }catch (Exception e) {}
        return data
    }


    private def loadAnnotationSimilarities(AnnotationDomain searchAnnotation,List<Long> projectSearch) {
        log.info "get similarities for userAnnotation " + searchAnnotation.id + " on " + projectSearch
        if(!RetrievalServer.list().isEmpty()) {
            RetrievalServer server = RetrievalServer.list().get(0)
            def cropUrl = searchAnnotation.urlImageServerCrop(abstractImageService)
            def responseJSON = doRetrievalSearch(server.url+"/api/searchUrl",server.username,server.password,searchAnnotation.id,cropUrl,projectSearch.collect{it+""})
            def result =  readRetrievalResponse(searchAnnotation,responseJSON.data)
            log.info "result=$result"
            return result
        } else {
            log.info "No retrieval server found"
        }



    }



    public def doRetrievalSearch(String url, String username, String password, BufferedImage image,List<String> storages) {

        url = url+"?max=30&storages=${storages.join(";")}"

        HttpClient client = new HttpClient()

        log.info "url=$url"
        log.info "username=$username password=$password"

        client.connect(url,username,password)

        MultipartEntity entity = createEntityFromImage(image)

        client.post(entity)

        String response = client.getResponseData()
        int code = client.getResponseCode()
        log.info "code=$code response=$response"
        def json = JSON.parse(response)

        return json
    }

    public def doRetrievalSearch(String url, String username, String password, Long id,String imageURL,List<String> storages) {

        url = url+"?max=30&id=$id&url=${URLEncoder.encode(imageURL)}&storages=${storages.join(";")}"

        HttpClient client = new HttpClient()

        log.info "url=$url"

        client.connect(url,username,password)

        client.post("")

        String response = client.getResponseData()
        int code = client.getResponseCode()
        log.info "code=$code response=$response"
        def json = JSON.parse(response)

        return json
    }


    private def readRetrievalResponse(AnnotationDomain searchAnnotation,def responseJSON) {
        def data = []
        for (int i = 0; i < responseJSON.length(); i++) {
            def annotationjson = responseJSON.get(i)
            try {
                UserAnnotation annotation = UserAnnotation.read(annotationjson.id)
                if (annotation && annotation.id != searchAnnotation.id) {
                    annotation.similarity = new Double(annotationjson.similarities)
                    data << annotation
                }
            }
            catch (AccessDeniedException ex) {log.info "User cannot have access to this userAnnotation"}
            catch (NotFoundException ex) {log.info "User cannot have access to this userAnnotation"}
        }
        return data
    }



    /**
     * Get missing annotation
     */
    public void indexMissingAnnotation() {

        RetrievalServer server = RetrievalServer.findByDeletedIsNull()
        if(server==null) {
            throw new ServerException("No retrieval found!")
        }

        //Get indexed resources
        List<Long> resources = getIndexedResource()
        log.info "Size of indexed resources : " + resources.size()
        Set<Long> ressourcesSet = new HashSet<Long>(resources)
        //Check if each annotation is well indexed
        def annotations = UserAnnotationService.extractAnnotationForRetrieval(dataSource)
        int i = 0
        def data = []
        int count = 0;
        int totalSize = annotations.size();
        int limit = 1000;
        int indexFirstAnnot;

        while(i < totalSize) {
            def annotation = annotations[i];
            log.info "Annotation "+(i+1)+"/" + totalSize
            if (!ressourcesSet.contains(annotation.id)) {
                log.debug "Annotation $annotation.id IS NOT INDEXED"
                try {

                    def cropUrl = AnnotationDomain.getAnnotationDomain(annotation.id).urlImageServerCrop(abstractImageService)
                    if(data.size() == 0) {
                        indexFirstAnnot = i;
                    }
                    data << [id:annotation.id,storage:annotation.container,url:cropUrl]
                    count++;
                    if(count >= limit) {
                        callIndexFullOnRetrieval(server, data);
                        count = 0;
                        data = [];
                    }
                } catch (NoHttpResponseException e) {
                    log.error "Retrieval throw a NoHttpResponseException with $limit items"
                    log.info "Retry with smaller sets"
                    i = indexFirstAnnot;
                    limit /=2;
                    data = [];
                    count = 0;
                } catch (Exception e) {log.error e}
            } else {
                log.debug "Annotation $annotation.id IS INDEXED"
            }
            i++
        }

        callIndexFullOnRetrieval(server, data)

    }

    private void  callIndexFullOnRetrieval(RetrievalServer server, def data) {
        log.info "Size of resources to index : " + data.size()
        String jsonData = (data as JSON).toString(true)
        log.info jsonData.substring(0,Math.min(500,jsonData.length()-1))

        log.info "Server $server!"
        String url = server.url + "/api/index/full"
        HttpClient client = new HttpClient()
        client.connect(url,server.username,server.password)
        client.post(jsonData)
        String response = client.getResponseData()
        int code = client.getResponseCode()
        log.info "code $code!"
        log.info "response $response!"
    }


//    /**
//     * Get missing annotation
//     */
//    public void indexMissingAnnotation() {
//        //Get indexed resources
//        List<Long> resources = getIndexedResource()
//        //Check if each annotation is well indexed
//        def annotations = userAnnotationService.listLightForRetrieval()
//        int i = 1
//        annotations.each { annotation ->
//            log.debug "Annotation $i/" + annotations.size()
//            if (!resources.contains(annotation.id)) {
//                log.debug "Annotation $annotation.id IS NOT INDEXED"
//                try {
//                    def cropUrl = annotation.urlImageServerCrop(abstractImageService)
//                    indexImageSync(
//                            ImageIO.read(new URL(cropUrl)),
//                            annotation.id+"",
//                            annotation.project+"",
//                            [:]
//                    )
//
//                } catch (Exception e) {log.error e}
//            } else {
//                log.debug "Annotation $annotation.id IS INDEXED"
//            }
//            i++
//        }
//    }

    /**
     * Get all annotation indexed from retrieval server
     */
    private List<Long> getIndexedResource() {
        RetrievalServer server = RetrievalServer.findByDeletedIsNull()
        log.info "server is $server"
        String URL = server.url+"/api/images"
        log.info "URL is $URL"
        List json = JSON.parse(getGetResponse(URL,server.username,server.password))
        List<Long> resources = new ArrayList<Long>()
        json.each { image ->
            log.debug "resource=" + Long.parseLong(image.id)
            resources.add(Long.parseLong(image.id))
        }
        resources
    }

    private String getGetResponse(String URL, String username, String password) {
        HttpClient client = new HttpClient();
        client.connect(URL,username,password);
        client.get()
        String response = client.getResponseData()
        client.disconnect();
        return response
    }

























    public void indexImage(BufferedImage image,String id, String storage, Map<String,String> properties, String url, String username, String password) {
        if(!RetrievalServer.list().isEmpty()) {
//            RetrievalServer server = RetrievalServer.list().get(0)
            log.info "Index to retrieval"
            doRetrievalIndex(url+"/api/images",username,password,image,id,storage,properties)
        } else {
            log.info "No retrieval server found"
        }
    }

    public def doRetrievalIndex(String url, String username, String password, BufferedImage image,String id, String storage, Map<String,String> properties) {
        try {
            if(RetrievalServer.list().isEmpty()) {
                throw new ServerException("No retrieval found!")
            }
            List<String> keys = []
            List<String> values = []
            properties.each {
                keys << it.key
                values << it.value
            }

            url = url+"?id=$id&storage=$storage&keys=${keys.join(";")}&values=${values.join(";")}"

            HttpClient client = new HttpClient()

            log.info "url=$url"

            client.connect(url,username,password)

            MultipartEntity entity = createEntityFromImage(image)

            client.post(entity)

            String response = client.getResponseData()
            int code = client.getResponseCode()
            log.info "code=$code response=$response"
            return [code:code,response:response] }
        catch (Exception e) {
            log.info "Unable to index resource"
            log.error e
        }
    }

    public MultipartEntity createEntityFromImage(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        MultipartEntity myEntity = new MultipartEntity();
        myEntity.addPart("file", new ByteArrayBody(imageInByte, "file"));
        return myEntity
    }
}
