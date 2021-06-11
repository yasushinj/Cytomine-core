package be.cytomine.project

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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.ontology.Ontology
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * A project is the main cytomine domain
 * It structure user data
 */
@RestApiObject(name = "Project", description="A project is the main cytomine domain, its a workspace to store images, annotations,...")
class Project extends CytomineDomain implements Serializable {

    static enum EditingMode {
        CLASSIC, RESTRICTED, READ_ONLY
    }

    /**
     * Project name
     */
    @RestApiObjectField(description = "The name of the project")
    String name

    /**
     * Project ontology link
     */
    @RestApiObjectField(description = "The ontology identifier of the project")
    Ontology ontology

    /**
     * Project discipline link
     */
    @RestApiObjectField(description = "The discipline identifier of the project", mandatory = false)
    Discipline discipline


    @RestApiObjectField(description = "Blind mode (if true, image filename are hidden)",mandatory = false)
    boolean blindMode = false

    @RestApiObjectField(description = "Allow contributors to download image instances (if true, image can be downloaded)",mandatory = false)
    boolean areImagesDownloadable = false

    /**
     * Number of projects user annotations
     */
    @RestApiObjectField(description = "Number of annotations created by human user in the project", apiFieldName="numberOfAnnotations", useForCreation = false)
    long countAnnotations

    /**
     * Number of projects algo annotations
     */
    @RestApiObjectField(description = "Number of annotations created by software in the project", apiFieldName="numberOfJobAnnotations",useForCreation = false)
    long countJobAnnotations

    /**
     * Number of projects images
     */
    @RestApiObjectField(description = "Number of image in the project", apiFieldName="numberOfImages",useForCreation = false)
    long countImages

    /**
     * Number of projects reviewed annotations
     */
    @RestApiObjectField(description = "Number of annotations validated in the project", apiFieldName="numberOfReviewedAnnotations",useForCreation = false)
    long countReviewedAnnotations

    /**
     * Flag if retrieval is disable
     * If true, don't suggest similar annotations
     */
    @RestApiObjectField(description = "If true, don't suggest similar annotations")
    boolean retrievalDisable = false

    /**
     * Flag for retrieval search on all ontologies
     * If true, search similar annotations on all project that share the same ontology
     */
    @RestApiObjectField(description = "If true, search similar annotations on all project that share the same ontology",defaultValue = "true")
    boolean retrievalAllOntology = true

    @RestApiObjectField(description = "If true, project is closed",mandatory = false)
    boolean isClosed = false

    @RestApiObjectField(description = "If true, an user (which is not an administrator of the project) cannot see others users layers",mandatory = false)
    boolean hideUsersLayers = false

    @RestApiObjectField(description = "If true, an user (which is not an administrator of the project) cannot see admins layers", mandatory = false)
    boolean hideAdminsLayers = false

    @RestApiObjectField(description = "Editing mode of the current project (read_only, restricted or classic)", mandatory = true)
    EditingMode mode = EditingMode.CLASSIC;

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "users", description = "Users id that will be in the project",allowedType = "list",useForCreation = true, presentInResponse = false),
        @RestApiObjectField(apiFieldName = "admins", description = "Admins id that will be in the project",allowedType = "list",useForCreation = true, presentInResponse = false),
        @RestApiObjectField(apiFieldName = "ontologyName", description = "The ontology name for the project",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "disciplineName", description = "The discipline name for the project",allowedType = "string",useForCreation = false),
        @RestApiObjectField(apiFieldName = "numberOfSlides", description = "The number of samples in the project", allowedType = "long",useForCreation = false),
        @RestApiObjectField(apiFieldName = "retrievalProjects", description = "List all projects id that are used for retrieval search (if retrievalDisable = false and retrievalAllOntology = false)",allowedType = "list",mandatory = false)
    ])


    static transients = ["isAdmin"]


    static belongsTo = [ontology: Ontology]

    static hasMany = [retrievalProjects : Project]

    static constraints = {
        name(maxSize: 150, unique: true, blank: false)
        discipline(nullable: true)
        ontology(nullable: true)
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        Project.withNewSession {
            Project projectAlreadyExist = Project.findByName(name)
            if(projectAlreadyExist && (projectAlreadyExist.id!=id))  throw new AlreadyExistException("Project "+projectAlreadyExist?.name + " already exist!")
        }
    }

    static mapping = {
        id generator: "assigned"
        ontology fetch: 'join'
        discipline fetch: 'join'
        sort "id"
        cache true
    }

    String toString() {
        name
    }

    def countImageInstance() {
        countImages
    }

    def countAnnotations() {
        countAnnotations
    }

    def countJobAnnotations() {
        countJobAnnotations
    }


    def countSamples() {
        //TODO::implement
        return 0
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Project insertDataIntoDomain(def json,def domain = new Project()) {

        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name',true)
        domain.ontology = JSONUtils.getJSONAttrDomain(json, "ontology", new Ontology(), false)
        domain.discipline = JSONUtils.getJSONAttrDomain(json, "discipline", new Discipline(), false)

        domain.retrievalDisable = JSONUtils.getJSONAttrBoolean(json, 'retrievalDisable', false)
        domain.retrievalAllOntology = JSONUtils.getJSONAttrBoolean(json, 'retrievalAllOntology', true)

        domain.blindMode = JSONUtils.getJSONAttrBoolean(json, 'blindMode', false)
        domain.areImagesDownloadable = JSONUtils.getJSONAttrBoolean(json, 'areImagesDownloadable', false)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")
        domain.isClosed = JSONUtils.getJSONAttrBoolean(json, 'isClosed', false)
        domain.mode = EditingMode.CLASSIC;
        if(JSONUtils.getJSONAttrBoolean(json, 'isRestricted', false)) domain.mode = EditingMode.RESTRICTED;
        if(JSONUtils.getJSONAttrBoolean(json, 'isReadOnly', false)) domain.mode = EditingMode.READ_ONLY;

        domain.hideUsersLayers = JSONUtils.getJSONAttrBoolean(json, 'hideUsersLayers', false)
        domain.hideAdminsLayers = JSONUtils.getJSONAttrBoolean(json, 'hideAdminsLayers', false)

        if(!json.retrievalProjects.toString().equals("null")) {
            domain.retrievalProjects?.clear()
            json.retrievalProjects.each { idProject ->
                Long proj = Long.parseLong(idProject.toString())
                //-1 = project himself, project has no id when client send request
                Project projectRetrieval = (proj==-1 ? domain : Project.read(proj))
                if(projectRetrieval) {
                    ((Project)domain).addToRetrievalProjects(projectRetrieval)
                }
            }
        }
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['ontology'] = domain?.ontology?.id
        returnArray['ontologyName'] = domain?.ontology?.name
        returnArray['discipline'] = domain?.discipline?.id
        returnArray['blindMode'] = (domain?.blindMode != null &&  domain?.blindMode)
        returnArray['areImagesDownloadable'] = (domain?.areImagesDownloadable != null &&  domain?.areImagesDownloadable)
        returnArray['disciplineName'] = domain?.discipline?.name
        returnArray['numberOfSlides'] = domain?.countSamples()
        returnArray['numberOfImages'] = domain?.countImageInstance()
        returnArray['numberOfAnnotations'] = domain?.countAnnotations()
        returnArray['numberOfJobAnnotations'] = domain?.countJobAnnotations()
        returnArray['retrievalProjects'] = domain?.retrievalProjects.collect { it.id }
        returnArray['numberOfReviewedAnnotations'] = domain?.countReviewedAnnotations
        returnArray['retrievalDisable'] = domain?.retrievalDisable
        returnArray['retrievalAllOntology'] = domain?.retrievalAllOntology
        returnArray['isClosed'] = domain?.isClosed

        returnArray['isReadOnly'] = false
        returnArray['isRestricted'] = false
        if(domain?.mode.equals(EditingMode.READ_ONLY)){
            returnArray['isReadOnly'] = true
        } else if(domain?.mode.equals(EditingMode.RESTRICTED)){
            returnArray['isRestricted'] = true
        }
        returnArray['hideUsersLayers'] = domain?.hideUsersLayers
        returnArray['hideAdminsLayers'] = domain?.hideAdminsLayers

        return returnArray
    }


    public boolean equals(Object o) {
        if (!o) {
            return false
        } else {
            try {
                return ((Project) o).getId() == this.getId()
            } catch (Exception e) {
                return false
            }
        }
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return this;
    }


    boolean canUpdateContent() {
        return !mode.equals(EditingMode.READ_ONLY)
    }



}
