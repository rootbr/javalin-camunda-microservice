<template>
  <div>
    <Bpmn />
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
      };
    },
    methods: {

    },
  };
</script>
