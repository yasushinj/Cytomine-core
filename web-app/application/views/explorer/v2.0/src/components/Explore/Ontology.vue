<template>
  <div>
        <ul>
            <li v-for="term in terms" :key="'term-'+term.id">
                <input @change="handlePost(term.id)" v-model="featureTerms" :value="term.id" type="checkbox" :name="'term-'+term.id" :id="'term-'+term.id">
                <label :for="'term-'+term.id">{{term.key}} ({{term.value}})</label>
                <label :for="'show-term-'+term.id">Show</label>
                <input v-model="visibleTerms" :value="term.id" type="checkbox" :id="'show-term-'+term.id">
            </li>
        </ul>

        <input v-model="showWithNoTerm" type="checkbox" name="showNoTermAnnotation" id="showNoTermAnnotation">
        <label for="showNoTermAnnotation">Show annotations without terms</label>

        <button @click="showAllTerms">Show all</button>
        <button @click="hideAllTerms">Hide all</button>
  </div>
</template>

<script>
import intersection from 'lodash.intersection'
import Style from 'ol/style/style';
import Fill from 'ol/style/fill';
import Stroke from 'ol/style/stroke';
import hexToRgb from '../../helpers/hexToRgb'

export default {
  name: 'Ontology',
  props: [
      'featureSelectedData',
      'featureSelected',
      'vectorLayersOpacity',
  ],
  data() {
      return {
          terms: [],
          visibleTerms: [],
          showWithNoTerm: true,
          featureTerms: [],
      }
  },
  computed: {
      termsId() {
          return this.terms.map(term => term.id);
      },
      featureSelectedDataToShow() {
          if(this.featureSelected !== undefined && this.featureSelected.hasOwnProperty('id_')) {
              return this.featureSelectedData;
          } else {
              return undefined;
          }
      }
  },
  watch: {
      visibleTerms(newValue) {
          this.$emit('showTerms', newValue);
      },
      showWithNoTerm(newValue) {
          this.$emit('showWithNoTerm', newValue);
      },
      featureSelectedDataToShow(newValue) {
          if(newValue === undefined) {
              this.featureTerms = [];
          } else {
              this.featureTerms = newValue.term;
          }
      }
  },
  methods: {
      showAllTerms() {
          this.visibleTerms = this.termsId;
          this.showWithNoTerm = true;
      },
      hideAllTerms() {
          this.visibleTerms = [];
          this.showWithNoTerm = false;
      },
      changeFeatureColor() {
            let alpha = this.vectorLayersOpacity + 0.3;
            let index = this.terms.findIndex(term => term.id === this.featureSelectedData.term[0])
            let fillColor = this.featureSelectedData.term.length == 1 ? hexToRgb(this.terms[index].color, alpha) : [204, 204, 204, alpha];
            this.featureSelected.setStyle(new Style({
                fill: new Fill({
                    color: fillColor,
                }),
                stroke: new Stroke({
                    color: [0,0,0, alpha],
                    width: 3,
                })
            }))
      },
      handlePost(termId) {
          if(this.featureSelectedDataToShow.term.length > this.featureTerms.length) {
                api.delete(`/api/annotation/${this.featureSelected.getId()}/term/${termId}.json`).then(data => {
                    let index = this.featureSelectedDataToShow.term.findIndex(term => term === termId);
                    this.featureSelectedData.term.splice(index, 1);
                    this.changeFeatureColor()
                })
          } else {
                api.post(`/api/annotation/1655/term/1481.json`, {
                    term: termId, 
                    userannotation: this.featureSelected.getId()
                }).then(data => {
                    this.featureSelectedData.term.push(termId);
                    this.changeFeatureColor()
                })
          }
      }
  },
  created() {
      api.get(`/api/project/1493/stats/term.json`).then(data => {
          this.terms = data.data.collection;
          this.visibleTerms = this.termsId;
          this.$emit('showTerms', this.termsId);
          this.$emit('allTerms', this.terms);
      })
  }
}
</script>
