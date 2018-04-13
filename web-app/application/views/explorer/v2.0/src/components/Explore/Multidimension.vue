<template>
  <div>
      <h4>Multidimension</h4>
      <select @change="setImageGroup(currentMap.imageId, imageGroupSelected)" v-model="imageGroupSelected" name="imageGroupSelect" id="imageGroupSelect">
          <option v-for="sequence in imageSequences" :key="sequence.id" :value="sequence.imageGroup">{{getImageGroupName(sequence.imageGroup)}}</option>
      </select>
      <p>Picture is in position</p>
      <dl>
          <dt>c:</dt>
          <dd>{{currentSequence.channel}}</dd>
          <dt>z:</dt>
          <dd>{{currentSequence.zStack}}</dd>
          <dt>s:</dt>
          <dd>{{currentSequence.slice}}</dd>
          <dt>t:</dt>
          <dd>{{currentSequence.time}}</dd>
      </dl>
      <input v-model.number="sequenceSelected" min="1" :max="imageGroup.length" type="number" name="channel-select" id="channel-select">
      <button @click="selectImageSequence">Select</button>
      <overlay :imageSequence="currentSequence" :imageGroup="imageGroup" :currentMap="currentMap" :imsBaseUrl="imsBaseUrl" :filterUrl="filterUrl"></overlay>
      <spectra :imageSequence="currentSequence" :imageGroup="imageGroup" :currentMap="currentMap"></spectra>
  </div>
</template>

<script>
import Spectra from './Multidimension/Spectra'
import Overlay from './Multidimension/Overlay'

import OlTile from 'ol/layer/tile';
import Zoomify from 'ol/source/zoomify';
import Group from 'ol/layer/group';

export default {
    name: 'Multidimension',
    components: {
        Spectra,
        Overlay,
    },
    props: [
        'currentMap',
        'imsBaseUrl',
        'filterUrl',
        'imageGroupIndex',
    ],
    data() {
        return {
            imageSequences: [],
            imageGroup: [],
            sequenceSelected: "",
            imageGroupSelected: "",
            currentSequence: {},
        }
    },
    methods: {
        selectImageSequence() {
            this.currentSequence = this.imageGroup[this.sequenceSelected - 1];
            this.$emit('imageHasChanged', this.imageGroup[this.sequenceSelected - 1].model);

            let layer = new OlTile({
                source: new Zoomify({
                    url: `${this.filterUrl}${this.imsBaseUrl}image/tile?zoomify=${this.currentMap.data.fullPath}/&tileGroup={TileGroup}&z={z}&x={x}&y={y}&channels=0&layer=0&timeframe=0&mimeType=${this.currentMap.data.mime}`,
                    size: [this.currentMap.data.width, this.currentMap.data.height],
                    extent: [0, 0, this.currentMap.data.width, this.currentMap.data.height],
                }),
                extent: [0, 0, this.currentMap.data.width, this.currentMap.data.height],
            })
            
            this.$openlayers.getMap(this.currentMap.id).setLayerGroup(new Group({layers: [layer]}));    
        },
        getImageGroupName(imageGroupId) {
            let index = this.imageGroupIndex.findIndex(group => {
                return group.id === imageGroupId;
            })

            return this.imageGroupIndex[index].name;
        },
        setImageGroup(imageId, imageGroupId) {
            this.$emit('imageGroupHasChanged', imageGroupId);
            api.get(`/api/imageinstance/${imageId}/imagesequence.json`).then(data => {
                this.imageSequences = data.data.collection;
                api.get(`/api/imagegroup/${imageGroupId}/imagesequence.json`).then(data => {
                    if(data.data.collection) {
                        this.imageGroup = data.data.collection.sort((a, b) => {
                            return a.channel - b.channel;
                        });
                        let index = this.imageGroup.findIndex(image => {
                            return image.image == this.currentMap.imageId;
                        }) 
                        this.sequenceSelected = index + 1;
                        index = this.imageSequences.findIndex(sequence => {
                            return sequence.imageGroup === imageGroupId;
                        })
                        this.currentSequence = this.imageSequences[index];
                    }
                })
            })
        }
    },
    created() {
        this.imageGroupSelected = this.currentMap.imageGroup;
        this.setImageGroup(this.currentMap.imageId, this.currentMap.imageGroup)
    }
}
</script>
