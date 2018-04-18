<template>
    <div class="overview-container">
        <div v-show="showOverviewMap" id="overview-map"></div>
        <button class="btn btn-default" style="width: 100%;" id="overview-map-collapse" @click="showOverviewMap = !showOverviewMap">
            <span :class="`glyphicon glyphicon-chevron-${showOverviewMap ? 'right' : 'left'}`"></span>
        </button>
    </div>
</template>

<script>
import OverviewMap from 'ol/control/overviewmap'; 
import Projection from 'ol/proj/projection';
import View from 'ol/view';

export default {
  name: 'OverviewMap',
  data() {
      return {
          overviewMap: {},
          overviewMapCount: 0,
          showOverviewMap: true,
      }
  },
  props: [
      'lastEventMapId',
      'maps',
  ],
  watch: {
    maps() {
        if(this.overviewMapCount < 1) {
            this.initOverviewMap();
            this.overviewMapCount++
        }
    },
    lastEventMapId(newId, oldId) {
        let index = (id) => {
            return this.maps.findIndex(map => {
                return map.id === id;
            })
        }
        if(newId === 'reload') {
            return;
        } else if(oldId === 'reload') {
            this.initOverviewMap(this.maps[index(newId)])
            return;
        } else if(newId !== oldId && oldId) {
            this.$openlayers.getMap(oldId).removeControl(this.overviewMap)
            this.initOverviewMap(this.maps[index(newId)]);
        }   
    },
  },
  methods: {
      initOverviewMap(map = this.maps[0]) {
        this.overviewMap = new OverviewMap({
            collapsed: true,
            // collapsible: false,
            target: "overview-map",
            view: new View({
                projection: new Projection({
                    code: 'CYTO',
                    extent: [0, 0, parseInt(map.data.width), parseInt(map.data.height)],
                }),
                center:[0, 0],
                minZoom: 1,
                maxZoom: 2,
            }),
        })
        this.$openlayers.getMap(map.id).addControl(this.overviewMap);
      },
  },
}
</script>

<style>
    .overview-container {
        position: fixed;
        right: 15px;
        z-index: 9999; 
        border: 3px solid black;
        background: grey;
    }
    .ol-overviewmap-map {
        width: 256px;
        height: 256px;
    }
    .ol-overviewmap .ol-overviewmap-box {
        border: 2px solid red;
    }
    button[title="Overview map"] {
        display: none;
    }
</style>

