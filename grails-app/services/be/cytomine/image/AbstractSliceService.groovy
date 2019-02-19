package be.cytomine.image

import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

class AbstractSliceService extends ModelService {

    static transactional = true
    
    def cytomineService

    def currentDomain() {
        return AbstractSlice
    }

    def read(def id) {
        def slice = AbstractSlice.read(id)
        if (slice) {
            //TODO: security: can read image / uf
            //TODO: checkDeleted
        }
        slice
    }

    def read(AbstractImage image, double c, double z, double t) {
        def slice = AbstractSlice.findByImageAndChannelAndZStackAndTime(image, c, z, t)
        if (slice) {
            // TODO: security
        }
        slice
    }

    def list(AbstractImage image) {
        //TODO: security: can read image

        AbstractSlice.findAllByImage(image)
    }

    def add(def json) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(AbstractSlice slice, def json) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, slice, json)
    }

    def delete(AbstractSlice slice, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        //TODO: security
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, slice, null)
    }
}
