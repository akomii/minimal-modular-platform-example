<script setup lang="ts">
import { onMounted } from "vue"
import ConfirmDialog from "primevue/confirmdialog"
import Toast from "primevue/toast"
import Tabs from "primevue/tabs"
import TabList from "primevue/tablist"
import Tab from "primevue/tab"
import TabPanels from "primevue/tabpanels"
import TabPanel from "primevue/tabpanel"
import ModuleTable from "./ModuleTable.vue"
import LogsPanel from "./LogsPanel.vue"
import UserManagement from "./UserManagement.vue"
import UserBar from "./UserBar.vue"
import type { UserInfo } from "../auth"
import { useModules } from "../composables/useModules"

const props = defineProps<{ user: UserInfo }>()
const { list } = useModules()

onMounted(() => {
  void list()
})
</script>

<template>
  <Tabs value="modules" class="layout">
    <header class="topbar">
      <TabList>
        <Tab value="modules">Modules</Tab>
        <Tab value="users">Users</Tab>
      </TabList>
      <UserBar :user="props.user" />
    </header>
    <TabPanels>
      <TabPanel value="modules">
        <div class="columns">
          <section>
            <ModuleTable />
          </section>
          <section class="logs">
            <LogsPanel />
          </section>
        </div>
      </TabPanel>
      <TabPanel value="users">
        <UserManagement :current-username="props.user.username" />
      </TabPanel>
    </TabPanels>
    <ConfirmDialog />
    <Toast />
  </Tabs>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0 1.5rem;
  border-bottom: 1px solid #ddd;
  font-family: system-ui, sans-serif;
}

/* zero padding on the OUTER tab panels only (so our paddings line up) — leave the nested logs tabs alone */
.layout > :deep(.p-tabpanels),
.layout > :deep(.p-tabpanels > .p-tabpanel) {
  padding: 0;
}

/* the top-level Modules / Users tabs read a touch larger than the nested log tabs */
.topbar :deep(.p-tab) {
  font-size: 1.1rem;
}

.columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 1.5rem;
  padding: 1.5rem;
}

.logs {
  border-left: 1px solid #ddd;
  padding-left: 1.5rem;
}

/* stack the columns on narrow screens — the divider becomes a horizontal rule */
@media (max-width: 1200px) {
  .columns {
    grid-template-columns: minmax(0, 1fr);
  }

  .logs {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #ddd;
    padding-top: 1.5rem;
  }
}
</style>
