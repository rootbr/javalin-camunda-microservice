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
  import { mapState } from 'vuex';

  export default {
    name: 'Table',
    props: {
      url: {
        type: String,
        required: true,
      },
      processId: {
        type: String,
        required: false,
      },
    },
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
            showClearButton: false,
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
        this.$store.dispatch('fetchData');
      },
      fetchData() {
        if (this.selectedProcessId) {
          this.columns = [
            {
              label: 'variable',
              name: 'id',
              sort: true,
              uniqueId: true,
            },
            {
              label: 'value',
              name: 'value',
              sort: true,
            },
          ];
          fetch(`${this.url}/variables/${this.selectedProcessId}`)
            .then(response => response.json())
            .then(response => (this.rows = response))
            .catch(err => console.log(err));
        } else {
          this.columns = [
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
          ];
          fetch(`${this.url}/processes`)
            .then(response => response.json())
            .then(response => (this.rows = response))
            .catch(err => console.log(err));
        }
      },
      pollingData() {
        this.polling = setInterval(() => {
          this.fetchData();
        }, 400)
      },
    },
    watch: {
      selectedProcessId(val) {
        this.config.rows_selectable = !val;
        this.fetchData();
        this.$emit('onSelectRow', val);
      },
      activities(oldVal, newVal) {
        console.log(`Updating from ${JSON.stringify(oldVal)} to ${JSON.stringify(newVal)}`);
      }
    },
    computed: mapState(['activities']),
    components: {
      VueBootstrap4Table,
    },
    beforeDestroy() {
      clearInterval(this.polling)
    },
    created() {
      this.selectedProcessId = this.processId;
      this.fetchData();
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
