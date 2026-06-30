<script setup lang="ts">
import { computed, onMounted, reactive, watch } from "vue"
import Card from "primevue/card"
import Fieldset from "primevue/fieldset"
import Button from "primevue/button"
import SettingInput from "./SettingInput.vue"
import {
  useConfiguration,
  type ConfigTypeValue
} from "../composables/useConfiguration"
import { useModules } from "../composables/useModules"

const {
  coreSettings,
  moduleConfigs,
  loadCore,
  saveCore,
  resetCore,
  loadModuleConfig,
  saveModuleConfig,
  resetModuleConfig,
  applyModuleConfig
} = useConfiguration()
const { modules, list } = useModules()

// only modules that declare a config block appear in the Modules section
// only installed modules that declare a config block appear (a NOT_CREATED module has no container
// to apply config to, so its settings would be meaningless)
const configurableModules = computed(() =>
  modules.value.filter((m) => m.configurable && m.status !== "NOT_CREATED")
)

// PrimeVue inputs need real types (number/boolean), but values travel as strings; these edit maps
// hold the typed working copies and are serialized back on save. They are loosely typed (any)
// because the form is schema-driven. Core is keyed by setting key; modules use "<moduleId>::<key>".
const coreEdits = reactive<Record<string, any>>({})
const moduleEdits = reactive<Record<string, any>>({})

function moduleKey(moduleId: string, key: string): string {
  return `${moduleId}::${key}`
}

function toModel(type: ConfigTypeValue, value: string | null): string | number | boolean {
  if (type === "number") {
    return value ? Number(value) : 0
  }
  if (type === "boolean") {
    return value === "true"
  }
  return value ?? ""
}

function toStr(type: ConfigTypeValue, model: unknown): string {
  if (type === "boolean") {
    return model ? "true" : "false"
  }
  return model === null || model === undefined ? "" : String(model)
}

// rebuild the typed edit copies whenever the loaded settings/fields change
watch(coreSettings, (settings) => {
  for (const s of settings) {
    coreEdits[s.key] = toModel(s.type, s.value)
  }
})
watch(moduleConfigs, (configs) => {
  for (const [id, fields] of Object.entries(configs)) {
    for (const f of fields) {
      moduleEdits[moduleKey(id, f.key)] = toModel(f.type, f.value)
    }
  }
})

function saveCoreSettings(): void {
  const values: Record<string, string> = {}
  for (const s of coreSettings.value) {
    values[s.key] = toStr(s.type, coreEdits[s.key])
  }
  void saveCore(values)
}

function saveModule(id: string): void {
  const values: Record<string, string> = {}
  for (const f of moduleConfigs.value[id] ?? []) {
    values[f.key] = toStr(f.type, moduleEdits[moduleKey(id, f.key)])
  }
  void saveModuleConfig(id, values)
}

function resetCoreSettings(): void {
  void resetCore()
}

function resetModule(id: string): void {
  void resetModuleConfig(id)
}

function applyModule(id: string): void {
  void applyModuleConfig(id)
}

// load each configurable module's fields once it appears (also covers a module installed later)
watch(
  configurableModules,
  (mods) => {
    for (const m of mods) {
      if (!(m.id in moduleConfigs.value)) {
        void loadModuleConfig(m.id)
      }
    }
  },
  { immediate: true }
)

onMounted(() => {
  void loadCore()
  void list()
})
</script>

<template>
  <div class="config">
    <!-- Core -->
    <section class="block">
      <header class="block-head">
        <h2>Core</h2>
        <span class="block-sub">Platform settings — applied immediately</span>
      </header>
      <Card>
        <template #content>
          <div class="card-head">
            <Button
              label="Restore defaults"
              icon="pi pi-replay"
              text
              severity="secondary"
              size="small"
              @click="resetCoreSettings"
            />
          </div>
          <div v-for="s in coreSettings" :key="s.key" class="field">
            <label :for="s.key">{{ s.label }}</label>
            <div class="control">
              <SettingInput
                :type="s.type"
                :input-id="s.key"
                v-model="coreEdits[s.key]"
              />
            </div>
          </div>
          <div class="actions">
            <Button label="Save" @click="saveCoreSettings" />
          </div>
        </template>
      </Card>
    </section>

    <!-- Modules -->
    <section class="block">
      <header class="block-head">
        <h2>Modules</h2>
        <span class="block-sub"
          >Per-module settings — Save persists, Apply restarts the module</span
        >
      </header>
      <p v-if="!configurableModules.length" class="empty">
        No installed module declares configurable settings.
      </p>
      <Fieldset
        v-for="m in configurableModules"
        :key="m.id"
        :legend="m.id"
        class="module"
      >
        <div class="card-head">
          <Button
            label="Restore defaults"
            icon="pi pi-replay"
            text
            severity="secondary"
            size="small"
            @click="resetModule(m.id)"
          />
        </div>
        <div
          v-for="f in moduleConfigs[m.id] ?? []"
          :key="f.key"
          class="field"
        >
          <label :for="`${m.id}-${f.key}`"
            >{{ f.label }}<span v-if="f.required" class="req"> *</span></label
          >
          <div class="control">
            <SettingInput
              :type="f.type"
              :input-id="`${m.id}-${f.key}`"
              v-model="moduleEdits[m.id + '::' + f.key]"
            />
          </div>
        </div>
        <div class="actions">
          <Button label="Save" @click="saveModule(m.id)" />
          <Button label="Apply" severity="secondary" @click="applyModule(m.id)" />
        </div>
      </Fieldset>
    </section>
  </div>
</template>

<style scoped>
.config {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
  padding: 1.5rem;
  font-family: system-ui, sans-serif;
}

/* each top-level block (Core, Modules) is clearly set apart by a strong, accented header */
.block-head {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #6366f1;
}

.block-head h2 {
  margin: 0;
  font-size: 1.35rem;
}

.block-sub {
  color: #888;
  font-size: 0.9rem;
}

.module {
  margin-bottom: 1.25rem;
}

/* right-aligned header row holding the Restore defaults button, in the card's upper-right corner */
.card-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.5rem;
}

.field {
  display: grid;
  grid-template-columns: 16rem 1fr;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.control {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.req {
  color: #e11d48;
}

.empty {
  color: #888;
}

.actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
