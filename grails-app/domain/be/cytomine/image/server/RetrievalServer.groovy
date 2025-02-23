package be.cytomine.image.server

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

/**
 * Cytomine
 * User: stevben
 * Date: 27/01/11
 * Time: 15:21
 * Retrieval server provide similar images to an images.
 * It can be used to suggest class for images (=> terms for annotation)
 */
class RetrievalServer extends CytomineDomain {

    String description
    String url
    String path

    String username
    String password

    int port = 0

    String toString() { return getFullURL(); }

    public String getFullURL() {
        return url + (path?:"")
    }

    static constraints = {
        path nullable: true
        username nullable:true
        password nullable:true
    }


    def beforeInsert() {
        super.beforeInsert()
    }

    def beforeValidate() {
        super.beforeValidate()
    }

}
