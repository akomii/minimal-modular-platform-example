<script setup lang="ts">
import { computed, ref } from "vue"
import Tabs from "primevue/tabs"
import TabList from "primevue/tablist"
import Tab from "primevue/tab"
import TabPanels from "primevue/tabpanels"
import TabPanel from "primevue/tabpanel"
import RequestLogView from "./RequestLogView.vue"
import LogConsole from "./LogConsole.vue"
import { useModules } from "../composables/useModules"

const { modules } = useModules()
const active = ref("requests")

// installed = container exists (status != NOT_CREATED); docker logs work for stopped ones too.
const installed = computed(() =>
  modules.value.filter((module) => module.status !== "NOT_CREATED")
)
</script>

<template>
  <div class="logs-panel">
    <Tabs :value="active" @update:value="active = String($event)">
      <TabList>
        <Tab value="requests">Requests</Tab>
        <Tab value="server">Server</Tab>
        <Tab v-for="module in installed" :key="module.id" :value="module.id">
          {{ module.id }}
          <span
            class="status-dot"
            :class="{ online: module.status === 'RUNNING' }"
            :title="module.status === 'RUNNING' ? 'online' : 'offline'"
          />
        </Tab>
      </TabList>
      <TabPanels>
        <TabPanel value="requests">
          <RequestLogView :active="active === 'requests'" />
        </TabPanel>
        <TabPanel value="server">
          <LogConsole
            url="/api/server/logs/stream"
            :active="active === 'server'"
          />
        </TabPanel>
        <TabPanel
          v-for="module in installed"
          :key="module.id"
          :value="module.id"
        >
          <LogConsole
            :url="`/api/modules/${module.id}/logs/stream`"
            :active="active === module.id"
          />
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.logs-panel {
  margin-top: 0;
}

/* breathing room between the log tabs and their content */
.logs-panel :deep(.p-tabpanel) {
  padding: 1rem 0 0;
}

/* run-state dot in each container tab: green = running, grey = stopped */
.status-dot {
  display: inline-block;
  width: 0.6rem;
  height: 0.6rem;
  margin-left: 0.4rem;
  border-radius: 50%;
  background: #9ca3af;
  vertical-align: middle;
  position: relative;
  top: -1px;
}

.status-dot.online {
  background: #22c55e;
}
</style>
