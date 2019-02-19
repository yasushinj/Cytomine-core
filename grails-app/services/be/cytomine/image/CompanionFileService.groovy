package be.cytomine.image

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

class CompanionFileService extends ModelService {

    static transactional = true
    def cytomineService

    def currentDomain() {
        return CompanionFile
    }

    def read(def id) {
        def file = CompanionFile.read(id)
        if (file) {
            //TODO: security: can read image / uf
            //TODO: checkDeleted
        }
        file
    }

    def list(AbstractImage image) {
        //TODO: security: can read image

        CompanionFile.findAllByImage(image)
    }

    def add(def json) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(CompanionFile file, def json) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, file, json)
    }

    def delete(CompanionFile file, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, file, null)
    }
}
