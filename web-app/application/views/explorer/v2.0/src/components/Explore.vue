<template>
    <div class="map">
        <div @mousemove="sendView" @mousewheel="sendView($event, true)" :id="currentMap.id" ref="exploreMap">
        </div>
        <digital-zoom :currentMap="currentMap"></digital-zoom>
        <label :for="'link-'+currentMap.id">Link the map</label>
        <select @change="sendLink" v-model="linkValue" name="link" :id="'link-'+currentMap.id">
            <option value="">Select a map</option>
            <template v-for="(map, index) in maps">
                <option v-if="index !== mapIndex" :key="map.id" :value="map.id">{{mapNames[index]}}</option>
            </template>
        </select>
        <div>
            <label :for="'original-filter-'+currentMap.id">Original</label>
            <input v-model="filterSelected" type="radio" :name="'filter-original-'+currentMap.id" :id="'filter-original-'+currentMap.id" value="">
            <div v-for="filter in filters" :key="filter.id">
                <label :for="'filter-'+filter.id+'-'+currentMap.id">{{filter.name}}</label>
                <input v-model="filterSelected" type="radio" :name="'filter-'+filter.id+'-'+currentMap.id" :id="'filter-'+filter.id+'-'+currentMap.id" :value="filter">
            </div>
        </div>
        <color-maps :currentMap="currentMap"></color-maps>
        <annotation-layers @updateLayers="setUpdateLayers" @vectorLayersOpacity="setVectorLayersOpacity" @layersSelected="setLayersSelected" @userLayers="setUserLayers" :onlineUsers="onlineUsers" :isReviewing="isReviewing" :updateLayers="updateLayers" :termsToShow="termsToShow" :showWithNoTerm="showWithNoTerm" :allTerms="allTerms" :currentMap="currentMap"></annotation-layers>
        <interactions @updateLayers="setUpdateLayers" @featureSelected="setFeatureSelected" :currentMap="currentMap" :isReviewing="isReviewing" :vectorLayersOpacity="vectorLayersOpacity"></interactions>
        <ontology :featureSelectedData="featureSelectedData" :featureSelected="featureSelected" :vectorLayersOpacity="vectorLayersOpacity" @showTerms="showTerms" @showWithNoTerm="setShowWithNoTerm" @allTerms="setAllTerms"></ontology>
        <review v-if="isReviewing" @updateAnnotationsIndex="setUpdateAnnotationsIndex" @updateLayers="setUpdateLayers" @featureSelectedData="setFeatureSelectedData" @updateMap="updateMap" :layersSelected="layersSelected" :currentMap="currentMap" :featureSelectedData="featureSelectedData" :featureSelected="featureSelected" :userLayers="userLayers"></review>
        <multidimension v-if="imageGroupIndex[0]" @imageGroupHasChanged="setImageGroup" :imageGroupIndex="imageGroupIndex" :filterUrl="filterUrl" :imsBaseUrl="imsBaseUrl" @imageHasChanged="updateMap" :currentMap="currentMap"></multidimension>
        <properties :layersSelected="layersSelected" :currentMap="currentMap"></properties>
        <annotation-details @featureSelectedData="setFeatureSelectedData" :users="userLayers" :terms="allTerms" :featureSelected="featureSelected" :currentMap="currentMap"></annotation-details>
        <informations @updateMap="updateMap" @updateOverviewMap="updateOverviewMap" :filterUrl="filterUrl" :imsBaseUrl="imsBaseUrl" :currentMap="currentMap"></informations>
        <position :mousePosition="mousePosition" :currentMapId="currentMap.id"></position>
        <annotations @updateAnnotationsIndex="setUpdateAnnotationsIndex" :updateAnnotationsIndex="updateAnnotationsIndex" :isReviewing="isReviewing" :users="userLayers" :terms="allTerms" :currentMap="currentMap"></annotations>
        <button @click="deleteMap">Delete the map</button>
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
        return false;
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
    }
  },
  methods: {
    // Sends view infos
    sendView(e, updateLayers = false) {
        let payload = {
            mapId: this.currentMap.id,
            view: this.$openlayers.getView(this.currentMap.id),
        }
        let rect = this.$refs.exploreMap.getBoundingClientRect();
        this.mousePosition = [
            e.clientX - rect.left,
            e.clientY - rect.top
        ]
        if(updateLayers) {
            this.updateLayers = true;
        }
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
    }
  }, 
  mounted() {
    this.extent = [0, 0, this.mapWidth, this.mapHeight];

    // Init map
    this.$openlayers.init({
      element: this.currentMap.id,
      center: [this.mapWidth/2, this.mapHeight/2],
      zoom: this.mapZoom,
      enableZoomButton: true,
      enablePan: true,
      enableMouseWheelZoom: true,
      enableDoubleClickZoom: true,
      enableScaleLine: true,
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
    setInterval(this.postPosition, 5000);
    setInterval(this.getOnlineUsers, 5000)
  }
}

</script>

<style>
  .map {
    width: 100%;
    height: 100vh;
  }
</style>
