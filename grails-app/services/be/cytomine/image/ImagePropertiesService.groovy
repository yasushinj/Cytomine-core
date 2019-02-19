package be.cytomine.image

import be.cytomine.ontology.Property

/*
* Copyright (c) 2009-2019. Authors: see NOTICE file.
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

class ImagePropertiesService implements Serializable {

    def grailsApplication
    def abstractImageService
    def imageServerProxyService

    def clear(AbstractImage image) {
        Property.findAllByDomainIdent(image.id)?.each {
            it.delete()
        }
    }

    def populate(AbstractImage abstractImage) {
        def properties = imageServerProxyService.properties(abstractImage)
        properties.each {
            String key = it.key.trim()
            String value = it.value.trim()
            if (key && value) {
                def property = Property.findByDomainIdentAndKey(abstractImage.id, key)
                if (!property) {
                    log.info("New property: $key => $value for abstract image $abstractImage")
                    property = new Property(key: key, value: value, domainIdent: abstractImage.id, domainClassName: abstractImage.class.name)
                    property.save(failOnError: true)
                }
            }
        }
        abstractImage.save()
    }


    def extractUseful(AbstractImage image) {
        def parseString = { x -> x }
        def parseInt = { x -> Integer.parseInt(x) }
        def parseDouble = { x -> Double.parseDouble(x) }

        def keys = [
                width        : [name: 'cytomine.width', parser: parseInt],
                height       : [name: 'cytomine.height', parser: parseDouble],
                physicalSizeX: [name: 'cytomine.physicalSizeX', parser: parseDouble],
                physicalSizeY: [name: 'cytomine.physicalSizeY', parser: parseDouble],
                physicalSizeZ: [name: 'cytomine.physicalSizeZ', parser: parseDouble],
                fps          : [name: 'cytomine.fps', parser: parseDouble],
                bitDepth     : [name: 'cytomine.bitdepth', parser: parseInt],
                colorspace   : [name: 'cytomine.colorspace', parser: parseString],
                magnification: [name: 'cytomine.magnification', parser: parseInt],
                resolution   : [name: 'cytomine.resolution', parser: parseDouble]
        ]

        keys.each { k, v ->
            def property = Property.findByDomainIdentAndKey(image.id, v.name)
            if (property)
                image[k] = v.parser(property.value)
            else
                log.info "No property ${v.name} for abstract image $image"

            image.save(flush: true, failOnError: true)
        }
    }
}
