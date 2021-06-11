package be.cytomine.api

import be.cytomine.image.AbstractImage

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

import grails.util.Holders

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 1/03/11
 * Time: 13:33
 * Utility class to build url for specific data.
 * Some service has special url. E.g. An annotation can be downloaded via a jpg file from url.
 */
class UrlApi {

    def grailsApplication

    static def getApiURL(String type, Long id) {
        return "${serverUrl()}/api/$type/${id}.json"
    }

    static def getCropURL(Long idImage, def parameters, String format="png") {
        String url = "${serverUrl()}/api/abstractimage/$idImage/crop.$format"
        String query = parameters.collect { key, value ->
            if (value instanceof String)
                value = URLEncoder.encode(value, "UTF-8")
            "$key=$value"
        }.join("&")
        return "$url?$query"
    }

    static def getMaskURL(Long idImage, def parameters, def format="png") {
        String url = "${serverUrl()}/api/abstractimage/$idImage/mask.$format"
        String query = parameters.collect { key, value ->
            if (value instanceof String)
                value = URLEncoder.encode(value, "UTF-8")
            "$key=$value"
        }.join("&")
        return "$url?$query"
    }

    /**
     * Return cytomine url to get an image metadata
     * @param url Cytomine base url
     * @param idImage Image id
     * @return full cytomine url
     */
    static def getMetadataURLWithImageId(Long idImage) {
        return "${serverUrl()}/api/abstractimage/$idImage/metadata.json"
    }

    /**
     * Return cytomine url to get a user crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getUserAnnotationCropWithAnnotationId(Long idAnnotation, def format="jpg") {
        return "${serverUrl()}/api/userannotation/$idAnnotation/crop.$format"
    }

    /**
     * Return cytomine url to get a user crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @param maxWidthOrHeight Max size for the downloaded picture
     * @return full cytomine url
     */
    static def getUserAnnotationCropWithAnnotationIdWithMaxWithOrHeight(Long idAnnotation, int maxWidthOrHeight, def format="png") {
        return "${serverUrl()}/api/userannotation/$idAnnotation/crop.$format?maxSize=$maxWidthOrHeight"
    }

    /**
     * Return cytomine url to get a roi crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getROIAnnotationCropWithAnnotationId(Long idAnnotation, def format="jpg") {
        return "${serverUrl()}/api/roiannotation/$idAnnotation/crop.$format"
    }

    /**
     * Return cytomine url to get a roi crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @param maxWidthOrHeight Max size for the downloaded picture
     * @return full cytomine url
     */
    static def getROIAnnotationCropWithAnnotationIdWithMaxWithOrHeight(Long idAnnotation, int maxWidthOrHeight) {
        return "${serverUrl()}/api/roiannotation/$idAnnotation/crop.png?maxSize=$maxWidthOrHeight"
    }

    /**
     * Return cytomine url to get an algo crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getAlgoAnnotationCropWithAnnotationId(Long idAnnotation, def format="png") {
        return "${serverUrl()}/api/algoannotation/$idAnnotation/crop.$format"
    }

    static def getAnnotationCropWithAnnotationId(Long idAnnotation, def maxSize = null, def format="png") {
        return "${serverUrl()}/api/annotation/$idAnnotation/crop.$format" + (maxSize? "?maxSize=$maxSize" :"")
    }

    static def getCompleteAnnotationCropDrawedWithAnnotationId(Long idAnnotation, def maxSize = null) {
        String params = (maxSize ? "maxSize=$maxSize&" : "") + "draw=true&complete=true"
        return "${serverUrl()}/api/annotation/$idAnnotation/crop.png?" + params
    }

    static def getAssociatedImage(Long idAbstractImage, String label, def maxSize = null) {
        if(label == "macro") {
            AbstractImage abstractImage = AbstractImage.read(idAbstractImage)
            if(["image/pyrtiff", "image/tiff", "image/tif", "image/jp2"].contains(abstractImage?.mimeType)) return null
        }
        String size = maxSize ? "?maxWidth=$maxSize" : "";
        return "${serverUrl()}/api/abstractimage/$idAbstractImage/associated/$label" + ".png$size"
    }

    static def getThumbImage(Long idAbstractImage, def maxSize, def format="png") {
        return "${serverUrl()}/api/abstractimage/$idAbstractImage/thumb.$format?maxSize=$maxSize"
    }

    static def getThumbMultiDimImage(Long idImageGroup, def maxSize) {
        return "${serverUrl()}/api/imagegroup/$idImageGroup/thumb.png?maxSize=$maxSize"
    }

    /**
     * Return cytomine url to get a small crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getAnnotationMinCropWithAnnotationId(Long idAnnotation, def format="png") {
        return "${serverUrl()}/api/annotation/$idAnnotation/crop.$format?maxSize=256"
    }
    static def getAnnotationMinCropWithAnnotationIdOld(Long idAnnotation) {
        return "${serverUrl()}/api/annotation/$idAnnotation/cropMin.jpg"
    }

    /**
     * Return cytomine url to get an algo crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @param maxWidthOrHeight Max size for the downloaded picture
     * @return full cytomine url
     */
    static def getAlgoAnnotationCropWithAnnotationIdWithMaxWithOrHeight(Long idAnnotation, int maxWidthOrHeight, def format="png") {
        return "${serverUrl()}/api/algoannotation/$idAnnotation/crop.$format?maxSize=$maxWidthOrHeight"
    }

    /**
     * Return cytomine url to get an reviewed crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getReviewedAnnotationCropWithAnnotationId(Long idAnnotation, def format="jpg") {
        return "${serverUrl()}/api/reviewedannotation/$idAnnotation/crop.$format"
    }

    /**
     * Return cytomine url to get a reviewed crop annotation
     * @param url Cytomine base url
     * @param idAnnotation Annotation id
     * @param maxWidthOrHeight Max size for the downloaded picture
     * @return full cytomine url
     */
    static def getReviewedAnnotationCropWithAnnotationIdWithMaxWithOrHeight(Long idAnnotation, int maxWidthOrHeight, def format="jpg") {
        return "${serverUrl()}/api/reviewedannotation/$idAnnotation/crop.$format?maxSize=$maxWidthOrHeight"
    }

    /**
     * Return cytomine url to access to an annotation with the UI client
     * @param url Cytomine base url
     * @param idProject Project id
     * @param idImage Image id
     * @param idAnnotation Annotation id
     * @return full cytomine url
     */
    static def getAnnotationURL(Long idProject, Long idImage, Long idAnnotation) {
        return  "${UIUrl()}/#/project/$idProject/image/$idImage/annotation/$idAnnotation"
    }

    /**
     * Return cytomine url to access to an image with the UI client
     * @param url Cytomine base url
     * @param idProject Project id
     * @param idImage Image id
     * @return full cytomine url
     */
    static def getBrowseImageInstanceURL(Long idProject, Long idImage) {
        return  "${UIUrl()}/#/project/$idProject/image/$idImage"
    }

    static def getDashboardURL(Long idProject) {
        return  "${UIUrl()}/#/project/$idProject"
    }

    /**
     * Return cytomine url to access an image thumb
     * @param url  Cytomine base url
     * @param idImage Image id
     * @return full cytomine url
     */
    static def getAbstractImageThumbURL(Long idImage, def format="png") {
        return  "${serverUrl()}/api/abstractimage/$idImage/thumb.$format"
    }

    static def serverUrl() {
        Holders.getGrailsApplication().config.grails.serverURL
    }

    static def UIUrl() {
        return Holders.getGrailsApplication().config.grails.UIURL?:serverUrl()
    }
}
