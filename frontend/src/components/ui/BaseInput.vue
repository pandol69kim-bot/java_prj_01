<template>
  <div class="field">
    <label v-if="label" :for="inputId" class="field__label">
      {{ label }}
      <span v-if="required" class="field__required" aria-label="필수">*</span>
    </label>
    <div :class="['field__control', { 'field__control--error': error }]">
      <input
        :id="inputId"
        v-bind="$attrs"
        :value="modelValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :required="required"
        class="field__input"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
    </div>
    <p v-if="error" class="field__error" role="alert">{{ error }}</p>
    <p v-else-if="hint" class="field__hint">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  modelValue?: string
  label?: string
  type?: string
  placeholder?: string
  error?: string
  hint?: string
  disabled?: boolean
  required?: boolean
  id?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  type: 'text',
  disabled: false,
  required: false,
})

defineEmits<{ 'update:modelValue': [value: string] }>()

const inputId = computed(() => props.id ?? `input-${Math.random().toString(36).slice(2, 8)}`)
</script>

<style scoped>
.field { display: flex; flex-direction: column; gap: var(--space-1); }

.field__label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text);
}

.field__required { color: var(--color-danger); margin-left: 2px; }

.field__control { position: relative; }

.field__input {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--text-sm);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
  outline: none;
}

.field__input:focus {
  border-color: var(--color-border-focus);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.field__input:disabled {
  background: var(--color-bg);
  color: var(--color-text-disabled);
  cursor: not-allowed;
}

.field__control--error .field__input {
  border-color: var(--color-danger);
}

.field__control--error .field__input:focus {
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.field__error { font-size: var(--text-xs); color: var(--color-danger); }
.field__hint  { font-size: var(--text-xs); color: var(--color-text-muted); }
</style>
