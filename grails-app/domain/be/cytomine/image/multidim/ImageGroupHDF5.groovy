/*
* Copyright (c) 2009-2020. Authors: see NOTICE file.
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

package be.cytomine.image.multidim

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

/**
 * Created by laurent on 06.02.17.
 */
@RestApiObject(name = "image hdf5 group", description = "A group of image from the same source with different dimension and hdf5 support")

class ImageGroupHDF5  extends CytomineDomain implements  Serializable {

   ImageGroup group

    @RestApiObjectField(description = "The HDF5 filenames for the whole multidim  image")
    String filenames

    static mapping = {
        id generator: "assigned"
        sort "id"
    }

    static constraints = {
        filenames nullable: false
    }


    /**
     * Check if this domain will cause unique constraint fail if saving on database
     */
    void checkAlreadyExist() {
        ImageGroupHDF5.withNewSession {
            ImageGroupHDF5 imageAlreadyExist = ImageGroupHDF5.findByGroup(group)
            if (imageAlreadyExist != null && (imageAlreadyExist.id != id)) {
                throw new AlreadyExistException("I")
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static ImageGroupHDF5 insertDataIntoDomain(def json, def domain = new ImageGroupHDF5()) {
        domain.group = JSONUtils.getJSONAttrDomain(json, "group", new ImageGroup(), true)
        domain.filenames = JSONUtils.getJSONAttrStr(json, "filenames")


        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['group'] = domain?.group?.id
        returnArray['filenames'] = domain?.filenames
        return returnArray
    }


    public CytomineDomain container() {
        return group.container();
    }

}