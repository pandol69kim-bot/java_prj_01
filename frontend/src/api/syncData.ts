import apiClient from './client'

export type SyncStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export interface SyncDataItem {
  id: number
  externalSystemId: number
  externalRefId: string
  status: SyncStatus
  payload: string | null
  errorMessage: string | null
  syncedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface SyncDataPage {
  content: SyncDataItem[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export type SyncLogStatus = 'RUNNING' | 'COMPLETED' | 'PARTIAL_FAILED' | 'FAILED'
export type TriggerType = 'SCHEDULER' | 'MANUAL' | 'RETRY'

export interface SyncLog {
  id: number
  externalSystemId: number
  systemCode: string
  status: SyncLogStatus
  triggerType: TriggerType
  totalCount: number
  successCount: number
  failCount: number
  startedAt: string
  completedAt: string | null
}

export interface SyncSummary {
  activeSystems: number
  todayTotal: number
  todaySuccess: number
  todayFail: number
  pendingCount: number
  recentLogs: SyncLog[]
}

export const syncDataApi = {
  getListBySystem(
    systemId: number,
    page = 0,
    size = 20,
    sort = 'createdAt',
    direction: 'asc' | 'desc' = 'desc',
    status?: string,
    refId?: string,
  ) {
    return apiClient.get<{ data: SyncDataPage }>('/sync-data', {
      params: {
        systemId,
        page,
        size,
        sort,
        direction,
        status: status || undefined,
        refId: refId || undefined,
      },
    })
  },

  getById(id: number) {
    return apiClient.get<{ data: SyncDataItem }>(`/sync-data/${id}`)
  },

  createManual(payload: {
    systemId: number
    externalRefId: string
    status?: string
    rawPayload?: string
  }) {
    return apiClient.post<{ data: SyncDataItem }>('/sync-data/manual', payload)
  },

  exportExcel(systemId: number, status?: string, refId?: string) {
    return apiClient.get('/sync-data/export', {
      params: {
        systemId,
        status: status || undefined,
        refId: refId || undefined,
      },
      responseType: 'blob',
    })
  },

  getSummary() {
    return apiClient.get<{ data: SyncSummary }>('/sync/summary')
  },

  triggerSync(systemCode: string) {
    return apiClient.post<{ data: string }>(`/sync/trigger/${systemCode}`)
  },

  retryFailed() {
    return apiClient.post<{ data: string }>('/sync/retry-failed')
  },
}
