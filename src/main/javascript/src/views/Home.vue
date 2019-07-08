<template>
  <div>
    <Bpmn
      :url="url"
    />
    <Table />
  </div>
</template>

<script>
  import Bpmn from '@/components/Bpmn.vue';
  import Table from '@/components/Table.vue';

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
    methods: {

    },
  };
</script>
