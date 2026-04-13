<template>
  <aside :class="['app-sidebar', { 'app-sidebar--collapsed': collapsed }]" aria-label="사이드 내비게이션">
    <nav>
      <ul class="app-sidebar__menu" role="list">
        <li v-for="item in menuItems" :key="item.path">
          <RouterLink
            :to="item.path"
            :class="['app-sidebar__link', { 'app-sidebar__link--active': isActive(item.path) }]"
            :title="collapsed ? item.label : undefined"
          >
            <span class="app-sidebar__icon" aria-hidden="true">{{ item.icon }}</span>
            <span v-if="!collapsed" class="app-sidebar__label">{{ item.label }}</span>
          </RouterLink>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

defineProps<{ collapsed?: boolean }>()

const route = useRoute()

const menuItems = [
  { path: '/dashboard', label: '대시보드', icon: '◈' },
  { path: '/systems',   label: '외부 시스템', icon: '⚙' },
  { path: '/sync-data', label: '동기화 데이터', icon: '↺' },
]

function isActive(path: string) {
  return route.path.startsWith(path)
}
</script>

<style scoped>
.app-sidebar {
  width: 14rem;
  min-height: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  transition: width var(--transition-fast);
  overflow: hidden;
  flex-shrink: 0;
}

.app-sidebar--collapsed { width: 3.5rem; }

.app-sidebar__menu {
  list-style: none;
  margin: 0;
  padding: var(--space-2) 0;
}

.app-sidebar__link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-4);
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: 500;
  border-radius: 0;
  transition: background var(--transition-fast), color var(--transition-fast);
  white-space: nowrap;
}

.app-sidebar__link:hover {
  background: var(--color-bg);
  color: var(--color-text);
}

.app-sidebar__link--active {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  color: var(--color-primary);
}

.app-sidebar__icon { font-size: 1rem; flex-shrink: 0; }
</style>
