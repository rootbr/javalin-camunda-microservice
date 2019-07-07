import Vue from 'vue';
import Vuex from 'vuex';
import Axios from 'axios';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    processId: null,
    url: 'http://localhost:8080/api',
    data: null,
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
  },
  actions: {
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
