/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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
        var columns = [
            { data: "id", orderable: false, targets: [0]},
            { data: "username", targets: [1]},
            { data: "lastname", orderable: false, targets: [2]},
            { data: "firstname", orderable: false, targets: [3]},
            { data: "role", render : function (data) {
                switch(data){
                    case "ROLE_SUPER_ADMIN" :
                        return "<span class='label label-default'>Super Admin</span>";
                    case "ROLE_ADMIN" :
                        return "<span class='label label-danger'>Admin</span>";
                    case "ROLE_USER" :
                        return "<span class='label label-success'>User</span>";
                    case "ROLE_GUEST" :
                        return "<span class='label label-primary'>Guest</span>";
                }
            }, targets: [4]},
            { data: "email", targets: [5]},
            { data: "created", render : function ( data, type ) {
                if(type === "display"){
                    return window.app.convertLongToPrettyDate(data);
                } else {
                    return data
                }
            }, targets: [6]},
            { data: "updated", defaultContent: "No record", orderable: false, render : function (data) {
                return window.app.convertLongToPrettyDate(data);
            }, targets: [7]},
            { data: "id", orderable: false, render : function( data, type, row ) {
                return "<button class='btn btn-xs btn-primary UserDetailsButton' data-id="+data+" >Info</button>"
                    +" <button class='btn btn-xs btn-primary UserEditButton' data-id="+data+" >Edit</button>";
            }, targets: [8]},
            { searchable: true, targets: [0,1,2,5] },
            { searchable: false, targets: [3,4,6,7,8] }
        ];

        table.DataTable({
            destroy: true,
            processing: true,
            serverSide: false,
            ajax: {
                url: new UserCollection({withRoles:true}).url(),
                data: {
                    "datatables": "true"
                }
            },

            columnDefs : columns,
            order: [[ 0, "desc" ]],
            pageLength: 50,
            lengthMenu: [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]]
        });
    },
    getValues: function (doLayout) {
        doLayout();
    }
});
