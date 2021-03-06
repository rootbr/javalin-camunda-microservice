<template>
  <div
    ref="container"
    class="container"
  />
</template>

<script>
import BpmnJS from 'bpmn-js';
import { mapState } from 'vuex';

export default {
  name: 'Bpmn',
  data() {
    return {
      url: 'http://localhost:8080/api',
      readyImportDoneBpmn: false,
      diagramXML: null,
      activities: null,
    };
  },
  mounted() {
    const { container } = this.$refs;
    this.bpmnViewer = new BpmnJS({ container });
    const { bpmnViewer, fetchDiagram } = this;
    bpmnViewer.on('import.done', ({ error, warnings }) => {
      if (error) {
        this.$emit('error', error);
      } else {
        this.$emit('shown', warnings);
      }

      bpmnViewer
        .get('canvas')
        .zoom('fit-viewport');

      this.readyImportDoneBpmn = true;
    });
    fetchDiagram();
  },
  beforeDestroy() {
    this.bpmnViewer.destroy();
  },
  watch: {
    url() {
      this.$emit('loading');
      this.fetchDiagram();
    },
    diagramXML(val) {
      this.bpmnViewer.importXML(val);
    },
    data(newVal, oldVal) {
      if (this.readyImportDoneBpmn) this.update(newVal, oldVal);
    },
    readyImportDoneBpmn(v) {
      if (v) this.update(this.data, null);
    },
  },
  methods: {
    fetchDiagram() {
      fetch(`${this.url}/process`)
        .then(response => response.text())
        // eslint-disable-next-line no-return-assign
        .then(text => (this.diagramXML = text))
        .catch(err => this.$emit('error', err));
    },
    update(newVal, oldVal) {
      const overlays = this.bpmnViewer.get('overlays');
      if (oldVal) oldVal.activities.forEach(e => overlays.remove({ element: e.id }));
      if (newVal) {
        newVal.activities.forEach((e) => {
          let instances = '';
          let canceled = '';
          let finished = '';
          if (e.instances > 0) instances = `<span class="badge badge-pill badge-success">${e.instances}</span>`;
          if (e.canceled > 0) canceled = `<span class="badge badge-pill badge-danger">${e.canceled}</span>`;
          if (e.finished > 0) finished = `<span class="badge badge-pill badge-dark">${e.finished}</span>`;
          overlays.add(e.id, {
            position: {
              top: -5,
              left: 15,
            },
            html: `<div class="d-flex p-2 bd-highlight">${instances}${canceled}${finished}</div>`,
          });
        });
      }
    },
  },
  computed: mapState(['data']),
};
</script>


<style lang="scss">
  .container {
    height: 50vh;
  }
</style>
