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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 19/02/14
 * Time: 7:33
 * A job template is a job with pre-filled parameters. It can be used to init a new "real" job on the basis of this template.
 */
@RestApiObject(name = "Job template", description = "A job template is a job with pre-filled parameters. It can be used to init a new 'real' job on the basis of this template.")
class JobTemplate extends Job implements Serializable {

    @RestApiObjectField(description = "The template name")
    String name

    static constraints = {
        name nullable: false
    }

    static mapping = {
        id generator: "assigned"
        sort "id"
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        JobTemplate.withNewSession {
            JobTemplate job = JobTemplate.findByNameAndProject(name,super.project)
            if (job != null && (job.id != id)) {
                throw new AlreadyExistException("Job template" + name + " already exist in project" +super.project.name)
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static JobTemplate insertDataIntoDomain(def json, def domain = new JobTemplate()) {
        domain = (JobTemplate)Job.insertDataIntoDomain(json,domain)
        domain.name = JSONUtils.getJSONAttrStr(json,"name",true)
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = Job.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        return returnArray
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return project.container()
    }

}
