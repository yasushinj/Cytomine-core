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
 * A parameter for a software.
 * It's a template to create job parameter.
 */
@RestApiObject(name = "Software parameter", description = "A parameter for a software. It's a template to create job parameter. When job is init, we create job parameter list based on software parameter list.")
class SoftwareParameter extends CytomineDomain {

    /**
     * Software for parameter
     */
    @RestApiObjectField(description = "The software of the parameter")
    Software software

    /**
     * Parameter name
     */
    @RestApiObjectField(description = "The parameter name")
    String name

    /**
     * Parameter type
     */
    @RestApiObjectField(description = "The parameter data type (Number, String, Date, Boolean, Domain (e.g: image instance id,...), ListDomain )")
    String type

    /**
     * Default value when creating job parameter
     * All value are stored in (generic) String
     */
    @RestApiObjectField(description = "Default value when creating job parameter", mandatory = false, apiFieldName = "defaultParamValue")
    String defaultValue

    /**
     * Flag if value is mandatory
     */
    @RestApiObjectField(description = "Flag if value is mandatory", mandatory = false)
    Boolean required = false

    /**
     * Index for parameter position.
     * When launching software, parameter will be send ordered by index (asc)
     */
    @RestApiObjectField(description = "Index for parameter position. When launching software, parameter will be send ordered by index (asc).", mandatory = false, defaultValue="-1")
    Integer index=-1

    /**
     * Used for UI
     * If parameter has "Domain" type, the URI will provide a list of choice.
     *
     */
    @RestApiObjectField(description = "Used for UI. If parameter has '(List)Domain' type, the URI will provide a list of choice. E.g. if uri is 'api/project.json', the choice list will be cytomine project list", mandatory = false)
    String uri

    /**
     * JSON Fields to print in choice list
     * E.g. if uri is api/project.json and uriPrintAttribut is "name", the choice list will contains project name
     */
    @RestApiObjectField(description = "Used for UI. JSON Fields to print in choice list. E.g. if uri is api/project.json and uriPrintAttribut is 'name', the choice list will contains project name ", mandatory = false)
    String uriPrintAttribut

    /**
     * JSON Fields used to sort choice list
     */
    @RestApiObjectField(description = "Used for UI. JSON Fields used to sort choice list. E.g. if uri is api/project.json and uriSortAttribut is 'id', projects will be sort by id (not by name) ", mandatory = false)
    String uriSortAttribut

    /**
     * Indicated if the field is autofilled by the server
     */
    @RestApiObjectField(description = "Indicated if the field is autofilled by the server", mandatory = false)
    Boolean setByServer = false

    @RestApiObjectField(description = "Indicates if the field is a parameter used by a processingServer", mandatory = false)
    Boolean serverParameter = false

    @RestApiObjectField(description = "The parameter name in a human readable form")
    String humanName

    @RestApiObjectField(description = "The placeholder for parameter in the command line of software. By default, [NAME]")
    String valueKey

    @RestApiObjectField(description = "The optional command line flag to put before parameter value in the command line.")
    String commandLineFlag

    static belongsTo = [Software]

    static constraints = {
        name (nullable: false, blank : false)
        type (inList: ["String", "Boolean", "Number","Date","List","ListDomain","Domain"])
        defaultValue (nullable: true, blank : true)
        uri (nullable: true, blank : true)
        uriPrintAttribut (nullable: true, blank : true)
        uriSortAttribut (nullable: true, blank : true)
        serverParameter(nullable: true)
        humanName(nullable: true)
        valueKey(nullable: true, blank: true)
        commandLineFlag(nullable: true, blank: true)
    }

    public beforeInsert() {
        super.beforeInsert()
        //if index is not set, automaticaly set it to lastIndex+1
        SoftwareParameter softwareParam = SoftwareParameter.findBySoftware(software,[max: 1,sort: "index",order: "desc"])
        if(this.index==-1) {
              if(softwareParam)
                this.index =  softwareParam.index
              else
                this.index = 0
        }
    }

    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
   void checkAlreadyExist() {
        SoftwareParameter.withNewSession {
            SoftwareParameter softwareParamAlreadyExist=SoftwareParameter.findBySoftwareAndName(software,name)
            if(softwareParamAlreadyExist!=null && (softwareParamAlreadyExist.id!=id)) {
                throw new AlreadyExistException("Parameter " + softwareParamAlreadyExist?.name + " already exist for software " + softwareParamAlreadyExist?.software?.name)
            }
        }
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['type'] = domain?.type
        returnArray['defaultParamValue'] = domain?.defaultValue  //defaultValue & default are reserved
        returnArray['required'] = domain?.required
        returnArray['software'] = domain?.software?.id
        returnArray['index'] = domain?.index
        returnArray['uri'] = domain?.uri
        returnArray['uriPrintAttribut'] = domain?.uriPrintAttribut
        returnArray['uriSortAttribut'] = domain?.uriSortAttribut
        returnArray['setByServer'] = domain?.setByServer
        returnArray['serverParameter'] = domain?.serverParameter
        returnArray['humanName'] = domain?.humanName ?: domain?.name
        returnArray['valueKey'] = domain?.valueKey
        returnArray['commandLineFlag'] = domain?.commandLineFlag
        return returnArray
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */    
    static SoftwareParameter insertDataIntoDomain(def json, def domain = new SoftwareParameter()) {
        log.info(json)
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name', true)
        domain.software = JSONUtils.getJSONAttrDomain(json, "software", new Software(), true)
        domain.type = JSONUtils.getJSONAttrStr(json, 'type', true)
        domain.defaultValue = JSONUtils.getJSONAttrStr(json, 'defaultValue')
        if(!domain.defaultValue) {
            domain.defaultValue = JSONUtils.getJSONAttrStr(json, 'defaultParamValue')
        }
        domain.required = JSONUtils.getJSONAttrBoolean(json, 'required',false)
        domain.index = JSONUtils.getJSONAttrInteger(json, 'index', -1)
        domain.uri = JSONUtils.getJSONAttrStr(json,'uri')
        domain.uriPrintAttribut = JSONUtils.getJSONAttrStr(json,'uriPrintAttribut')
        domain.uriSortAttribut = JSONUtils.getJSONAttrStr(json,'uriSortAttribut')
        domain.setByServer = JSONUtils.getJSONAttrBoolean(json,'setByServer', false)
        domain.serverParameter = JSONUtils.getJSONAttrBoolean(json, 'serverParameter', false)
        domain.humanName = JSONUtils.getJSONAttrStr(json, 'humanName') ?: domain.name
        domain.valueKey = JSONUtils.getJSONAttrStr(json, "valueKey") ?: "[${domain.name.toUpperCase()}]"
        domain.commandLineFlag = JSONUtils.getJSONAttrStr(json, "commandLineFlag")
        return domain;
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return software.container();
    }

}
