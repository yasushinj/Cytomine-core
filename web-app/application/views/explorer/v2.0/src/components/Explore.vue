<template>
    <div :style="`height:${elementHeight}px;width:${elementWidth}%;`" class="map">
        <div style="height:100vh;" @mousemove="sendView" @mousewheel="sendView" :id="currentMap.id" ref="exploreMap">
        </div>
        <div class="controls" :id="'controls-'+currentMap.id"></div>
        <interactions v-show="this.lastEventMapId == this.currentMap.id" @updateLayers="setUpdateLayers" @featureSelected="setFeatureSelected" :currentMap="currentMap" :isReviewing="isReviewing" :vectorLayersOpacity="vectorLayersOpacity"></interactions>
        <div>
            <div v-show="this.lastEventMapId == this.currentMap.id" class="bottom-panel">
                <button @click="setShowComponent('informations')" :class="['btn', 'btn-default', {active: showComponent == 'informations' }]">
                    <span class="glyphicon glyphicon-info-sign" aria-hidden="true"></span>
                </button>
                <button @click="setShowComponent('linkmap')" :class="['btn', 'btn-default', {active: showComponent == 'linkmap' }]">
                    <span class="glyphicon glyphicon-link" aria-hidden="true"></span>
                </button>
                <button @click="setShowComponent('filter')" :class="['btn', 'btn-default', {active: showComponent == 'filter' }]">
                    <span class="glyphicon glyphicon-filter" aria-hidden="true"></span>
                </button>
                <button @click="setShowComponent('digitalZoom')" :class="['btn', 'btn-default', {active: showComponent == 'digitalZoom' }]">
                    <span class="glyphicon glyphicon-zoom-in" aria-hidden="true"></span>
                </button>
                <button @click="setShowComponent('colormap')" :class="['btn', 'btn-default', {active: showComponent == 'colormap' }]">
                    <span class="glyphicon glyphicon-adjust" aria-hidden="true"></span>
                </button>
                <button @click="setShowComponent('annotationLayers')" :class="['btn', 'btn-default', {active: showComponent == 'annotationLayers' }]">
                    Annotation layers
                </button>
                <button @click="setShowComponent('annotationList')" :class="['btn', 'btn-default', {active: showComponent == 'annotationList' }]">
                    <span class="glyphicon glyphicon-list" aria-hidden="true"></span>
                    Annotation list
                </button>
                <button v-if="imageGroupIndex[0]" @click="setShowComponent('multidimension')" :class="['btn', 'btn-default', {active: showComponent == 'multidimension' }]">
                    Multidimension
                </button>
                <button v-if="isReviewing" @click="setShowComponent('review')" :class="['btn', 'btn-default', {active: showComponent == 'review' }]">
                    <span class="glyphicon glyphicon-check" aria-hidden="true"></span>
                    Review
                </button>
                <button @click="setShowComponent('properties')" :class="['btn', 'btn-default', {active: showComponent == 'properties' }]">
                    <span class="glyphicon glyphicon-tag" aria-hidden="true"></span>
                </button>
                <button class="btn btn-danger" @click="deleteMap">
                    <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>
                    Close
                </button>
                <!-- <div class="panel postion-panel">
                    <div class="panel-body">
                        <position :mousePosition="mousePosition" :currentMapId="currentMap.id"></position>
                    </div>
                </div> -->
            </div>
        </div>
        <div v-show="(this.lastEventMapId == this.currentMap.id && showComponent != '') || featureSelected != undefined" class="panel component-panel" :style="`max-height:${2*elementHeight/3}px;overflow-y:${isComponentInformations};`">
            <div class="panel-body">
                <div v-show="showComponent == 'linkmap'">
                    <div class="alert alert-info">Choose a map to link</div>
                    <label :for="'link-'+currentMap.id">Link the map</label>
                    <select class="btn btn-default" @change="sendLink" v-model="linkValue" name="link" :id="'link-'+currentMap.id">
                        <option value="">Select a map</option>
                        <template v-for="(map, index) in maps">
                            <option v-if="index !== mapIndex" :key="map.id" :value="map.id">{{mapNames[index]}}</option>
                        </template>
                    </select>
                </div>
                <digital-zoom v-show="showComponent == 'digitalZoom'" :currentMap="currentMap"></digital-zoom>
                <div v-show="showComponent == 'filter'">
                    <div class="alert alert-info">Choose a filter to apply</div>
                    <label :for="'original-filter-'+currentMap.id">Original</label>
                    <input v-model="filterSelected" type="radio" :name="'filter-original-'+currentMap.id" :id="'filter-original-'+currentMap.id" value="">
                    <div v-for="filter in filters" :key="filter.id">
                        <label :for="'filter-'+filter.id+'-'+currentMap.id">{{filter.name}}</label>
                        <input v-model="filterSelected" type="radio" :name="'filter-'+filter.id+'-'+currentMap.id" :id="'filter-'+filter.id+'-'+currentMap.id" :value="filter">
                    </div>
                </div>
                <color-maps v-show="showComponent == 'colormap'" :currentMap="currentMap"></color-maps>
                <div v-show="showComponent == 'annotationLayers'">
                    <annotation-layers @updateLayers="setUpdateLayers" @vectorLayersOpacity="setVectorLayersOpacity" @layersSelected="setLayersSelected" @userLayers="setUserLayers" :onlineUsers="onlineUsers" :isReviewing="isReviewing" :updateLayers="updateLayers" :termsToShow="termsToShow" :showWithNoTerm="showWithNoTerm" :allTerms="allTerms" :currentMap="currentMap"></annotation-layers>
                    <ontology :featureSelectedData="featureSelectedData" :featureSelected="featureSelected" :vectorLayersOpacity="vectorLayersOpacity" @showTerms="showTerms" @showWithNoTerm="setShowWithNoTerm" @allTerms="setAllTerms"></ontology>
                </div>
                <review v-if="isReviewing" v-show="showComponent == 'review'" @updateAnnotationsIndex="setUpdateAnnotationsIndex" @updateLayers="setUpdateLayers" @featureSelectedData="setFeatureSelectedData" @updateMap="updateMap" :layersSelected="layersSelected" :currentMap="currentMap" :featureSelectedData="featureSelectedData" :featureSelected="featureSelected" :userLayers="userLayers"></review>
                <multidimension v-if="imageGroupIndex[0]" v-show="showComponent == 'multidimension'" @imageGroupHasChanged="setImageGroup" :imageGroupIndex="imageGroupIndex" :filterUrl="filterUrl" :imsBaseUrl="imsBaseUrl" @imageHasChanged="updateMap" :currentMap="currentMap"></multidimension>
                <properties v-show="showComponent == 'properties'" :layersSelected="layersSelected" :currentMap="currentMap"></properties>
                <annotation-details @featureSelectedData="setFeatureSelectedData" :users="userLayers" :terms="allTerms" :featureSelected="featureSelected" :currentMap="currentMap"></annotation-details>
                <informations v-show="showComponent == 'informations'" @updateMap="updateMap" @updateOverviewMap="updateOverviewMap" :filterUrl="filterUrl" :imsBaseUrl="imsBaseUrl" :currentMap="currentMap"></informations>
                <annotations v-show="showComponent == 'annotationList'" @updateAnnotationsIndex="setUpdateAnnotationsIndex" :updateAnnotationsIndex="updateAnnotationsIndex" :isReviewing="isReviewing" :users="userLayers" :terms="allTerms" :currentMap="currentMap"></annotations>
            </div>
        </div>
    </div>
</template>

<script>
import AnnotationLayers from './Explore/AnnotationLayers'
import Interactions from './Explore/Interactions';
import Informations from './Explore/Informations';
import Position from './Explore/Position';
import Ontology from './Explore/Ontology';
import AnnotationDetails from './Explore/AnnotationDetails';
import Annotations from './Explore/Annotations';
import Properties from './Explore/Properties';
import Multidimension from './Explore/Multidimension';
import DigitalZoom from './Explore/DigitalZoom'
import Review from './Explore/Review'
import ColorMaps from './Explore/Colormaps'

import OlTile from 'ol/layer/tile';
import Zoomify from 'ol/source/zoomify';
import Group from 'ol/layer/group';
import ZoomControls from 'ol/control/zoom';
import RotateControls from 'ol/control/rotate';

export default {
  name: 'Explore',
  components: {
      AnnotationLayers,
      Interactions,
      Informations,
      Position,
      Ontology,
      AnnotationDetails,
      Annotations,
      Properties,
      Multidimension,
      DigitalZoom,
      Review,
      ColorMaps,
  },
  data () {
    return {
        linkValue: "",
        mapNames: ['1', '2', '3', '4'],
        imsBaseUrl: 'http://localhost-ims/',
        filterSelected: "",
        extent: [],
        mousePosition: [0, 0],
        termsToShow: [],
        showWithNoTerm: true,
        allTerms: [],
        featureSelected: undefined,
        featureSelectedData: {},
        userLayers: [],
        layersSelected: [],
        vectorLayersOpacity: 0.5,
        updateLayers: false,
        updateAnnotationsIndex: false,
        onlineUsers: [],
        showComponent: '',
        showPanel: true,
    }
  },
  props: [
    'mapView',
    'maps',
    'currentMap',
    'lastEventMapId',
    'filters',
    'imageGroupIndex',
  ],
  computed: {
    linkedTo() {
        return this.currentMap.linkedTo;
    },
    mapIndex() {
        return this.maps.findIndex(map => map.id === this.currentMap.id);
    },
    filterUrl() {
        if(this.filterSelected !== "") {
            return `${this.imsBaseUrl}${this.filterSelected.baseUrl}`;
        } else {
            return "";
        }
    },
    mapWidth() {
        return parseInt(this.currentMap.data.width)
    },
    mapHeight() {
        return parseInt(this.currentMap.data.height)
    },
    isReviewing() {
        let type = document.querySelector('.get-data').dataset.type;
        let from = type.indexOf('-');
        type.substr(from + 1) == 'review' ? true : false; 
    },
    getCurrentZoom() {
        return this.mapView.mapResolution;
    },
    innerHeight() {
        return window.innerHeight;
    },
    elementHeight() {
        let fullHeight = this.innerHeight - (92+15);
        let heights = [
            [fullHeight],
            [fullHeight, fullHeight],
            [fullHeight/2, fullHeight/2, fullHeight/2],
            [fullHeight/2, fullHeight/2, fullHeight/2, fullHeight/2],
        ]
        return heights[this.maps.length - 1][this.mapIndex]
    },
    elementWidth() {
        let fullWidth = 100;
        let widths = [
            [fullWidth],
            [fullWidth/2, fullWidth/2],
            [fullWidth/2, fullWidth/2, fullWidth],
            [fullWidth/2, fullWidth/2, fullWidth/2, fullWidth/2],
        ]
        return widths[this.maps.length - 1][this.mapIndex]
    },
    isComponentInformations() {
        return this.showComponent == 'informations' ? 'visible' : 'scroll'
    }
  },
  watch: {
    mapView: {
        handler() {
            let {mapCenter, mapResolution, mapRotation} = this.mapView;
            if(this.currentMap.linkedTo == this.lastEventMapId) {
                this.$openlayers.getView(this.currentMap.id).setProperties({
                    center: mapCenter,
                    resolution: mapResolution,
                    rotation: mapRotation,
                })
            }
        },
        deep: true,
    },
    linkedTo() {
        // Sets the local value to the value sent by the parent
        this.linkValue = this.currentMap.linkedTo;
    },
    filterSelected() {
        //sets filter on change 
        let layer = new OlTile({
            source: new Zoomify({
                url: `${this.filterUrl}${this.imsBaseUrl}image/tile?zoomify=${this.currentMap.data.fullPath}/&tileGroup={TileGroup}&z={z}&x={x}&y={y}&channels=0&layer=0&timeframe=0&mimeType=${this.currentMap.data.mime}`,
                size: [this.mapWidth, this.mapHeight],
                extent: this.extent,
            }),
            extent: this.extent,
        })
        this.$openlayers.getMap(this.currentMap.id).getLayers().getArray()[0] = layer;
        this.$openlayers.getMap(this.currentMap.id).render();
        this.updateOverviewMap();
    },
    getCurrentZoom() {
        this.updateLayers = true;
    },
    lastEventMapId() {
        if(this.lastEventMapId != this.currentMap.id) {
            this.showPanel = false;
        } else {
            this.showPanel = true;
        }
    }
  },
  methods: {
    // Sends view infos
    sendView(e) {
        let payload = {
            mapId: this.currentMap.id,
            view: this.$openlayers.getView(this.currentMap.id),
        }
        let rect = this.$refs.exploreMap.getBoundingClientRect();
        this.mousePosition = [
            e.clientX - rect.left,
            e.clientY - rect.top
        ]
        this.$emit('dragged', payload);
    },
    // Sends which map is linked to this one to the parent
    sendLink() {
        let payload = [this.currentMap.id, this.linkValue];
        this.$emit('mapIsLinked', payload);
    },
    postPosition() {
        let extent = this.$openlayers.getView(this.currentMap.id).calculateExtent();
        let payload = {
            bottomLeftX: Math.round(extent[0]),
            bottomLeftY: Math.round(extent[1]),
            bottomRightX: Math.round(extent[2]),
            bottomLeftY: Math.round(extent[1]),
            image: parseInt(this.currentMap.imageId),
            topLeftX: Math.round(extent[0]),
            topLeftY: Math.round(extent[3]),
            topRightX: Math.round(extent[2]),
            topRightY: Math.round(extent[3]),
            zoom: this.$openlayers.getView(this.currentMap.id).getZoom(),
        }
        api.post(`/api/imageinstance/${this.currentMap.imageId}/position.json`, payload);
    },
    getOnlineUsers() {
        api.get(`/api/project/${this.currentMap.data.project}/online/user.json`).then(data => {
            this.onlineUsers = data.data.collection;
        })
    },
    updateOverviewMap() {
        this.$emit('updateOverviewMap');
    },
    deleteMap() {
        this.$emit('deleteMap', this.currentMap.id);
    },
    showTerms(payload) {
        this.termsToShow = payload;
    },
    setShowWithNoTerm(payload) {
        this.showWithNoTerm = payload;
    },
    setAllTerms(payload) {
        this.allTerms = payload; 
    },
    setFeatureSelected(payload) {
        this.featureSelected = payload;
    },
    setUserLayers(payload) {
        this.userLayers = payload;
    },
    setLayersSelected(payload) {
        this.layersSelected = payload;
    },
    updateMap(payload) {
        this.$emit('updateMap', {old: this.currentMap, new: payload});
    },
    setVectorLayersOpacity(payload) {
        this.vectorLayersOpacity = payload;
    },
    setImageGroup(payload) {
        this.currentMap.imageGroup = payload;
    },
    setFeatureSelectedData(payload) {
        this.featureSelectedData = payload;
    },
    setUpdateLayers(payload) {
        this.updateLayers = payload;
    },
    setUpdateAnnotationsIndex(payload) {
        this.updateAnnotationsIndex = payload;
    },
    setShowComponent(component) {
        if(component == this.showComponent) {
            this.showComponent = '';
        } else {
            this.showComponent = component;
        }
    }
  }, 
  mounted() {
    this.extent = [0, 0, this.mapWidth, this.mapHeight];

    // Init map
    this.$openlayers.init({
      element: this.currentMap.id,
      center: [this.mapWidth/2, this.mapHeight/2],
      zoom: this.mapZoom,
      controls: [
          new ZoomControls({
            target: document.getElementById('controls-'+this.currentMap.id),
          }),
          new RotateControls({
            target: document.getElementById('controls-'+this.currentMap.id),
          })
      ],
      enablePan: true,
      enableMouseWheelZoom: true,
      enableDoubleClickZoom: true,
      minZoom: 2,
      projection: {
        code: 'CYTO',
        extent: this.extent,
      },
    })

    // Adds layer
    let layer = new OlTile({
        source: new Zoomify({
            url: `${this.filterUrl}${this.imsBaseUrl}image/tile?zoomify=${this.currentMap.data.fullPath}/&tileGroup={TileGroup}&z={z}&x={x}&y={y}&channels=0&layer=0&timeframe=0&mimeType=${this.currentMap.data.mime}`,
            size: [this.mapWidth, this.mapHeight],
            extent: this.extent,
        }),
        extent: this.extent,
    })

    this.$openlayers.getMap(this.currentMap.id).addLayer(layer)
    this.$openlayers.getView(this.currentMap.id).setMaxZoom(this.currentMap.data.depth);
    this.$openlayers.getMap(this.currentMap.id).on('moveend', () => {
        this.postPosition();
    })
    this.$openlayers.getMap(this.currentMap.id).getControls().getArray()[0].element.childNodes.forEach(child => {
        child.classList.add('btn');
        child.classList.add('btn-default');
    })
    this.$openlayers.getMap(this.currentMap.id).getControls().getArray()[1].element.childNodes.forEach(child => {
        child.classList.add('btn');
        child.classList.add('btn-default');
    })
    setInterval(this.postPosition, 5000);
    setInterval(this.getOnlineUsers, 5000)
  }
}

</script>

<style>
  .map {
    position: relative;
    overflow: hidden;
  }
  .controls {
      position: absolute;
      top: 1em;
      left: 1em;
      display: flex;
      flex-direction: column;
  }
  .ol-zoom {
      margin-bottom: 1em;
      display: flex;
      flex-direction: column;
  }
  .ol-zoom-in {
      margin-bottom: 1em;
  }
  .bottom-panel {
      display: flex;
      position: absolute;
      bottom: 1em;
      left: 1em;
  }
  .postion-panel {
      margin-bottom: 0;
  }
  .component-panel {
      position: absolute;
      bottom: 4em;
      left: 1em;
  }
</style>
