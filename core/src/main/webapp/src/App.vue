<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { fetchUser, login, type UserInfo } from "./auth"
import UserBar from "./components/UserBar.vue"
import { useModuleUis } from "./composables/useModuleUis"

const loading = ref(true)
const unreachable = ref(false)
const user = ref<UserInfo | null>(null)
const { moduleUis, refresh } = useModuleUis()

// /api/user reports realm roles upper-snake-cased (platform-admin -> PLATFORM_ADMIN)
const isAdmin = computed(() => user.value?.roles.includes("PLATFORM_ADMIN") ?? false)

onMounted(async () => {
  try {
    user.value = await fetchUser()
    if (user.value) {
      await refresh()
    }
  } catch {
    unreachable.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- Admins get the management tabs; anyone with a module UI tab gets in too. The view owns the header (tabs + user bar). -->
  <router-view
    v-if="user && (isAdmin || moduleUis.length > 0)"
    :user="user"
    :ui-tabs="moduleUis"
    :is-admin="isAdmin"
  />

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
      ✗ No access — you don't have a role for any module. Ask a platform admin
      to grant you one.
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
