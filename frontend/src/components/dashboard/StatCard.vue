<template>
  <div class="stat-card">
    <div class="stat-card__header">
      <span class="stat-card__label">{{ label }}</span>
      <span class="stat-card__icon">{{ icon }}</span>
    </div>
    <div class="stat-card__value">
      <span v-if="loading" class="stat-card__skeleton" />
      <span v-else>{{ value.toLocaleString() }}</span>
    </div>
    <div v-if="sub" class="stat-card__sub">{{ sub }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: number
  icon?: string
  sub?: string
  loading?: boolean
}>()
</script>

<style scoped>
.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card__label {
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.stat-card__icon { font-size: 1.25rem; }

.stat-card__value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.stat-card__skeleton {
  display: block;
  width: 4rem;
  height: 2rem;
  background: var(--color-border);
  border-radius: var(--radius-sm);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}

.stat-card__sub {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
