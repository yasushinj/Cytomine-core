package be.cytomine.image

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

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

@RestApiObject(name = "Abstract slice", description = "An abstract slice of a N-dimensional image")
class AbstractSlice extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The abstract image encapsulating this slice")
    AbstractImage image

    @RestApiObjectField(description = "The underlying file for the slice")
    UploadedFile uploadedFile

    @RestApiObjectField(description = "The Cytomine internal slice mime type.")
    Mime mime

    @RestApiObjectField(description = "The channel c of the slice")
    Double channel

    @RestApiObjectField(description = "The depth z of the slice")
    Double zStack

    @RestApiObjectField(description = "The timestamp t of the slice")
    Double time

    static belongsTo = [AbstractImage]

    static mapping = {
        id(generator: 'assigned', unique: true)
        sort([time: 'asc', zStack: 'asc', channel: 'asc'])
        mime fetch: 'join'
    }

    static constraints = {
    }

    void checkAlreadyExist() {
        withNewSession {
            AbstractSlice slice = AbstractSlice.findByImageAndChannelAndZStackAndTime(image, channel, zStack, time)
            if (slice?.id != id)
                throw new AlreadyExistException("AbstractSlice (C:${channel}, Z:${zStack}, T:${time}) already exists for AbstractImage ${image?.id}")
        }
    }

    static AbstractSlice insertDataIntoDomain(def json, def domain = new AbstractSlice()) {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        domain.uploadedFile = JSONUtils.getJSONAttrDomain(json, "uploadedFile", new UploadedFile(), true)
        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new AbstractImage(), true)
        domain.mime = JSONUtils.getJSONAttrDomain(json,"mime",new Mime(),'mimeType','String',true)

        domain.channel = JSONUtils.getJSONAttrDouble(json, "channel", 0)
        domain.zStack = JSONUtils.getJSONAttrDouble(json, "zStack", 0)
        domain.time = JSONUtils.getJSONAttrDouble(json, "time", 0)

        domain
    }

    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['uploadedFile'] = domain?.uploadedFile?.id
        returnArray['path'] = domain?.path
        returnArray['image'] = domain?.image?.id
        returnArray['mime'] = domain?.mime?.mimeType
        returnArray['channel'] = domain?.channel
        returnArray['zStack'] = domain?.zStack
        returnArray['time'] = domain?.time

        returnArray
    }

    def getPath() {
        return uploadedFile?.path
    }

    def getImageServerUrl() {
        return uploadedFile?.imageServer?.url
    }

    def getMimeType(){
        return mime?.mimeType
    }

    CytomineDomain[] containers() {
        return image?.containers()
    }
}
