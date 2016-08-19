var GraphView = Backbone.View.extend({
    title : null,
    initialize: function (options) {
        if(!window.app.isUndefined(options.title)){
            this.title = options.title;
        }
    },
    render: function () {
        var self = this;
        if(!window.app.isUndefined(this.title)){
            $(self.el).append("<div class='header_h'>"+
                "<h4 class='header-graph'><i class='glyphicon glyphicon-stats'></i> "+this.title+"</h4>"+
                "<span class='resize-button' aria-hidden='true'><i class='glyphicon glyphicon-resize-full'></i></span>"+
                "</div>");
        }
        $(self.el).on("click",".resize-button",function(){
            $(this).closest(".graph").toggleClass('graph-enlarge');
            if (typeof self.resize == 'function') {
                setTimeout(function(){
                    self.resize();
                }, 1000);
            }
            $(this).children('i').toggleClass("glyphicon-resize-full");
            $(this).children('i').toggleClass("glyphicon-resize-small");
        })
    }
});
