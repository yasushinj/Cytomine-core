<template>
  <div>
    <section>
      <h4>Review | Action selection</h4>
      <div v-if="isSelected()">
        <dl>
          <dt>Annotation:</dt>
          <dd>{{featureId}}</dd>
          <dt>User</dt>
          <dd>{{featureUser}}</dd>
          <dt>Created:</dt>
          <dd>{{featureDate}}</dd>
          <dt>Term(s):</dt>
          <dd></dd>
        </dl>
        <button :disabled="this.featureSelectedData.reviewed" @click="acceptReview">Accept</button>
        <button :disabled="!this.featureSelectedData.reviewed" @click="rejectReview">Reject</button>
      </div>  
    </section>
    <section>
      <h4>Review | Action Image</h4>
      <div>
        <button @click="acceptAll">Accept all</button>
        <button @click="rejectAll">Reject all</button>
        <button v-if="currentMap.data.reviewed" @click="validateImage">Validate Image</button>
        <button v-else @click="unvalidateImage">Unvalidate Image</button>
      </div>
    </section>
  </div>
</template>

<script>
import humanDate from '../../helpers/humanDate'

export default {
    name:'Review',
    props: [
      'featureSelected',
      'featureSelectedData',
      'userLayers',
      'currentMap',
      'layersSelected',
    ],
    computed: {
      featureId() {
        return this.isSelected() ? this.featureSelectedData.id : "";
      },
      featureUser() {
        return this.isSelected() ? this.displayName(this.featureSelectedData.user) : "";
      },
      featureDate() {
        return this.featureSelectedData.created ? humanDate(this.featureSelectedData.created) : "";
      },
    },
    methods: {
      isSelected() {
        return this.featureSelected != undefined;
      },
      displayName(userId) {
        let index = this.userLayers.findIndex(user => user.id == userId);
        return index < 0 ? "" : `${this.userLayers[index].lastname} ${this.userLayers[index].firstname} (${this.userLayers[index].username})`
      },
      acceptReview() {
        let id = this.featureSelectedData.parentIdent ? this.featureSelectedData.parentIdent : this.featureId;
        api.put(`/api/annotation/${id}/review.json`, {
          id:this.featureId,
          terms: this.featureSelectedData.term,
        }).then(data => {
          this.featureSelected.getStyle().getStroke().setColor([91, 183, 91]);
          this.featureSelected.changed();
          this.$emit('featureSelectedData', data.data.reviewedannotation);
          this.$emit('updateLayers', true);
          this.$emit('updateAnnotationsIndex', true);
        })
      },
      acceptAll() {
        api.post(`/api/task.json?&project=${this.currentMap.data.project}`, {
          project: this.currentMap.data.project,
        }).then(data => {
          let task = data.data.task;
          api.put(`/api/imageinstance/${this.currentMap.imageId}/annotation/review.json?users=${this.currentMap.user.id}&task=${task.id}`, {
            image: this.currentMap.imageId,
            layers: this.layersSelected.map(layer => layer.id),
            task: task.id
          }).then(() => {
            this.$emit('updateLayers', true);
            this.$emit('updateAnnotationsIndex', true);
          })
        })
      },
      rejectReview() {
        let id = this.featureSelectedData.parentIdent;
        api.delete(`/api/annotation/${id}/review.json`).then(data => {
          this.featureSelected.getStyle().getStroke().setColor([189, 54, 47]);
          this.featureSelected.changed();
          api.get(`/api/annotation/${id}.json`).then(data => {
            this.$emit('featureSelectedData', data.data);
            this.$emit('updateLayers', true);
            this.$emit('updateAnnotationsIndex', true);
          })
        })
      },
      rejectAll() {
        api.post(`/api/task.json?&project=${this.currentMap.data.project}`, {
          project: this.currentMap.data.project,
        }).then(data => {
          let task = data.data.task;
          api.delete(`/api/imageinstance/${this.currentMap.imageId}/annotation/review.json?users=${this.currentMap.user.id}&task=${task.id}`).then(() => {
            this.$emit('updateLayers', true);
            this.$emit('updateAnnotationsIndex', true);
          });
        })
      },
      validateImage() {
        api.delete(`/api/imageinstance/${this.currentMap.imageId}/review.json`).then(data => {
          this.$emit('updateMap', data.data.imageinstance);
        })
      },
      unvalidateImage() {
        api.delete(`/api/imageinstance/${this.currentMap.imageId}/review.json?cancel=true`).then(data => {
          this.$emit('updateMap', data.data.imageinstance);
        })
      }
    }
}
</script>

<style>

</style>
