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
  import {mapState} from 'vuex';

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
      },
      onSelectRow(event) {
        this.selectedProcessId = event.selected_item.id;
        this.$socket.sendObj({awesome: 'data'})
      },
      pollingData() {
        this.$store.dispatch('fetchData');
        this.polling = setInterval(() => {
          this.$store.dispatch('fetchData');
        }, 350)
      },
      update(data) {
        this.columns = data.columns;
        this.rows = data.rows;
      }
    },
    watch: {
      selectedProcessId(val) {
        this.config.rows_selectable = !val;
        this.$store.dispatch('changeProcessId', val);
      },
      data(val) {
        if (val != null) this.update(val)
      },
    },
    computed: mapState(['data']),
    components: {
      VueBootstrap4Table,
    },
    beforeDestroy() {
      clearInterval(this.polling)
    },
    created() {
      this.pollingData();
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
