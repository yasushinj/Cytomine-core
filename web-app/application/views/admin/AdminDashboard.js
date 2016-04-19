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
    totals : {},
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
        } else {
            this.update();
        }
    },
    update: function () {
        console.log("dashboard update");
    },

    doLayout: function(tpl) {
        var self = this;

        var modelView = self.totals;
        modelView.instance = window.location.host;

        var view = _.template(tpl, modelView);
        $(this.el).append(view);


        $(self.el).find("#currentStats").append();


        // TODO not yet finished : the url doesn't return the project name
        /*new ProjectUsersTotalActivitiesGraph({
            model: self.model,
            el: $(self.el).find("#test"),
            url : "api/total/project/connections.json",
            property : "total"
        }).render();*/


        this.update();

        return this;
    },

    getValues: function (callback) {
        var self = this;
        $.get( "api/stats/all.json", function( data ) {
            self.totals = data;
            callback();
        });
    }
});
