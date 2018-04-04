/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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

var DetailedUploadedFileTreeDialog = Backbone.View.extend({

    initialize: function () {
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/upload/DetailedUploadedFileTreeDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },

    doLayout: function (tpl) {
        var self = this;

        var htmlCode = _.template(tpl, self.model);
        $(this.el).html(htmlCode);

        var root = [{
            title : self.getNodeInfo(self.model).title,
            expand: true,
            key : self.model.id,
            noLink : true,
            unselectable : true,
            children : []
        }];
        $.get( new UploadedFileCollection({ parent: self.model.id}).url(), function( data ) {
            var children = [];
            for(var i = 0; i<data.collection.length;i++) {
                var infos = self.getNodeInfo(data.collection[i]);
                children.push({
                    title : infos.title,
                    isLazy : infos.lazy,
                    key : data.collection[i].id,
                    noLink : true,
                    unselectable : true//,
                });
            }
            root[0].children = children;

            $("#treefile").empty();
            $("#treefile").dynatree({
                children: root,
                onLazyRead: function(node) {
                    $.ajax({
                        url: new UploadedFileCollection({parent: node.data.key}).url(),
                        success: function (data, textStatus) {

                            // Convert the response to a native Dynatree JavaScript object.
                            var list = data.collection;
                            var res = [];
                            for(var i=0; i<list.length; i++){
                                var file = list[i];

                                var infos = self.getNodeInfo(file);

                                res.push({
                                    title : infos.title,
                                    isLazy : infos.lazy,
                                    key : file.id,
                                    noLink : true,
                                    unselectable : true//,
                                    //hideCheckbox: true,
                                });
                            }
                            // PWS status OK
                            node.setLazyNodeStatus(DTNodeStatus_Ok);
                            node.addChild(res);
                        }
                    });
                }
            });
        });

        $(this.el).on('click', '.previewUploadedFile', function(e) {
            var url = $(this).data('url');
            $(self.el).find("#filePreview")[0].src = url;
            return false;
        });

        $("#detailedUploadedFileTreeDialog").modal('show');
    },

    getNodeInfo : function (file) {
        var title = "";
        title += file.originalFilename;

        var model = new UploadedFileModel(file);

        var clazz = "label ";
        var icon = "glyphicon ";

        switch (model.getStatus()) {
            case "UPLOADED":
            case "TO DEPLOY":
            case "TO CONVERT":
                clazz += "label-warning";
                icon += "glyphicon-repeat";
                break;
            case "CONVERTED":
            case "DEPLOYED":
            case "UNCOMPRESSED":
                clazz += "label-success";
                icon += "glyphicon-ok";
                break;
            case "ERROR FORMAT":
            case "ERROR CONVERSION":
            case "ERROR DEPLOYMENT":
                clazz += "label-danger";
                icon += "glyphicon-remove";
                break;
        }

        title += " <span title=\"" + model.getStatus() + "\" class=\"" + clazz + "\" style='margin-left:20px;'> <i class=\"" + icon + "\"></i></span>";
        //title += " <span title=\""+model.getStatus()+"\" class=\""+clazz+"\"> <i class=\""+icon+"\"></i></span>";
        title += " test";
        //title += " <img src ='http://localhost-core:8080/api/abstractimage/25914/associated/macro.png'>";

        var size = model.get("size");
        if(size < 1024) {
            size = size + "o";
        } else if(size < 1024^2) {
            size = (size / 1024).toFixed(2) + "Ko";
        } else if(size < 1024^3) {
            size = (size / (1024^2)).toFixed(2) + "Mo";
        } else if(size < 1024^3) {
            size = (size / (1024^3)).toFixed(2) + "Go";
        }

        title += " <span style='margin-left:20px;'>"+size+"</span>";

        if(model.getStatus() === "DEPLOYED"){
            title += " <span style='margin-left:20px;'><a href='#' class='previewUploadedFile' data-url='"+file.thumbURL+"' style='color:#428bca !important;'>See preview</a></span>";
        }

        var lazy = false;
        if(model.getStatus() === "CONVERTED" || model.getStatus() === "UNCOMPRESSED" || model.getStatus() === "ERROR CONVERSION"){
            lazy = true;
        }


        return {title : title, lazy : lazy};
    }
});


