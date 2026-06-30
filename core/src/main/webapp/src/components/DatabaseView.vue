<script setup lang="ts">
import { onMounted, ref } from "vue"
import DataTable, { type DataTablePageEvent } from "primevue/datatable"
import Column from "primevue/column"
import { useDatabase } from "../composables/useDatabase"

// demo viewer: read-only, fixed page size — paging is offset-based against the backend's row cap
const PAGE_SIZE = 100

const { schemas, current, loadSchemas, loadTable } = useDatabase()
const selected = ref<{ schema: string; table: string } | null>(null)

onMounted(() => {
  void loadSchemas()
})

function isSelected(schema: string, table: string): boolean {
  return selected.value?.schema === schema && selected.value?.table === table
}

function select(schema: string, table: string): void {
  selected.value = { schema, table }
  void loadTable(schema, table, PAGE_SIZE, 0)
}

function onPage(event: DataTablePageEvent): void {
  if (selected.value) {
    void loadTable(selected.value.schema, selected.value.table, PAGE_SIZE, event.first)
  }
}

function format(value: unknown): string {
  return value === null || value === undefined ? "NULL" : String(value)
}
</script>

<template>
  <div class="db-viewer">
    <p class="hint" role="alert">
      <i class="pi pi-info-circle"></i>
      <span>Read-only demo tool: browse the live database schemas and table contents.</span>
    </p>
    <div class="columns">
      <nav class="tree">
        <div v-for="schema in schemas" :key="schema.name" class="schema">
          <div class="schema-name">{{ schema.name }}</div>
          <ul>
            <li
              v-for="table in schema.tables"
              :key="table"
              :class="{ active: isSelected(schema.name, table) }"
              @click="select(schema.name, table)"
            >
              {{ table }}
            </li>
          </ul>
        </div>
      </nav>
      <section class="data">
        <template v-if="current">
          <h3>
            {{ current.schema }}.{{ current.table }}
            <span class="count">{{ current.totalRows }} rows</span>
          </h3>
          <DataTable
            :value="current.rows"
            :lazy="true"
            :paginator="true"
            :rows="current.limit"
            :totalRecords="current.totalRows"
            :first="current.offset"
            @page="onPage"
          >
            <Column
              v-for="col in current.columns"
              :key="col"
              :field="col"
              :header="col"
            >
              <template #body="{ data }">
                <span :class="{ null: data[col] === null }">{{
                  format(data[col])
                }}</span>
              </template>
            </Column>
            <template #empty>This table is empty.</template>
          </DataTable>
        </template>
        <p v-else class="placeholder">Select a table to view its contents.</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.db-viewer {
  padding: 1.5rem;
}

.hint {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0 0 1.25rem;
  padding: 0.85rem 1rem;
  background: #eef6ff;
  border: 1px solid #93c5fd;
  border-left: 4px solid #3b82f6;
  border-radius: 6px;
  color: #1e3a5f;
  font-weight: 600;
  font-size: 0.95rem;
}

.hint .pi {
  font-size: 1.2rem;
}

.columns {
  display: grid;
  grid-template-columns: minmax(0, 16rem) minmax(0, 1fr);
  gap: 1.5rem;
}

.tree {
  border-right: 1px solid #ddd;
  padding-right: 1rem;
  font-family: system-ui, sans-serif;
}

.schema-name {
  font-weight: 700;
  margin: 0.75rem 0 0.35rem;
  color: #333;
}

.tree ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.tree li {
  padding: 0.3rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  color: #555;
  font-size: 0.9rem;
}

.tree li:hover {
  background: #f1f5f9;
}

.tree li.active {
  background: #3b82f6;
  color: #fff;
}

.data h3 {
  margin: 0 0 1rem;
  font-family: system-ui, sans-serif;
}

.count {
  font-weight: 400;
  color: #777;
  font-size: 0.9rem;
  margin-left: 0.5rem;
}

.null {
  color: #aaa;
  font-style: italic;
}

.placeholder {
  color: #777;
}

@media (max-width: 1000px) {
  .columns {
    grid-template-columns: minmax(0, 1fr);
  }

  .tree {
    border-right: none;
    padding-right: 0;
    border-bottom: 1px solid #ddd;
  }
}
</style>
