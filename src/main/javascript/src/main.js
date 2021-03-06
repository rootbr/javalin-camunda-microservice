import Vue from 'vue';
import VueNativeSock from 'vue-native-websocket';
import Notifications from 'vue-notification';
import velocity from 'velocity-animate';
import App from './App.vue';
import router from './router';
import store from './store';

Vue.config.productionTip = false;

Vue.use(VueNativeSock, 'ws://localhost:8080/events', {
  store,
  format: 'json',
  reconnection: true,
  reconnectionAttempts: 5,
  reconnectionDelay: 3000,
});

Vue.use(Notifications, { velocity });

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app');
