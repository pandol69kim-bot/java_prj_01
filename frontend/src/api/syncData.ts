import apiClient from './client'

export type SyncStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export interface SyncDataItem {
  id: number
  systemCode: string
  externalRefId: string
  status: SyncStatus
  rawPayload: string | null
  processedPayload: string | null
  errorMessage: string | null
  retryCount: number
  syncedAt: string | null
  createdAt: string
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

export interface SchedulerSummary {
  dataPending: number
  dataProcessing: number
  dataCompleted: number
  dataFailed: number
  dataSkipped: number
  jobRunning: number
  jobCompleted: number
  jobPartialFailed: number
  jobFailed: number
  since: string
}

export interface SyncLogDetail {
  id: number
  systemId: number
  systemCode: string
  status: 'RUNNING' | 'COMPLETED' | 'PARTIAL_FAILED' | 'FAILED'
  triggerType: 'SCHEDULED' | 'MANUAL' | 'API'
  totalCount: number
  successCount: number
  failCount: number
  triggeredAt: string
  completedAt: string | null
  errorSummary: string | null
}

export interface SyncLogPage {
  content: SyncLogDetail[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface TriggerResult {
  id: number
  systemCode: string
  status: string
  triggerType: string
  totalCount: number
  successCount: number
  failCount: number
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
    return apiClient.post<{ data: TriggerResult }>(`/sync/trigger/${systemCode}`)
  },

  retryFailed() {
    return apiClient.post<{ data: { retriedCount: number } }>('/sync/retry-failed')
  },

  getSchedulerSummary() {
    return apiClient.get<{ data: SchedulerSummary }>('/sync/summary')
  },

  getSyncLogs(params: {
    systemId?: number
    status?: string
    triggerType?: string
    hours?: number
    page?: number
    size?: number
  }) {
    return apiClient.get<{ data: SyncLogPage }>('/sync/logs', { params })
  },
}
