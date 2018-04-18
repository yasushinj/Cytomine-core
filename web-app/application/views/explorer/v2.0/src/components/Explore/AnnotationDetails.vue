<template>
  <section v-if="annotationIsClicked">
      <h3>Current selection</h3>
      <section>
          <h4>General information</h4>
        <dl>
            <dt>Area</dt>
            <dd>{{data.area}} {{data.areaUnit}}</dd>
            <dt>Perimeter</dt>
            <dd>{{data.perimeter}} {{data.perimeterUnit}}</dd>
            <dt>Term(s)</dt>
            <dd v-for="term in data.term" :key="'term-'+term">{{displayTerm(term)}}</dd>
            <dt>User</dt>
            <dd>{{displayName(data.user)}}</dd>
        </dl>
      </section>
      <section>
          <h4>Annotation preview</h4>
        <img class="thumbnail" :src="data.smallCropURL" alt="An image of the annotation area">
      </section>
      <section>
          <h4>Similarities</h4>
      </section>
      <section>
          <h4>Properties</h4>
          <a :href="'#tabs-annotationproperties-'+ data.container +'-'+data.id">Add a property</a>
      </section>
      <section>
          <h4>Description</h4>
          <a :href="'#descriptionModal'+data.id">Add description</a>
      </section>
  </section>
</template>

<script>
import Style from 'ol/style/style';
import Fill from 'ol/style/fill';
import Stroke from 'ol/style/stroke';

export default {
  name: 'AnnotationDetails',
  data() {
      return {
          annotationIsClicked: false,
          data: {},
      }
  },
  props: [
      'currentMap',
      'featureSelected',
      'users',
      'terms',
  ],
  watch: {
    featureSelected(newFeature, oldFeature) {
        if(oldFeature === undefined || oldFeature.hasOwnProperty('id_')) {
            this.annotationIsClicked = false;
        }
        if(newFeature !== undefined) {
            api.get(`/api/annotation/${newFeature.getId()}.json`).then(data => {
                this.data = data.data;
                this.$emit('featureSelectedData', this.data);
                this.annotationIsClicked = true;
            })
        } else {
            this.annotationIsClicked = false;
        }
    }
  },
  methods: {
      findIndex(array, toFind) {
          return array.findIndex(item => item.id === toFind);
      },
      displayName(userId) {
          let index = this.findIndex(this.users, userId);
          return `${this.users[index].lastname} ${this.users[index].firstname} (${this.users[index].username})`;
      },
      displayTerm(termId) {
          let index = this.findIndex(this.terms, termId);
          return this.terms[index].key;
      }
  },
}
</script>
