<template>
  <div class="app-layout">
    <AppHeader @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed" />
    <div class="app-layout__body">
      <AppSidebar :collapsed="sidebarCollapsed" />
      <main class="app-layout__main">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

const sidebarCollapsed = ref(false)
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-layout__body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.app-layout__main {
  flex: 1;
  padding: var(--space-6);
  overflow-y: auto;
  background: var(--color-bg);
}
</style>
