import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { externalSystemApi, type ExternalSystem } from '@/api/externalSystem'
import { syncDataApi, type SyncDataItem, type SyncSummary, type SyncDataPage } from '@/api/syncData'

export const useSyncDataStore = defineStore('syncData', () => {
  // 외부 시스템 목록
  const systems = ref<ExternalSystem[]>([])
  const activeSystems = ref<ExternalSystem[]>([])
  const systemsLoading = ref(false)
  const systemsError = ref<string | null>(null)

  // 선택된 시스템의 동기화 데이터
  const selectedSystemId = ref<number | null>(null)
  const syncItems = ref<SyncDataItem[]>([])
  const syncPage = ref(0)
  const syncTotalPages = ref(0)
  const syncTotalElements = ref(0)
  const syncLoading = ref(false)
  const syncError = ref<string | null>(null)

  // 정렬·필터 상태
  const sortField = ref('createdAt')
  const sortDir = ref<'asc' | 'desc'>('desc')
  const filterStatus = ref('')
  const filterRefId = ref('')

  // 대시보드 요약
  const summary = ref<SyncSummary | null>(null)
  const summaryLoading = ref(false)

  const selectedSystem = computed(() =>
    systems.value.find((s) => s.id === selectedSystemId.value) ?? null,
  )

  async function fetchSystems(page = 0, size = 20) {
    systemsLoading.value = true
    systemsError.value = null
    try {
      const res = await externalSystemApi.getList(page, size)
      systems.value = res.data.data.content
    } catch (err: unknown) {
      systemsError.value = err instanceof Error ? err.message : '시스템 목록 조회 실패'
    } finally {
      systemsLoading.value = false
    }
  }

  async function fetchActiveSystems() {
    try {
      const res = await externalSystemApi.getActive()
      activeSystems.value = res.data.data
    } catch {
      activeSystems.value = []
    }
  }

  async function fetchSyncItems(systemId: number, page = 0, size = 20) {
    selectedSystemId.value = systemId
    syncLoading.value = true
    syncError.value = null
    try {
      const res = await syncDataApi.getListBySystem(
        systemId,
        page,
        size,
        sortField.value,
        sortDir.value,
        filterStatus.value || undefined,
        filterRefId.value || undefined,
      )
      const pageData: SyncDataPage = res.data.data
      syncItems.value = pageData.content
      syncPage.value = pageData.number
      syncTotalPages.value = pageData.totalPages
      syncTotalElements.value = pageData.totalElements
    } catch (err: unknown) {
      syncError.value = err instanceof Error ? err.message : '동기화 데이터 조회 실패'
    } finally {
      syncLoading.value = false
    }
  }

  async function exportExcel() {
    if (!selectedSystemId.value) return
    try {
      const res = await syncDataApi.exportExcel(
        selectedSystemId.value,
        filterStatus.value || undefined,
        filterRefId.value || undefined,
      )
      const url = URL.createObjectURL(new Blob([res.data as BlobPart]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'sync-data.xlsx'
      a.click()
      URL.revokeObjectURL(url)
    } catch (err: unknown) {
      syncError.value = err instanceof Error ? err.message : '엑셀 다운로드 실패'
    }
  }

  async function fetchSummary() {
    summaryLoading.value = true
    try {
      const res = await syncDataApi.getSummary()
      summary.value = res.data.data
    } catch {
      summary.value = null
    } finally {
      summaryLoading.value = false
    }
  }

  async function triggerSync(systemCode: string) {
    await syncDataApi.triggerSync(systemCode)
    await fetchSummary()
  }

  async function retryFailed() {
    await syncDataApi.retryFailed()
    await fetchSummary()
  }

  return {
    systems,
    activeSystems,
    systemsLoading,
    systemsError,
    selectedSystemId,
    selectedSystem,
    syncItems,
    syncPage,
    syncTotalPages,
    syncTotalElements,
    syncLoading,
    syncError,
    sortField,
    sortDir,
    filterStatus,
    filterRefId,
    summary,
    summaryLoading,
    fetchSystems,
    fetchActiveSystems,
    fetchSyncItems,
    fetchSummary,
    triggerSync,
    retryFailed,
    exportExcel,
  }
})
