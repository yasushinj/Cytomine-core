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

class JobDataUrlMappings {

    static mappings = {
        /* Job */
        "/api/jobdata.$format"(controller: "restJobData"){
            action = [GET:"list", POST:"add"]
        }
        "/api/jobdata/$id.$format"(controller: "restJobData"){
            action = [GET:"show", PUT:"update", DELETE:"delete"]
        }

        "/api/job/$id/jobdata.$format"(controller: "restJobData"){
            action = [GET:"listByJob"]
        }

        "/api/jobdata/$id/upload"(controller:"restJobData"){
            action = [PUT:"upload", POST: "upload"]
        }

        "/api/jobdata/$id/download"(controller:"restJobData"){
            action = [GET: "download"]
        }

        "/api/jobdata/$id/view"(controller:"restJobData"){
            action = [GET: "view"]
        }
    }
}
