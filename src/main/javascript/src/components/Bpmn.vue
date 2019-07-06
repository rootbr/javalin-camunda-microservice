<template>
  <div
    ref="container"
    class="container"
  />
</template>

<script>
  import BpmnJS from 'bpmn-js';

  export default {
    name: 'Bpmn',
    props: {
      url: {
        type: String,
        required: true,
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
      const { url, bpmnViewer, fetchDiagram } = this;
      bpmnViewer.on('import.done', ({error, warnings}) => {
        error
          ? this.$emit('error', error)
          : this.$emit('shown', warnings);

        bpmnViewer
          .get('canvas')
          .zoom('fit-viewport');

        fetch('http://localhost:8080/api/activities')
          .then(response => response.json())
          .then(myJson => this.activities = myJson);
      });
      if (url) {
        fetchDiagram(url);
      }
    },
    beforeDestroy() {
      this.bpmnViewer.destroy();
    },
    watch: {
      url(val) {
        this.$emit('loading');
        this.fetch(url);
      },
      diagramXML(val) {
        this.bpmnViewer.importXML(val);
      },
      activities(val) {
        Object.entries(val).forEach(([key, value]) => {
          this.bpmnViewer.get('overlays').add(key, {
            position: {
              top: 5,
              right: 25,
            },
            html: `<div class="success-message">${value}</div>`
          });
        })
      }
    },
    methods: {
      fetchDiagram(url) {
        fetch(url)
          .then(response => response.text())
          .then(text => (this.diagramXML = text))
          .catch(err => this.$emit('error', err));
      }
    }
  }
</script>


<style lang="scss">
  .container {
    height: 40vh;
  }
  .success-message {
    color: green;
    text-shadow: darkgreen;
    background-color: #DCFECC;
  }
</style>
