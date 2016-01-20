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

var ProjectDashboardUsersConfig = Backbone.View.extend({
    adminMagicSuggest: null,
    representativeMagicSuggest: null,
    projectUsers: [],
    projectAdmins: [],
    projectRepresentatives: [],
    showOnlyOnlineUsers : false,
    showOnlyAdmins : false,
    statsUsersHeatmapView : null,
    statsUsersGlobalActivitiesView: null,
    initialize: function () {
        this.rendered = false;
    },
    render: function () {
        var self = this;
        if (!this.rendered) {
            require(["text!application/templates/dashboard/config/UsersConfig.tpl.html"],
                function (imageTableTemplate) {
                    self.doLayout(imageTableTemplate);
                    self.rendered = true;
                });
        } else {
            this.update();
        }
    },
    update: function () {
        var self = this;
        this.getValues(function(){
            self.updateMagics();
        });
        this.updateTable();
    },
    updateTable: function () {
        var self = this;

        var table = $(this.el).find("#userProjectTable" + self.model.get('id'));
        if(table && table.dataTable()) {
            table.dataTable().fnDestroy();
        }

        var columns = [
            { "mDataProp": "", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return "<input type='checkbox' data-id='"+o.aData["id"]+"' class='userchckbox-"+self.model.get('id')+"'>";
            }},
            //{ sClass: 'center', "mData": "id", "bSearchable": false},
            { "mDataProp": "Username", sDefaultContent: "", "bSearchable": false, "fnRender" : function(o) {
                return o.aData["username"];
            }},
            { "mData": "Fullname", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                return o.aData["firstname"] + " "+o.aData["lastname"];
            }},
            { "mDataProp": "LastConnexion", sDefaultContent: "", "bSearchable": false,"fnRender" : function(o) {
                var last = o.aData["lastConnection"];
                if(last === null) {
                    last = "No record";
                } else {
                    last = window.app.convertLongToPrettyDate(last);
                }
                return last;
            }},
            { "mDataProp": "LastImg", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                var last = o.aData["lastImageName"];
                if(last === null) {
                    last = "No record";
                } else {
                    // TODO Put a link to the image ? or display the image ?
                }
                return last;
            }},
            { "mDataProp": "LDAP", sDefaultContent: "", "bSearchable": false,"fnRender" : function(o) {
                if(o.aData["LDAP"]){
                    return "<div class = 'led-green'></div>";
                } else {
                    return "<div class = 'led-red'></div>";
                }
            }},
            { "mDataProp": "email", "bSearchable": false,"bSortable": true },
            { "mDataProp": "nbVisit", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function(o) {
                return o.aData["frequency"];

            }},
            { "mDataProp": "action", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                o.aData["project"]  = self.model.get('id');
                return _.template('<button class="btn btn-info btn-xs" id="UserDetailsButton<%=  id  %>">Details</button>', o.aData);
            }}
        ];

        table.dataTable({
            "bProcessing": true,
            "bServerSide": true,
            "sAjaxSource": new UserActivitiesCollection({project: this.model.get('id'), admins : self.showOnlyAdmins, online : self.showOnlyOnlineUsers}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },

            "fnRowCallback": function( nRow, aData ) {
                var id = aData.id; // ID is returned by the server as part of the data
                var $nRow = $(nRow); // cache the row wrapped up in jQuery
                // TODO a better color. Put a color for online users ?
                if (self.projectAdmins.indexOf(id) >= 0) {
                    $('td', $nRow).css({"background-color":"green"});
                }
                if (id == window.app.status.user.id) {
                    $('td', $nRow).css({"background-color":"red"});
                }
                return nRow;
            },

            "fnDrawCallback": function(oSettings, json) {
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]],
            "aLengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]]


        });

        $('#selectAllUsers'+self.model.id).prop('checked', false);


    },
    updateMagics: function () {
        var self = this;
        $(self.adminMagicSuggest).off('selectionchange');
        $(self.representativeMagicSuggest).off('selectionchange');

        self.adminMagicSuggest.setData(self.projectUsers);
        self.adminMagicSuggest.setValue(self.projectAdmins);
        self.representativeMagicSuggest.setData(self.projectUsers);
        self.representativeMagicSuggest.setValue(self.projectRepresentatives);

        $(self.adminMagicSuggest).on('selectionchange', function(e,m){
            self.projectAdmins = this.getValue();
            self.updateUsersInProject(null,self.projectAdmins,null);
        });
        $(self.representativeMagicSuggest).on('selectionchange', function(e,m){
            self.projectRepresentatives = this.getValue();
            self.updateUsersInProject(null, null, self.projectRepresentatives);
        });

    },
    getValues: function (callBack) {
        var self = this;
        var projectUser = null;
        var projectRepresentative = null;
        var projectAdmin = null;


        var loadUsers = function() {
            if(projectUser == null || projectAdmin == null || projectRepresentative == null) {
                return;
            }

            self.projectUsers = [];
            self.projectAdmins = [];
            self.projectRepresentatives = [];

            projectUser.each(function(user) {
                self.projectUsers.push({id:user.id,label:user.prettyName()});
            });


            projectAdmin.each(function(user) {
                self.projectAdmins.push(user.id);
            });

            projectRepresentative.each(function(user) {
                self.projectRepresentatives.push(user.id);
            });

            callBack();

        };

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
                window.app.models.projectAdmin = projectUserCollection;
                loadUsers();
            }
        });

        new UserCollection({project: self.model.id, representative:true}).fetch({
            success: function (projectUserCollection) {
                projectRepresentative = projectUserCollection;
                window.app.models.projectRepresentative = projectUserCollection;
                loadUsers();
            }
        });
    },

    doLayout: function (imageTableTemplate) {
        var self = this;

        // table
        var view = _.template(imageTableTemplate, {id : self.model.get('id')});
        $(this.el).append(view);

        $(this.el).find("#ProjectUserAdd"+self.model.get('id')).on("click", function() {
            //The close action save the user modifications
            new AddUserToProjectDialog({el: "#dialogs", model: self.model, closeAction: function(newUsersId){
                self.addUsersInProject(newUsersId);
            }}).render();
        });
        $(this.el).find("#UserRefresh"+self.model.get('id')).on("click", function() {
            self.updateTable();
        });

        $(this.el).find("#ProjectUserExport"+self.model.get('id')).on("click", function() {
            window.open("/api/project/"+self.model.id+"/user/download?format=csv");
        });




        $(this.el).find("#ProjectUserDelete"+self.model.get('id')).on("click", function() {
            self.deleteUsersInProject();
        });

        $(this.el).find("#ProjectUserArchive"+self.model.get('id')).on("click", function() {
            self.archiveAnnotationsOfUsers();
        });




        $(this.el).find("#ShowOnlyOnline"+self.model.get('id')).change(function() {
            if ($(this).is(':checked')) {
                self.showOnlyOnlineUsers = true;
            } else {
                self.showOnlyOnlineUsers = false;
            }
            self.updateTable();
        });
        $(this.el).find("#ShowOnlyAdmin"+self.model.get('id')).change(function() {
            if ($(this).is(':checked')) {
                self.showOnlyAdmins = true;
            } else {
                self.showOnlyAdmins = false;
            }
            self.updateTable();
        });

        $('#selectAllUsers'+self.model.id).on('change', function(){
            $(".userchckbox-"+self.model.get('id')).prop('checked', $(this).prop('checked'));
        });

        $(this.el).on("change", ".userchckbox-"+self.model.get('id'), function() {
            console.log($(this).data("id"));
            if ($(this).prop('checked')) {
                if($(".userchckbox-"+self.model.get('id')+":checked").length === $(".userchckbox-"+self.model.get('id')).length){
                    $('#selectAllUsers'+self.model.id).prop('checked', true);
                }
            } else {
                $('#selectAllUsers'+self.model.id).prop('checked', false);
            }
        });


        // magicsuggest
        self.adminMagicSuggest = $(self.el).find('#projecteditmanager'+self.model.get('id')).magicSuggest({
            data: null,
            displayField: 'label',
            value: null,
            width: 590,
            maxSelection:null
        });
        self.representativeMagicSuggest = $(self.el).find('#projecteditrepresentative'+self.model.get('id')).magicSuggest({
            data: null,
            displayField: 'label',
            value: null,
            width: 590,
            maxSelection:null
        });


        //var isAdmin = window.app.status.currentProjectModel.isAdmin(window.app.models.projectAdmin);




        self.update();

        if (this.statsUsersGlobalActivitiesView == null) {
            this.statsUsersGlobalActivitiesView = new ProjectUsersTotalActivitiesView({
                model: self.model,
                el: $(self.el).find(".custom-ui-project-users-global-activities-graph")[0]
            });
        }
        if (this.statsUsersHeatmapView == null) {
            this.statsUsersHeatmapView = new ProjectUsersHeatmapView({
                model: self.model,
                el: $(self.el).find(".custom-ui-project-users-heatmap-graph")[0]
            });
        }

        this.statsUsersGlobalActivitiesView.render();
        this.statsUsersHeatmapView.render();


    },

    archiveAnnotationsOfUsers: function() {
        var self = this;
        var usersToArchive = $(self.el).find(".userchckbox-"+self.model.get('id')+":checked");

        usersToArchive = $.map( usersToArchive, function(n) {
            return ( Number(n.dataset.id) );
        });

        if(usersToArchive.length == 0) return;

        var level = 'CONFIRMATIONWARNING';
        // todo put the after br in red
        var message = "Do you really want to archive these users ? <br/><label class='label label-danger'>You won't be able to reverse this!</label>";
        var callback = function(){
            // TODO : A post request not yet implemented
        };

        DialogModal.initDialogModal(null, self.model.id, 'ArchiveUsers', message, level, callback);
    },
    addUsersInProject: function(newUsersId) {
        var self = this;
        var users = [];

        new UserCollection({project: self.model.id}).fetch({
            success: function (projectUserCollection) {
                projectUserCollection.each(function (user) {
                    users.push(user.id)
                });
                for(var i = 0; i< newUsersId.length; i++) {
                    users.push(newUsersId[i]);
                }
                self.updateUsersInProject(users, null, null)
            }
        });
    },
    deleteUsersInProject: function() {
        var self = this;

        var usersToDelete = $(self.el).find(".userchckbox-"+self.model.get('id')+":checked");

        usersToDelete = $.map( usersToDelete, function(n) {
            return ( Number(n.dataset.id) );
        });

        if(usersToDelete.length == 0) return;

        var level = 'CONFIRMATIONWARNING';
        var message = 'Do you want to delete these users ?';
        var callback = null;

        // cannot delete current users and admins.
        // TODO if a projectRepresentative is deleted he will be no more a representative.
        if(usersToDelete.indexOf(window.app.status.user.id)>=0){
            level = 'ERROR';
            message = 'Impossible to delete these users. You cannot delete yourself of a project.';
        } else {
            var checkAdminSelected = self.projectAdmins.some(function(currentValue) {
                return usersToDelete.indexOf(currentValue)>=0;
            });

            if(checkAdminSelected) {
                message += "<br/>Be careful, some project managers are selected!";
            }

            callback = function(){
                var users = [];

                new UserCollection({project: self.model.id}).fetch({
                    success: function (projectUserCollection) {
                        projectUserCollection.each(function (user) {
                            if(usersToDelete.indexOf(user.id) == -1) {
                                users.push(user.id)
                            }
                        });
                        self.updateUsersInProject(users, null, null)
                    }
                });
            };
        }

        DialogModal.initDialogModal(null, self.model.id, 'DeleteUsers', message, level, callback);
    },
    updateUsersInProject: function(projectUsers, projectManagers, projectRepresentatives) {
        var self = this;

        var project = self.model;

        project.set({users: projectUsers, admins: projectManagers, representatives: projectRepresentatives});
        project.save({users:projectUsers, admins: projectManagers, representatives: projectRepresentatives}, {
            success: function (model, response) {
                console.log("1. Project edited!");
                window.app.view.message("Project", response.message, "success");
                self.update();
                // TODO update the config panel (for default layers) for admins and users
                // TODO update the dashboard panel as representative is also there
            },
            error: function (model, response) {
                var json = $.parseJSON(response.responseText);
                window.app.view.message("Project", json.errors, "error");
            }
        });
    }

});
