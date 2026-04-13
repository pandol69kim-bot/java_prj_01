<template>
  <span :class="['badge', `badge--${statusClass}`]">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type SyncStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'
  | 'RUNNING' | 'PARTIAL_FAILED' | 'ACTIVE' | 'INACTIVE'

const STATUS_MAP: Record<SyncStatus, { label: string; cls: string }> = {
  PENDING:        { label: '대기',     cls: 'pending' },
  PROCESSING:     { label: '처리중',   cls: 'processing' },
  COMPLETED:      { label: '완료',     cls: 'completed' },
  FAILED:         { label: '실패',     cls: 'failed' },
  SKIPPED:        { label: '건너뜀',   cls: 'skipped' },
  RUNNING:        { label: '실행중',   cls: 'processing' },
  PARTIAL_FAILED: { label: '부분실패', cls: 'warning' },
  ACTIVE:         { label: '활성',     cls: 'completed' },
  INACTIVE:       { label: '비활성',   cls: 'skipped' },
}

const props = defineProps<{ status: string }>()

const resolved = computed(() => {
  const key = props.status as SyncStatus
  return STATUS_MAP[key] ?? { label: props.status, cls: 'skipped' }
})

const label      = computed(() => resolved.value.label)
const statusClass = computed(() => resolved.value.cls)
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: 0.02em;
}
.badge--pending    { background: #fef3c7; color: #92400e; }
.badge--processing { background: #dbeafe; color: #1e40af; }
.badge--completed  { background: #dcfce7; color: #166534; }
.badge--failed     { background: #fee2e2; color: #991b1b; }
.badge--skipped    { background: #f1f5f9; color: #475569; }
.badge--warning    { background: #ffedd5; color: #9a3412; }
</style>
