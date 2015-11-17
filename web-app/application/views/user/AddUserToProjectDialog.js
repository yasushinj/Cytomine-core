/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
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

var AddUserToProjectDialog = Backbone.View.extend({
    allUsers: [],
    projectUsers: [],
    projectAdmins: [],
    userMaggicSuggest : null,
    groups: null,
    closeCallback:null,
    initialize: function (options) {
        _.bindAll(this, 'render');
        this.closeCallback = options.closeAction;
    },
    render: function () {
        var self = this;
        require([
                "text!application/templates/user/AddUserToProjectDialog.tpl.html"
            ],
            function (tpl) {
                self.doLayout(tpl);
            });
        return this;
    },

    doLayout: function (tpl) {
        var self = this;
        var htmlCode = _.template(tpl, self.model.toJSON());
        $(this.el).html(htmlCode);

        var creation = function(){
            self.createUserList();
            self.createMultiSelectUser();

            $(self.el).find("input#addUsersByName-radio-config,input#addUsersByGroup-radio-config,input#addUsersByMail-radio-config").change(function () {
                if ($(self.el).find("input#addUsersByName-radio-config").is(':checked')) {
                    $(self.el).find("div#projectedituser").show();
                    $(self.el).find(".uix-multiselect").hide();
                    $(self.el).find("#invite_new_user").hide();
                } else if ($(self.el).find("input#addUsersByMail-radio-config").is(':checked')){
                    $(self.el).find("div#projectedituser").hide();
                    $(self.el).find("#invite_new_user").show();
                    $(self.el).find(".uix-multiselect").hide();
                } else {
                    $(self.el).find("div#projectedituser").hide();
                    $(self.el).find("#invite_new_user").hide();
                    $(self.el).find(".uix-multiselect").show();
                }
            });
            $(self.el).find("input#addUsersByName-radio-config,input#addUsersByGroup-radio-config").trigger('change');

            $(self.el).find("#invitenewuserbutton").click(function (event) {
                var username = $(self.el).find("#new_username").val();
                var mail = $(self.el).find("#new_mail").val();

                $.ajax({
                    type: "POST",
                    url: "api/project/"+self.model.id+"/invitation.json",
                    data: " {name : "+username+", mail:"+mail+"}",
                    contentType:"application/json; charset=utf-8",
                    dataType:"json",
                    success: function() {
                        window.app.view.message("Project", username+" invited!", "success");
                        self.refreshUserList(true);
                        self.loadMultiSelectUser();
                        $(self.el).find("#new_username").val("");
                        $(self.el).find("#new_mail").val("");
                    },
                    error: function(x) {
                        window.app.view.message("Project", x.responseJSON.errors, "error");
                    }
                });
            });
        };

        self.getValues(function() {
            creation();
        });

        $('#addUserToProject').on('hidden.bs.modal', function () {
            self.closeCallback();
        });

        $("#addUserToProject").modal('show');
    },
    getValues: function (doLayout) {
        var self = this;
        var allUser = null;
        var projectUser = null;
        var projectAdmin = null;


        var loadUsers = function() {
            if(allUser == null || projectUser == null || projectAdmin == null) {
                return;
            }

            self.allUsers = [];
            self.projectUsers = [];
            self.projectAdmins = [];

            allUser.each(function(user) {
                self.allUsers.push({id:user.id,label:user.prettyName()});
            });

            projectUser.each(function(user) {
                self.projectUsers.push(user.id);
            });


            projectAdmin.each(function(user) {
                self.projectAdmins.push(user.id);
            });

            doLayout();

        };

        new UserCollection({}).fetch({
            success: function (allUserCollection) {
                allUser = allUserCollection;
                loadUsers();
            }
        });

        new UserCollection({project: self.model.id}).fetch({
            success: function (projectUserCollection) {
                projectUser = projectUserCollection;
                window.app.models.projectUser = projectUserCollection;
                loadUsers();
            }
        });

        new UserCollection({project: self.model.id, admin:true}).fetch({
            success: function (projectUserCollection) {
                projectAdmin = projectUserCollection;
                window.app.models.projectAddmin = projectUserCollection;
                loadUsers();
            }
        });

    },
    createUserList: function () {
        var self = this;
        self.userMaggicSuggest = $(self.el).find('#projectedituser').magicSuggest({
            data: self.allUsers,
            displayField: 'label',
            value: self.projectUsers,
            width: 590,
            maxSelection:null
        });
        $(self.userMaggicSuggest).on('selectionchange', function(e,m){
            self.projectUsers = this.getValue();
            self.update(function() {
                self.loadMultiSelectUser();
            });
        });
    },
    createMultiSelectUser: function() {

        var self = this;

        $(self.el).find("#usersByGroup").multiselectNext().bind("multiselectChange", function(evt, ui) {

            self.projectUsers = [];
            //var values = $.map(ui.optionElements, function(opt) { return $(opt).attr('value'); });
            //console.log("Multiselect change event! " + ui.optionElements.length + ' value ' + (ui.selected ? 'selected' : 'deselected') + ' (' + values + ')');
            $(this).find("option:selected").each(function(i, o) {
                self.projectUsers.push(o.value);
            });
            self.update(function() {
                self.refreshUserList();
            });

            $(self.el).find("#usersByGroup").multiselectNext('refresh', function() {
                $(self.el).find(".ui-button-icon-only .ui-icon").css("margin-top", "-8px");
            });
        });

        $(self.el).find(".ui-button-icon-only .ui-icon").css("margin-top", "-8px");
        $(self.el).find("div.uix-multiselect").css("background-color", "#DDDDDD");

        self.loadMultiSelectUser();
    },
    loadMultiSelectUser: function() {

        var self = this;
        var currentUsers;

        $(self.el).find("#usersByGroup").empty();
        $(self.el).find("#usersByGroup").multiselectNext('refresh');

        // I need to restart multiselect to include to options append to the select
        var reload = function(currentUsers, groupUsers) {
            if(currentUsers==null || groupUsers==null || currentUsers==undefined || groupUsers==undefined) {
                return;
            }

            currentUsers.each(function(user) {
                if($.inArray( user.id, self.projectAdmins ) === -1){
                    $(self.el).find("#usersByGroup").append('<option value="' + user.id + '" selected>' + user.prettyName() + '</option>');
                } else {
                    $(self.el).find("#usersByGroup").append('<option value="' + user.id + '" selected disabled>' + user.prettyName() + '</option>');
                }
            });

            var ids = $.map( currentUsers.models, function( a ) {
                return a.id;
            });

            groupUsers.each(function(group) {

                $(self.el).find("#usersByGroup").append('<optgroup label="'+group.attributes.name+'">');
                var optGroup = $(self.el).find("#usersByGroup optgroup").last();
                for(var i=0; i<group.attributes.users.length ; i++) {
                    var currentUser = group.attributes.users[i];
                    if($.inArray( currentUser.id, ids ) === -1){
                        optGroup.append('<option value="' + currentUser.id + '">' + currentUser.lastname + ' ' + currentUser.firstname + '(' + currentUser.username + ')' + '</option>');
                    } else {
                        optGroup.append('<option value="' + currentUser.id + '" disabled>' + currentUser.lastname + ' ' + currentUser.firstname + '(' + currentUser.username + ')' + '</option>');
                    }
                }
                $(self.el).find("#usersByGroup").append('</optgroup>');


            });
            $(self.el).find("#usersByGroup").multiselectNext('refresh', function() {
                $(self.el).find(".ui-button-icon-only .ui-icon").css("margin-top", "-8px");
            });
        };


        // load current users
        new UserCollection({project:self.model.id}).fetch({
            success: function (currentUsersCollection) {
                currentUsers = currentUsersCollection;
                reload(currentUsers, self.groups);
            }
        });

        // do a request to have all users of this group
        new GroupWithUserCollection().fetch({
            success: function (groupUsersCollection, response) {
                self.groups = groupUsersCollection;
                reload(currentUsers, self.groups);
            }
        });
    },
    refreshUserList: function (reloadAllUsers) {
        var self = this;
        var projectUsers = null;
        var allUsers = null;
        var reloadDone = false;

        var loadUser = function() {

            if(projectUsers == null || (reloadAllUsers && !reloadDone)) {
                return;
            }

            var projectUserArray=[];
            projectUsers.each(function(user) {
                projectUserArray.push(user.id);
            });

            // Avoid an infinite loop between the 2 listeners.
            $(self.userMaggicSuggest).off('selectionchange');
            self.userMaggicSuggest.clear();
            self.userMaggicSuggest.setValue(projectUserArray);

            $(self.userMaggicSuggest).on('selectionchange', function(e,m){
                self.projectUsers = this.getValue();
                self.update(function() {
                    self.loadMultiSelectUser();
                });
            });
        };

        new UserCollection({project: self.model.id}).fetch({
            success: function (projectUserCollection, response) {
                projectUsers = projectUserCollection;
                window.app.models.projectUser = projectUserCollection;
                loadUser();
            }});


        if(reloadAllUsers) {
            new UserCollection({}).fetch({
                success: function (allUserCollection, response) {
                    var allUserArray = [];
                    allUserCollection.each(function(user) {
                        allUserArray.push({id:user.id,label:user.prettyName()});
                    });

                    self.userMaggicSuggest.setData(allUserArray);
                    reloadDone = true;
                    loadUser();
                }});

        }
   },
    update: function(callbackSuccess) {
        var self = this;

        var project = self.model;

        var users = self.projectUsers;

        // CHHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANGE THIS !!!, we will not save automatically but when the admin click on save

        project.set({users: users});
        project.save({users:users}, {
            success: function (model, response) {
                console.log("1. Project edited!");
                window.app.view.message("Project", response.message, "success");
                if(callbackSuccess != null && callbackSuccess != undefined) {
                    callbackSuccess();
                }
                // here, we need a refresh of the DefaultLayerPanel as the users have changed !!!
                self.callback(users);
            },
            error: function (model, response) {
                var json = $.parseJSON(response.responseText);
                window.app.view.message("Project", json.errors, "error");
            }
        });
    }
});