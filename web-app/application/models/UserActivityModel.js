var UserActivitiesCollection = PaginatedCollection.extend({
    url: function () {
        var url;
        if (!window.app.isUndefined(this.project)) {
            url= "api/project/" + this.project;
            if (!window.app.isUndefined(this.last) && this.last) {
                url+= "/lastConnection/"+this.user+".json"
            } else if (!window.app.isUndefined(this.frequency) && this.frequency) {
                url+= "/connectionFrequency/"+this.user+".json"
            } else {
                url+= "/connectionHistory/"+this.user+".json"
            }
        }
        return url;
    },
    initialize: function (options) {
        this.initPaginator(options);
        this.project = options.project;
        this.user = options.user;
        this.last = options.last;
        this.frequency = options.frequency;
    }
});
var UserActivityDetailsCollection = PaginatedCollection.extend({
    url: function () {
        return "/api/projectConnection/"+this.activity+".json";
    },
    initialize: function (options) {
        this.initPaginator(options);
        this.activity = options.activity;
    }
});