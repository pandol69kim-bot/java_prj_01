<template>
  <AppLayout>
    <div class="dashboard">
      <div class="dashboard__header">
        <h1 class="dashboard__title">대시보드</h1>
        <div class="dashboard__actions">
          <BaseButton
            variant="secondary"
            size="sm"
            :loading="store.summaryLoading"
            @click="store.fetchSummary()"
          >새로고침</BaseButton>
          <BaseButton
            variant="danger"
            size="sm"
            :loading="retrying"
            @click="handleRetryFailed"
          >실패 재시도</BaseButton>
        </div>
      </div>

      <!-- 에러 -->
      <BaseAlert
        v-if="errorMsg"
        :message="errorMsg"
        type="error"
        class="dashboard__alert"
      />

      <!-- 통계 카드 -->
      <section class="dashboard__stats">
        <StatCard
          label="활성 시스템"
          icon="⚙"
          :value="store.summary?.activeSystems ?? 0"
          :loading="store.summaryLoading"
        />
        <StatCard
          label="오늘 전체"
          icon="↺"
          :value="store.summary?.todayTotal ?? 0"
          :loading="store.summaryLoading"
        />
        <StatCard
          label="오늘 성공"
          icon="✔"
          :value="store.summary?.todaySuccess ?? 0"
          :loading="store.summaryLoading"
          :sub="successRate"
        />
        <StatCard
          label="오늘 실패"
          icon="✖"
          :value="store.summary?.todayFail ?? 0"
          :loading="store.summaryLoading"
        />
        <StatCard
          label="대기 중"
          icon="⏳"
          :value="store.summary?.pendingCount ?? 0"
          :loading="store.summaryLoading"
        />
      </section>

      <!-- 차트 + 최근 로그 -->
      <div class="dashboard__grid">
        <SyncTrendChart :logs="store.summary?.recentLogs ?? []" />

        <div class="dashboard__recent">
          <h2 class="dashboard__section-title">최근 동기화 로그</h2>
          <BaseTable
            :columns="logColumns"
            :rows="logRows"
            :loading="store.summaryLoading"
          >
            <template #status="{ value }">
              <StatusBadge :status="String(value)" />
            </template>
            <template #triggerType="{ value }">
              <span class="tag">{{ triggerLabel(String(value)) }}</span>
            </template>
          </BaseTable>
        </div>
      </div>

      <!-- 활성 시스템 빠른 트리거 -->
      <div class="dashboard__systems">
        <h2 class="dashboard__section-title">시스템별 수동 동기화</h2>
        <div v-if="store.systemsLoading" class="dashboard__systems-loading">로딩 중...</div>
        <div v-else class="dashboard__system-list">
          <div
            v-for="sys in store.activeSystems"
            :key="sys.id"
            class="system-chip"
          >
            <span class="system-chip__name">{{ sys.systemCode }}</span>
            <BaseButton
              variant="primary"
              size="sm"
              :loading="triggeringCode === sys.systemCode"
              @click="handleTrigger(sys.systemCode)"
            >동기화</BaseButton>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import SyncTrendChart from '@/components/dashboard/SyncTrendChart.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseAlert from '@/components/ui/BaseAlert.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { useSyncDataStore } from '@/stores/syncData'
import { useAutoRefresh } from '@/composables/useAutoRefresh'

const store = useSyncDataStore()
const errorMsg = ref<string | null>(null)
const retrying = ref(false)
const triggeringCode = ref<string | null>(null)

// 30초 자동 갱신
useAutoRefresh(async () => {
  await Promise.all([store.fetchSummary(), store.fetchActiveSystems()])
})

const successRate = computed(() => {
  const total = store.summary?.todayTotal ?? 0
  const success = store.summary?.todaySuccess ?? 0
  if (!total) return ''
  return `성공률 ${Math.round((success / total) * 100)}%`
})

const logColumns = [
  { key: 'systemCode',  label: '시스템',    width: '120px' },
  { key: 'status',      label: '상태',      width: '100px' },
  { key: 'triggerType', label: '트리거',    width: '90px'  },
  { key: 'totalCount',  label: '전체',      width: '70px'  },
  { key: 'successCount',label: '성공',      width: '70px'  },
  { key: 'failCount',   label: '실패',      width: '70px'  },
  { key: 'startedAt',   label: '시작 시각'             },
]

const logRows = computed(() =>
  (store.summary?.recentLogs ?? []).map((log) => ({
    systemCode:   log.systemCode,
    status:       log.status,
    triggerType:  log.triggerType,
    totalCount:   log.totalCount,
    successCount: log.successCount,
    failCount:    log.failCount,
    startedAt:    formatDateTime(log.startedAt),
  })),
)

function triggerLabel(type: string) {
  const map: Record<string, string> = { SCHEDULER: '스케줄', MANUAL: '수동', RETRY: '재시도' }
  return map[type] ?? type
}

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

async function handleRetryFailed() {
  retrying.value = true
  errorMsg.value = null
  try {
    await store.retryFailed()
  } catch (err: unknown) {
    errorMsg.value = err instanceof Error ? err.message : '재시도 요청 실패'
  } finally {
    retrying.value = false
  }
}

async function handleTrigger(systemCode: string) {
  triggeringCode.value = systemCode
  errorMsg.value = null
  try {
    await store.triggerSync(systemCode)
  } catch (err: unknown) {
    errorMsg.value = err instanceof Error ? err.message : `${systemCode} 동기화 실패`
  } finally {
    triggeringCode.value = null
  }
}
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: var(--space-6); }

.dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.dashboard__title {
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-text);
}

.dashboard__actions { display: flex; gap: var(--space-2); }

.dashboard__alert { margin-top: calc(var(--space-6) * -1 + var(--space-2)); }

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
  gap: var(--space-4);
}

.dashboard__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

@media (max-width: 900px) {
  .dashboard__grid { grid-template-columns: 1fr; }
}

.dashboard__recent {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dashboard__section-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.dashboard__systems {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dashboard__systems-loading { font-size: var(--text-sm); color: var(--color-text-muted); }

.dashboard__system-list { display: flex; flex-wrap: wrap; gap: var(--space-2); }

.system-chip {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.system-chip__name { font-size: var(--text-sm); font-weight: 500; color: var(--color-text); }

.tag {
  display: inline-block;
  padding: 1px var(--space-2);
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
</style>
