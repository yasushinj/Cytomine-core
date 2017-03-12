/**
 * Created by laurent
 * Date : 12.03.17.
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
                        project : self.model.id,
                        id : o.aData.id,
                        thumb : o.aData["macroURL"]
                    });
            }},
            { "mDataProp": "originalFilename", sDefaultContent: "", "bSearchable": true,"bSortable": true, "fnRender" : function (o) {
                return o.aData["originalFilename"];
            }} ,
            { "mDataProp": "add", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                self.images.push(o.aData);
                var html =    ' <button class="btn btn-info btn-xs" id="add-button-<%=  id  %>">Add</button>';
                return _.template(html, o.aData);
            }}
        ];
        self.imagesdDataTables = table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new ImageInstanceCollection({project: self.model.get("project")}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },
            "fnDrawCallback": function(oSettings, json) {

                _.each(self.images, function(aData) {

                    $("#add-button-"+aData.id).click(function() {
                        self.model.feed();
                        var nextchan = self.model.channel[self.model.channel.length - 1] + 1;
                        var imgSeq = new ImageSequenceModel({
                            image: aData.id,
                            channel: nextchan,
                            zStack: 0,
                            slice: 0,
                            time: 0,
                            imageGroup: self.model.id});
                        imgSeq.save();

                    });



                });
                self.images = [];
            },
            "aoColumns" : columns
        });
        $('#addImageSequenceTable' + self.idProject).show();
    }
});