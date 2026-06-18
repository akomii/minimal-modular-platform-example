<script setup lang="ts">
import { computed, watch } from "vue"
import Tag from "primevue/tag"
import { useSse } from "../composables/useSse"
import { statusSeverity } from "../composables/useApi"

// Streams /api/requests/stream (JSON per event) and renders a newest-first list with the
// color-coded status tag. Streams only while `active`.
const props = defineProps<{ active: boolean }>()

const { lines, open, close } = useSse()

// number of most recent requests shown
const LIMIT = 100

watch(
  () => props.active,
  (isActive) => {
    if (isActive) {
      open("/api/requests/stream")
    } else {
      close()
    }
  },
  { immediate: true }
)

interface RequestEntry {
  time: string
  method: string
  path: string
  status: number
}

const entries = computed<RequestEntry[]>(() => {
  const parsed: RequestEntry[] = []
  for (const line of lines.value) {
    try {
      parsed.push(JSON.parse(line) as RequestEntry)
    } catch {
      // skip non-JSON lines
    }
  }
  return parsed.slice(-LIMIT).reverse()
})

function formatTime(iso: string): string {
  const date = new Date(iso)
  return isNaN(date.getTime()) ? iso : date.toLocaleTimeString()
}
</script>

<template>
  <div>
    <div class="req-list">
      <div v-for="(entry, index) in entries" :key="index" class="req-row">
        <Tag
          :value="String(entry.status)"
          :severity="statusSeverity(entry.status)"
        />
        <span class="req-method">{{ entry.method }}</span>
        <span class="req-path">{{ entry.path }}</span>
        <span class="req-time">{{ formatTime(entry.time) }}</span>
      </div>
      <p v-if="!entries.length" class="empty">No requests yet.</p>
    </div>
  </div>
</template>

<style scoped>
.req-list {
  /* fill down to near the bottom of the viewport (offsets app header, outer + nested tab strips, and column padding) */
  height: calc(100vh - 15rem);
  overflow: auto;
  font-family: monospace;
  font-size: 0.85rem;
}

.req-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.2rem 0.25rem;
  border-bottom: 1px solid #eee;
}

.req-method {
  font-weight: 600;
  min-width: 4rem;
}

.req-path {
  flex: 1;
  word-break: break-all;
}

.req-time {
  color: #888;
}

.empty {
  color: #888;
}
</style>
