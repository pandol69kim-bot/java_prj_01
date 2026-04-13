<template>
  <div class="field">
    <label v-if="label" :for="selectId" class="field__label">
      {{ label }}
      <span v-if="required" class="field__required">*</span>
    </label>
    <select
      :id="selectId"
      v-bind="$attrs"
      :value="modelValue"
      :disabled="disabled"
      :required="required"
      :class="['field__select', { 'field__select--error': error }]"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <option
        v-for="opt in options"
        :key="opt.value"
        :value="opt.value"
      >{{ opt.label }}</option>
    </select>
    <p v-if="error" class="field__error" role="alert">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Option { value: string; label: string }

interface Props {
  modelValue?: string
  label?: string
  options: Option[]
  placeholder?: string
  error?: string
  disabled?: boolean
  required?: boolean
  id?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  required: false,
})

defineEmits<{ 'update:modelValue': [value: string] }>()

const selectId = computed(() => props.id ?? `select-${Math.random().toString(36).slice(2, 8)}`)
</script>

<style scoped>
.field { display: flex; flex-direction: column; gap: var(--space-1); }
.field__label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text); }
.field__required { color: var(--color-danger); margin-left: 2px; }

.field__select {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--text-sm);
  cursor: pointer;
  outline: none;
  transition: border-color var(--transition-fast);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='%2364748b' viewBox='0 0 16 16'%3E%3Cpath d='M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right var(--space-3) center;
  padding-right: var(--space-8);
}

.field__select:focus { border-color: var(--color-border-focus); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
.field__select:disabled { background-color: var(--color-bg); cursor: not-allowed; color: var(--color-text-disabled); }
.field__select--error { border-color: var(--color-danger); }
.field__error { font-size: var(--text-xs); color: var(--color-danger); }
</style>
