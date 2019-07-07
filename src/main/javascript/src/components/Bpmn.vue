<template>
  <div
    ref="container"
    class="container"
  />
</template>

<script>
  import BpmnJS from 'bpmn-js';
  import {mapState} from 'vuex';

  export default {
    name: 'Bpmn',
    props: {
      url: {
        type: String,
        required: true,
      },
      processId: {
        type: String,
        required: false,
      },
    },
    data() {
      return {
        diagramXML: null,
        activities: null,
      };
    },
    mounted() {
      const {container} = this.$refs;
      this.bpmnViewer = new BpmnJS({container});
      const {bpmnViewer, fetchDiagram} = this;
      bpmnViewer.on('import.done', ({error, warnings}) => {
        error
          ? this.$emit('error', error)
          : this.$emit('shown', warnings);

        bpmnViewer
          .get('canvas')
          .zoom('fit-viewport');
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
        this.update(newVal, oldVal)
      },
    },
    methods: {
      fetchDiagram() {
        fetch(`${this.url}/process`)
          .then(response => response.text())
          .then(text => (this.diagramXML = text))
          .catch(err => this.$emit('error', err));
      },
      update(newVal, oldVal) {
        let overlays = this.bpmnViewer.get('overlays');
        if (oldVal) {
          oldVal.activities.forEach(e => {
            overlays.remove({element: e.id});
          });
        }
        if (newVal) {
          newVal.activities.forEach(e => {
            var instances = '', canceled = '', finished = '';
            if (e.instances > 0) instances = `<span class="badge badge-pill badge-success">${e.instances}</span>`;
            if (e.canceled > 0) canceled = `<span class="badge badge-pill badge-danger">${e.canceled}</span>`;
            if (e.finished > 0) finished = `<span class="badge badge-pill badge-dark">${e.finished}</span>`;
            overlays.add(e.id, {
              position: {
                top: -5,
                left: 15,
              },
              html: `<div class="d-flex p-2 bd-highlight">${instances}${canceled}${finished}</div>`
            });
          });
        }
      }
    },
    computed: mapState(['data']),
  }
</script>


<style lang="scss">
  .container {
    height: 50vh;
  }
</style>
