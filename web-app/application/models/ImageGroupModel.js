/**
 * Created by laurent on 16.02.17.
 *
 */


var ImageGroupModel = Backbone.Model.extend({
    channel:"",
    zstack:0,
    time:"",
    slice:"",
    feeded:false,


    url: function () {
        var base = 'api/imagegroup';
        var format = '.json';
        if (this.isNew()) return base + format;
        return base + (base.charAt(base.length - 1) == '/' ? '' : '/') + this.id + format;
    },

    forcefeed: function(){
        this.feeded = false;
        this.feed();
    },

    feed: function (callback) {
        var self = this;
        var coll = new ImageSequenceCollection({group: this.id});
        if(self.feeded)
            return;
        coll.fetch({success: function (collection, response) {
            if(collection.length == 0)
                return;
            var iS = collection.at(0);

            $.get("/api/imageinstance/"+iS.attributes.image+"/imagesequence/possibilities.json", function(data) {

                if(data.channel!=null && data.channel!=undefined) {
                    self.channel=data.channel;
                    self.slice=data.slice;
                    self.time=data.time;
                }
                if(callback != undefined)
                    callback();
            });
            self.feeded=true;
        },
        error: function (collection, response) {
            console.log(response);
        }});

    },

    initialize: function (options) {
        this.channel=options.channel;
        this.zstack=0;
        this.time=options.time;
        this.slice=options.slice;
        this.feeded=false;

    },
});

var ImageGroupCollection = PaginatedCollection.extend({
    model: ImageGroupModel,
    url: function() {

        var format = '.json';
        return "api/project/"+ this.project + "/imagegroup" + format;
    },

    initialize: function (options) {
      this.project=options.project;
    }
});