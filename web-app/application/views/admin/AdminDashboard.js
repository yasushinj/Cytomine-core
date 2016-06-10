/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

var AdminDashboard = Backbone.View.extend({
    totals : null,
    currents : null,
    rendered: false,
    render: function () {
        var self = this;

        if (!this.rendered) {
            require(["text!application/templates/admin/AdminDashboard.tpl.html"],
                function (tpl) {
                    self.getValues(function() {
                        self.doLayout(tpl);
                        self.rendered = true;
                    });
                }
            );
        }
    },
    refreshCurrentConnections: function (){
        var self = this;
        $.get( "api/stats/currentStats.json", function( data ) {
            self.currents = data;
            $(self.el).find("#statsTotalOnlineUsers").text(self.currents.users);
            $(self.el).find("#statsTotalActiveProjects").text(self.currents.projects);
            var mostActiveProject = "No record";
            if(self.currents.mostActiveProject) {
                mostActiveProject = self.currents.mostActiveProject.project.name+" ("+self.currents.mostActiveProject.users+" users online)";
            }
            $(self.el).find("#statsMostActiveProject").text(mostActiveProject);
        });
    },

    doLayout: function(tpl) {
        var self = this;

        var modelView = self.totals;
        modelView.instance = window.location.host;

        var view = _.template(tpl, modelView);
        $(this.el).append(view);

        new LastConnexionsGraphsView({
            el: $(self.el).find("#lastConnectionsGraph")}).render();
        new AverageConnexionsGraphsView({
            el: $(self.el).find("#avgConnectionsGraph")}).render();
        /*new LastAnnotationsGraphsView({
            el: $(self.el).find("#lastAnnotationsGraph")}).render();*/

        self.refreshCurrentConnections();
        this.refreshCurrentStatsInterval = window.app.view.addInterval(function () {
            self.refreshCurrentConnections();
        }, 30*1000);
        window.app.view.intervals.push(this.refreshCurrentStatsInterval);

        return this;
    },

    getValues: function (callback) {
        var self = this;

        self.totals = null;
        self.currents = null;

        var loadedCallBack = function() {
            if (self.totals === null) {
                return;
            }
            callback();
        };
        $.get( "api/stats/all.json", function( data ) {
            self.totals = data;
            loadedCallBack();
        });
    }
});

var LastConnexionsGraphsView = Backbone.View.extend({
    graphs : [],
    initialize: function (options) {
        var url;
        if(window.app.isUndefined(options.project)){
            url = "api/connectionFrequency.json?period=";
        } else {
            url = "api/project/"+options.project+"/connectionFrequency.json?period=";
        }

        var lastDay = new Date();
        var firstDay = new Date(lastDay.getTime());

        firstDay.setDate(lastDay.getDate()-1);
        firstDay.setHours(lastDay.getHours()+1,0,0);
        this.graphs.push({name : "Day", period : "hour", url: url+"hour&afterThan="+firstDay.getTime(), panel : null});

        firstDay = new Date(lastDay.getTime());
        firstDay.setDate(lastDay.getDate()-7);
        firstDay.setHours(0,0,0);
        this.graphs.push({name : "Week", period : "day", url: url+"day&afterThan="+firstDay.getTime(), panel : null});

        firstDay = new Date(lastDay.getTime());
        firstDay.setYear(lastDay.getFullYear()-1);
        firstDay.setMonth(lastDay.getMonth()+1);
        firstDay.setDate(1);
        firstDay.setHours(0,0,0);
        this.graphs.push({name : "Year", period : "week", url: url+"week&afterThan="+firstDay.getTime(), panel : null});
    },
    render: function () {
        var self = this;

        var tpl = '<h4 class="header_h"><i class="glyphicon glyphicon-stats"></i> Last Connections</h4>'+
            'Last <select id="lastConnectionsSelection">';

        _.each(self.graphs, function(it){
            tpl+= '<option>'+it.name+'</option>';
        });

        tpl += '</select>';

        _.each(self.graphs, function(it){
            tpl+= '<div class="col-md-offset-1 graph-last" id="LastConnectionsBy'+it.name+'" style="text-align: center;"></div>';
        });

        $(self.el).append(tpl);

        $(self.el).find("#lastConnectionsSelection").on("change", function(e){

            $(self.el).find(".graph-last").hide();

            var chosen = $(this).val();
            chosen = _.find(self.graphs, function(it) {
                return it.name === chosen;
            });

            var panel = $(self.el).find("#LastConnectionsBy"+chosen.name);
            // create objet if not null
            if(chosen.panel === null){
                chosen.panel = new HistogramActivitiesChart({
                    el: panel,
                    period : chosen.period,
                    url : chosen.url,
                    enableTooltip : true
                });
                chosen.panel.render();
            }
            panel.show();
        });

        $(self.el).find("#lastConnectionsSelection").trigger("change");

        return this;
    }
});

var LastAnnotationsGraphsView = Backbone.View.extend({
    graphs : []
    //TODO mongodb entry for each created annotation
});

var AverageConnexionsGraphsView = Backbone.View.extend({
    graphs : [],
    initialize: function (options) {
        var url = "api/averageConnections.json?";
        if(!window.app.isUndefined(options.project)){
            url += "project="+options.project+"&";
        }
        url += "period=";

        this.graphs.push({name : "Hour", period : "hour", url: url+"hour", panel : null});
        this.graphs.push({name : "Day", period : "day", url: url+"day", panel : null});
        this.graphs.push({name : "Week", period : "week", url: url+"week", panel : null});
    },
    render: function () {
        var self = this;

        var tpl = '<h4 class="header_h"><i class="glyphicon glyphicon-stats"></i> Average Connections</h4>'+
            'By <select id="avgConnectionsSelection">';

        _.each(self.graphs, function(it){
            tpl+= '<option>'+it.name+'</option>';
        });

        tpl += '</select>';
        tpl += '<span class="col-md-offset-1">Since  <input type="text" id="avgConnectionDatepickerSince" class="avgConnectionDatepicker" placeholder="date"><span class="add-on"><i class="icon-calendar"></i> </span></span>';
        tpl += '<span class="col-md-offset-1">Until  <input type="text" id="avgConnectionDatepickerUntil" class="avgConnectionDatepicker" placeholder="date"><span class="add-on"><i class="icon-calendar"></i> </span></span>';

        _.each(self.graphs, function(it){
            tpl+= '<div class="col-md-offset-1 graph-avg" id="AvgConnectionsBy'+it.name+'" style="text-align: center;"></div>';
        });

        $(self.el).append(tpl);

        $(self.el).find(".avgConnectionDatepicker").datepicker({
            beforeShowDay: function (date) {
                var tomorrow = new Date();
                tomorrow.setDate(tomorrow.getDate()+1);
                if (date <= tomorrow) {
                    return [true, "", ""];
                } else {
                    return [false, "", "Cannot select into the future."];
                }
            },
            onSelect: function (dateStr) {
                // every avg-graph is deleted.
                $(self.el).find(".graph-avg").empty();
                _.each(self.graphs, function(it){
                    it.panel = null;
                });
                // force refresh
                $(self.el).find("#avgConnectionsSelection").trigger("change");
            }
        });

        var lastDay = new Date();
        var firstDay = new Date(lastDay.getTime());
        firstDay.setMonth(lastDay.getMonth()-12);
        lastDay.setDate(lastDay.getDate()+1);
        $(self.el).find("#avgConnectionDatepickerSince").datepicker('setDate', firstDay);
        $(self.el).find("#avgConnectionDatepickerUntil").datepicker('setDate', lastDay);


        $(self.el).find("#avgConnectionsSelection").on("change", function(e){

            $(self.el).find(".graph-avg").hide();

            var chosen = $(this).val();
            chosen = _.find(self.graphs, function(it) {
                return it.name === chosen;
            });

            var panel = $(self.el).find("#AvgConnectionsBy"+chosen.name);
            // create objet if not null
            if(chosen.panel === null){
                var afterThan = $(self.el).find("#avgConnectionDatepickerSince").datepicker('getDate');
                var beforeThan = $(self.el).find("#avgConnectionDatepickerUntil").datepicker('getDate');
                chosen.panel = new HistogramActivitiesChart({
                    el: panel,
                    period : chosen.period,
                    url : chosen.url+"&afterThan="+afterThan.getTime()+"&beforeThan="+beforeThan.getTime(),
                    format: '.2r'
                });
                chosen.panel.render();
            }
            panel.show();
        });

        $(self.el).find("#avgConnectionsSelection").trigger("change");

        return this;
    }
});
