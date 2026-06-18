<script setup lang="ts">
import { onMounted, ref } from "vue"
import DataTable from "primevue/datatable"
import Column from "primevue/column"
import Checkbox from "primevue/checkbox"
import { fetchUser } from "../auth"
import {
  type AssignableRole,
  type PlatformUser,
  useUsers
} from "../composables/useUsers"

const { users, roles, list, setRole } = useUsers()
const currentUsername = ref<string | null>(null)

onMounted(async () => {
  currentUsername.value = (await fetchUser())?.username ?? null
  await list()
})

function roleHeader(role: AssignableRole): string {
  return role.module ? `${role.module} / ${role.label}` : role.label
}

// You can't strip your own platform-admin (the backend rejects it too).
function isLocked(user: PlatformUser, roleId: string): boolean {
  return roleId === "platform-admin" && user.username === currentUsername.value
}

function toggle(user: PlatformUser, roleId: string, assigned: boolean): void {
  // optimistic: reflect the click immediately; the refresh inside setRole reconciles with the server
  user.roles = assigned
    ? [...user.roles, roleId]
    : user.roles.filter((r) => r !== roleId)
  void setRole(user.id, roleId, assigned)
}
</script>

<template>
  <div class="user-management">
    <p class="hint">Role changes take effect at the user's next sign-in.</p>
    <DataTable :value="users" dataKey="id">
      <Column header="User">
        <template #body="{ data }">
          <strong>{{ data.username }}</strong>
          <div v-if="data.email" class="email">{{ data.email }}</div>
        </template>
      </Column>
      <Column v-for="role in roles" :key="role.id" :header="roleHeader(role)">
        <template #body="{ data }">
          <Checkbox
            :modelValue="data.roles.includes(role.id)"
            :binary="true"
            :disabled="isLocked(data, role.id)"
            @update:modelValue="(val) => toggle(data, role.id, !!val)"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.user-management {
  padding: 1.5rem;
}

.hint {
  margin: 0 0 1rem;
  color: #555;
  font-size: 0.9rem;
}

.email {
  color: #777;
  font-size: 0.85rem;
}
</style>
