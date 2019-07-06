<template>
  <div class="Process">
    <Bpmn
      :url="url"
      :processId="processId"
    />
    <Table
      :url="url"
      :processId="processId"
    />
  </div>
</template>

<script>
  import Bpmn from '@/components/Bpmn';
  import Table from "@/components/Table";

  export default {
    name: 'Process',
    components: {
      Table,
      Bpmn,
    },
    data() {
      return {
        url: 'http://localhost:8080/api',
        processId: '1316'
      };
    },
  };
</script>
