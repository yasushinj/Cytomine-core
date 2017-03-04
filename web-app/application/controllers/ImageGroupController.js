/**
 * Created by laurent
 * Date : 16.02.17.
 */

var ImageGroupController = Backbone.Router.extend({

    currentlySelected: [],

    routes: {
        "imagegroup/addall": "addall", // #user/list => list()
        "imagegroup/addsome": "addsome",
        "imagegroup/convert-:group": "convert",
    },

    addall: function () {

     /*  var ig2 = new ImageGroupModel({name: "stfu", project: window.app.status.currentProject})
        ig2.save(); */

        var ig = new ImageGroupModel({id:1718});
        var tmp;
        ig.fetch({
            success: function (igm, response) {
                console.log("LLLL"  + igm.project);
                tmp = igm;
            }
        });
        var imageCol = new ImageInstanceCollection({project: window.app.status.currentProject});
        imageCol.fetch({
            success: function (collection, response) {
                new ImageGroupTmpView({
                    model: collection
                }).render();
                var i = 0;
                collection.each(function (image) {
                    var imgSeq = new ImageSequenceModel({
                        image: image.id,
                        channel: i,
                        zStack: 0,
                        slice: 0,
                        time: 0,
                        imageGroup: tmp.id});
                    imgSeq.save();
                    ++i;
                });

            },
            error: function (collection, response) {
                //une erreur s'est produite
            }
        });
    },

    addsome: function () {

        /*  var ig2 = new ImageGroupModel({name: "stfu", project: window.app.status.currentProject})
         ig2.save(); */

        var ig = new ImageGroupModel({id:1718});
        var tmp;

        console.log("hookkkk");
    },

    convert: function(group){
        console.log("good hook " + group);
        var h5group = new ImageGroupHDF5Model({
            group: group,
            filenames: "hdf5_"+group
        });
        h5group.save();
    }
});