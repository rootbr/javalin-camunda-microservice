import Vue from 'vue';
import Vuex from 'vuex';
import Axios from 'axios';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    processId: null,
    url: 'http://localhost:8080/api',
    activities: [],
  },
  getters: {
    processId: state => state.processId,
    activities: state => state.activities,
  },
  mutations: {
    updateProcessId(state, processId) {
      Vue.set(state, 'processId', processId);
    },
    updateActivities(state, activities) {
      Vue.set(state, 'activities', activities);
    },
  },
  actions: {
    async fetchData({state, commit}) {
      let {data} = await Axios.get(`${state.url}/activities`);
      commit('updateActivities', data);
      console.log(JSON.stringify(state.activities));
    },
  },
});
