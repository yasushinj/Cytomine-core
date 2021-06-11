package be.cytomine.search.engine

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

import be.cytomine.image.ImageInstance

/**
 * Created by lrollus on 7/22/14.
 */
class ImageInstanceSearch extends EngineSearch {

    public String createRequestOnAttributes(List<String> words) {
        return """
            SELECT ii.id as id,'${ImageInstance.class.name}' as type ${getMatchingValue("ii.instance_filename")} ${
            getName("ii.instance_filename")
        }
            FROM image_instance ii, abstract_image ai, acl_object_identity as aoi, acl_sid as sid, acl_entry as ae
            WHERE ii.base_image_id = ai.id
            ${getRestrictedIdForm("ii.id")}
            AND aoi.object_id_identity = ii.project_id
            ${idProject && !idProject.isEmpty() ? "AND ii.project_id IN (${idProject.join(",")})" : ""}
            AND sid.sid = '${currentUser.username}'
            AND ae.acl_object_identity = aoi.id
            AND ae.sid = sid.id
            AND ${formatCriteriaToWhere(words, "ii.instance_filename")}
            AND ii.deleted IS NULL
        """
    }

    public String createRequestOnProperty(List<String> words, String attribute = null) {
        String propertyRequest

        if(attribute == "key"){
            propertyRequest = "AND ${formatCriteriaToWhere(words, "property.key")}"
        } else if(attribute == "value"){
            propertyRequest = "AND ${formatCriteriaToWhere(words, "property.value")}"
        } else {
            propertyRequest = "AND (${formatCriteriaToWhere(words, "property.value")} OR ${formatCriteriaToWhere(words, "property.key")})"
        }

        return """
            SELECT DISTINCT property.domain_ident as id, property.domain_class_name as type ${
            getMatchingValue("property.key || ': ' || property.value")
        } ${getName("ii.instance_filename")}
            FROM property property, image_instance ii, abstract_image ai, acl_object_identity as aoi, acl_sid as sid, acl_entry as ae
            WHERE property.domain_class_name like '${ImageInstance.class.name}'
            ${getRestrictedIdForm("property.domain_ident")}
            AND ii.base_image_id = ai.id
            ${idProject && !idProject.isEmpty() ? "AND ii.project_id IN (${idProject.join(",")})" : ""}
            AND property.domain_ident = ii.id
            AND aoi.object_id_identity = ii.project_id
            AND sid.sid = '${currentUser.username}'
            AND ae.acl_object_identity = aoi.id
            AND ae.sid = sid.id
            $propertyRequest
            AND ii.deleted IS NULL
        """
    }

    public String createRequestOnDescription(List<String> words) {
        return """
            SELECT description.domain_ident as id, description.domain_class_name as type ${
            getMatchingValue("description.data")
        } ${getName("ii.instance_filename")}
            FROM description description, image_instance ii, abstract_image ai,acl_object_identity as aoi, acl_sid as sid, acl_entry as ae
            WHERE description.domain_class_name like '${ImageInstance.class.name}'
            ${getRestrictedIdForm("description.domain_ident")}
            AND description.domain_ident = ii.id
            AND ii.base_image_id = ai.id
            ${idProject && !idProject.isEmpty() ? "AND ii.project_id IN (${idProject.join(",")})" : ""}
            AND aoi.object_id_identity = ii.project_id
            AND sid.sid = '${currentUser.username}'
            AND ae.acl_object_identity = aoi.id
            AND ae.sid = sid.id
            AND ${formatCriteriaToWhere(words, "description.data")}
            AND ii.deleted IS NULL
        """
    }
}
