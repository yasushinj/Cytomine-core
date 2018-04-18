<template>
  <div>
      <h4>Overlay</h4>
      <ul>
            <li v-for="overlay in overlayedLayer" :key="overlay.id">
              <button @click="removeOverlay(overlay)">Remove</button>
              Channel{{overlay.channel}}
              <input @change="setOverlayColor(overlay)" type="text" placeholder="Color">
              <input @input="setOverlayOpacity($event, overlay)" type="range" step="0.1" min="0" max="1">
            </li>
      </ul>
      <div>
        <label :for="'overlay-' + currentMap.id">Choose a channel to add as an overlay</label>
      </div>
      <div class="overlay-select">
        <input v-model.number="sequenceSelected" type="number" :name="'overlay-' + currentMap.id" :id="'overlay-' + currentMap.id">
        <input v-model.number="sequenceSelected" type="range" step="1" min="1" :max="imageGroup.length">
      </div>
      <button class="btn btn-default mt-4" @click="addOverlay">Add as an overlay</button>
  </div>
</template>

<script>
import OlTile from 'ol/layer/tile';
import Zoomify from 'ol/source/zoomify';
import Group from 'ol/layer/group';

export default {
    name: 'Overlay',
    props: [
        'currentMap',
        'imageSequence',
        'imageGroup',
        'imsBaseUrl',
        'filterUrl'
    ],
    data() {
        return {
            sequenceSelected: 1,
            overlayedLayer: [],
        }
    },
    methods: {
        addOverlay() {
            let layersArray = this.$openlayers.getMap(this.currentMap.id).getLayers().getArray();
            let vectorIndex = layersArray.findIndex(layer => layer.getType() == 'VECTOR');
            let imageToAdd = this.imageGroup[this.sequenceSelected - 1];

            let layerToAdd = new OlTile({
                source: new Zoomify({
                    url: `${this.filterUrl}${this.imsBaseUrl}image/tile?zoomify=${imageToAdd.model.fullPath}/&tileGroup={TileGroup}&z={z}&x={x}&y={y}&channels=0&layer=0&timeframe=0&mimeType=${imageToAdd.model.mime}`,
                    size: [parseInt(imageToAdd.model.width), parseInt(imageToAdd.model.height)],
                    extent: [0, 0, parseInt(imageToAdd.model.width), parseInt(imageToAdd.model.height)],
                }),
                extent: [0, 0, parseInt(imageToAdd.model.width), parseInt(imageToAdd.model.height)],
            })

            layerToAdd.set('channel', imageToAdd.channel);

            if(vectorIndex > 0) {
                layersArray.splice(vectorIndex, 0, layerToAdd);
            } else {
                layersArray.push(layerToAdd);
            }

            this.overlayedLayer.push(imageToAdd);

            this.$openlayers.getMap(this.currentMap.id).setLayerGroup(new Group({layers: layersArray}))
            console.log(this.$openlayers.getMap(this.currentMap.id).getLayers().getArray())
        },
        removeOverlay(overlay) {
            let layersArray = this.$openlayers.getMap(this.currentMap.id).getLayers().getArray();
            let index = layersArray.findIndex(layer => layer.get('channel') == overlay.channel);
            layersArray.splice(index, 1);
            this.$openlayers.getMap(this.currentMap.id).setLayerGroup(new Group({layers: layersArray}));
            console.log(layersArray)
            index = this.overlayedLayer.findIndex(item => overlay == item);
            this.overlayedLayer.splice(index, 1);
        },
        setOverlayColor(overlay) {
            //
        },
        setOverlayOpacity(evt, overlay) {
            let opacity = evt.target.value;
            let layersArray = this.$openlayers.getMap(this.currentMap.id).getLayers().getArray();
            let index = layersArray.findIndex(layer => layer.get('channel') == overlay.channel);

            layersArray[index].setOpacity(opacity);
        }
    }
}
</script>

<style>
    .overlay-select {
        display: flex;
    }
</style>
