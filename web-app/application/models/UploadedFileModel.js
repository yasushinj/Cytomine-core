/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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

var UploadedFileModel = Backbone.Model.extend({

    initialize: function (options) {
        if (!options) {
            return;
        }
        this.image = options.image;
    },
    getStatus: function() {
        var result;
        switch(this.get('status')){
            case 0:
                result = "UPLOADED";
                break;
            case 1:
                result = "CONVERTED";
                break;
            case 2:
                result = "DEPLOYED";
                break;
            case 3:
                result = "ERROR FORMAT";
                break;
            case 4:
                result = "ERROR CONVERSION";
                break;
            case 5:
                result = "UNCOMPRESSED";
                break;
            case 6:
                result = "TO DEPLOY";
                break;
            case 7:
                result = "TO CONVERT";
                break;
            case 8:
                result = "ERROR DEPLOYMENT";
                break;
        }
        return result;
    },
    url: function () {
        var base = 'api/uploadedfile';
        var format = '.json';
        if(this.image != null && this.image != undefined) {
            return base + '/image/'+ this.image + format;
        } else{
            if (this.isNew()) {
                return base + format;
            }
            return base + (base.charAt(base.length - 1) == '/' ? '' : '/') + this.id + format;
        }
    }
});


var UploadedFileCollection = PaginatedCollection.extend({
    model: UploadedFileModel,
    initialize: function (options) {
        this.initPaginator(options);
        if (!options) {
            return;
        }
        this.dataTables = options.dataTables;
        this.parent = options.parent;
        this.onlyRoot = options.onlyRoot;
    },
    url: function () {
        var baseUrl = 'api/uploadedfile.json';

        if (this.dataTables || this.parent || this.onlyRoot) {
            baseUrl += '?';
        } else {
            return baseUrl
        }

        if (this.dataTables) {
            baseUrl += 'dataTables=true&';
        }
        if (this.parent) {
            baseUrl += 'parent='+this.parent+'&';
        } else if (this.onlyRoot) {
            baseUrl += 'onlyRoot='+this.onlyRoot+'&';
        }
        return baseUrl.substr(0,baseUrl.length-1);
    }
});