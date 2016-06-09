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

var AdminUsersView = Backbone.View.extend({

    rendered: false,
    render: function () {
        var self = this;
        if (!this.rendered) {
            require(["text!application/templates/admin/AdminUsers.tpl.html"],
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
        this.updateTable();
    },

    doLayout: function(tpl) {
        var self = this;

        var view = _.template(tpl, {});
        $(this.el).append(view);

        $(self.el).find("#AddNewUserBtn").click(function() {
            new AdminUserDialog({
                el: "#dialogs",
                model : new Backbone.Model(),
                callback : function() {self.updateTable()}}
            ).render();
        });

        $(self.el).on("click", ".UserEditButton", function() {
            var user = $(this).data("id");

            new UserModel({id:user}).fetch({
                success: function(model){
                    new AdminUserDialog({
                            el: "#dialogs",
                            model : model,
                            callback : function() {self.updateTable()}
                        }
                    ).render();
                }
            });
        });

        $(self.el).on("click", ".UserDetailsButton", function() {
            var user = $(this).data("id");

            new UserModel({id:user}).fetch({
                success: function(model){
                    new DetailedUserInfoDialog({
                            el: "#dialogs",
                            model : model
                        }
                    ).render();
                }
            });
        });

        this.update();

        return this;
    },

    updateTable: function () {
        var self = this;

        var table = $(this.el).find("#usersTable");
        if(table && table.dataTable()) {
            table.dataTable().fnDestroy();
        }

        var columns = [
            { "mDataProp": "id", "bSearchable": false,"bSortable": false},
            { "mDataProp": "Username", sDefaultContent: "", "bSearchable": true, "fnRender" : function(o) {
                return o.aData["username"];
            }},
            { "mData": "Lastname", sDefaultContent: "", "bSearchable": true,"bSortable": false, "fnRender" : function (o) {
                return o.aData["lastname"];
            }},
            { "mData": "Firstname", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                return o.aData["firstname"];
            }},
            { "mData": "Role", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o) {
                switch(o.aData["role"]){
                    case "ROLE_SUPER_ADMIN" :
                        return "<span class='label label-default'>Super Admin</span>";
                    case "ROLE_ADMIN" :
                        return "<span class='label label-danger'>Admin</span>";
                    case "ROLE_USER" :
                        return "<span class='label label-success'>User</span>";
                    case "ROLE_GUEST" :
                        return "<span class='label label-primary'>Guest</span>";
                }
            }},
            { "mDataProp": "email", "bSearchable": false,"bSortable": true },
            { "mData": "created", sDefaultContent: "", "bSearchable": false,"bSortable": true, "fnRender" : function (o) {
                return window.app.convertLongToPrettyDate(o.aData["created"]);
            }},
            { "mData": "updated", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function (o) {
                if(o.aData["updated"]) return window.app.convertLongToPrettyDate(o.aData["updated"]);
            }},
            { "mDataProp": "action", sDefaultContent: "", "bSearchable": false,"bSortable": false, "fnRender" : function(o) {
                return "<button class='btn btn-xs btn-primary UserDetailsButton' data-id="+o.aData["id"]+" >Info</button>"
                    +" <button class='btn btn-xs btn-primary UserEditButton' data-id="+o.aData["id"]+" >Edit</button>";
            }}
        ];

        table.dataTable({
            "bProcessing": true,
            "bServerSide": false,
            "sAjaxSource": new UserCollection({withRoles:true}).url(),
            "fnServerParams": function ( aoData ) {
                aoData.push( { "name": "datatables", "value": "true" } );
            },

            "fnDrawCallback": function(oSettings, json) {
            },
            "aoColumns" : columns,
            "aaSorting": [[ 0, "desc" ]],
            "aLengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]]
        });
    },
    getValues: function (doLayout) {
        doLayout();
    }
});
