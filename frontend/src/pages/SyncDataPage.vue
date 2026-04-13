<template>
  <AppLayout>
    <div class="sync-data-page">
      <div class="sync-data-page__header">
        <h1 class="sync-data-page__title">동기화 데이터</h1>
      </div>

      <!-- 시스템 선택 -->
      <div class="sync-data-page__filter">
        <BaseSelect
          v-model="selectedSystemId"
          label="시스템 선택"
          :options="systemOptions"
          placeholder="시스템을 선택하세요"
          @update:model-value="onSystemChange"
        />
      </div>

      <BaseAlert v-if="store.syncError" :message="store.syncError" type="error" />

      <template v-if="selectedSystemId">
        <BaseTable
          :columns="columns"
          :rows="rows"
          :loading="store.syncLoading"
        >
          <template #status="{ value }">
            <StatusBadge :status="String(value)" />
          </template>
          <template #errorMessage="{ value }">
            <span v-if="value" class="error-text" :title="String(value)">
              {{ String(value).slice(0, 40) }}{{ String(value).length > 40 ? '…' : '' }}
            </span>
            <span v-else class="empty-text">—</span>
          </template>
        </BaseTable>

        <BasePagination
          :current-page="store.syncPage"
          :total-pages="store.syncTotalPages"
          :total-elements="store.syncTotalElements"
          @change="loadPage"
        />
      </template>

      <div v-else class="sync-data-page__empty">
        좌측에서 시스템을 선택하면 동기화 데이터를 확인할 수 있습니다.
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import BasePagination from '@/components/ui/BasePagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useSyncDataStore } from '@/stores/syncData'

const store = useSyncDataStore()
const selectedSystemId = ref('')

const systemOptions = computed(() =>
  store.activeSystems.map((s) => ({ value: String(s.id), label: s.systemCode })),
)

const columns = [
  { key: 'externalRefId',  label: '외부 참조 ID' },
  { key: 'status',         label: '상태',         width: '100px' },
  { key: 'errorMessage',   label: '오류 메시지' },
  { key: 'syncedAt',       label: '동기화 시각',  width: '150px' },
  { key: 'createdAt',      label: '생성 시각',    width: '150px' },
]

const rows = computed(() =>
  store.syncItems.map((item) => ({
    externalRefId: item.externalRefId,
    status:        item.status,
    errorMessage:  item.errorMessage,
    syncedAt:      item.syncedAt ? formatDt(item.syncedAt) : null,
    createdAt:     formatDt(item.createdAt),
  })),
)

function formatDt(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

function onSystemChange(val: string) {
  if (val) store.fetchSyncItems(Number(val), 0)
}

function loadPage(page: number) {
  if (selectedSystemId.value) {
    store.fetchSyncItems(Number(selectedSystemId.value), page)
  }
}

onMounted(() => store.fetchActiveSystems())
</script>

<style scoped>
.sync-data-page { display: flex; flex-direction: column; gap: var(--space-4); }

.sync-data-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sync-data-page__title { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); }

.sync-data-page__filter { max-width: 20rem; }

.sync-data-page__empty {
  text-align: center;
  padding: var(--space-12);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.error-text { color: var(--color-danger); font-size: var(--text-xs); }
.empty-text { color: var(--color-text-muted); }
</style>
