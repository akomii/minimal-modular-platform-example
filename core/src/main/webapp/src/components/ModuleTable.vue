<script setup lang="ts">
import DataTable from "primevue/datatable"
import Column from "primevue/column"
import Button from "primevue/button"
import SplitButton from "primevue/splitbutton"
import Tag from "primevue/tag"
import { useConfirm } from "primevue/useconfirm"
import {
  type ModuleInfo,
  type ModuleStatusValue,
  useModules
} from "../composables/useModules"

const { modules, list, install, authorize, start, stop, remove } = useModules()
const confirm = useConfirm()

function statusSeverity(
  status: ModuleStatusValue
): "success" | "secondary" | "info" | "warn" {
  switch (status) {
    case "RUNNING":
      return "success"
    case "STOPPED":
      return "secondary"
    case "NOT_CREATED":
      return "info"
    default:
      return "warn"
  }
}

// Modules requesting core access must be authorized first — confirm both steps in one dialog.
function confirmInstall(module: ModuleInfo): void {
  if (module.coreAccess === "none" || module.authorized) {
    void install(module.id)
    return
  }
  confirm.require({
    header: "Authorize + Install",
    message: `"${module.id}" requests ${module.coreAccess} access to the core schema. Authorize it and install?`,
    icon: "pi pi-exclamation-triangle",
    accept: async () => {
      await authorize(module.id)
      await install(module.id)
    }
  })
}

function confirmRemove(module: ModuleInfo, purge: boolean): void {
  confirm.require({
    header: purge ? "Remove + Purge" : "Remove",
    message: purge
      ? `Remove "${module.id}" and purge its database (drops schema and data)?`
      : `Remove "${module.id}"? Its database schema and data are kept.`,
    icon: "pi pi-exclamation-triangle",
    accept: () => {
      void remove(module.id, purge)
    }
  })
}
</script>

<template>
  <div>
    <div class="table-toolbar">
      <Button label="Refresh" icon="pi pi-refresh" text @click="() => list()" />
    </div>
    <DataTable :value="modules" dataKey="id">
      <Column field="id" header="ID" />
      <Column field="version" header="Version" />
      <Column field="type" header="Type" />
      <Column header="Status">
        <template #body="{ data }">
          <Tag :value="data.status" :severity="statusSeverity(data.status)" />
        </template>
      </Column>
      <Column field="coreAccess" header="Core access" />
      <Column header="Authorized">
        <template #body="{ data }">
          <Tag
            v-if="data.coreAccess !== 'none'"
            :value="data.authorized ? 'yes' : 'no'"
            :severity="data.authorized ? 'success' : 'warn'"
          />
          <span v-else>–</span>
        </template>
      </Column>
      <Column header="Actions">
        <template #body="{ data }">
          <div class="actions">
            <Button
              v-if="data.status === 'NOT_CREATED'"
              label="Install"
              size="small"
              @click="confirmInstall(data)"
            />
            <Button
              v-if="data.status === 'STOPPED'"
              label="Start"
              size="small"
              severity="success"
              @click="start(data.id)"
            />
            <Button
              v-if="data.status === 'RUNNING'"
              label="Stop"
              size="small"
              severity="warn"
              @click="stop(data.id)"
            />
            <SplitButton
              v-if="data.status === 'STOPPED'"
              label="Remove"
              size="small"
              severity="danger"
              :model="[
                {
                  label: 'Remove + Purge',
                  icon: 'pi pi-trash',
                  command: () => confirmRemove(data, true)
                }
              ]"
              @click="confirmRemove(data, false)"
            />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.5rem;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
