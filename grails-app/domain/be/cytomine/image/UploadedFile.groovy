package be.cytomine.image

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

import be.cytomine.CytomineDomain
import be.cytomine.Exception.CytomineException
import be.cytomine.Exception.WrongArgumentException
import be.cytomine.api.UrlApi
import be.cytomine.image.server.ImageServer
import be.cytomine.image.server.Storage
import be.cytomine.image.server.StorageAbstractImage
import be.cytomine.postgresql.LTreeType
import be.cytomine.security.SecUser
import be.cytomine.security.UserJob
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField
import org.restapidoc.annotation.RestApiObjectFields

/**
 * An UploadedFile is a file uploaded through the API.
 * Uploaded are temporaly instances, files related to them are placed
 * in a buffer space before being converted into the right format and copied to the storages
 */
@RestApiObject(name = "Uploaded file", description = "A file uploaded on the server")
class UploadedFile extends CytomineDomain implements Serializable{

    public static int UPLOADED = 0
    public static int CONVERTED = 1
    public static int DEPLOYED = 2
    public static int ERROR_FORMAT = 3
    public static int ERROR_CONVERSION = 4
    public static int UNCOMPRESSED = 5
    public static int TO_DEPLOY = 6
    public static int TO_CONVERT = 7
    public static int ERROR_DEPLOYMENT = 8

    @RestApiObjectField(description = "The uploader")
    SecUser user

    @RestApiObjectField(description = "List of projects (id) that will have the image, if it can be deployed")
    Long[] projects //projects ids that we have to link with the new file

    @RestApiObjectField(description = "The internal filename path, including extension")
    String filename

    @RestApiObjectField(description = "The original filename, including extension")
    String originalFilename

    @RestApiObjectField(description = "Extension name")
    String ext

    @RestApiObjectField(description = "The image server managing the file")
    ImageServer imageServer

    @RestApiObjectField(description = "File content type")
    String contentType

    @RestApiObjectField(description = "The parent uploaded file in the hierarchy")
    UploadedFile parent

    @RestApiObjectField(description = "File size", mandatory = false)
    Long size

    @RestApiObjectField(description = "File status (UPLOADED = 0, CONVERTED = 1, DEPLOYED = 2, ERROR_FORMAT = 3,ERROR_CONVERSION = 4, UNCOMPRESSED = 5, TO_DEPLOY = 6, TO_CONVERT = 7, ERROR_DEPLOYMENT = 8)", mandatory = false)
    int status = 0

    @RestApiObjectField(description = "Hierarchical tree of uploaded files", mandatory = false, presentInResponse = false)
    String lTree

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "uploaded", description = "Indicates if the file is uploaded", useForCreation = false),
        @RestApiObjectField(apiFieldName = "converted", description = "Indicates if the file is converted", useForCreation = false),
        @RestApiObjectField(apiFieldName = "deployed", description = "Indicates if the file is deployed", useForCreation = false),
        @RestApiObjectField(apiFieldName = "error_format", description = "Indicates if there is a error with file format", useForCreation = false),
        @RestApiObjectField(apiFieldName = "error_convert", description = "Indicates if there is an error with file conversion", useForCreation = false),
        @RestApiObjectField(apiFieldName = "uncompressed", description = "Indicates if the file is not compressed", useForCreation = false)
    ])

    static hasMany = [storages: Storage]

    static belongsTo = [ImageServer]

    static mapping = {
        id(generator: 'assigned', unique: true)
        lTree(type: LTreeType, sqlType: 'ltree')
    }

    static constraints = {
        projects nullable: true
        parent(nullable : true)
        lTree nullable : true
    }

    static def getDataFromDomain(def uploaded) {
        def returnArray = CytomineDomain.getDataFromDomain(uploaded)
        returnArray['user'] = uploaded?.user?.id
        returnArray['parent'] = uploaded?.parent?.id
        returnArray['imageServer'] = uploaded?.imageServer?.id
        returnArray['storages'] = uploaded?.storages?.collect{ it.id }

        returnArray['originalFilename'] = uploaded?.originalFilename
        returnArray['filename'] = uploaded?.filename
        returnArray['ext'] = uploaded?.ext
        returnArray['contentType'] = uploaded?.contentType
        returnArray['size'] = uploaded?.size
        returnArray['path'] = uploaded?.path

        returnArray['status'] = uploaded?.status
        returnArray['uploaded'] = (uploaded?.status == UPLOADED)
        returnArray['converted'] = (uploaded?.status == CONVERTED)
        returnArray['deployed'] = (uploaded?.status == DEPLOYED)
        returnArray['error_format'] = (uploaded?.status == ERROR_FORMAT)
        returnArray['error_convert'] = (uploaded?.status == ERROR_CONVERSION)
        returnArray['uncompressed'] = (uploaded?.status == UNCOMPRESSED)
        returnArray['to_deploy'] = (uploaded?.status == TO_DEPLOY)
        returnArray['to_convert'] = (uploaded?.status = TO_CONVERT)
        returnArray['error_deployment'] = (uploaded?.status = ERROR_DEPLOYMENT)

        returnArray['projects'] = uploaded?.projects

//        returnArray['thumbURL'] = uploaded?.status == DEPLOYED && uploaded?.image ? UrlApi.getAssociatedImage(uploaded?.image?.id, "macro") : null
        returnArray
    }

    static UploadedFile insertDataIntoDomain(def json, def domain = new UploadedFile()) throws CytomineException {
        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')
        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        def user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        domain.user = (user instanceof UserJob) ? ((UserJob) user).user : user

        domain.parent = JSONUtils.getJSONAttrDomain(json, "parent", new UploadedFile(), false)
        domain.imageServer = JSONUtils.getJSONAttrDomain(json, "imageServer", new ImageServer(), true)

        if (json.storages == null || json.storages.equals("null")) {
            throw new WrongArgumentException("No storage for uploaded file.")
        }
        else {
            domain.storages.clear()
            json.storages?.each { id ->
                def storage = Storage.read(id)
                if (storage) domain.addToStorages(storage)
            }
        }

        domain.filename = JSONUtils.getJSONAttrStr(json,'filename')
        domain.originalFilename = JSONUtils.getJSONAttrStr(json,'originalFilename')
        domain.ext = JSONUtils.getJSONAttrStr(json,'ext')
        domain.contentType = JSONUtils.getJSONAttrStr(json,'contentType')
        domain.size = JSONUtils.getJSONAttrLong(json,'size',0)

        domain.status = JSONUtils.getJSONAttrInteger(json,'status',0)
        domain.projects = JSONUtils.getJSONAttrListLong(json,'projects')

        domain
    }

    def getPath() {
        return [imageServer.basePath, user.id, filename].join(File.separator)
    }

    def getAbsolutePath() {
        return getPath()
    }

    def beforeInsert() {
        super.beforeInsert()
        lTree = parent ? parent.lTree+"." : ""
        lTree += id
    }

    def beforeUpdate() {
        super.beforeUpdate()
        lTree = parent ? parent.lTree+"." : ""
        lTree += id
    }

    CytomineDomain[] containers() {
        return storages
    }
}
