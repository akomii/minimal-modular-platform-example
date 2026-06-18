<script setup lang="ts">
import { onMounted, ref } from "vue"
import {
  fetchUser,
  login,
  type ModuleAccess,
  probeModules,
  type UserInfo
} from "./auth"
import UserBar from "./components/UserBar.vue"

const loading = ref(true)
const unreachable = ref(false)
const user = ref<UserInfo | null>(null)
const moduleAccess = ref<ModuleAccess | null>(null)

onMounted(async () => {
  try {
    user.value = await fetchUser()
    if (user.value) {
      moduleAccess.value = await probeModules()
    }
  } catch {
    unreachable.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- Signed-in admin: the management UI owns the header (tabs + user bar) and body -->
  <router-view v-if="user && moduleAccess?.allowed" :user="user" />

  <!-- Every other state keeps the plain title bar plus a notice -->
  <template v-else>
    <header class="topbar">
      <h1>Modular Platform</h1>
      <UserBar v-if="user" :user="user" />
    </header>

    <p v-if="loading" class="notice">Loading…</p>

    <p v-else-if="unreachable" class="notice denied">
      ✗ Platform unreachable — is the backend running?
    </p>

    <section v-else-if="!user" class="notice">
      <p>You must sign in to use the platform.</p>
      <button @click="login">Log in</button>
    </section>

    <p v-else class="notice denied">
      ✗ Module API forbidden ({{ moduleAccess?.status }}) — you lack the
      <code>platform-admin</code> role.
    </p>
  </template>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid #ddd;
  font-family: system-ui, sans-serif;
}

.topbar h1 {
  margin: 0;
  font-size: 1.25rem;
}

.notice {
  max-width: 40rem;
  margin: 3rem auto;
  padding: 0 1rem;
  font-family: system-ui, sans-serif;
  text-align: center;
}

.denied {
  color: #b3261e;
}

button {
  padding: 0.5rem 1rem;
  cursor: pointer;
}
</style>
