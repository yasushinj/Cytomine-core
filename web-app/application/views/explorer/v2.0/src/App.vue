<template>
  <div>
    <div v-if="maps.length < maxMapsToShow">
      <template v-if="imageGroupIndex[0]">
        <select class="btn" v-model.number="imageGroupToAdd" name="image-groups" id="image-groups">
          <option value="">Select an imagegroup</option>
          <option v-for="imageGroup in imageGroupIndex" :key="imageGroup.id" :value="imageGroup.id">{{imageGroup.name}}</option>
        </select>
        <button class="btn" @click="addImageGroup()">Add image group</button>
      </template>
      <template v-else>
        <select class="btn" v-model.number="imageToAdd" name="images" id="images">
          <option value="">Select an image to add</option>
          <option v-for="image in images" :key="image.id" :value="image.id">{{image.instanceFilename}}</option>
        </select>
        <button class="btn" @click="addMap(imageToAdd)">Add a map</button>
      </template>
    </div>
    <p v-else>You can only have {{maxMapsToShow}} maps displayed</p>
    <overview-map :lastEventMapId="lastEventMapId" :maps="maps"></overview-map>  
    <div class="maps-container">
      <explore v-for="map in maps" :key="map.id" @updateMap="updateMap" @dragged="setMap" @mapIsLinked="linkMaps" @deleteMap="deleteMap" @updateOverviewMap="updateOverviewMap" :mapView="mapView" :maps='maps' :currentMap="map" :lastEventMapId="lastEventMapId" :filters="filters" :imageGroupIndex="imageGroupIndex"></explore>
    </div>
  </div>
</template>

<script>
import Explore from './components/Explore'
import OverviewMap from './components/OverviewMap'

import uuid from 'uuid'

export default {
  name: 'app',
  components: {
    Explore,
    OverviewMap
  },
  data() {
    return {
      mapView: {
        mapCenter: [0, 0],
        mapZoom: 2,
        mapRotation: 0,
      },
      maxMapsToShow: 4,
      maps: [],
      lastEventMapId: null,
      images: [],
      projectId: '82029',
      imageToAdd: "",
      imageGroupToAdd: "",
      baseImage: '82258',
      filters: [],
      imageGroupIndex: [],
	  imageSequences: [],
    baseSequence: {},
	onlineUsers: [],
	currentUser: {},
    }
  },
  methods: {
    imageIndex(imageId) {
      return this.images.findIndex(image => image.id == imageId);
    },
    mapIndex(mapId) {
      return this.maps.findIndex(map => map.id == mapId)
    },
    setMap(payload) {
      this.mapView = {
        mapCenter: payload.view.getCenter(),
        mapResolution: payload.view.getResolution(),
        mapRotation: payload.view.getRotation(),
      }
      this.lastEventMapId = payload.mapId;
      this.$openlayers.getMap(payload.mapId).updateSize();
    },
    linkMaps(payload) {
      // Removes last linked map
      let index = this.maps.findIndex((map) => {
        return map.linkedTo === payload[0];
      })
      if(index !== -1) {
        this.maps[index].linkedTo = "";
      }

      // Finds map index
      index = this.mapIndex(payload[0])
      // Links maps
      this.maps[index].linkedTo = payload[1];

      index = this.mapIndex(payload[1])
      this.maps[index].linkedTo = payload[0];
    },
    addMap(imageId = this.imageToAdd, imageGroup = "", id = uuid()) {
      if(this.maps.length < this.maxMapsToShow && imageId !== "") {
        this.maps.push({
          id,
          imageId,
          linkedTo: "",
          imageGroup,
          user: this.currentUser,
          data: this.images[this.imageIndex(imageId)]
        })
      }
    },
    addImageGroup() {
      if(this.imageGroupToAdd !== "") {
        api.get(`/api/imagegroup/${this.imageGroupToAdd}/imagesequence.json`).then(data => {
          this.imageSequences = data.data.collection;
          this.addMap(this.imageSequences[0].image, this.imageGroupToAdd);
        })
      }
    },
    deleteMap(payload) {
      let index = this.maps.findIndex(map => {
        return map.id === payload;
      })
      this.maps.splice(index, 1);
    },
    updateOverviewMap() {
      this.lastEventMapId = 'reload';
    },
    updateMap(payload) {
      let index = this.maps.findIndex(map => map == payload.old);
      this.maps[index].data = payload.new;
      this.maps[index].imageId = payload.new.id;
    },
    ping() {
      api.post(`http://localhost-core:8080/server/ping.json`, {project: this.projectId});
    },
  },
  created() {
    api.get(`api/project/${this.projectId}/imagegroup.json`).then(data => {
      this.imageGroupIndex = data.data.collection;
    })

    api.get(`api/project/${this.projectId}/imageinstance.json`).then(data => {
		let id = uuid();
		this.lastEventMapId = id;
		this.images = data.data.collection;
		this.projectId = this.images[0].project;
		api.get(`api/user/current.json`).then(data => {
			this.currentUser = data.data;
			if(this.imageGroupIndex[0]) {
				api.get(`/api/imageinstance/${this.baseImage}/imagesequence.json`).then(resp => {
					this.baseSequence = resp.data.collection[0];
					this.addMap(this.baseImage, this.baseSequence.imageGroup, id);
				})
			} else {
				this.addMap(this.baseImage, "", id);
			}
		})

    })

    api.get(`api/project/${this.projectId}/imagefilterproject.json`).then(data => {
      this.filters = data.data.collection;
    })

    api.get(`/api/project/82029/online/user.json`).then(data => {
      this.onlineUsers = data.data.collection;
    })

    // setInterval(this.ping, 20000);
    
  },
}
</script>

<style>
  .maps-container {
    display: flex;
    flex-wrap: wrap;
  }
</style>

