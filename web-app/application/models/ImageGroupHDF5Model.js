/**
 * Created by laurent
 * Date : 03.03.17.
 */



var ImageGroupHDF5Model = Backbone.Model.extend({

    url: function () {
        var base = 'api/imagegrouph5';
        var format = '.json';
        if (this.isNew()) return base + format;
        return base + "/" + this.group + "" + format;
    },

    initialize: function (options) {
        this.id = options.id;
        this.group = options.group;

    }

});

var ImageGroupSpectraModel = Backbone.Model.extend({
   url: function () {
       var base = 'api/imagegrouph5';
       var format = '.json';
       return base + "/" + this.group + "/" + this.x + "/" + this.y + "/pxl" + format;
   },

    initialize: function(options){
        this.group = options.group;
        this.x = Math.round(options.x);
        this.y = Math.round(options.y);
    }


});