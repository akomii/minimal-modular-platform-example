<script setup lang="ts">
import { nextTick, ref, watch } from "vue"
import { useSse } from "../composables/useSse"

// Text log console for the Server tab and per-container tabs. Streams only while `active` (the
// selected tab), so just one SSE connection is open at a time.
const props = defineProps<{ url: string; active: boolean }>()

const { lines, open, close } = useSse()
const output = ref<HTMLElement | null>(null)

watch(
  () => props.active,
  (isActive) => {
    if (isActive) {
      open(props.url)
    } else {
      close()
    }
  },
  { immediate: true }
)

// autoscroll when a new line arrives
watch(
  () => lines.value.length,
  async () => {
    await nextTick()
    if (output.value) {
      output.value.scrollTop = output.value.scrollHeight
    }
  }
)
</script>

<template>
  <div>
    <pre ref="output" class="log-output">{{ lines.join("\n") }}</pre>
  </div>
</template>

<style scoped>
.log-output {
  /* fill down to near the bottom of the viewport (offsets app header, outer + nested tab strips, and column padding) */
  height: calc(100vh - 15rem);
  margin: 0;
  padding: 0.75rem;
  border-radius: 6px;
  background: #1e1e1e;
  color: #d4d4d4;
  overflow: auto;
  white-space: pre-wrap;
  font-family: monospace;
}
</style>
