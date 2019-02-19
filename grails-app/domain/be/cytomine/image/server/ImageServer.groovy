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
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Image server", description = "An image server (IMS) instance")
class ImageServer extends CytomineDomain {

    @RestApiObjectField(description = "A user friendly name for IMS instance.", mandatory = false)
    String name

    @RestApiObjectField(description = "The URL of the image server instance")
    String url

    @RestApiObjectField(description = "The base path used by the image server")
    String basePath

    @RestApiObjectField(description = "A flag for the server availability")
    Boolean available

    static constraints = {
        name blank: false
        url blank: false
        basePath blank: false
        available nullable: false
    }
}
