import Vue from 'vue';
import Vuex from 'vuex';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    processId: null,
    url: 'http://localhost:8080/api',
    data: null,
    socket: {
      isConnected: false,
      message: '',
      reconnectError: false,
    },
  },
  mutations: {
    updateProcessId(state, processId) {
      Vue.set(state, 'processId', processId);
    },
    updateData(state, data) {
      Vue.set(state, 'data', data);
    },
    SOCKET_ONOPEN(state, event) {
      Vue.prototype.$socket = event.currentTarget;
      state.socket.isConnected = true;
    },
    SOCKET_ONCLOSE(state) {
      state.socket.isConnected = false;
    },
    SOCKET_ONERROR(state, event) {
      console.error(state, event);
    },
    SOCKET_ONMESSAGE(state, message) {
      state.socket.message = message;
      state.data = message;
    },
    SOCKET_RECONNECT(state, count) {
      console.info(state, count);
    },
    SOCKET_RECONNECT_ERROR(state) {
      state.socket.reconnectError = true;
    },
  },
  actions: {
    sendMessage(context, message) {
      Vue.prototype.$socket.send(message);
    },
  },
});
