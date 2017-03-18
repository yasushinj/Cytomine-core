/**
 * Created by laurent
 * Date : 12.03.17.
 */

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

var AddImageSequenceDialog = Backbone.View.extend({
    initialize: function (options) {
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/dashboard/AddImageSequenceDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },

    doLayout: function (tpl) {
        var self = this;
        console.log(self);
        var htmlCode = _.template(tpl, self.model.toJSON());
        $(this.el).html(htmlCode);
        $("#addImageSequence").modal('show');
        self.createArray();
    },
    createArray: function () {
        var self = this;
        self.images = [];
        var table = $("#addImageSequenceTable" + self.model.id);
        var columns = [
            { sClass: 'center', "mData": "id", "bSearchable": false,"bSortable": true},
            { "mData": "macroURL", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                return _.template("<div style='width : 130px;'><a href='#tabs-image-<%= project %>-<%=  id  %>-'><img src='<%= thumb %>?maxSize=128' alt='originalFilename' style='max-height : 45px;max-width : 128px;'/></a></div>",
                    {
                        project : self.model.get("project"),
                        id : o.aData.id,
                        thumb : o.aData["macroURL"]
                    });
            }},
            { "mDataProp": "originalFilename", sDefaultContent: "", "bSearchable": true,"bSortable": true, "fnRender" : function (o) {
                return o.aData.originalFilename;
            }} ,
            { "mDataProp": "add", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                self.images.push(o.aData);
                console.log(self.model);
                var html =    ' <button class="btn btn-info btn-xs" id="add-button-<%=  id  %>">Add</button>';
                return _.template(html, o.aData);
            }}
        ];
        self.imagesdDataTables = table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new ImageInstanceCollection({project: self.model.get("project"), imagegroup: self.model.id}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },
            "fnDrawCallback": function(oSettings, json) {

                $("#imageAddSeqResearch" + self.model.id).click(function () {
                    console.log(self.images);

                });

                _.each(self.images, function(aData) {

                    $("#add-button-"+aData.id).click(function() {
                        self.model.feed();
                        var nextchan = 0;
                        if(self.model.channel != undefined)
                            nextchan = self.model.channel[self.model.channel.length - 1] + 1;
                        var imgSeq = new ImageSequenceModel({
                            image: aData.id,
                            slice:0,
                            imageGroup: self.model.id});
                        imgSeq.save({}, {
                            success: function () {

                                $("#add-button-" + aData.id).replaceWith('<label id="labelAddToGroup' + aData.id + '">Added</label>');
                                self.model.forcefeed();
                            }
                        });

                    });



                });
                //self.images = [];
            },
            "aoColumns" : columns
        });
        $('#addImageSequenceTable' + self.idProject).show();
    }
});