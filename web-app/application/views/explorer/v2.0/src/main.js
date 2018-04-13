import Vue from 'vue'
import App from './App.vue'
import VueOpenLayers from './vue-openlayers'
import Axios from 'axios';
import 'vue-popperjs/dist/css/vue-popper.css';

window.api = Axios.create({
  baseUrl: 'http://localhost:8085/',
})

Vue.use(VueOpenLayers);

new Vue({
  el: '#app',
  render: h => h(App)
})
