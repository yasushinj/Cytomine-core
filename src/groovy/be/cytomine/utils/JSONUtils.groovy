package be.cytomine.utils

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

import be.cytomine.Exception.ServerException
import be.cytomine.Exception.WrongArgumentException
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.json.JSONArray

/**
 * User: lrollus
 * Date: 11/01/13
 *
 *
 * Utility class to extract/read data from JSON
 * Usefull when you want to create a cytomine domain from a JSON
 */
class JSONUtils {

    /**
     * Get attr string value from JSON and check if not null (if mandatory = true)
     * @return Value as String
     */
    static public String getJSONAttrStr(def json, String attr, boolean mandatory = false) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            return json[attr].toString()
        } else {
            if (mandatory) {
                throw new WrongArgumentException("$attr must be set! value=${json[attr]}")
            }
            return null
        }
    }

    /**
     * Get attr date value from JSON
     * @return Value as Date
     */
    static public Date getJSONAttrDate(def json, String attr) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            return new Date(Long.parseLong(json[attr].toString()))
        } else {
            return null
        }
    }

    /**
     * Get attr long value from JSON and check if not null (if mandatory = true)
     * @return Value as Long
     */
    static public Long getJSONAttrLong(def json, String attr, Long defaultValue) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            try {
                return Long.parseLong(json[attr].toString())
            } catch (Exception e) {
                return defaultValue
            }
        } else {
            return defaultValue
        }
    }

    /**
     * Get attr int value from JSON and check if not null (if mandatory = true)
     * @return Value as Integer
     */
    static public Integer getJSONAttrInteger(def json, String attr, Long defaultValue) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            try {
                return Integer.parseInt(json[attr].toString())
            } catch (Exception e) {
                return defaultValue
            }
        } else {
            return defaultValue
        }
    }

    /**
     * Get attr double value from JSON and check if not null (if mandatory = true)
     * @return Value as Double
     */
    static public Double getJSONAttrDouble(def json, String attr, Double defaultValue) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            try {
                return Double.parseDouble(json[attr].toString())
            } catch (Exception e) {
                return defaultValue
            }
        } else {
            return defaultValue
        }
    }

    /**
     * Get attr bool value from JSON and check if not null (if mandatory = true)
     * @return Value as Boolean
     */
    static public Boolean getJSONAttrBoolean(def json, String attr, Boolean defaultValue) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            try {
                return Boolean.parseBoolean(json[attr].toString())
            } catch (Exception e) {
                return defaultValue
            }
        } else {
            return defaultValue
        }
    }

    static public List getJSONAttrListLong(def json, String attr) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            return json[attr].collect{Long.parseLong(it+"")}
        } else {
            return null
        }
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument class and its id (domain.read)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    static public def getJSONAttrDomain(def json, String attr, def domain, boolean mandatory) {
        getJSONAttrDomain(json, attr, domain, 'id', 'Long', mandatory)
    }

    /**
     * Get attr domain value from json
     * Read domain thanks to domain argument, get the correct object thanks to value from column (type: columnType)
     * If mandatory flag is true, check if domain exists
     * @return  Value as Cytomine Domain
     */
    static public def getJSONAttrDomain(def json, String attr, def domain, String column, String columnType, boolean mandatory) {
        if (json[attr] != null && !json[attr].toString().equals("null")) {
            def domainRead
            if(column.equals('id')) {
                domainRead = domain.read(Long.parseLong(json[attr].toString()))
            } else {
                def value = convertValue(json[attr].toString(), columnType)
                domainRead = domain.findWhere(("$column".toString()): value)
            }
            if (!domainRead) {
                throw new WrongArgumentException("$attr was not found with id:${json[attr]}")
            }
            return domainRead
        } else {
            if (mandatory) {
                throw new WrongArgumentException("$attr must be set! value=${json[attr]}")
            }
            return null
        }
    }

    static public def getJSONList(def item) {
        if(item==null || item instanceof JSONObject.Null) {
            return []
        } else if(item instanceof List || item instanceof ArrayList) {
           return item
        } else if(item instanceof JSONArray) {
            return item
        }else if(item instanceof String) {
            return JSON.parse(item)
        }
        return item
    }



    static public def convertValue(String value, String type) {
        if(value.equals("null")) {
            return null
        }else if (type.equals("String")) {
            return value.toString()
        } else if (type.equals("Long")) {
            return Long.parseLong(value);
        }
        throw new ServerException("Type $type not supported! See cytominedomain class")
    }


    static public String toJSONString(def data) {
        return (data as JSON).toString(false)
    }
}
