<template>
  <div class="chart-wrap">
    <h3 class="chart-wrap__title">최근 동기화 현황</h3>

    <div v-if="!bars.length" class="chart-wrap__empty">데이터가 없습니다.</div>

    <div v-else class="chart">
      <!-- Y축 눈금 -->
      <div class="chart__y-axis">
        <span v-for="tick in yTicks" :key="tick" class="chart__y-tick">{{ tick }}</span>
      </div>

      <!-- 바 영역 -->
      <div class="chart__bars">
        <div
          v-for="bar in bars"
          :key="bar.label"
          class="chart__bar-group"
        >
          <div class="chart__bar-pair">
            <div
              class="chart__bar chart__bar--success"
              :style="{ height: pct(bar.success) + '%' }"
              :title="`성공: ${bar.success}`"
            />
            <div
              class="chart__bar chart__bar--fail"
              :style="{ height: pct(bar.fail) + '%' }"
              :title="`실패: ${bar.fail}`"
            />
          </div>
          <span class="chart__bar-label">{{ bar.label }}</span>
        </div>
      </div>
    </div>

    <!-- 범례 -->
    <div class="chart__legend">
      <span class="chart__legend-item chart__legend-item--success">성공</span>
      <span class="chart__legend-item chart__legend-item--fail">실패</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SyncLog } from '@/api/syncData'

const props = defineProps<{ logs: SyncLog[] }>()

interface Bar { label: string; success: number; fail: number }

const bars = computed<Bar[]>(() => {
  return props.logs.slice(0, 10).map((log) => ({
    label: log.systemCode,
    success: log.successCount,
    fail: log.failCount,
  }))
})

const maxValue = computed(() =>
  Math.max(1, ...bars.value.map((b) => b.success + b.fail)),
)

const yTicks = computed(() => {
  const max = maxValue.value
  const step = Math.ceil(max / 4)
  return [max, step * 3, step * 2, step, 0]
})

function pct(value: number) {
  return Math.round((value / maxValue.value) * 100)
}
</script>

<style scoped>
.chart-wrap {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.chart-wrap__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: var(--space-4);
}

.chart-wrap__empty {
  text-align: center;
  padding: var(--space-8);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.chart {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  height: 10rem;
}

.chart__y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  padding-bottom: 1.5rem;
}

.chart__y-tick {
  font-size: 0.65rem;
  color: var(--color-text-muted);
  text-align: right;
  min-width: 2rem;
}

.chart__bars {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);
  flex: 1;
  height: 100%;
}

.chart__bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
  height: 100%;
  justify-content: flex-end;
}

.chart__bar-pair {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  width: 100%;
  height: calc(100% - 1.5rem);
}

.chart__bar {
  flex: 1;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  min-height: 2px;
  transition: height var(--transition-fast);
}

.chart__bar--success { background: #4ade80; }
.chart__bar--fail    { background: #f87171; }

.chart__bar-label {
  font-size: 0.6rem;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
  text-align: center;
}

.chart__legend {
  display: flex;
  gap: var(--space-4);
  margin-top: var(--space-3);
  justify-content: flex-end;
}

.chart__legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.chart__legend-item::before {
  content: '';
  display: inline-block;
  width: 0.6rem;
  height: 0.6rem;
  border-radius: 2px;
}

.chart__legend-item--success::before { background: #4ade80; }
.chart__legend-item--fail::before    { background: #f87171; }
</style>
