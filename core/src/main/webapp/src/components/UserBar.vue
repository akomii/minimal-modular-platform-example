<script setup lang="ts">
import { logout, type UserInfo } from "../auth"

defineProps<{ user: UserInfo }>()
</script>

<template>
  <div class="userbar">
    <span>Signed in as <strong>{{ user.username }}</strong></span>
    <code class="roles">{{
      user.roles.length ? user.roles.join(", ") : "none"
    }}</code>
    <button @click="logout">Log out</button>
  </div>
</template>

<style scoped>
.userbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-family: system-ui, sans-serif;
}

.roles {
  color: #555;
}

button {
  padding: 0.5rem 1rem;
  cursor: pointer;
}
</style>
