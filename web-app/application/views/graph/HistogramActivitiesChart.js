var HistogramActivitiesChart = Backbone.View.extend({
    data : null,
    initialize: function () {
        this.format = this.options.format;
        this.period = this.options.period;
        this.url = this.options.url;
    },
    render: function () {
        var self = this;
        self.getValues(function() {

            nv.addGraph(function () {

                //TODO BrowserSupport.addMessage($("#userNbAnnotationsChart"),BrowserSupport.CHARTS);

                if(!self.format) self.format = 'f';

                var chart = nv.models.discreteBarChart()
                        .x(function (d) {
                            return d.label
                        })    //Specify the data accessors.
                        .y(function (d) {
                            return d.value
                        })
                        .staggerLabels(true)    //Too many bars and not enough room? Try staggering labels.
                        .valueFormat(d3.format(self.format))
                        .tooltips(false)        //Don't show tooltips
                        .showValues(true)       //...instead, show the bar value right on top of each bar.
                    ;

                d3.select(self.el)
                    .append("div")
                    .append("svg")
                    .datum(parseData())
                    .call(chart);

                nv.utils.windowResize(chart.update);

                return chart;
            });
            //Each bar represents a single discrete quantity.
            function parseData() {

                var result = {};
                result.key = "Evolution of Cytomine connections";
                result.values = [];

                var labels = [];
                var getPeriod;
                if(self.period === "week") {
                    for(var i=1;i<=52;i++) labels.push(""+i);
                    getPeriod = function(date){return date.getWeekNumber()};
                } else if(self.period === "day"){
                    labels = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
                    getPeriod = function(date){return date.getDay()};
                } else if(self.period === "hour") {
                    for(var i=0;i<24;i++) labels.push(""+i);
                    getPeriod = function(date){return date.getHours()};
                }

                for (var i = 0; i < labels.length; i++) {
                    result.values.push({label: labels[i], value: 0});
                }


                for (var i = 0; i < self.data.length; i++) {
                    var date = new Date(parseInt(self.data[i].time));
                    result.values[getPeriod(date)].value = self.data[i].frequency;
                }

                var pivot = getPeriod(new Date())+1;
                if(pivot>0) {
                    var toRotate = result.values.slice(pivot);
                    result.values = result.values.slice(0,pivot);
                    for(var i = toRotate.length-1; i>=0 ;i--) {
                        result.values.unshift(toRotate[i]);
                    }
                }
                return [result];
            }
        });
    },
    getValues : function(callback) {
        var self = this;

        $.get(self.url, function(data){
            self.data = data.collection;
            self.data.sort(function(a, b){
                return a.time-b.time;
            });
            callback();
        });
    }
});