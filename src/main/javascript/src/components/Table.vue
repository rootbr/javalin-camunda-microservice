<template>
  <div id="Table">
    <vue-bootstrap4-table
      :rows="rows"
      :columns="columns"
      :config="config"
      :classes="classes"
      @on-select-row="onSelectRow"
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
      onSelectRow(event) {
        this.selectedProcessId = event.selected_item.id;
        this.fetchData();
        this.$emit('onSelectRow', this.selectedProcessId)
      },
      fetchData() {
        if (this.selectedProcessId) {
          fetch(`${this.url}/variables/${this.selectedProcessId}`)
            .then(response => response.json())
            .then(response => (this.rows = response))
            .catch(err => console.log(err));
        } else {
          fetch(`${this.url}/processes`)
            .then(response => response.json())
            .then(response => (this.rows = response))
            .catch(err => console.log(err));
        }
      },
      pollingData() {
        this.polling = setInterval(() => {
          this.fetchData();
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
      if (this.processId) {
        this.selectedProcessId = processId;
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
        ]
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
        ]
      };
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
