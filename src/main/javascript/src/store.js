import Vue from 'vue';
import Vuex from 'vuex';
import Axios from 'axios';

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
  getters: {
    processId: state => state.processId,
    activities: state => state.activities,
  },
  mutations: {
    updateProcessId(state, processId) {
      Vue.set(state, 'processId', processId);
    },
    updateData(state, data) {
      Vue.set(state, 'data', data);
    },
    SOCKET_ONOPEN (state, event)  {
      Vue.prototype.$socket = event.currentTarget
      state.socket.isConnected = true
    },
    SOCKET_ONCLOSE (state, event)  {
      state.socket.isConnected = false
    },
    SOCKET_ONERROR (state, event)  {
      console.error(state, event)
    },
    // default handler called for all methods
    SOCKET_ONMESSAGE (state, message)  {
      state.socket.message = message
    },
    SOCKET_RECONNECT(state, count) {
      console.info(state, count)
    },
    SOCKET_RECONNECT_ERROR(state) {
      state.socket.reconnectError = true;
    },
  },
  actions: {
    sendMessage(context, message) {
      Vue.prototype.$socket.send(message);
    },
    async fetchData({state, commit}) {
      let url = `${state.url}/state`;
      if (state.processId != null) url = url + `/${state.processId}`;
      let {data} = await Axios.get(url);
      commit('updateData', data);
    },
    async changeProcessId({commit, dispatch}, id) {
      await commit('updateProcessId', id);
      await dispatch('fetchData');
    },
  },
});
