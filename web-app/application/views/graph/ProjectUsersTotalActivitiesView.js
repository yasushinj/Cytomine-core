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

var ProjectUsersTotalActivitiesView = Backbone.View.extend({
    activities : [],
    initialize: function () {
        this.activities.push({name : "Connexions",  url : "api/project/" + this.model.get('id') + "/usersActivity.json", property : "frequency", panel : null});
        this.activities.push({name : "Annotations", url : "api/project/" + this.model.get('id') + "/stats/user.json", property : "value", panel : null});
    },
    render: function () {
        var self = this;

        var tpl = '<h4 class="header_h"><i class="glyphicon glyphicon-stats"></i> Activities of contributors</h4>'+
            '<select id="activitySelection<%= id %>">' +
                '<option>Select an activity</option>';

        _.each(self.activities, function(it){
            tpl+= '<option>'+it.name+'</option>';
        });

        tpl += '</select>';

        _.each(self.activities, function(it){
            tpl+= '<div class="col-md-offset-1 graph" id="UsersGlobal'+it.name+'<%= id %>" style="text-align: center;"></div>';
        });


        $(self.el).append(_.template(tpl, {id : self.model.get('id')}));


        $(self.el).find("#activitySelection"+self.model.id).on("change", function(e){

            $(self.el).find(".graph").hide();

            var chosen = $(this).val();
            chosen = _.find(self.activities, function(it) {
                return it.name === chosen;
            });

            if(!window.app.isUndefined(chosen)){
                var panel = $(self.el).find("#UsersGlobal"+chosen.name+self.model.id);
                // create objet if not null
                if(chosen.panel === null){
                    chosen.panel = new ProjectUsersTotalActivitiesGraph({
                        model: self.model,
                        el: panel,
                        url : chosen.url,
                        property : chosen.property
                    });
                    chosen.panel.render();
                }
                panel.show();
            }
        });
    }
});
