<script setup lang="ts">
import DataTable from "primevue/datatable"
import Column from "primevue/column"
import Button from "primevue/button"
import SplitButton from "primevue/splitbutton"
import Tag from "primevue/tag"
import { useConfirm } from "primevue/useconfirm"
import {
  type ModuleDependency,
  type ModuleInfo,
  type ModuleStatusValue,
  useModules
} from "../composables/useModules"

const { modules, list, install, authorize, start, stop, remove } = useModules()
const confirm = useConfirm()

function formatDependencies(deps: ModuleDependency[]): string {
  return deps.map((d) => `${d.id} ${d.version}`).join(", ")
}

// Published host port(s) — the "host" side of each host:container mapping.
function hostPorts(ports: string[]): string[] {
  return ports.map((p) => p.split(":")[0])
}

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
      <Column header="Port">
        <template #body="{ data }">
          <template v-if="data.ports?.length">
            <a
              v-for="port in hostPorts(data.ports)"
              :key="port"
              :href="`http://localhost:${port}`"
              target="_blank"
              rel="noopener"
              class="port-link"
              >{{ port }}</a
            >
          </template>
          <span v-else>–</span>
        </template>
      </Column>
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
      <Column header="Depends on">
        <template #body="{ data }">
          <span v-if="data.dependsOn?.length">{{
            formatDependencies(data.dependsOn)
          }}</span>
          <span v-else>–</span>
        </template>
      </Column>
      <Column header="Endpoints">
        <template #body="{ data }">
          <template v-if="data.endpoints?.length">
            <div v-for="ep in data.endpoints" :key="ep.label" class="endpoint">
              <span class="ep-method">{{ ep.method }}</span> {{ ep.path }}
              <span class="ep-label">{{ ep.label }}</span>
            </div>
          </template>
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

.endpoint {
  font-size: 0.85rem;
  white-space: nowrap;
}

.ep-method {
  font-weight: 600;
}

.ep-label {
  color: #888;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.port-link {
  margin-right: 0.5rem;
}
</style>
