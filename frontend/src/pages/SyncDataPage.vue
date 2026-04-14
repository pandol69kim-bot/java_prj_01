<template>
  <AppLayout>
    <div class="sync-data-page">
      <div class="sync-data-page__header">
        <h1 class="sync-data-page__title">동기화 데이터</h1>
        <div v-if="selectedSystemId" class="header-actions">
          <BaseButton
            variant="secondary"
            :disabled="store.syncLoading"
            @click="store.exportExcel()"
          >
            엑셀 다운로드
          </BaseButton>
          <BaseButton variant="primary" @click="showAddModal = true">
            + 데이터 추가
          </BaseButton>
        </div>
      </div>

      <!-- 필터 영역 -->
      <div class="sync-data-page__filters">
        <BaseSelect
          v-model="selectedSystemId"
          label="시스템 선택"
          :options="systemOptions"
          placeholder="시스템을 선택하세요"
          @update:model-value="onSystemChange"
        />
        <BaseSelect
          v-model="store.filterStatus"
          label="상태 필터"
          :options="statusOptions"
          placeholder="전체 상태"
          @update:model-value="onFilterChange"
        />
        <BaseInput
          v-model="store.filterRefId"
          label="참조 ID 검색"
          placeholder="외부 참조 ID 입력..."
          @keyup.enter="onFilterChange"
        />
        <button
          v-if="store.filterStatus || store.filterRefId"
          class="filter-reset"
          @click="onResetFilters"
        >
          필터 초기화
        </button>
      </div>

      <BaseAlert v-if="store.syncError" :message="store.syncError" type="error" />

      <template v-if="selectedSystemId">
        <BaseTable
          :columns="columns"
          :rows="rows"
          :loading="store.syncLoading"
          :sort-key="store.sortField"
          :sort-dir="store.sortDir"
          @sort="onSort"
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

      <div v-if="!selectedSystemId" class="sync-data-page__empty">
        좌측에서 시스템을 선택하면 동기화 데이터를 확인할 수 있습니다.
      </div>
    </div>

    <!-- 데이터 수동 입력 모달 (Teleport 사용으로 레이아웃 바깥에 위치) -->
    <BaseModal v-model="showAddModal" title="동기화 데이터 추가">
      <BaseInput
        v-model="addForm.externalRefId"
        label="외부 참조 ID"
        placeholder="예: REF-2024-001"
        :error="addError.externalRefId"
        required
      />
      <BaseSelect
        v-model="addForm.status"
        label="처리 상태"
        :options="statusCreateOptions"
        required
      />
      <div class="form-field">
        <label class="form-label">원본 페이로드 <span class="form-hint">(선택)</span></label>
        <textarea
          v-model="addForm.rawPayload"
          class="form-textarea"
          placeholder='예: {"id": 1, "data": "값"}'
          rows="4"
        />
      </div>
      <template #footer>
        <BaseButton variant="secondary" @click="showAddModal = false">취소</BaseButton>
        <BaseButton variant="primary" :loading="adding" @click="handleAddData">추가</BaseButton>
      </template>
    </BaseModal>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import BasePagination from '@/components/ui/BasePagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useSyncDataStore } from '@/stores/syncData'
import { syncDataApi } from '@/api/syncData'

const store = useSyncDataStore()
const selectedSystemId = ref('')
const showAddModal = ref(false)
const adding = ref(false)

const addForm = reactive({ externalRefId: '', status: 'COMPLETED', rawPayload: '' })
const addError = reactive({ externalRefId: '' })

const statusCreateOptions = [
  { value: 'COMPLETED', label: 'COMPLETED' },
  { value: 'PENDING', label: 'PENDING' },
  { value: 'FAILED', label: 'FAILED' },
  { value: 'SKIPPED', label: 'SKIPPED' },
]

const systemOptions = computed(() =>
  store.activeSystems.map((s) => ({ value: String(s.id), label: s.systemCode })),
)

const statusOptions = [
  { value: '', label: '전체' },
  { value: 'PENDING', label: 'PENDING' },
  { value: 'PROCESSING', label: 'PROCESSING' },
  { value: 'COMPLETED', label: 'COMPLETED' },
  { value: 'FAILED', label: 'FAILED' },
  { value: 'SKIPPED', label: 'SKIPPED' },
]

const columns = [
  { key: 'externalRefId', label: '외부 참조 ID', sortable: true },
  { key: 'status',        label: '상태',         width: '110px', sortable: true },
  { key: 'errorMessage',  label: '오류 메시지' },
  { key: 'syncedAt',      label: '동기화 시각',  width: '150px', sortable: true },
  { key: 'createdAt',     label: '생성 시각',    width: '150px', sortable: true },
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

async function handleAddData() {
  addError.externalRefId = ''
  if (!addForm.externalRefId.trim()) {
    addError.externalRefId = '외부 참조 ID를 입력해주세요.'
    return
  }
  if (!selectedSystemId.value) return

  adding.value = true
  try {
    await syncDataApi.createManual({
      systemId: Number(selectedSystemId.value),
      externalRefId: addForm.externalRefId.trim(),
      status: addForm.status,
      rawPayload: addForm.rawPayload || undefined,
    })
    showAddModal.value = false
    addForm.externalRefId = ''
    addForm.rawPayload = ''
    addForm.status = 'COMPLETED'
    await store.fetchSyncItems(Number(selectedSystemId.value), 0)
  } catch (err: unknown) {
    addError.externalRefId = err instanceof Error ? err.message : '데이터 추가에 실패했습니다.'
  } finally {
    adding.value = false
  }
}

function onSystemChange(val: string) {
  if (val) {
    store.filterStatus = ''
    store.filterRefId = ''
    store.fetchSyncItems(Number(val), 0)
  }
}

function onFilterChange() {
  if (selectedSystemId.value) {
    store.fetchSyncItems(Number(selectedSystemId.value), 0)
  }
}

function onResetFilters() {
  store.filterStatus = ''
  store.filterRefId = ''
  if (selectedSystemId.value) {
    store.fetchSyncItems(Number(selectedSystemId.value), 0)
  }
}

function onSort(key: string, dir: 'asc' | 'desc') {
  store.sortField = key
  store.sortDir = dir
  if (selectedSystemId.value) {
    store.fetchSyncItems(Number(selectedSystemId.value), 0)
  }
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

.sync-data-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: flex-end;
}

.sync-data-page__filters > * {
  flex: 1;
  min-width: 12rem;
  max-width: 20rem;
}

.filter-reset {
  flex: none;
  height: 2.25rem;
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--transition-fast), border-color var(--transition-fast);
}

.filter-reset:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.sync-data-page__empty {
  text-align: center;
  padding: var(--space-12);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.header-actions { display: flex; gap: var(--space-2); }

.error-text { color: var(--color-danger); font-size: var(--text-xs); }
.empty-text { color: var(--color-text-muted); }

.form-field { display: flex; flex-direction: column; gap: var(--space-1); }
.form-label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text); }
.form-hint { font-weight: 400; color: var(--color-text-muted); font-size: var(--text-xs); }
.form-textarea {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font-size: var(--text-sm);
  font-family: monospace;
  resize: vertical;
  outline: none;
  transition: border-color var(--transition-fast);
}
.form-textarea:focus { border-color: var(--color-border-focus); }
</style>
