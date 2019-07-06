<template>
  <div id="Table">
    <vue-bootstrap4-table
      :rows="rows"
      :columns="columns"
      :config="config"
      :classes="classes"
    >
    </vue-bootstrap4-table>
  </div>
</template>

<script>
  import VueBootstrap4Table from 'vue-bootstrap4-table';

  export default {
    name: 'Table',
    data() {
      return {
        polling: null,
        rows: [],
        columns: [
          {
            label: 'id',
            name: 'id',
            sort: true,
            uniqueId: true,
          },
          {
            label: 'businessKey',
            name: 'businessKey',
            sort: true,
            uniqueId: true,
          },
        ],
        classes: {
          table: {
            "table table-sm": true,
          },
        },
        config: {
          card_mode: false,
          show_refresh_button: false,
          show_reset_button: false,
          pagination: false,
          pagination_info: false,
          rows_selectable: false,
          global_search: {
            placeholder: "search text",
            visibility: true,
            case_sensitive: false,
            showClearButton: false,
          },
          server_mode: false,
        },
      };
    },
    methods: {
      fetchData() {
        this.polling = setInterval(() => {
          fetch('http://localhost:8080/api/processes')
            .then(response => response.json())
            .then(response => (this.rows = response))
            .catch(err => console.log(err));
        }, 500)
      },
    },
    components: {
      VueBootstrap4Table,
    },
    beforeDestroy() {
      clearInterval(this.polling)
    },
    created() {
      this.fetchData()
    },
  };
</script>

<style lang="scss">
  .my-slim {
    color: green;
    text-shadow: darkgreen;
    background-color: #DCFECC;
  }
</style>
