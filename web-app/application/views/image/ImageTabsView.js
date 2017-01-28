/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

var ImageTabsView = Backbone.View.extend({
    tagName: "div",
    images: null, //array of images that are printed
    idProject: null,
    searchPanel: null,
    project : null,
    initialize: function (options) {
        this.idProject = options.idProject;
        this.project = options.project;
    },
    refresh: function () {
        this.afterDeleteImageEvent();
    },
    afterDeleteImageEvent: function () {
        var table = $(this.el).find("#imageProjectTable" + this.idProject);
        if(table && table.dataTable()) {
            table.dataTable().fnDestroy();
        }
        this.update()
    },
    render : function() {
        var self = this;
        require(["text!application/templates/image/ImageReviewAction.tpl.html"], function (actionMenuTpl) {
            self.doLayout(actionMenuTpl);
            if(window.app.status.user.model.get('guest')){
                $("#imageAdd"+self.idProject).hide()
            } else {
                $("#imageAdd"+self.idProject).click(function() {
                    new AddImageToProjectDialog({el: "#dialogs", model: self.project}).render();
                });
            }
            $("#imageRefresh"+self.idProject).click(function() {
                self.refresh();
            });

        });
        return this;
    },
    update : function() {
        var self = this;
        require(["text!application/templates/image/ImageReviewAction.tpl.html"], function (actionMenuTpl) {
            self.doLayout(actionMenuTpl);
        });
        return this;
    },
    doLayout: function (actionMenuTpl) {
        var self = this;
        self.images = [];
        var isAdmin = window.app.status.currentProjectModel.isAdmin(window.app.models.projectAdmin);
        var table = $(this.el).find("#imageProjectTable" + self.idProject);
        var body = $(this.el).find("#imageProjectArray" + self.idProject);
        var columns = [
            { sClass: 'center', "mData": "id", "bSearchable": false},
            { "mData": "macroURL", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                return _.template("<div style='width : 130px;'><a href='#tabs-image-<%= project %>-<%=  id  %>-'><img src='<%= thumb %>' alt='originalFilename' style='max-height : 45px;max-width : 128px;'/></a></div>",
                    {
                        project : self.idProject,
                        id : o.aData.id,
                        thumb : o.aData["macroURL"]+"?maxWidth=128"
                    });
            }},
            { "mDataProp": "originalFilename", sDefaultContent: "", "bSearchable": true,"bSortable": true, "fnRender" : function (o) {
                var imageInstanceModel = new ImageInstanceModel({});
                imageInstanceModel.set(o.aData);
                var names = imageInstanceModel.getVisibleName(window.app.status.currentProjectModel.get('blindMode'),isAdmin);
                if(isAdmin) {
                    if(window.app.status.currentProjectModel.get('blindMode')) {
                        return names[1]+"<br/><i>"+names[0]+"</i>";
                    } else {
                        return names[0];
                    }
                }
                return names;

            }}
            ,
            { "mDataProp": "width", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return o.aData["width"];
            }},
            { "mDataProp": "height", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return o.aData["height"];
            } },
            { "mDataProp": "magnification", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                var magnification = o.aData["magnification"];
                if (magnification) {
                    try {
                        return o.aData["magnification"] + " X";
                    }catch(e) {return "";}
                } else {
                    return '<span class="label label-default">Undefined</span>'
                }

            }},
            { "mDataProp": "resolution", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                var resolution = o.aData["resolution"];
                if (resolution) {
                    try {
                        return resolution.toFixed(3);
                    }catch(e) {return "";}
                } else {
                    return '<span class="label label-default">Undefined</span>'
                }

            }},
            { "mDataProp": "numberOfAnnotations", "bSearchable": false,"bSortable": true },
            { "mDataProp": "numberOfJobAnnotations", "bSearchable": false,"bSortable": true },
            { "mDataProp": "numberOfReviewedAnnotations", "bSearchable": false,"bSortable": true },
            { "mDataProp": "mimeType", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                var mimeType = o.aData["mime"];
                if (mimeType == "openslide/ndpi" || mimeType == "openslide/vms") {
                    return '<img src="images/brands/hamamatsu.jpg" alt="hamamatsu photonics" style="max-width : 100px;max-height : 40px;" >';
                } else if (mimeType == "openslide/mrxs") {
                    return '<img src="images/brands/3dh.png" alt="hamamatsu photonics" style="max-width : 100px;max-height : 40px;" >';
                } else if (mimeType == "openslide/svs") {
                    return '<img src="images/brands/aperio.jpg" alt="hamamatsu photonics" style="max-width : 100px;max-height : 40px;" >';
                } else if (mimeType == "openslide/scn") {
                    return '<img src="images/brands/leica.png" alt="hamamatsu photonics" style="max-width : 100px;max-height : 40px;" >';
                } else if (mimeType == "ventana/tif" || mimeType == "ventana/bif") {
                    return '<img src="images/brands/roche.gif" alt="hamamatsu photonics" style="max-width : 100px;max-height : 40px;" >';
                } else if (mimeType == "philips/tif") {
                    return '<img src="images/brands/philips.jpg" alt="philips" style="max-width : 100px;max-height : 40px;" >';
                }
                else return '<span class="label label-default">Undefined</span>';
            } },
            { "mDataProp": "created", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o, created) {
                return window.app.convertLongToDate(created);
            }} ,
            { "mDataProp": "reviewStatus", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                if (o.aData.reviewStart && o.aData.reviewStop) {
                    return '<span class="label label-success">Reviewed</span>';
                } else if (o.aData.reviewStart) {
                    return '<span class="label label-warning">In review</span>';
                } else {
                    return '<span class="label label-info">None</span>';
                }
            }},
            { "mDataProp": "updated", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                self.images.push(o.aData);
                o.aData["project"]  = self.idProject;
                return _.template(actionMenuTpl, o.aData);

            }}
        ];
        self.imagesdDataTables = table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new ImageInstanceCollection({project: this.idProject}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },
            "fnDrawCallback": function(oSettings, json) {

                _.each(self.images, function(aData) {
                    var model = new ImageInstanceModel(aData);
                    var action = new ImageReviewAction({el:body,model:model, container : self});
                    action.configureAction();
                });

                $(".dropdown-menu").css("left", "-140px");

                self.images = [];
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]]

    });
//        $('#projectImageListing' + self.idProject).hide();
//        $('#projectImageTable' + self.idProject).show();
    }
});
