<template>
  <AppLayout>
    <div class="scheduler-page">
      <!-- 헤더 -->
      <div class="scheduler-page__header">
        <h1 class="scheduler-page__title">스케줄러 관리</h1>
        <div class="header-actions">
          <BaseButton variant="secondary" :disabled="refreshing" @click="refresh">
            {{ refreshing ? '새로고침 중...' : '새로고침' }}
          </BaseButton>
          <BaseButton
            variant="danger"
            :loading="retrying"
            :disabled="summary?.dataFailed === 0"
            @click="handleRetryFailed"
          >
            실패 재처리 ({{ summary?.dataFailed ?? 0 }}건)
          </BaseButton>
        </div>
      </div>

      <BaseAlert v-if="actionMsg" :message="actionMsg.text" :type="actionMsg.type" />

      <!-- 스케줄러 상태 요약 카드 -->
      <section class="summary-section">
        <div class="summary-grid">
          <div class="stat-card stat-card--blue">
            <div class="stat-card__label">Job 완료</div>
            <div class="stat-card__value">{{ summary?.jobCompleted ?? '—' }}</div>
            <div class="stat-card__sub">최근 24시간</div>
          </div>
          <div class="stat-card stat-card--yellow">
            <div class="stat-card__label">Job 실행 중</div>
            <div class="stat-card__value">{{ summary?.jobRunning ?? '—' }}</div>
            <div class="stat-card__sub">현재 RUNNING</div>
          </div>
          <div class="stat-card stat-card--red">
            <div class="stat-card__label">Job 실패</div>
            <div class="stat-card__value">{{ (summary?.jobFailed ?? 0) + (summary?.jobPartialFailed ?? 0) }}</div>
            <div class="stat-card__sub">FAILED + PARTIAL</div>
          </div>
          <div class="stat-card stat-card--green">
            <div class="stat-card__label">데이터 완료</div>
            <div class="stat-card__value">{{ summary?.dataCompleted ?? '—' }}</div>
            <div class="stat-card__sub">최근 24시간</div>
          </div>
          <div class="stat-card stat-card--orange">
            <div class="stat-card__label">대기 중</div>
            <div class="stat-card__value">{{ summary?.dataPending ?? '—' }}</div>
            <div class="stat-card__sub">PENDING</div>
          </div>
          <div class="stat-card stat-card--red">
            <div class="stat-card__label">데이터 실패</div>
            <div class="stat-card__value">{{ summary?.dataFailed ?? '—' }}</div>
            <div class="stat-card__sub">재처리 대상</div>
          </div>
        </div>
      </section>

      <!-- 시스템별 수동 트리거 -->
      <section class="section">
        <h2 class="section__title">시스템별 수동 동기화</h2>
        <div v-if="activeSystems.length === 0" class="empty-msg">활성 시스템이 없습니다.</div>
        <div v-else class="system-cards">
          <div v-for="sys in activeSystems" :key="sys.id" class="system-card">
            <div class="system-card__info">
              <span class="system-card__code">{{ sys.systemCode }}</span>
              <span class="system-card__name">{{ sys.name }}</span>
              <span class="system-card__url">{{ sys.baseUrl }}</span>
            </div>
            <BaseButton
              variant="primary"
              size="sm"
              :loading="triggeringId === sys.id"
              @click="handleTrigger(sys)"
            >
              즉시 실행
            </BaseButton>
          </div>
        </div>
      </section>

      <!-- 동기화 로그 -->
      <section class="section">
        <div class="section__header">
          <h2 class="section__title">동기화 실행 로그</h2>
          <div class="log-filters">
            <BaseSelect
              v-model="filterStatus"
              label=""
              :options="statusOptions"
              placeholder="전체 상태"
              @update:model-value="loadLogs(0)"
            />
            <BaseSelect
              v-model="filterTrigger"
              label=""
              :options="triggerOptions"
              placeholder="전체 트리거"
              @update:model-value="loadLogs(0)"
            />
            <BaseSelect
              v-model="filterHours"
              label=""
              :options="hoursOptions"
              @update:model-value="loadLogs(0)"
            />
          </div>
        </div>

        <BaseTable
          :columns="logColumns"
          :rows="logRows"
          :loading="logsLoading"
        >
          <template #status="{ value }">
            <span :class="['log-status', `log-status--${String(value).toLowerCase()}`]">
              {{ String(value) }}
            </span>
          </template>
          <template #triggerType="{ value }">
            <span :class="['trigger-badge', `trigger-badge--${String(value).toLowerCase()}`]">
              {{ String(value) }}
            </span>
          </template>
          <template #counts="{ row }">
            <span class="counts">
              <span class="counts__total">{{ (row as LogRow).totalCount }}</span>
              /
              <span class="counts__success">{{ (row as LogRow).successCount }}</span>
              /
              <span class="counts__fail">{{ (row as LogRow).failCount }}</span>
            </span>
          </template>
          <template #errorSummary="{ value }">
            <span v-if="value" class="error-text" :title="String(value)">
              {{ String(value).slice(0, 50) }}{{ String(value).length > 50 ? '…' : '' }}
            </span>
            <span v-else class="empty-text">—</span>
          </template>
          <template #action="{ row }">
            <button class="payload-btn" @click="openPayloadModal(row as LogRow)">
              payload
            </button>
          </template>
        </BaseTable>

        <BasePagination
          :current-page="logsPage"
          :total-pages="logsTotalPages"
          :total-elements="logsTotalElements"
          @change="loadLogs"
        />
      </section>

      <!-- Payload 확인 모달 -->
      <BaseModal
        v-model="payloadModalOpen"
        :title="`Payload 확인 — ${selectedLog?.systemCode ?? ''}`"
        max-width="56rem"
      >
        <div v-if="selectedLog" class="payload-modal">
          <!-- 로그 요약 -->
          <div class="payload-meta">
            <span :class="['log-status', `log-status--${selectedLog.status.toLowerCase()}`]">{{ selectedLog.status }}</span>
            <span class="payload-meta__item">트리거: <strong>{{ selectedLog.triggerType }}</strong></span>
            <span class="payload-meta__item">실행: <strong>{{ selectedLog.triggeredAt ? formatDt(selectedLog.triggeredAt) : '—' }}</strong></span>
            <span class="payload-meta__item">전체/성공/실패:
              <strong>{{ selectedLog.totalCount }}/{{ selectedLog.successCount }}/{{ selectedLog.failCount }}</strong>
            </span>
          </div>

          <!-- 데이터 아이템 목록 -->
          <div v-if="payloadLoading" class="payload-loading">데이터 로딩 중...</div>
          <div v-else-if="payloadItems.length === 0" class="payload-empty">연결된 데이터 레코드가 없습니다.</div>
          <div v-else class="payload-list">
            <div v-for="item in payloadItems" :key="item.id" class="payload-item">
              <div class="payload-item__header">
                <span class="payload-item__ref">{{ item.externalRefId }}</span>
                <span :class="['data-status', `data-status--${item.status.toLowerCase()}`]">{{ item.status }}</span>
                <span v-if="item.retryCount > 0" class="payload-item__retry">재시도 {{ item.retryCount }}회</span>
              </div>
              <div v-if="item.rawPayload || item.processedPayload" class="payload-tabs">
                <div v-if="item.rawPayload" class="payload-block">
                  <div class="payload-block__label">Raw Payload</div>
                  <pre class="payload-block__code">{{ formatJson(item.rawPayload) }}</pre>
                </div>
                <div v-if="item.processedPayload" class="payload-block">
                  <div class="payload-block__label">Processed Payload</div>
                  <pre class="payload-block__code">{{ formatJson(item.processedPayload) }}</pre>
                </div>
              </div>
              <div v-if="item.errorMessage" class="payload-item__error">{{ item.errorMessage }}</div>
            </div>
          </div>

          <!-- 페이지네이션 -->
          <BasePagination
            v-if="payloadTotalPages > 1"
            :current-page="payloadPage"
            :total-pages="payloadTotalPages"
            :total-elements="payloadTotalElements"
            @change="loadPayloadItems"
          />
        </div>
      </BaseModal>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BasePagination from '@/components/ui/BasePagination.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { syncDataApi, type SchedulerSummary, type SyncLogDetail, type SyncDataItem } from '@/api/syncData'
import { externalSystemApi, type ExternalSystem } from '@/api/externalSystem'

const summary = ref<SchedulerSummary | null>(null)
const activeSystems = ref<ExternalSystem[]>([])
const refreshing = ref(false)
const retrying = ref(false)
const triggeringId = ref<number | null>(null)
const actionMsg = ref<{ text: string; type: 'success' | 'error' } | null>(null)

// 로그 테이블 상태
const logs = ref<SyncLogDetail[]>([])
const logsLoading = ref(false)
const logsPage = ref(0)
const logsTotalPages = ref(0)
const logsTotalElements = ref(0)
const filterStatus = ref('')
const filterTrigger = ref('')
const filterHours = ref('24')

interface LogRow {
  _log: SyncLogDetail
  systemCode: string
  status: string
  triggerType: string
  totalCount: number
  successCount: number
  failCount: number
  triggeredAt: string | null
  completedAt: string | null
  errorSummary: string | null
  counts?: string
}

// Payload 모달 상태
const payloadModalOpen = ref(false)
const selectedLog = ref<SyncLogDetail | null>(null)
const payloadItems = ref<SyncDataItem[]>([])
const payloadLoading = ref(false)
const payloadPage = ref(0)
const payloadTotalPages = ref(0)
const payloadTotalElements = ref(0)

const statusOptions = [
  { value: '', label: '전체 상태' },
  { value: 'RUNNING', label: 'RUNNING' },
  { value: 'COMPLETED', label: 'COMPLETED' },
  { value: 'PARTIAL_FAILED', label: 'PARTIAL_FAILED' },
  { value: 'FAILED', label: 'FAILED' },
]

const triggerOptions = [
  { value: '', label: '전체 트리거' },
  { value: 'SCHEDULED', label: 'SCHEDULED' },
  { value: 'MANUAL', label: 'MANUAL' },
  { value: 'API', label: 'API' },
]

const hoursOptions = [
  { value: '6', label: '최근 6시간' },
  { value: '24', label: '최근 24시간' },
  { value: '72', label: '최근 3일' },
  { value: '168', label: '최근 7일' },
  { value: '0', label: '전체 기간' },
]

const logColumns = [
  { key: 'systemCode', label: '시스템' },
  { key: 'status', label: '상태', width: '130px' },
  { key: 'triggerType', label: '트리거', width: '110px' },
  { key: 'counts', label: '전체/성공/실패', width: '120px' },
  { key: 'triggeredAt', label: '실행 시각', width: '145px' },
  { key: 'completedAt', label: '완료 시각', width: '145px' },
  { key: 'errorSummary', label: '오류' },
  { key: 'action', label: '', width: '80px' },
]

const logRows = computed<LogRow[]>(() =>
  logs.value.map((l) => ({
    _log: l,
    systemCode: l.systemCode,
    status: l.status,
    triggerType: l.triggerType,
    totalCount: l.totalCount,
    successCount: l.successCount,
    failCount: l.failCount,
    triggeredAt: l.triggeredAt ? formatDt(l.triggeredAt) : null,
    completedAt: l.completedAt ? formatDt(l.completedAt) : null,
    errorSummary: l.errorSummary,
    counts: `${l.totalCount}/${l.successCount}/${l.failCount}`,
  })),
)

function formatDt(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function showMsg(text: string, type: 'success' | 'error') {
  actionMsg.value = { text, type }
  setTimeout(() => { actionMsg.value = null }, 4000)
}

async function loadSummary() {
  try {
    const res = await syncDataApi.getSchedulerSummary()
    summary.value = res.data.data
  } catch {
    summary.value = null
  }
}

async function loadActiveSystems() {
  try {
    const res = await externalSystemApi.getActive()
    activeSystems.value = res.data.data
  } catch {
    activeSystems.value = []
  }
}

async function loadLogs(page = 0) {
  logsLoading.value = true
  try {
    const res = await syncDataApi.getSyncLogs({
      status: filterStatus.value || undefined,
      triggerType: filterTrigger.value || undefined,
      hours: Number(filterHours.value),
      page,
      size: 20,
    })
    const data = res.data.data
    logs.value = data.content
    logsPage.value = data.page
    logsTotalPages.value = data.totalPages
    logsTotalElements.value = data.totalElements
  } catch {
    logs.value = []
  } finally {
    logsLoading.value = false
  }
}

async function refresh() {
  refreshing.value = true
  await Promise.all([loadSummary(), loadActiveSystems(), loadLogs(0)])
  refreshing.value = false
}

async function handleRetryFailed() {
  retrying.value = true
  try {
    const res = await syncDataApi.retryFailed()
    const count = res.data.data.retriedCount
    showMsg(`${count}건 재처리 완료`, 'success')
    await loadSummary()
    await loadLogs(0)
  } catch (err: unknown) {
    showMsg(err instanceof Error ? err.message : '재처리 실패', 'error')
  } finally {
    retrying.value = false
  }
}

async function handleTrigger(sys: ExternalSystem) {
  triggeringId.value = sys.id
  try {
    const res = await syncDataApi.triggerSync(sys.systemCode)
    const r = res.data.data
    showMsg(
      `[${sys.systemCode}] 동기화 완료 — 성공: ${r.successCount} / 실패: ${r.failCount}`,
      r.failCount === 0 ? 'success' : 'error',
    )
    await loadSummary()
    await loadLogs(0)
  } catch (err: unknown) {
    showMsg(
      `[${sys.systemCode}] ${err instanceof Error ? err.message : '동기화 실패'}`,
      'error',
    )
  } finally {
    triggeringId.value = null
  }
}

function formatJson(raw: string | null): string {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function loadPayloadItems(page = 0) {
  if (!selectedLog.value?.systemId) return
  payloadLoading.value = true
  try {
    const res = await syncDataApi.getListBySystem(selectedLog.value.systemId, page, 10)
    const data = res.data.data
    payloadItems.value = data.content as unknown as SyncDataItem[]
    payloadPage.value = data.page
    payloadTotalPages.value = data.totalPages
    payloadTotalElements.value = data.totalElements
  } catch {
    payloadItems.value = []
  } finally {
    payloadLoading.value = false
  }
}

function openPayloadModal(row: LogRow) {
  selectedLog.value = row._log
  payloadPage.value = 0
  payloadItems.value = []
  payloadModalOpen.value = true
  loadPayloadItems(0)
}

onMounted(() => {
  loadSummary()
  loadActiveSystems()
  loadLogs(0)
})
</script>

<style scoped>
.scheduler-page { display: flex; flex-direction: column; gap: var(--space-6); }

.scheduler-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.scheduler-page__title { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); }

.header-actions { display: flex; gap: var(--space-2); }

/* 요약 카드 */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-3);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  border-left: 4px solid currentColor;
}

.stat-card--blue    { color: var(--color-primary); }
.stat-card--green   { color: #16a34a; }
.stat-card--yellow  { color: #ca8a04; }
.stat-card--orange  { color: #ea580c; }
.stat-card--red     { color: var(--color-danger); }

.stat-card__label { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
.stat-card__value { font-size: var(--text-2xl); font-weight: 700; color: var(--color-text); margin: var(--space-1) 0; }
.stat-card__sub   { font-size: var(--text-xs); color: var(--color-text-muted); }

/* 섹션 */
.section { display: flex; flex-direction: column; gap: var(--space-3); }
.section__header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: var(--space-2); }
.section__title { font-size: var(--text-lg); font-weight: 600; color: var(--color-text); margin: 0; }

/* 시스템 카드 */
.system-cards { display: flex; flex-direction: column; gap: var(--space-2); }

.system-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.system-card__info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.system-card__code { font-weight: 600; font-size: var(--text-sm); color: var(--color-text); }
.system-card__name { font-size: var(--text-xs); color: var(--color-text-muted); }
.system-card__url  { font-size: var(--text-xs); color: var(--color-text-muted); font-family: monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 로그 필터 */
.log-filters { display: flex; gap: var(--space-2); flex-wrap: wrap; }
.log-filters > * { min-width: 130px; }

/* 로그 상태 뱃지 */
.log-status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-xs);
  font-weight: 600;
}
.log-status--running        { background: #fef9c3; color: #854d0e; }
.log-status--completed      { background: #dcfce7; color: #166534; }
.log-status--partial_failed { background: #ffedd5; color: #9a3412; }
.log-status--failed         { background: #fee2e2; color: #991b1b; }

/* 트리거 뱃지 */
.trigger-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-xs);
  font-weight: 500;
}
.trigger-badge--scheduled { background: var(--color-bg); color: var(--color-text-muted); border: 1px solid var(--color-border); }
.trigger-badge--manual    { background: #eff6ff; color: #1d4ed8; }
.trigger-badge--api       { background: #f5f3ff; color: #6d28d9; }

/* 카운트 */
.counts { font-size: var(--text-sm); font-family: monospace; }
.counts__total   { color: var(--color-text-muted); }
.counts__success { color: #16a34a; }
.counts__fail    { color: var(--color-danger); }

.error-text { color: var(--color-danger); font-size: var(--text-xs); }
.empty-text { color: var(--color-text-muted); }
.empty-msg  { color: var(--color-text-muted); font-size: var(--text-sm); padding: var(--space-4); text-align: center; }

/* payload 버튼 */
.payload-btn {
  padding: 2px 10px;
  font-size: var(--text-xs);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary);
  cursor: pointer;
  white-space: nowrap;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.payload-btn:hover { background: var(--color-primary); color: #fff; }

/* payload 모달 */
.payload-modal { display: flex; flex-direction: column; gap: var(--space-4); }

.payload-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}
.payload-meta__item { color: var(--color-text-muted); }
.payload-meta__item strong { color: var(--color-text); }

.payload-loading { color: var(--color-text-muted); font-size: var(--text-sm); text-align: center; padding: var(--space-6); }
.payload-empty   { color: var(--color-text-muted); font-size: var(--text-sm); text-align: center; padding: var(--space-6); }

.payload-list { display: flex; flex-direction: column; gap: var(--space-3); }

.payload-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.payload-item__header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.payload-item__ref   { font-family: monospace; font-size: var(--text-sm); font-weight: 600; color: var(--color-text); }
.payload-item__retry { font-size: var(--text-xs); color: var(--color-text-muted); margin-left: auto; }
.payload-item__error { padding: var(--space-2) var(--space-3); font-size: var(--text-xs); color: var(--color-danger); background: #fff5f5; }

/* data status badge */
.data-status {
  display: inline-block;
  padding: 1px 6px;
  border-radius: var(--radius-full, 9999px);
  font-size: 10px;
  font-weight: 600;
}
.data-status--completed   { background: #dcfce7; color: #166534; }
.data-status--failed      { background: #fee2e2; color: #991b1b; }
.data-status--pending     { background: #fef9c3; color: #854d0e; }
.data-status--processing  { background: #eff6ff; color: #1d4ed8; }
.data-status--skipped     { background: var(--color-bg); color: var(--color-text-muted); border: 1px solid var(--color-border); }

.payload-tabs { display: flex; flex-direction: column; gap: 0; }

.payload-block { border-top: 1px solid var(--color-border); }
.payload-block__label {
  padding: var(--space-1) var(--space-3);
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}
.payload-block__code {
  margin: 0;
  padding: var(--space-3);
  font-family: monospace;
  font-size: var(--text-xs);
  line-height: 1.5;
  color: var(--color-text);
  background: var(--color-surface);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

</style>
