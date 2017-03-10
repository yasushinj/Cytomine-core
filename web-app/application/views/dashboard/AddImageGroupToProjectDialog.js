/**
 * Created by laurent
 * Date : 09.03.17.
 */


var AddImageGroupToProjectDialog = Backbone.View.extend({
    initialize: function (options) {
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/dashboard/AddImageGroupToProjectDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },

    doLayout: function (tpl) {
        var self = this;
        var htmlCode = _.template(tpl, self.model.toJSON());
        $(this.el).html(htmlCode);
        $("#addImageGroupToProjectProject").modal('show');
        $("#add-image-group-sub").click(function (event) {
            event.preventDefault();
            $("#add-image-group").submit();
            return false;
        });
        $("#add-image-group").submit(function () {
            self.createImageGroup();
            return false;
        });
    },


    createImageGroup: function () {
        console.log("create Image group");
        var self = this;

        var gname = $("#group-name").val().toUpperCase();
        var addall = $("input#add-all").is(':checked');
        var cmin =  Math.round($("#cmin").val());
        var tmin = Math.round($("#tmin").val());
        var smin = Math.round($("#smin").val());
        var cmax = Math.round($("#cmax").val());
        var tmax = Math.round($("#tmax").val());
        var smax = Math.round($("#smax").val());

        var ig = new ImageGroupModel({name: gname, project: window.app.status.currentProject});
        ig.save({}, {
            success: function (model, response) {
                console.log(model);
                console.log(response);
                var igId = model.get("imagegroup").id;
                console.log(igId);
                if(addall){
                    var imageCol = new ImageInstanceCollection({project: window.app.status.currentProject});
                    imageCol.fetch({
                        success: function (collection, response) {
                            var channel = cmin;
                            var time = tmin;
                            var slice = smin;
                            var incChannel =  1;
                            var incTime =  1;
                            var incSlice =  1;
                            if(cmin == null || cmax == null || cmin == cmax)
                                incChannel = 0;
                            else if(cmax - cmin > collection.length)
                                incChannel = Math.round((cmax - cmin) / collection.length);
                            if(tmin == null || tmax == null || tmin == tmax)
                                incTime = 0;
                            else if(tmax - tmin > collection.length)
                                incTime = Math.round((tmax - tmin) / collection.length);
                            if(smin == null || smax == null || smin == smax)
                                incSlice = 0;
                            else if(smax - smin > collection.length)
                                incSlice = Math.round((smax - smin) / collection.length);

                            collection.each(function (image) {
                                var imgSeq = new ImageSequenceModel({
                                    image: image.id,
                                    channel: channel,
                                    zStack: 0,
                                    slice: slice,
                                    time: time,
                                    imageGroup: igId});
                                imgSeq.save();
                                channel += incChannel;
                                slice += incSlice;
                                time += incTime;
                            });

                        },
                        error: function (collection, response) {
                            //une erreur s'est produite
                        }
                    });
                }

            },
            error: function (response) {
                console.log("error");
            }
        });
    }
});