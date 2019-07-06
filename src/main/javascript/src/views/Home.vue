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

  export default {
    name: 'Home',
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
        this.processIdScheme = s
      },
    },
  };
</script>
