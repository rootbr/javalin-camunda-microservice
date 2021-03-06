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
      state.socket.processId = processId;
    },
    updateData(state, data) {
      state.socket.data = data;
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

      if (message.type === 'ACTIVITY_INSTANCE_UPDATE') {
        state.data = message.payload;
      } else if (message.type === 'TASK_INSTANCE_UPDATE' || message.type === 'ERROR') {
        Vue.notify({
          group: 'events',
          title: message.payload.message,
          type: message.payload.type,
          duration: 5000,
          speed: 1000,
        });
      }
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
