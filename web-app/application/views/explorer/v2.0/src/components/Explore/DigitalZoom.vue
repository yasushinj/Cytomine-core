<template>
  <div>
        <div class="alert alert-info">Enable or disable digital zoom (interpolation)</div>
        <input @change="toggleDigitalZoom" v-model="digitalZoom" type="checkbox" :name="'digital-zoom-'+currentMap.id" :id="'digital-zoom-'+currentMap.id">
        <label :for="'digital-zoom-'+currentMap.id">Digital zoom</label>
  </div>
</template>

<script>
export default {
    name: 'DigitalZoom',
    props: [
        'currentMap'
    ],
    data() {
        return {
            digitalZoom: false,
        }
    },
    methods: {
        toggleDigitalZoom() {
            if(this.digitalZoom === true) {
                this.$openlayers.getView(this.currentMap.id).setMaxZoom(this.currentMap.data.depth + 4);
            } else if(this.digitalZoom === false) {
                this.$openlayers.getView(this.currentMap.id).setMaxZoom(this.currentMap.data.depth);
            }
        }
    }
}
</script>

<style>

</style>
