<template>
  <AppLayout>
    <div class="systems-page">
      <div class="systems-page__header">
        <h1 class="systems-page__title">외부 시스템</h1>
        <BaseButton variant="primary" size="sm" @click="store.fetchSystems()">새로고침</BaseButton>
      </div>

      <BaseAlert v-if="store.systemsError" :message="store.systemsError" type="error" />

      <BaseTable
        :columns="columns"
        :rows="rows"
        :loading="store.systemsLoading"
      >
        <template #active="{ value }">
          <StatusBadge :status="value ? 'ACTIVE' : 'INACTIVE'" />
        </template>
        <template #authType="{ value }">
          <span class="auth-tag">{{ value }}</span>
        </template>
        <template #actions="{ row }">
          <BaseButton
            variant="danger"
            size="sm"
            :loading="deactivatingId === row.id"
            @click="handleDeactivate(Number(row.id))"
          >비활성화</BaseButton>
        </template>
      </BaseTable>

      <BasePagination
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-elements="totalElements"
        @change="loadPage"
      />
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import BasePagination from '@/components/ui/BasePagination.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useSyncDataStore } from '@/stores/syncData'
import { externalSystemApi } from '@/api/externalSystem'

const store = useSyncDataStore()
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const deactivatingId = ref<number | null>(null)

const columns = [
  { key: 'systemCode', label: '시스템 코드', width: '140px' },
  { key: 'systemName', label: '시스템명' },
  { key: 'baseUrl',    label: 'Base URL' },
  { key: 'authType',   label: '인증 방식', width: '100px' },
  { key: 'active',     label: '상태',      width: '80px' },
  { key: 'actions',    label: '',          width: '90px' },
]

const rows = computed(() =>
  store.systems.map((s) => ({
    id:         s.id,
    systemCode: s.systemCode,
    systemName: s.systemName,
    baseUrl:    s.baseUrl,
    authType:   s.authType,
    active:     s.active,
  })),
)

async function loadPage(page: number) {
  currentPage.value = page
  await store.fetchSystems(page)
  // 페이징 메타는 API 응답에서 직접 가져와야 하므로 별도 호출
  try {
    const res = await externalSystemApi.getList(page)
    totalPages.value = res.data.data.totalPages
    totalElements.value = res.data.data.totalElements
  } catch { /* store.systemsError에 표시됨 */ }
}

async function handleDeactivate(id: number) {
  deactivatingId.value = id
  try {
    await externalSystemApi.deactivate(id)
    await loadPage(currentPage.value)
  } finally {
    deactivatingId.value = null
  }
}

onMounted(() => loadPage(0))
</script>

<style scoped>
.systems-page { display: flex; flex-direction: column; gap: var(--space-4); }

.systems-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.systems-page__title { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); }

.auth-tag {
  padding: 1px var(--space-2);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: monospace;
}
</style>
