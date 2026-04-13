<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="페이지 탐색">
    <button
      class="pagination__btn"
      :disabled="currentPage === 0"
      aria-label="이전 페이지"
      @click="$emit('change', currentPage - 1)"
    >‹</button>

    <button
      v-for="p in pageRange"
      :key="p"
      :class="['pagination__btn', { 'pagination__btn--active': p === currentPage }]"
      :aria-current="p === currentPage ? 'page' : undefined"
      @click="$emit('change', p)"
    >{{ p + 1 }}</button>

    <button
      class="pagination__btn"
      :disabled="currentPage === totalPages - 1"
      aria-label="다음 페이지"
      @click="$emit('change', currentPage + 1)"
    >›</button>

    <span class="pagination__info">
      {{ currentPage + 1 }} / {{ totalPages }} 페이지 (총 {{ totalElements }}건)
    </span>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  currentPage: number
  totalPages: number
  totalElements: number
}>()

defineEmits<{ change: [page: number] }>()

const pageRange = computed(() => {
  const range: number[] = []
  const start = Math.max(0, props.currentPage - 2)
  const end   = Math.min(props.totalPages - 1, props.currentPage + 2)
  for (let i = start; i <= end; i++) range.push(i)
  return range
})
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.pagination__btn {
  min-width: 2rem;
  height: 2rem;
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.pagination__btn:hover:not(:disabled):not(.pagination__btn--active) {
  background: var(--color-bg);
  border-color: var(--color-primary);
}

.pagination__btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.pagination__btn:disabled { opacity: 0.4; cursor: not-allowed; }

.pagination__info {
  margin-left: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
