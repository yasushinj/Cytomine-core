<template>
  <div>
      <div>
        <dt>Name</dt>
        <dd>{{currentMap.data.instanceFilename}}</dd>
        <dt>Width</dt>
        <dd>{{currentMap.data.width}} pixels</dd>
        <dt>Height</dt>
        <dd>{{currentMap.data.height}} pixels</dd>
      </div>
      <div>
        <ul>
          <li><a :href="`#tabs-image-${currentMap.data.project}-${currentMap.imageId}-0`">Explore</a></li>
          <li><a @click="reviewMode" :href="`#tabs-review-${this.currentMap.data.project}-${this.currentMap.imageId}-`">Review</a></li>
          <li><a :href="`#tabs-reviewdash-${currentMap.data.project}-${currentMap.imageId}-null-null`">Review (Cyto)</a></li>
          <li><a @click="validateImage" href="#">Validate image</a></li>
          <li><a href="#">Copy image and annotations</a></li>
          <li><a href="#">Import user annotations</a></li>
          <li><a href="#">Description</a></li>
          <li><a :href="`/api/imageinstance/${currentMap.imageId}/download`">Download</a></li>
          <li><a href="#">Rename</a></li>
          <li><a href="#">Delete</a></li>
          <li><a href="#">More info</a></li>
        </ul>
      </div>
      <div>
        <button @click="setAdjacentImage('previous')">Previous image</button>
        <button @click="setAdjacentImage('next')">Next image</button>
        <small v-if="adjacentImageError != ''">{{adjacentImageError}}</small>
      </div>
  </div>
</template>

<script>
import OlTile from 'ol/layer/tile';
import Zoomify from 'ol/source/zoomify';
import Group from 'ol/layer/group';

export default {
  name: 'Informations',
  props: [
      'currentMap',
      'imsBaseUrl',
      'filterUrl',
  ],
  data() {
    return {
      adjacentImage: {},
      adjacentImageError: '',
    }
  },
  watch: {
    adjacentImage(newValue) {
      this.$emit('updateMap', newValue);
    }
  },
  methods: {
    reviewMode() {
      api.put(`/api/imageinstance/${this.currentMap.imageId}/review.json`, {
        id: this.currentMap.imageId,
      }).then(data => {
        this.$emit('updateMap', data.data.imageinstance);
      })
    },
    validateImage() {
      api.delete(`/api/imageinstance/${this.currentMap.imageId}/review.json`).then(data => {
        this.$emit('updateMap', data.data.imageinstance);
      })
    },
    setAdjacentImage(position) {
      api.get(`/api/imageinstance/${this.currentMap.imageId}/${position}.json`).then(data => {
        this.adjacentImageError = '';
        if(!data.data.hasOwnProperty('id')) {
          this.adjacentImageError = position == 'next' ? 'This is the last image' : 'This is the first image';;
          return;
        }
        this.adjacentImage = data.data;
        this.changeImage();
      })
    },
    changeImage() {
      let layer = new OlTile({
          source: new Zoomify({
              url: `${this.filterUrl}${this.imsBaseUrl}image/tile?zoomify=${this.adjacentImage.fullPath}/&tileGroup={TileGroup}&z={z}&x={x}&y={y}&channels=0&layer=0&timeframe=0&mimeType=${this.adjacentImage.mime}`,
              size: [this.adjacentImage.width, this.adjacentImage.height],
              extent: [0, 0, this.adjacentImage.width, this.adjacentImage.height],
          }),
          extent: [0, 0, this.adjacentImage.width, this.adjacentImage.height],
      })
      
      this.$openlayers.getMap(this.currentMap.id).setLayerGroup(new Group({layers: [layer]}));
      this.$emit('updateOverviewMap');
  },
  },
}
</script>
