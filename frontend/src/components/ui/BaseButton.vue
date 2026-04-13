<template>
  <button
    :class="['btn', `btn--${variant}`, `btn--${size}`, { 'btn--loading': loading }]"
    :disabled="disabled || loading"
    :type="type"
    v-bind="$attrs"
  >
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  type?: 'button' | 'submit' | 'reset'
  loading?: boolean
  disabled?: boolean
}

withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  type: 'button',
  loading: false,
  disabled: false,
})
</script>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  font-weight: 500;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast),
              border-color var(--transition-fast), opacity var(--transition-fast);
  white-space: nowrap;
}

/* Size */
.btn--sm { font-size: var(--text-sm); padding: var(--space-1) var(--space-3); height: 2rem; }
.btn--md { font-size: var(--text-sm); padding: var(--space-2) var(--space-4); height: 2.5rem; }
.btn--lg { font-size: var(--text-base); padding: var(--space-3) var(--space-6); height: 3rem; }

/* Variant */
.btn--primary {
  background: var(--color-primary);
  color: #fff;
}
.btn--primary:hover:not(:disabled) { background: var(--color-primary-hover); }

.btn--secondary {
  background: var(--color-surface);
  color: var(--color-text);
  border-color: var(--color-border);
}
.btn--secondary:hover:not(:disabled) { background: var(--color-bg); }

.btn--danger {
  background: var(--color-danger);
  color: #fff;
}
.btn--danger:hover:not(:disabled) { background: var(--color-danger-hover); }

.btn--ghost {
  background: transparent;
  color: var(--color-primary);
}
.btn--ghost:hover:not(:disabled) { background: var(--color-primary-light); }

/* States */
.btn:disabled, .btn--loading { opacity: 0.6; cursor: not-allowed; }

/* Spinner */
.btn__spinner {
  width: 1em;
  height: 1em;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
