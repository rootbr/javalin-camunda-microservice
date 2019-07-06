<template>
  <div>
    <Bpmn :url="url"/>
    <Table :url="url"/>
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
      };
    },
  };
</script>
