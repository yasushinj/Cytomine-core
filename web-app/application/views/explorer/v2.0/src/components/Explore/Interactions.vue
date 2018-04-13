<template>
  <div>
        <ul>
            <li><button @click="addInteraction('Select')">Select</button></li>
            <li><button @click="addInteraction('Point')">Point</button></li>
            <li><button @click="addInteraction('Line')">Line</button></li>
            <li><button @click="addInteraction('Arrow')">Arrow</button></li>
            <li><button @click="addInteraction('Rectangle')">Rectangle</button></li>
            <li><button @click="addInteraction('Ellipse')">Ellipse</button></li>
            <li><button @click="addInteraction('Circle')">Circle</button></li>
            <li><button @click="addInteraction('Polygon')">Polygon</button></li>
            <li><button @click="addInteraction('MagicWand')">MagicWand</button></li>
            <li><button @click="addInteraction('Polygon', true)">Freehand</button></li>
            <li><button @click="addInteraction('Correction', true)">Union</button></li>
            <li><button @click="addInteraction('Correction', true, true)">Difference</button></li>
            <li><button @click="addInteraction('Ruler')">Ruler</button></li>
            <template v-if="featureSelected.getArray()[0]">
                <li><button @click="addInteraction('Fill')">Fill</button></li>
                <li><button @click="addInteraction('Edit')">Edit</button></li>
                <li><button @click="addInteraction('Rotate')">Rotate</button></li>
                <li><button @click="addInteraction('Resize')">Resize</button></li>
                <li><button @click="addInteraction('Drag')">Drag</button></li>
                <li><button @click="addInteraction('Remove')">Remove</button></li>
            </template>
        </ul>
  </div>
</template>

<script>
import WKT from 'ol/format/wkt';
import LayerVector from 'ol/layer/vector';
import SrcVector from 'ol/source/vector';
import Collection from 'ol/collection';
import Draw from 'ol/interaction/draw';
import Polygon from 'ol/geom/polygon';
import Style from 'ol/style/style';
import Fill from 'ol/style/fill';
import Stroke from 'ol/style/stroke';
import Select from 'ol/interaction/select';
import Translate from 'ol/interaction/translate';
import Modify from 'ol/interaction/modify';
import Rotate from 'ol-rotate-feature';
import Sphere from 'ol/sphere';
import Observable from 'ol/observable';
import Overlay from 'ol/overlay';

export default {
  name: 'Interactions',
  props: [
      'currentMap',
      'vectorLayersOpacity',
      'isReviewing',
  ],
  data() {
      return {
          draw: {
              layer: {},
              interaction: {},
              overlay: {
                  helpTooltip: {},
                  measureTooltip: {},
                  measureTooltipElement: "",
              }
          },
          featureSelected: new Collection(),
      }
  },
  computed: {
      extent() {
          return [0, 0, parseInt(this.currentMap.data.width), parseInt(this.currentMap.data.height)];
      },
      layersArray() {
          return this.$openlayers.getMap(this.currentMap.id).getLayers().getArray();
      },
      currentUserLayer() {
          let index = this.layersArray.findIndex(layer => layer.get('title') == this.currentMap.user.id);
          return this.layersArray[index];
      },
      deepFeatureSelected() {
          return this.featureSelected.getArray()[0];
      },
      featureSelectedId() {
          return this.featureSelected.getArray()[0].getId();
      }
  },
  watch: {
      deepFeatureSelected(newFeature, oldFeature) {
        this.$emit('featureSelected', newFeature);

        if(oldFeature !== undefined && oldFeature.hasOwnProperty('id_')) {
            let color = oldFeature.getStyle().getFill().getColor();
            let strokeColor = oldFeature.get('strokeColor');
            if(color.length > 3) {
                color.splice(color.length - 1, 1);
            }
            oldFeature.getStyle().setStroke(
                new Stroke({
                    color: strokeColor,
                    width: 3,
                })  
            )
            oldFeature.changed();
        }
        if(newFeature !== undefined) {
            let color = newFeature.getStyle().getFill().getColor();
            let strokeColor = this.isReviewing ? newFeature.getStyle().getStroke().getColor() : [0, 0, 255];
            color[3] = this.vectorLayersOpacity + 0.3;
            newFeature.getStyle().setStroke(
                new Stroke({
                    color: strokeColor,
                    width: 3,
                }) 
            )
            newFeature.set('strokeColor', strokeColor)
            newFeature.changed();
        }
      },
  },
  methods: {
    layerIndex(array, toFind) {
        return array.findIndex(item => item.get('title') === toFind);
    },
    termIndex(array, toFind) {
        return array.findIndex(item => item.id == toFind);
    },
    userDisplayName(user) {
        return `${user.lastname} ${user.firstname} (${user.username})`
    },
    getWktLocation(feature) {
        let format = new WKT();
        return format.writeFeature(feature);
    },
    /**
    * Creates a new measure tooltip
    */
    createMeasureTooltip() {
        if (this.draw.overlay.measureTooltipElement) {
            this.draw.overlay.measureTooltipElement.parentNode.removeChild(this.draw.overlay.measureTooltipElement);
        }
        this.draw.overlay.measureTooltipElement = document.createElement('div');
        this.draw.overlay.measureTooltipElement.className = 'tooltip tooltip-measure';
        this.draw.overlay.measureTooltip = new Overlay({
            element: this.draw.overlay.measureTooltipElement,
            offset: [0, -15],
            positioning: 'bottom-center'
        });
        this.$openlayers.getMap(this.currentMap.id).addOverlay(this.draw.overlay.measureTooltip);
    },
    addInteraction(interactionType, freehand = false, remove = false ) {
        let currentMap = this.$openlayers.getMap(this.currentMap.id)
        let style = undefined;
        this.removeInteraction();
        this.removeOverlay(this.draw.overlay.helpTooltip);

        // Creates layer if not found
        if(this.currentUserLayer == undefined && this.layerIndex(this.layersArray, 'draw') < 0) {
            this.draw.layer = new LayerVector({
                title: 'draw',  
                source: new SrcVector(),
                extent: this.extent,
            })
            currentMap.addLayer(this.draw.layer);
        } else if(this.currentUserLayer != undefined) {
            this.draw.layer = this.currentUserLayer;
        }
        // Adds interaction
        let source = this.draw.layer.getSource(),
            geometryFunction, type;
        switch (interactionType) {
            case 'Select':
                // this.removeInteraction();
                this.draw.interaction = new Select({
                    features: this.featureSelected,
                });
                currentMap.addInteraction(this.draw.interaction);
                return;
                break;
            case 'Rectangle':
                geometryFunction = Draw.createBox();    
                type = 'Circle';
                break;
            case 'Point':
                type = 'Point'
                break;
            case 'Circle':
                type = 'Circle'
                break;
            case 'Polygon':
                type = 'Polygon'
                break;
            case 'Ellipse':
                type = 'Circle'
                geometryFunction = function(coord, geometry) {
                    if (!geometry) {
                        geometry = new Polygon(null);
                    }
                    let originX = coord[0][0],
                        originY = coord[0][1],
                        mouseX = coord[1][0],
                        mouseY = coord[1][1],
                        newCoordinates = [],
                        deltaX = mouseX - originX,
                        deltaY = mouseY - originY,
                        centerX = originX + deltaX/2,
                        centerY = originY + deltaY/2;

                    for (var i = 0 * Math.PI; i < 2 * Math.PI; i += 2*Math.PI/15 ) {
                        let xPos = centerX + (deltaX/2 * Math.sin(i)) + (deltaY/2 * Math.cos(i));
                        let yPos = centerY + (deltaX/2 * Math.cos(i)) + (deltaY/2 * Math.sin(i));
                            
                        newCoordinates.push([xPos, yPos]);
                    }
                    geometry.setCoordinates([newCoordinates]);
                    return geometry;
                }
                break;
            case 'Arrow':
                type = 'Circle'
                geometryFunction = function(coord, geometry) {
                    if (!geometry) {
                        geometry = new Polygon(null);
                    }
                    let size = 300;
                    let originX = coord[0][0];
                    let originY = coord[0][1];
                    let newCoordinates = [
                        coord[0],
                        [originX - size/2, originY - size/2],
                        [originX - size/4, originY - size/2],
                        [originX - size/4, originY - size*2],
                        [originX + size/4, originY - size*2],
                        [originX + size/4, originY - size/2],
                        [originX + size/2, originY - size/2],
                        coord[0],
                    ];
                    geometry.setCoordinates([newCoordinates]);
                    return geometry;
                }
                break;
            case 'Line':
                type = "LineString"
                break;
            case 'Edit':
                this.draw.interaction = new Modify({
                    features: this.featureSelected,
                });
                currentMap.addInteraction(this.draw.interaction);
                this.draw.interaction.on('modifyend', evt => {
                    let newCoordinates = this.getWktLocation(this.featureSelected.getArray()[0]);
                    api.get(`/api/annotation/${this.featureSelectedId}.json`).then(data => {
                        data.data.location = newCoordinates;
                        api.put(`/api/annotation/${this.featureSelectedId}.json`, data.data);
                    })
                })
                return;
                break;
            case 'Drag':
                this.draw.interaction = new Translate({
                    features: this.featureSelected,
                })
                currentMap.addInteraction(this.draw.interaction);
                return;
                break;
            case 'Remove':
                let userId = this.featureSelected.getArray()[0].get('user');
                let layerIndex = this.layersArray.findIndex(layer => layer.get('title') == userId);
                let featureIndex = this.layersArray[layerIndex].getSource().getFeatures().findIndex(feature => feature.getId() == this.featureSelectedId)

                this.layersArray[layerIndex].getSource().removeFeature(this.layersArray[layerIndex].getSource().getFeatures()[featureIndex]);
                
                api.delete(`/api/annotation/${this.featureSelectedId}.json`).then(() => {
                    this.featureSelected.getArray().splice(0, 1);
                    this.addInteraction('Select');
                });

                return;
                break;
            case 'Resize':

                return;
                break;
            case 'Fill':
                api.put(`/api/annotation/${this.featureSelectedId}.json?&fill=true`, {fill: true, id: this.featureSelectedId}).then(data => {
                    let format = new WKT();
                    let newCoordinates = format.readFeature(data.data.data.annotation.location).getGeometry().getCoordinates()[0];
                    let userId = this.featureSelected.getArray()[0].get('user');
                    let layerIndex = this.layersArray.findIndex(layer => layer.get('title') == userId);
                    let featureIndex = this.layersArray[layerIndex].getSource().getFeatures().findIndex(feature => feature.getId() == this.featureSelectedId)

                    this.layersArray[layerIndex].getSource().getFeatures()[featureIndex].getGeometry().setCoordinates([newCoordinates]);
                })
                this.addInteraction('Select');
                return;
                break;
            case 'Rotate':
                this.draw.interaction = new Rotate({
                    features: this.featureSelected,
                })
                this.draw.interaction.on('rotateend', evt => {
                    let newCoordinates = this.getWktLocation(this.featureSelected.getArray()[0]);
                    api.get(`/api/annotation/${this.featureSelectedId}.json`).then(data => {
                        data.data.location = newCoordinates;
                        api.put(`api/annotation/${this.featureSelectedId}.json`, data.data);
                    })
                })
                currentMap.addInteraction(this.draw.interaction);
                return;
                break;
            case 'Correction':
                type = 'Polygon'
                break;
            case 'Ruler':
                /**
                * Currently drawn feature.
                * @type {ol.Feature}
                */
                var sketch;


                /**
                * The help tooltip element.
                * @type {Element}
                */
                var helpTooltipElement;

                /**
                * Handle pointer move.
                * @param {ol.MapBrowserEvent} evt The event.
                */
                var pointerMoveHandler = (evt) => {
                    if (evt.dragging) {
                        return;
                    }
                    /** @type {string} */
                    var helpMsg = 'Click to start drawing';

                    if (sketch) {
                        var geom = (sketch.getGeometry());
                        helpMsg = 'Click to continue drawing the line';
                    }

                    helpTooltipElement.innerHTML = helpMsg;
                    this.draw.overlay.helpTooltip.setPosition(evt.coordinate);

                    helpTooltipElement.classList.remove('hidden');
                };

                /**
                * Creates a new help tooltip
                */
                let createHelpTooltip = () => {
                    if (helpTooltipElement) {
                    helpTooltipElement.parentNode.removeChild(helpTooltipElement);
                    }
                    helpTooltipElement = document.createElement('div');
                    helpTooltipElement.className = 'tooltip hidden';
                    this.draw.overlay.helpTooltip = new Overlay({
                    element: helpTooltipElement,
                    offset: [15, 0],
                    positioning: 'center-left'
                    });
                    currentMap.addOverlay(this.draw.overlay.helpTooltip);
                }


                currentMap.on('pointermove', pointerMoveHandler);

                currentMap.getViewport().addEventListener('mouseout', function() {
                    helpTooltipElement.classList.add('hidden');
                });


                /**
                * Format length output.
                * @param {ol.geom.LineString} line The line.
                * @return {string} The formatted length.
                */
                var formatLength = function(line) {
                    var length = Sphere.getLength(line);
                    return Math.round(length * 100) / 1000 + ' px';
                };

                this.createMeasureTooltip();
                createHelpTooltip();

                var listener;
                type = 'LineString';
                break;
        }

        this.draw.interaction = new Draw({
            source,
            type,
            geometryFunction,
            freehand,
        })

        if(interactionType == 'Correction') {
            this.draw.interaction.on('drawend', evt => {
                let location = this.getWktLocation(evt.feature);
                let layers = this.layersArray.filter(layer => layer.getType() == "VECTOR" && layer.get('title') != 'draw').map(layer => layer.get('title'))
                api.post(`/api/annotationcorrection.json`, {
                    image: parseInt(this.currentMap.imageId),
                    layers,
                    location,
                    remove,
                    review: false,
                }).then(data => {
                    this.$emit('updateLayers', true);
                    this.$emit('updateAnnotationsIndex', true)
                })
            })
        } else if (interactionType == 'Ruler'){
            this.draw.interaction.on('drawstart',
                (evt) => {
                // set sketch
                sketch = evt.feature;

                /** @type {ol.Coordinate|undefined} */
                var tooltipCoord = evt.coordinate;

                listener = sketch.getGeometry().on('change', (evt) => {
                    var geom = evt.target;
                    var output;
                    
                    output = formatLength(geom);
                    tooltipCoord = geom.getLastCoordinate();
                    
                    this.draw.overlay.measureTooltipElement.innerHTML = output;
                    this.draw.overlay.measureTooltip.setPosition(tooltipCoord);
                });
                });

            this.draw.interaction.on('drawend',
                () => {
                    this.draw.overlay.measureTooltipElement.className = 'tooltip tooltip-static';
                    this.draw.overlay.measureTooltip.setOffset([0, -7]);
                    // unset sketch
                    sketch = null;
                    // unset tooltip so that a new one can be created
                    this.draw.overlay.measureTooltipElement = null;
                    this.createMeasureTooltip();
                    Observable.unByKey(listener);
                }, this);
        } else {
            this.draw.interaction.on('drawend', evt => {
                api.post(`/api/annotation.json`, {
                    name: "",
                    location: this.getWktLocation(evt.feature),
                    image: this.currentMap.imageId,
                    roi: false,
                    term: [],
                    user: this.currentMap.user.id,
                }).then(() => {
                    this.$emit('updateLayers', true)
                    this.$emit('updateAnnotationsIndex', true)
                })
            })
        }
        currentMap.addInteraction(this.draw.interaction);
    },
    removeInteraction() {
        this.$openlayers.getMap(this.currentMap.id).removeInteraction(this.draw.interaction);
    },
    removeOverlay(overlay) {
        this.$openlayers.getMap(this.currentMap.id).removeOverlay(overlay)
    }
  },
}
</script>
