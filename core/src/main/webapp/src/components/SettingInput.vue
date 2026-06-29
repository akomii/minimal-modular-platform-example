<script setup lang="ts">
import InputText from "primevue/inputtext"
import InputNumber from "primevue/inputnumber"
import ToggleSwitch from "primevue/toggleswitch"
import Password from "primevue/password"
import type { ConfigTypeValue } from "../composables/useConfiguration"

// The edit widget for one setting value, chosen by type, so core settings and module fields render
// identically. The model is loosely typed (any): it is a number, boolean or string per the type.
defineProps<{ type: ConfigTypeValue; inputId: string }>()
const model = defineModel<any>()
</script>

<template>
  <InputNumber v-if="type === 'number'" :id="inputId" v-model="model" />
  <ToggleSwitch v-else-if="type === 'boolean'" :id="inputId" v-model="model" />
  <Password
    v-else-if="type === 'secret'"
    :id="inputId"
    v-model="model"
    :feedback="false"
    toggle-mask
  />
  <InputText v-else :id="inputId" v-model="model" />
</template>
