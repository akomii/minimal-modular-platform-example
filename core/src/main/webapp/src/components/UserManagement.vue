<script setup lang="ts">
import { onMounted } from "vue"
import DataTable from "primevue/datatable"
import Column from "primevue/column"
import Checkbox from "primevue/checkbox"
import {
  type AssignableRole,
  type PlatformUser,
  useUsers
} from "../composables/useUsers"

const props = defineProps<{ currentUsername: string | null }>()
const { users, roles, list, setRole } = useUsers()

onMounted(() => {
  void list()
})

function roleHeader(role: AssignableRole): string {
  return role.module ? `${role.module} / ${role.label}` : role.label
}

// You can't strip your own platform-admin (the backend rejects it too).
function isLocked(user: PlatformUser, roleId: string): boolean {
  return roleId === "platform-admin" && user.username === props.currentUsername
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
    <p class="hint" role="alert">
      <i class="pi pi-info-circle"></i>
      <span>Role changes take effect at the user's next sign-in.</span>
    </p>
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
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0 0 1.25rem;
  padding: 0.85rem 1rem;
  background: #fff8e1;
  border: 1px solid #f6c343;
  border-left: 4px solid #f59e0b;
  border-radius: 6px;
  color: #7a4d00;
  font-weight: 600;
  font-size: 0.95rem;
}

.hint .pi {
  font-size: 1.2rem;
}

.email {
  color: #777;
  font-size: 0.85rem;
}
</style>
