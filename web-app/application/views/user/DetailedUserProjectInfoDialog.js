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

var DetailedUserProjectInfoDialog = Backbone.View.extend({
    initialize: function (options) {
        _.bindAll(this, 'render');
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/user/DetailedUserProjectInfoDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },

    doLayout: function (tpl) {
        var self = this;

        console.log("model");
        console.log(self.model);
        console.log(self.model.toJSON());

        var htmlCode = _.template(tpl, self.model.toJSON());
        $(this.el).html(htmlCode);

        /*var creation = function(){

        };

        $(this.el).find("#detailedUserInfoContent").hide();

        self.getValues(function() {
            $(self.el).find("#detailedUserInfoWaitingDiv").hide();
            $(self.el).find("#detailedUserInfoContent").show();
            creation();
        });*/

        $("#detailedUserInfoDialog").modal('show');
    },
    getValues: function (callback) {
        var self = this;
    }
});