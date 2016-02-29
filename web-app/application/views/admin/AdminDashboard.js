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
    rendered: false,
    render: function () {
        var self = this;

        if (!this.rendered) {
            self.getValues(function() {
                self.doLayout();
                self.rendered = true;
            });
        } else {
            this.update();
        }
    },
    update: function () {
        console.log("dashboard update");
    },

    doLayout: function() {
        var self = this;

        console.log("dashboard doLayout");

        $(self.el).append("<p>Test TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest " +
            "TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest" +
            " TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest" +
            " TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest" +
            " TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest" +
            " TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest TestTest" +
            " Test</p>");

        this.update();

        return this;
    },

    getValues: function (doLayout) {
        doLayout();
    }
});
