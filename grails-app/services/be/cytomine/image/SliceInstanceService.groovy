package be.cytomine.image

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.SQLUtils
import be.cytomine.utils.Task
import groovy.sql.Sql

import java.nio.file.Paths

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class SliceInstanceService extends ModelService {

    static transactional = true

    def cytomineService
    def securityACLService
    def dataSource

    def currentDomain() {
        return SliceInstance
    }

    def read(def id) {
        SliceInstance slice = SliceInstance.read(id)
        if (slice) {
            securityACLService.check(slice.container(), READ)
        }
        slice
    }

    def read(ImageInstance image, double c, double z, double t) {
        SliceInstance slice = SliceInstance.createCriteria().get {
            createAlias("baseSlice", "as")
            eq("image", image)
            eq("as.channel", c)
            eq("as.zStack", z)
            eq("as.time", t)
        }
        if (slice) {
            securityACLService.check(slice.container(), READ)
        }
        slice
    }

    def list(ImageInstance image) {
        securityACLService.check(image, READ)
        SliceInstance.findAllByImage(image)
    }

    def listWithRank(ImageInstance image) {
        securityACLService.check(image, READ)

        String request = "SELECT si.id AS id, " +
                "uf.id AS uploaded_file, " +
                "ise.base_path AS base_path, " +
                "uf.user_id AS user_path, " +
                "uf.filename AS filename, " +
                "si.image_id AS image, " +
                "m.mime_type AS mime_type, " +
                "bs.channel AS channel, " +
                "bs.z_stack AS z_stack, " +
                "bs.time AS time, " +
                "(DENSE_RANK() OVER (ORDER BY bs.channel) - 1) AS channel_rank, " +
                "(DENSE_RANK() OVER (ORDER BY bs.z_stack) - 1) AS z_stack_rank, " +
                "(DENSE_RANK() OVER (ORDER BY bs.time) - 1) AS time_rank " +
                "FROM slice_instance AS si " +
                "INNER JOIN abstract_slice AS bs ON si.base_slice_id = bs.id " +
                "INNER JOIN uploaded_file AS uf ON bs.uploaded_file_id = uf.id " +
                "INNER JOIN image_server AS ise ON uf.image_server_id = ise.id " +
                "INNER JOIN mime AS m ON bs.mime_id = m.id " +
                "WHERE si.image_id = :image " +
                "ORDER BY bs.time, bs.channel, bs.z_stack;"

        def data = []
        def sql = new Sql(dataSource)
        sql.eachRow(request, [image: image.id]) { resultSet ->
            def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
            row.path = Paths.get(row.basePath, row.userPath as String, row.filename).toString()
            row.remove("basePath")
            row.remove("userPath")
            row.remove("filename")
            data << row
        }
        sql.close()

        return data
    }

    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)
        securityACLService.check(json.project, Project,READ)
        securityACLService.checkisNotReadOnly(json.project,Project)

        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(SliceInstance slice, def json) {
        securityACLService.check(slice.container(),READ)
        securityACLService.check(json.project,Project,READ)
//        securityACLService.checkFullOrRestrictedForOwner(slice.container(),slice.user)
        securityACLService.checkisNotReadOnly(slice.container())
        securityACLService.checkisNotReadOnly(json.project,Project)
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, slice, json)
    }

    def delete(SliceInstance slice, Transaction transaction = null, Task task = null, boolean printMessage = true) {
//        securityACLService.checkAtLeastOne(slice, READ)
        //TODO security
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, slice, null)
    }
}
