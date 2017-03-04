/**
 * Created by laurent
 * Date : 16.02.17.
 */

var ImageGroupTmpView = Backbone.View.extend({

    tagName: "li",

    render: function () {
        var self = this;

            var tpl = "<li><%= filename %></li>"; //template html de base pour un
            this.model.each(function (image) { //on suppose que this.model existe car passé dans le constructeur
                //rendu HTML du user. La variable html contientra alors : "<li>John doe</li>"
                //pour le user dont le username est "johndoe"
               console.log(image.toJSON());
                var html = _.template(tpl, image.toJSON());
                console.log(html);
                //Enfin, on affiche le code généré dans la page HTML
                $("#projectInfoPanel").find("#imagegroup").append(html);

            });


        return this;
    },

    refresh: function () {
        // mise à jour de la liste
    }

});