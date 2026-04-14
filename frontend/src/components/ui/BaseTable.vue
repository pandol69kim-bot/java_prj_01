<template>
  <div class="table-wrap">
    <div v-if="loading" class="table-wrap__loading" aria-live="polite">
      <span class="spinner" />
      <span>데이터를 불러오는 중...</span>
    </div>

    <table v-else class="table" role="table">
      <thead class="table__head">
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            class="table__th"
            :class="{ 'table__th--sortable': col.sortable }"
            :style="col.width ? { width: col.width } : {}"
            :aria-sort="col.sortable ? (sortKey === col.key ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none') : undefined"
            @click="col.sortable ? onSort(col.key) : undefined"
          >
            {{ col.label }}
            <span v-if="col.sortable" class="sort-icon" aria-hidden="true">
              {{ sortKey === col.key ? (sortDir === 'asc' ? '↑' : '↓') : '↕' }}
            </span>
          </th>
        </tr>
      </thead>
      <tbody class="table__body">
        <tr v-if="!rows.length" class="table__empty">
          <td :colspan="columns.length">데이터가 없습니다.</td>
        </tr>
        <tr v-for="(row, idx) in rows" :key="idx" class="table__row">
          <td
            v-for="col in columns"
            :key="col.key"
            class="table__td"
          >
            <slot :name="col.key" :row="row" :value="row[col.key]">
              {{ row[col.key] ?? '—' }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
export interface Column {
  key: string
  label: string
  width?: string
  sortable?: boolean
}

const props = defineProps<{
  columns: Column[]
  rows: Record<string, unknown>[]
  loading?: boolean
  sortKey?: string
  sortDir?: 'asc' | 'desc'
}>()

const emit = defineEmits<{
  sort: [key: string, dir: 'asc' | 'desc']
}>()

function onSort(key: string) {
  const nextDir: 'asc' | 'desc' =
    props.sortKey === key && props.sortDir === 'desc' ? 'asc' : 'desc'
  emit('sort', key, nextDir)
}
</script>

<style scoped>
.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); border-radius: var(--radius-lg); }

.table-wrap__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-12);
  color: var(--color-text-muted);
}

.table { width: 100%; border-collapse: collapse; font-size: var(--text-sm); }

.table__th {
  padding: var(--space-3) var(--space-4);
  text-align: left;
  font-weight: 600;
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
  user-select: none;
}

.table__th--sortable {
  cursor: pointer;
}

.table__th--sortable:hover {
  color: var(--color-primary);
}

.sort-icon {
  margin-left: var(--space-1);
  font-size: var(--text-xs);
  opacity: 0.6;
}

.table__row { transition: background var(--transition-fast); }
.table__row:hover { background: var(--color-bg); }
.table__row:not(:last-child) td { border-bottom: 1px solid var(--color-border); }

.table__td { padding: var(--space-3) var(--space-4); color: var(--color-text); vertical-align: middle; }

.table__empty td {
  text-align: center;
  padding: var(--space-12);
  color: var(--color-text-muted);
}

.spinner {
  width: 1.25rem; height: 1.25rem;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
