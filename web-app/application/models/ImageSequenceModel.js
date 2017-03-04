/**
 * Created by laurent
 * Date : 16.02.17.
 */


/**
 * Created by laurent on 16.02.17.
 *
 */


var ImageSequenceModel = Backbone.Model.extend({


    url: function () {
        var base = 'api/imagesequence';
        var format = '.json';
        if (this.isNew()) return base + format;
        return base + (base.charAt(base.length - 1) == '/' ? '' : '/') + this.id + format;
    }
});


var ImageSequenceCollection = PaginatedCollection.extend({
    model: ImageSequenceModel,

    url: function() {
        var format = '.json';
        return "api/imagegroup/" + this.group + "/imagesequence" + format; //cette url renvoie la list de tous les utilisateurs
    },

    initialize: function (options) {
        this.group = options.group;

    }
});