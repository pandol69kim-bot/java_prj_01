<template>
  <header class="app-header">
    <button class="app-header__menu-btn" aria-label="메뉴 열기/닫기" @click="$emit('toggle-sidebar')">
      <span class="app-header__hamburger" />
    </button>

    <span class="app-header__title">외부 시스템 동기화 관리</span>

    <div class="app-header__actions">
      <span class="app-header__user">{{ username }}</span>
      <button class="app-header__logout" @click="handleLogout">로그아웃</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import apiClient from '@/api/client'

defineEmits<{ 'toggle-sidebar': [] }>()

const router = useRouter()
const auth = useAuthStore()

const username = computed(() => {
  const token = auth.token
  if (!token) return ''
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub ?? ''
  } catch {
    return ''
  }
})

async function handleLogout() {
  try {
    await apiClient.post('/auth/logout', { refreshToken: auth.refreshToken })
  } finally {
    auth.clearTokens()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 3.5rem;
  padding: 0 var(--space-4);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-header__menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
}

.app-header__menu-btn:hover { background: var(--color-bg); }

.app-header__hamburger,
.app-header__hamburger::before,
.app-header__hamburger::after {
  display: block;
  width: 1.1rem;
  height: 2px;
  background: currentColor;
  border-radius: 1px;
  position: relative;
  transition: transform var(--transition-fast);
}

.app-header__hamburger::before,
.app-header__hamburger::after {
  content: '';
  position: absolute;
  left: 0;
}

.app-header__hamburger::before { top: -5px; }
.app-header__hamburger::after  { top: 5px; }

.app-header__title {
  font-weight: 600;
  font-size: var(--text-base);
  color: var(--color-text);
  flex: 1;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.app-header__user {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.app-header__logout {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.app-header__logout:hover {
  background: var(--color-danger);
  border-color: var(--color-danger);
  color: #fff;
}
</style>
