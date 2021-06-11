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

/**
 * Cytomine
 * User: stevben
 * Date: 10/10/11
 * Time: 13:49
 */
class ImageFilterUrlMappings {

    static mappings = {
        /* Image Filters */
        "/api/imagefilter.$format"(controller: "restImageFilter"){
            action = [GET:"list", POST : "add"]
        }
        "/api/imagefilter/$id.$format"(controller: "restImageFilter"){
            action = [GET:"show", DELETE : "delete"]
        }

        "/api/project/imagefilter.$format"(controller: "restImageFilter"){
            action = [GET:"list"]
        }
        "/api/project/imagefilter/$id.$format"(controller: "restImageFilter"){
            action = [GET:"show"]
        }

        "/api/project/$project/imagefilterproject.$format"(controller: "restImageFilterProject"){
            action = [GET:"listByProject"]
        }
        "/api/imagefilterproject.$format" (controller: "restImageFilterProject"){
            action = [GET:"list", POST : "add"]
        }
        "/api/imagefilterproject/$id.$format"(controller: "restImageFilterProject"){
            action = [DELETE : "delete"]
        }
    }
}
