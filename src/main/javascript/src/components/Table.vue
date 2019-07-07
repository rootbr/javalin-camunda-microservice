<template>
  <div id="Table">
    <vue-bootstrap4-table
      :rows="rows"
      :columns="columns"
      :config="config"
      :classes="classes"
      @on-select-row="onSelectRow"
      :actions="actions"
      @on-back="onBack"
    >
      <template slot="sort-asc-icon">
        <i class="fas fa-sort-amount-down"></i>
      </template>
      <template slot="sort-desc-icon">
        <i class="fas fa-sort-amount-up"></i>
      </template>
      <template slot="no-sort-icon">
        <i class="fas fa-sort"></i>
      </template>
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
        selectedProcessId: null,
        rows: [],
        columns: [],
        classes: {
          table: {
            "table table-sm": true,
          },
        },
        actions: [
          {
            btn_text: "back",
            event_name: "on-back",
            class: "btn btn-danger",
          }
        ],
        config: {
          card_mode: false,
          show_refresh_button: false,
          show_reset_button: false,
          pagination: false,
          pagination_info: false,
          rows_selectable: true,
          global_search: {
            placeholder: "search text",
            visibility: true,
            case_sensitive: false,
            showClearButton: true,
          },
          server_mode: false,
        },
      };
    },
    methods: {
      onBack() {
        this.selectedProcessId = null;
        this.$socket.sendObj({selectedProcessId: this.selectedProcessId})
      },
      onSelectRow(event) {
        this.selectedProcessId = event.selected_item.id;
        this.$socket.sendObj({selectedProcessId: this.selectedProcessId})
      },
      update(data) {
        let parse = JSON.parse(data);
        this.columns = parse.columns;
        this.rows = parse.rows;
      }
    },
    watch: {
      selectedProcessId(val) {
        this.config.rows_selectable = !val;
        this.$store.dispatch('changeProcessId', val);
      },
    },
    components: {
      VueBootstrap4Table,
    },
    created() {
      this.$options.sockets.onmessage = (message) => this.update(message.data);
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
