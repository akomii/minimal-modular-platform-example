<script setup lang="ts">
import { computed, onMounted } from "vue"
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
import DatabaseView from "./DatabaseView.vue"
import ModuleUiTab from "./ModuleUiTab.vue"
import UserBar from "./UserBar.vue"
import type { ModuleUi, UserInfo } from "../auth"
import { useModules } from "../composables/useModules"

const props = defineProps<{
  user: UserInfo
  uiTabs: ModuleUi[]
  isAdmin: boolean
}>()
const { list } = useModules()

// each module UI page gets its own tab; this is its stable tab key
function tabValue(tab: ModuleUi): string {
  return `${tab.moduleId}:${tab.label}`
}

// admins land on Modules; a UI-only user lands on their first module tab
const activeTab = computed(() =>
  props.isAdmin ? "modules" : props.uiTabs.length ? tabValue(props.uiTabs[0]) : ""
)

onMounted(() => {
  // only admins may call the module API
  if (props.isAdmin) {
    void list()
  }
})
</script>

<template>
  <Tabs :value="activeTab" class="layout">
    <header class="topbar">
      <TabList>
        <Tab v-if="isAdmin" value="modules">Modules</Tab>
        <Tab v-if="isAdmin" value="users">Users</Tab>
        <Tab v-if="isAdmin" value="database">Database</Tab>
        <Tab v-for="tab in uiTabs" :key="tabValue(tab)" :value="tabValue(tab)">{{
          tab.label
        }}</Tab>
      </TabList>
      <UserBar :user="user" />
    </header>
    <TabPanels>
      <TabPanel v-if="isAdmin" value="modules">
        <div class="columns">
          <section>
            <ModuleTable />
          </section>
          <section class="logs">
            <LogsPanel />
          </section>
        </div>
      </TabPanel>
      <TabPanel v-if="isAdmin" value="users">
        <UserManagement :current-username="user.username" />
      </TabPanel>
      <TabPanel v-if="isAdmin" value="database">
        <DatabaseView />
      </TabPanel>
      <TabPanel
        v-for="tab in uiTabs"
        :key="tabValue(tab)"
        :value="tabValue(tab)"
      >
        <ModuleUiTab :url="tab.url" />
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

/* the top-level tabs read a touch larger than the nested log tabs */
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
