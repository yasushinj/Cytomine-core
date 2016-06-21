package be.cytomine.social

import be.cytomine.AnnotationDomain
import be.cytomine.image.ImageInstance
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class AnnotationActionService extends ModelService {

    def securityACLService

    def add(def json){

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(JSONUtils.getJSONAttrLong(json,"annotation",0))

        securityACLService.check(annotation,READ)
        ImageInstance image = annotation.image
        SecUser user = cytomineService.getCurrentUser()
        AnnotationAction action = new AnnotationAction()
        action.user = user
        action.image = image
        action.project = image.project
        action.action = JSONUtils.getJSONAttrStr(json,"action",true)
        action.created = new Date()
        action.annotation = annotation
        action.annotationCreator = annotation.user
        action.insert(flush:true) //don't use save (stateless collection)

        return action
    }
}
