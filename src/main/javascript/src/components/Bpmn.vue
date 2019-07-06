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
      const {fetchActivities, bpmnViewer, fetchDiagram} = this;
      bpmnViewer.on('import.done', ({error, warnings}) => {
        error
          ? this.$emit('error', error)
          : this.$emit('shown', warnings);

        bpmnViewer
          .get('canvas')
          .zoom('fit-viewport');

        fetchActivities();
      });
      fetchDiagram();
    },
    beforeDestroy() {
      this.bpmnViewer.destroy();
    },
    watch: {
      processId() {
        this.fetchActivities();
      },
      url() {
        this.$emit('loading');
        this.fetchDiagram();
      },
      diagramXML(val) {
        this.bpmnViewer.importXML(val);
      },
      activities(newVal, oldVal) {
        let overlays = this.bpmnViewer.get('overlays');
        if (oldVal) {
          Object.entries(oldVal).forEach(([key, value]) => {
            overlays.remove({element: key});
          });
        }
        if (newVal) {
          Object.entries(newVal).forEach(([key, value]) => {
            overlays.add(key, {
              position: {
                top: 5,
                right: 25,
              },
              html: `<div class="success-message">${value}</div>`
            });
          })
        }
      },
    },
    methods: {
      fetchDiagram() {
        fetch(`${this.url}/process`)
          .then(response => response.text())
          .then(text => (this.diagramXML = text))
          .catch(err => this.$emit('error', err));
      },
      fetchActivities() {
        if (this.processId) {
          fetch(`${this.url}/activities/${this.processId}`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        } else {
          fetch(`${this.url}/activities`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        }
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
