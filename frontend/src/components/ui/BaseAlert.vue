<template>
  <div v-if="visible" :class="['alert', `alert--${type}`]" role="alert">
    <span class="alert__icon">{{ iconMap[type] }}</span>
    <span class="alert__message">{{ message }}</span>
    <button v-if="dismissible" class="alert__close" aria-label="닫기" @click="visible = false">✕</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

type AlertType = 'error' | 'warning' | 'info' | 'success'

withDefaults(
  defineProps<{
    message: string
    type?: AlertType
    dismissible?: boolean
  }>(),
  {
    type: 'info',
    dismissible: true,
  },
)

const visible = ref(true)

const iconMap: Record<AlertType, string> = {
  error:   '✖',
  warning: '⚠',
  info:    'ℹ',
  success: '✔',
}
</script>

<style scoped>
.alert {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  border: 1px solid transparent;
}

.alert--error   { background: #fee2e2; color: #991b1b; border-color: #fca5a5; }
.alert--warning { background: #ffedd5; color: #9a3412; border-color: #fdba74; }
.alert--info    { background: #dbeafe; color: #1e40af; border-color: #93c5fd; }
.alert--success { background: #dcfce7; color: #166534; border-color: #86efac; }

.alert__icon    { flex-shrink: 0; font-size: var(--text-base); }
.alert__message { flex: 1; }
.alert__close {
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: inherit;
  opacity: 0.6;
  font-size: var(--text-xs);
  padding: 0;
  line-height: 1;
}
.alert__close:hover { opacity: 1; }
</style>
