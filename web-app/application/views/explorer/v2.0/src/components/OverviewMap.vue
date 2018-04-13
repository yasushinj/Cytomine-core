<template>
  <div id="overview-map"></div>
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
            collapsed: false,
            target: "overview-map",
            view: new View({
                projection: new Projection({
                    code: 'CYTO',
                    extent: [0, 0, parseInt(map.data.width), parseInt(map.data.height)],
                }),
                center:[0, 0],
                minZoom: 1,
                maxZoom: 2,
            })
        })
        this.$openlayers.getMap(map.id).addControl(this.overviewMap);
      },
  },
}
</script>

<style>
    .ol-overviewmap {
        position: fixed;
        top: 15px;
        right: 15px;
        z-index: 9999; 
        border: 3px solid black;
    }
    .ol-overviewmap-map {
        width: 256px;
        height: 256px;
    }
    .ol-overviewmap .ol-overviewmap-box {
        border: 2px solid red;
    }
</style>

