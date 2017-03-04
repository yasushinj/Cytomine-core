/**
 * Created by laurent
 * Date : 02.03.17.
 */
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

var ImageGroupTabsView = Backbone.View.extend({
    tagName: "div",
    groups: null, //array of images that are printed
    idProject: null,
    project : null,
    initialize: function (options) {
        this.idProject = options.idProject;
        this.project = options.project;
    },


    render : function() {
        var self = this;
        this.doLayout();

        return this;
    },

    doLayout: function () {
        var self = this;
        self.groups = [];
        var isAdmin = window.app.status.currentProjectModel.isAdmin(window.app.models.projectAdmin);
        var table = $(this.el).find("#imageGroupProjectTable" + self.idProject);
        var body = $(this.el).find("#imageGroupProjectArray" + self.idProject);
        var columns = [
            { sClass: 'center', "mData": "id", "bSearchable": false},

            { "mDataProp": "name", sDefaultContent: "", "bSearchable": true,"bSortable": true, "fnRender" : function (o) {
                return o.aData["name"];
            }}
            ,
            { "mDataProp": "channel", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                var imageGroupModel = new ImageGroupModel({});
                imageGroupModel.set(o.aData);
                imageGroupModel.feed();
                return imageGroupModel.channel;

            }},
            { "mDataProp": "zstack", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {

           /*     var imageGroupModel = new ImageGroupModel({});
                imageGroupModel.set(o.aData);
                imageGroupModel.feed();*/
             //   return imageGroupModel.zstack;
            } },
            { "mDataProp": "slice", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {

/*                var imageGroupModel = new ImageGroupModel({});
                imageGroupModel.set(o.aData);
                imageGroupModel.feed();*/
          //      return imageGroupModel.slice;

            }},
            { "mDataProp": "time", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {

/*                var imageGroupModel = new ImageGroupModel({});
                imageGroupModel.set(o.aData);
                imageGroupModel.feed();*/
              //  return imageGroupModel.time;

            }},
            { "mDataProp": "hdf5", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o) {
                var grouphdf5 = new ImageGroupHDF5Model({group: o.aData.id, id: 666});
                var toRet = '<div id="con-'+o.aData.id + '"></div>';
                grouphdf5.fetch({
                    success: function (data) {
                        toRet =  "" + data.filenames;
                        $("#tabs-images-"+self.idProject).find("#con-"+o.aData.id).append(toRet);

                    },
                    error: function () {
                        var tt =  '<a href="#imagegroup/convert-<%= id %>">Convert</a> ';
                        toRet = _.template(tt, o.aData);
                        $("#tabs-images-"+self.idProject).find("#con-"+o.aData.id).append(toRet);

                    }
                });


                return toRet;
            }}
        ];
        self.imagesdDataTables = table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new ImageGroupCollection({project: this.idProject}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },
            "fnDrawCallback": function(oSettings, json) {
                /*$("#tabs-images-1016").find("#con-1718").append("tg");

                _.each(self.groups, function(aData) {
                    console.log("AU p");
                    $("#tabs-images-1016").find("#con-1718").append(aData);
                });*/
                self.groups = [];
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]]

        });

    }


});
