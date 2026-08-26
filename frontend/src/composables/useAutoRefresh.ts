/**
 * 页面数据自动刷新 composable。
 * 用法：const { autoRefresh, refreshInterval, start, stop } = useAutoRefresh(loadData)
 */
import { ref, watch, onUnmounted } from 'vue'

export interface AutoRefreshOptions {
  /** 默认刷新间隔（秒），默认 10 */
  defaultInterval?: number
  /** 是否默认开启 */
  defaultEnabled?: boolean
  /** 最小间隔（秒） */
  minInterval?: number
  /** 最大间隔（秒） */
  maxInterval?: number
}

export function useAutoRefresh(
  refreshFn: () => void | Promise<void>,
  options: AutoRefreshOptions = {},
) {
  const {
    defaultInterval = 10,
    defaultEnabled = false,
    minInterval = 3,
    maxInterval = 300,
  } = options

  const autoRefresh = ref(defaultEnabled)
  const refreshInterval = ref(defaultInterval)
  let timer: ReturnType<typeof setInterval> | null = null

  function start() {
    stop()
    if (refreshInterval.value < minInterval) refreshInterval.value = minInterval
    if (refreshInterval.value > maxInterval) refreshInterval.value = maxInterval
    timer = setInterval(() => {
      void refreshFn()
    }, refreshInterval.value * 1000)
  }

  function stop() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  watch(autoRefresh, (val) => {
    if (val) start()
    else stop()
  })

  watch(refreshInterval, () => {
    if (autoRefresh.value) start()
  })

  onUnmounted(stop)

  return { autoRefresh, refreshInterval, start, stop }
}
