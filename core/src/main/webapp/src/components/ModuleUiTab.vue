<script setup lang="ts">
// Embeds a module's web page. The page authenticates as the logged-in user via the shared Keycloak SSO session.
defineProps<{ url: string }>()
</script>

<template>
  <iframe :src="url" class="module-ui-frame" />
</template>

<style scoped>
.module-ui-frame {
  display: block;
  width: 100%;
  /* fill the viewport below the header + tab strip */
  height: calc(100vh - 7rem);
  border: none;
}
</style>
