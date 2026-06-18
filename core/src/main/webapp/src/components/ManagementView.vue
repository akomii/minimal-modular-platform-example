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
import { useModules } from "../composables/useModules"

const { list } = useModules()

onMounted(() => {
  void list()
})
</script>

<template>
  <div class="management-view">
    <Tabs value="modules">
      <TabList>
        <Tab value="modules">Modules</Tab>
        <Tab value="users">Users</Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="modules">
          <div class="columns">
            <section>
              <h2>Module Management</h2>
              <ModuleTable />
            </section>
            <section>
              <h2>Logs</h2>
              <LogsPanel />
            </section>
          </div>
        </TabPanel>
        <TabPanel value="users">
          <UserManagement />
        </TabPanel>
      </TabPanels>
    </Tabs>
    <ConfirmDialog />
    <Toast />
  </div>
</template>

<style scoped>
.management-view {
  padding: 1.5rem;
}

.columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 1.5rem;
}

/* stack the columns on narrow screens */
@media (max-width: 1200px) {
  .columns {
    grid-template-columns: minmax(0, 1fr);
  }
}

h2 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}
</style>
