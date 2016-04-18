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

var AdminController = Backbone.Router.extend({

    view: null,
    initialize: function () {
    },

    routes: {
        "admin": "dashboard",
        "admin-tabs-dashboard": "dashboard",
        "admin-tabs-users": "users",
        "admin-tabs-groups": "groups",
        "admin-tabs-permissions": "permissions",
        "admin-tabs-config": "config"
    },

    init: function (callback) {
        $(window).scrollTop(0);
        if (this.view == null) {
            this.createView(callback);
        } else {
            callback();
        }
        this.showView();

    },

    createView: function (callback) {
        console.log("createView");
        var self = this;

        self.view = new AdminView({
            el: $("#admin-tab-content")
        }).render();
        callback.call();
    },
    showView: function () {
        window.app.view.showComponent(window.app.view.components.admin);
    },

    dashboard: function () {

        var self = this;
        var func = function () {
            self.view.refreshDashboard();
            var tabs = $("#admin").find(".nav-tabs");
            tabs.find('a[href=#admin-tabs-dashboard]').tab('show');
        };
        self.init(func);
    },


    users: function () {

        var self = this;
        var func = function () {
            self.view.refreshUsers();
            var tabs = $("#admin").find(".nav-tabs");
            tabs.find('a[href=#admin-tabs-users]').tab('show');
        };
        self.init(func);
    },

    groups: function () {

        var self = this;
        var func = function () {
            self.view.refreshGroups();
            var tabs = $("#admin").find(".nav-tabs");
            tabs.find('a[href=#admin-tabs-groups]').tab('show');
        };
        self.init(func);
    },

    permissions: function () {

        var self = this;
        var func = function () {
            self.view.refreshPermissions();
            var tabs = $("#admin").find(".nav-tabs");
            tabs.find('a[href=#admin-tabs-permissions]').tab('show');
        };
        self.init(func);
    },

    config: function () {

        var self = this;
        var func = function () {
            self.view.refreshConfig();
            var tabs = $("#admin").find(".nav-tabs");
            tabs.find('a[href=#admin-tabs-config]').tab('show');
        };
        self.init(func);
    }
});