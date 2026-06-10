<script setup lang="ts">
import {onMounted, ref} from "vue"
import {fetchUser, login, logout, type ModuleAccess, probeModules, type UserInfo} from "./auth"

const loading = ref(true)
const user = ref<UserInfo | null>(null)
const moduleAccess = ref<ModuleAccess | null>(null)

onMounted(async () => {
  user.value = await fetchUser()
  if (user.value) {
    moduleAccess.value = await probeModules()
  }
  loading.value = false
})
</script>

<template>
  <main>
    <h1>Modular Platform</h1>

    <p v-if="loading">Loading…</p>

    <section v-else-if="!user">
      <p>You must sign in to use the platform.</p>
      <button @click="login">Log in</button>
    </section>

    <section v-else>
      <p>Signed in as <strong>{{ user.username }}</strong></p>
      <p>Roles: <code>{{ user.roles.length ? user.roles.join(", ") : "none" }}</code></p>

      <p v-if="moduleAccess?.allowed" class="ok">
        ✓ Module API access granted ({{ moduleAccess.count }} module(s))
      </p>
      <p v-else-if="moduleAccess" class="denied">
        ✗ Module API forbidden ({{ moduleAccess.status }}) — you lack the
        <code>platform-admin</code> role.
      </p>

      <button @click="logout">Log out</button>
    </section>
  </main>
</template>

<style scoped>
main {
  font-family: system-ui, sans-serif;
  max-width: 40rem;
  margin: 3rem auto;
  padding: 0 1rem;
}

.ok {
  color: #1a7f37;
}

.denied {
  color: #b3261e;
}

button {
  padding: 0.5rem 1rem;
  cursor: pointer;
}
</style>
