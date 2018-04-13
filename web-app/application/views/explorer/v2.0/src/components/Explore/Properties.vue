<template>
  <div>
      <select @change="handleKey" v-model="colorSelected" name="property-color" id="property-color">
          <option v-for="color in colors" :key="color.value" :value="color.value">{{color.name}}</option>
      </select>
      <select @change="handleKey" v-model="propertySelected" name="properties" id="properties">
          <option value="">No key selected</option>
          <option v-for="property in propertiesToShow" :key="property.key" :value="property">{{property.key}}</option>
      </select>
  </div>
</template>

<script>
import Text from 'ol/style/text';
import Fill from 'ol/style/fill';
import uniqby from 'lodash.uniqby'

export default {
    name: 'Properties',
    props: [
        'currentMap',
        'layersSelected',
    ],
    data() {
        return {
            propertiesAvailable: [],
            propertySelected: "",
            propertiesToShow: [],
            colors: [
                {
                    value: '#ffffff',
                    name: 'White'
                },
                {
                    value: '#ff0000',
                    name: 'Red'
                },
                {
                    value: '#ff6600',
                    name: 'Orange'
                },
                {
                    value: '#ffff00',
                    name: 'Yellow'
                },
                {
                    value: '#008000',
                    name: 'Green',
                },
                {
                    value: '#0000ff',
                    name: 'Blue',
                },
                {
                    value: '#800080',
                    name: 'Purple',
                },
                {
                    value: '#000000',
                    name: 'Black',
                },
            ],
            colorSelected: '#000000',
            keys: [],
        }
    },
    watch: {
        layersSelected(newValue) {
            let toShow = uniqby(this.propertiesAvailable, 'key');
            this.propertiesToShow = newValue === [] ? [] : toShow;
            this.handleKey();
        }
    },
    methods: {
        getLayers() {
            return this.$openlayers.getMap(this.currentMap.id).getLayers().getArray();
        },
        getLayerIndex(userId) {
            return this.getLayers().findIndex(layer => layer.get('title') == userId)
        },
        getFeatures(userId) {
            let index = this.getLayerIndex(userId);
            return index > 0 ? this.getLayers()[index].getSource().getFeatures() : null;
        },
        handleKey() {
            this.layersSelected.map(layer => {
                this.removeKeys(layer.id); 
                api.get(`/api/user/${layer.id}/imageinstance/${this.currentMap.imageId}/annotationposition.json?key=${this.propertySelected.key}`).then(data => {
                    this.keys = data.data.collection;
                    this.keys.map(key => {
                        let index = this.getFeatures(layer.id).findIndex(feature => feature.getId() == key.idAnnotation);
                        let text = new Text({
                            font: 'bold 24px sans-serif',
                            fill: new Fill({
                                color: this.colorSelected,
                            }),
                            text: key.value,
                            overflow: true,
                        })
                        this.getFeatures(layer.id)[index].getStyle().setText(text);
                    })
                    this.updateSource(layer.id);
                })
            })        
        },
        removeKeys(userId) {
            this.getFeatures(userId).map(feature => feature.getStyle().setText(new Text({text: ""})));
        },
        updateSource(userId) {
            let index = this.getLayerIndex(userId)
            this.getLayers()[index].getSource().changed();
        }
    },
    created() {
        api.get(`/api/annotation/property/key.json?idImage=${this.currentMap.imageId}&user=true`).then(data => {
            this.propertiesAvailable = data.data.collection;
        })
    }
}
</script>

<style>

</style>
