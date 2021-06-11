package be.cytomine.image

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
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
import be.cytomine.api.UrlApi
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
@RestApiObject(name = "Uploaded file", description = "A file uploaded on the server, when finished, we create an 'abstract image' from this uploaded file")
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

    @RestApiObjectField(description = "The user that upload the file")
    SecUser user

    @RestApiObjectField(description = "List of project (id) that will get the image")
    Long[] projects //projects ids that we have to link with the new file

    @RestApiObjectField(description = "List of storage (id) that will store the image")
    Long[] storages //storage ids on which we have to upload files

    @RestApiObjectField(description = "Upload file filename")
    String filename

    @RestApiObjectField(description = "Upload file short name")
    String originalFilename

    @RestApiObjectField(description = "Extension name")
    String ext

    @RestApiObjectField(description = "Path name")
    String path

    @RestApiObjectField(description = "File content type", presentInResponse = false)
    String contentType

    UploadedFile parent

    @RestApiObjectField(description = "File size", mandatory = false)
    Long size

    @RestApiObjectField(description = "File status (UPLOADED = 0,CONVERTED = 1,DEPLOYED = 2,ERROR_FORMAT = 3,ERROR_CONVERT = 4,UNCOMPRESSED = 5,TO_DEPLOY = 6)", mandatory = false)
    int status = 0

    @RestApiObjectField(description = "The image created by this file")
    AbstractImage image

    /**
     * Indicates whether or not a conversion was done
     */
    @RestApiObjectField(description = "Indicates wether or not a conversion was done", mandatory = false)
    Boolean converted = false

    @RestApiObjectField(description = "ltree used for uploaded files hierarchy", mandatory = false)
    String lTree

    @RestApiObjectFields(params=[
        @RestApiObjectField(apiFieldName = "uploaded", description = "Indicates if the file is uploaded",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "converted", description = "Indicates if the file is converted",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "deployed", description = "Indicates if the file is deployed",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "error_format", description = "Indicates if there is a error with file format",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "error_convert", description = "Indicates if there is an error with file conversion",allowedType = "boolean",useForCreation = false),
        @RestApiObjectField(apiFieldName = "uncompressed", description = "Indicates if the file is not compressed",allowedType = "boolean",useForCreation = false)
    ])
    static transients = ["zoomLevels", "thumbURL"]

    static mapping = {
        lTree type: LTreeType, sqlType: 'ltree'
    }


    static constraints = {
        projects nullable: true
        storages nullable: false
        parent(nullable : true)
        image(nullable : true)
        lTree nullable : true
    }

    static def getDataFromDomain(def uploaded) {
        def returnArray = CytomineDomain.getDataFromDomain(uploaded)
        returnArray['user'] = uploaded?.user?.id
        returnArray['projects'] = uploaded?.projects
        returnArray['storages'] = uploaded?.storages
        returnArray['filename'] = uploaded?.filename
        returnArray['originalFilename'] = uploaded?.originalFilename
        returnArray['ext'] = uploaded?.ext
        returnArray['contentType'] = uploaded?.contentType
        returnArray['size'] = uploaded?.size
        returnArray['path'] = uploaded?.path
        returnArray['status'] = uploaded?.status

        returnArray['uploaded'] = (uploaded?.status == UploadedFile.UPLOADED)
        returnArray['converted'] = (uploaded?.status == UploadedFile.CONVERTED)
        returnArray['deployed'] = (uploaded?.status == UploadedFile.DEPLOYED)
        returnArray['error_format'] = (uploaded?.status == UploadedFile.ERROR_FORMAT)
        returnArray['error_convert'] = (uploaded?.status == UploadedFile.ERROR_CONVERSION)
        returnArray['uncompressed'] = (uploaded?.status == UploadedFile.UNCOMPRESSED)
        returnArray['to_deploy'] = (uploaded?.status == UploadedFile.TO_DEPLOY)


        returnArray['image'] = uploaded?.getAbstractImage()?.id
        returnArray['parent'] = uploaded?.parent?.id
        returnArray['thumbURL'] = uploaded?.status == UploadedFile.DEPLOYED && uploaded?.image ? UrlApi.getThumbImage(uploaded?.image?.id, 256) : null
        returnArray['macroURL'] = uploaded?.status == UploadedFile.DEPLOYED && uploaded?.image ? UrlApi.getAssociatedImage(uploaded?.image?.id, "macro", 256) : null
        returnArray
    }


    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static UploadedFile insertDataIntoDomain(def json, def domain = new UploadedFile()) throws CytomineException {

        domain.id = JSONUtils.getJSONAttrLong(json,'id',null)
        domain.created = JSONUtils.getJSONAttrDate(json,'created')
        domain.updated = JSONUtils.getJSONAttrDate(json,'updated')

        def user = JSONUtils.getJSONAttrDomain(json, "user", new SecUser(), true)
        if(user instanceof UserJob){
            domain.user = ((UserJob) user).user
        } else {
            domain.user = user
        }

        domain.projects = JSONUtils.getJSONAttrListLong(json,'projects')
        domain.storages = JSONUtils.getJSONAttrListLong(json,'storages')

        domain.filename = JSONUtils.getJSONAttrStr(json,'filename')
        domain.originalFilename = JSONUtils.getJSONAttrStr(json,'originalFilename')
        domain.ext = JSONUtils.getJSONAttrStr(json,'ext')
        domain.path = JSONUtils.getJSONAttrStr(json,'path')
        domain.contentType = JSONUtils.getJSONAttrStr(json,'contentType')

        domain.parent = JSONUtils.getJSONAttrDomain(json, "parent", new UploadedFile(), false)

        domain.size = JSONUtils.getJSONAttrLong(json,'size',0)

        domain.status = JSONUtils.getJSONAttrInteger(json,'status',0)

        domain.converted = JSONUtils.getJSONAttrBoolean(json,'converted',false)

        domain.image = JSONUtils.getJSONAttrDomain(json, "image", new AbstractImage(), false)

        domain.deleted = JSONUtils.getJSONAttrDate(json, "deleted")

        return domain;
    }

    def getAbsolutePath() {
        AbstractImage img = this.getAbstractImage();
        if(img) return [ StorageAbstractImage.findByAbstractImage(img).storage.basePath, this.filename].join(File.separator)
        // at creation, img are not set
        else return [ this.path, this.filename].join(File.separator)
    }

    def getAbstractImage() {
        try {
            if (image) return image
            if (parent?.image) return parent.image
            else return null
        } catch(Exception e) {
            return null
        }
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
}
