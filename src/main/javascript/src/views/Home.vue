<template>
  <div>
    <Bpmn :url="url"/>
    <hr />
    <input v-model="url" />
  </div>
</template>

<script>
  import Bpmn from '@/components/Bpmn';

  export default {
    name: 'Home',
    components: {
      Bpmn,
    },
    data() {
      return {
        url: 'http://localhost:8080/api/process',
      };
    },
  };
</script>
