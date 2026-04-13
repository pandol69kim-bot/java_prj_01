import { onMounted, onUnmounted } from 'vue'

/**
 * 지정된 간격으로 콜백을 자동 반복 실행합니다.
 * 컴포넌트 마운트 시 시작, 언마운트 시 자동 정리됩니다.
 *
 * @param callback - 반복 실행할 비동기 함수
 * @param intervalMs - 실행 간격 (밀리초, 기본값 30초)
 */
export function useAutoRefresh(callback: () => Promise<void>, intervalMs = 30_000) {
  let timerId: ReturnType<typeof setInterval> | null = null

  onMounted(async () => {
    await callback()
    timerId = setInterval(callback, intervalMs)
  })

  onUnmounted(() => {
    if (timerId !== null) {
      clearInterval(timerId)
      timerId = null
    }
  })
}
