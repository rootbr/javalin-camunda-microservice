<template>
  <div>
    <Bpmn
      :url="url"
      :processId="processIdScheme"
    />
    <Table
      :url="url"
      @onSelectRow='onSelectProcess'
    />
  </div>
</template>

<script>
  import Bpmn from '@/components/Bpmn';
  import Table from "@/components/Table";
  import { store } from '@/store'

  export default {
    name: 'Home',
    store,
    components: {
      Table,
      Bpmn,
    },
    data() {
      return {
        url: 'http://localhost:8080/api',
        processIdScheme: null,
      };
    },
    methods: {
      onSelectProcess(s) {
        console.log(JSON.stringify(this.$store.state));
        this.processIdScheme = s
      },
    },
  };
</script>
