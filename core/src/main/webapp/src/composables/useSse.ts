import { onUnmounted, ref } from "vue"

// Generic EventSource (SSE) wrapper: open(url) collects each event's data as a raw string into
// `lines`. Consumers decide how to render (plain log text, or JSON-per-line for the request log).
export function useSse() {
  const lines = ref<string[]>([])
  const connected = ref(false)
  let source: EventSource | null = null

  function open(url: string): void {
    close()
    lines.value = []
    const es = new EventSource(url)
    es.onopen = () => {
      connected.value = true
    }
    es.onmessage = (event) => {
      lines.value.push(String(event.data))
    }
    es.onerror = () => {
      // SSE completion surfaces here too; stop instead of letting it auto-retry.
      connected.value = false
      es.close()
    }
    source = es
  }

  function close(): void {
    if (source) {
      source.close()
      source = null
    }
    connected.value = false
  }

  onUnmounted(close)

  return { lines, connected, open, close }
}
