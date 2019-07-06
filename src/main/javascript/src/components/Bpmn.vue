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
      const { url, processId, bpmnViewer, fetchDiagram } = this;
      bpmnViewer.on('import.done', ({error, warnings}) => {
        error
          ? this.$emit('error', error)
          : this.$emit('shown', warnings);

        bpmnViewer
          .get('canvas')
          .zoom('fit-viewport');

        if (processId) {
          fetch(`${url}/activities/${processId}`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        } else {
          fetch(`${url}/activities`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        }
      });
      fetchDiagram(`${url}/process`);
    },
    beforeDestroy() {
      this.bpmnViewer.destroy();
    },
    watch: {
      processId(val) {
        if (val) {
          fetch(`${this.url}/activities/${val}`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        } else {
          fetch(`${this.url}/activities`)
            .then(response => response.json())
            .then(myJson => this.activities = myJson);
        }
      },
      url(val) {
        this.$emit('loading');
        this.fetchDiagram(`${val}/process`);
      },
      diagramXML(val) {
        this.bpmnViewer.importXML(val);
      },
      activities(newVal, oldVal){
        console.log("change old activities " + JSON.stringify(oldVal));
        console.log("change new activities " + JSON.stringify(newVal));
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
