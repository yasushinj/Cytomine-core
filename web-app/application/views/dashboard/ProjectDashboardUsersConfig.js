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
    statsView : null,
    initialize: function () {
        this.rendered = false;
    },
    render: function () {
        var self = this;
        if (!this.rendered) {
            require(["text!application/templates/dashboard/config/UsersConfig2.tpl.html"],
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
                if (id == "41") {
                    $('td', $nRow).css({"background-color":"red"});
                }
                return nRow;
            },

            "fnDrawCallback": function(oSettings, json) {
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]]


        });


    },
    updateMagics: function () {
        var self = this;
        self.adminMagicSuggest.setData(self.projectUsers);
        self.adminMagicSuggest.setValue(self.projectAdmins);
        self.representativeMagicSuggest.setData(self.projectUsers);
        // add representative
        //self.representativeMagicSuggest.setValue(self.projectAdmins);
    },
    getValues: function (callBack) {
        var self = this;
        var projectUser = null;
        var projectRepresentative = null;
        var projectAdmin = null;


        var loadUsers = function() {
            if(projectUser == null || projectAdmin == null /*|| projectRepresentative == null*/) {
                return;
            }

            self.projectUsers = [];
            self.projectAdmins = [];

            projectUser.each(function(user) {
                self.projectUsers.push({id:user.id,label:user.prettyName()});
            });


            projectAdmin.each(function(user) {
                self.projectAdmins.push(user.id);
            });

            /*projectRepresentative.each(function(user) {
                self.projectRepresentatives.push(user.id);
            });*/

            callBack();

        };

        new UserCollection({project: self.model.id}).fetch({
            success: function (projectUserCollection) {
                projectUser = projectUserCollection;
                window.app.models.projectUser = projectUserCollection;
                loadUsers();
            }});

        new UserCollection({project: self.model.id, admin:true}).fetch({
            success: function (projectUserCollection) {
                projectAdmin = projectUserCollection;
                window.app.models.projectAddmin = projectUserCollection;
                loadUsers();
            }});

        // need to get the Representative too


    },

    doLayout: function (imageTableTemplate) {
        var self = this;

        // table
        var view = _.template(imageTableTemplate, {id : self.model.get('id')});
        $(this.el).append(view);

        $(this.el).find("#UserRefresh"+self.model.get('id')).on("click", function() {
            self.update();
        });




        $(this.el).find("#ProjectUserDelete"+self.model.get('id')).on("click", function() {
            console.log("delete button");

            var users = $(self.el).find(".userchckbox-"+self.model.get('id')+":checked");

            users = $.map( users, function(n) {
                return ( n.dataset.id );
            });

            // donc ici appeller une customModal qui va demander si on est sur blabla et puis on delete tout ces users.
            // si y a des admins, mettre un warning dans la customModal
            // si y a le user en cours, mettre le panneau rouge disant que c'est impossible pour cette raison là
            console.log(users)
        });





        $(this.el).find("#ShowOnlyOnline"+self.model.get('id')).change(function() {
            if ($(this).is(':checked')) {
                self.showOnlyOnlineUsers = true;
            } else {
                self.showOnlyOnlineUsers = false;
            }
            self.update();
        });
        $(this.el).find("#ProjectUserAdd"+self.model.get('id')).on("click", function() {
            console.log("add button")
            /////////////////// NOPE seulement un callback pour la save action ! Et cela va aussi sauver les new user dans le projet AVANT de faire le refresh
            new AddUserToProjectDialog({el: "#dialogs", model: self.model, closeAction: function(){self.update()}}).render();
        });


        $(this.el).on("change", ".userchckbox-"+self.model.get('id'), function() {
            console.log("test");
            console.log($(this).data("id"));
            if ($(this).is(':checked')) {
                console.log("test2");
            } else {
                console.log("test3");
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

        //$(this.el).append(view)
        if (this.statsView == null) {
            this.statsView = new ProjectDashboardUsersStatsView({
                model: self.model,
                el: $(self.el).find("#UsersActivitiesGraph"+self.model.get('id'))
            });
        }

        this.statsView.render();


    }
});
