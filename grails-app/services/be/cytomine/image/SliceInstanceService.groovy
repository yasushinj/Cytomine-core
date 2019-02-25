package be.cytomine.image

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class SliceInstanceService extends ModelService {

    static transactional = true

    def cytomineService
    def securityACLService

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
