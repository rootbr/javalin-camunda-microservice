import Vue from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import VueNativeSock from 'vue-native-websocket'

Vue.config.productionTip = false;

Vue.use(VueNativeSock, 'ws://localhost:8080/events/1', {
  store: store,
  format: 'json',
  reconnection: true,
  reconnectionAttempts: 5,
  reconnectionDelay: 3000,
});

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app');
