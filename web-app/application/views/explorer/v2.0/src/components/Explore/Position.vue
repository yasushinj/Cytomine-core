<template>
  <div>
      <div>
        magnitude: unknown    
      </div>
      <div>
          x: {{currentCoordinate[0]}}, y: {{currentCoordinate[1]}}
      </div>
  </div>
</template>

<script>
export default {
  name: 'Position',
  props: [
      'currentMapId',
      'mousePosition'
  ],
  computed: {
      currentCoordinate() {
          if(this.$openlayers.getMap(this.currentMapId) !== undefined) {
              let coord = this.$openlayers.getMap(this.currentMapId).getCoordinateFromPixel(this.mousePosition);
              return coord.map(value => Math.round(value * 100) / 1000);
          } else {
              return this.mousePosition;
          }
      }
  },
}
</script>
