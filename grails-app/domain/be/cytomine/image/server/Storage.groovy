package be.cytomine.image.server

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Storage", description = "A virtual directory owned by a human user where uploaded files are stored.")
class Storage extends CytomineDomain {

    @RestApiObjectField(description = "The storage name")
    String name

    @RestApiObjectField(description = "The storage owner, which has administration rights on the domain.")
    SecUser user

    // TODO: remove
    String basePath

    static belongsTo = [SecUser]

    static constraints = {
        name(unique: false)
        basePath(nullable: true, blank: true)
    }

    static def getDataFromDomain(def storage) {
        def returnArray = CytomineDomain.getDataFromDomain(storage)
        returnArray['user'] = storage?.user?.id
        returnArray['name'] = storage?.name

        returnArray['basePath'] = storage?.basePath //TODO: remove

        returnArray
    }

    static Storage insertDataIntoDomain(def json, def domain = new Storage()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.created = JSONUtils.getJSONAttrDate(json, 'created')
        domain.updated = JSONUtils.getJSONAttrDate(json, 'updated')

        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name', true)

        domain.basePath = JSONUtils.getJSONAttrStr(json, 'basePath', false) //TODO: remove

        return domain
    }

    /**
     * Get the container domain for this domain (usefull for security)
     * @return Container of this domain
     */
    public CytomineDomain container() {
        return this;
    }
}
