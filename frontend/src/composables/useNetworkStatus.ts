/**
 * 网络状态监控 composable
 *
 * 监听 navigator.onLine + online/offline 事件，网络断开时显示全局警告通知，
 * 恢复连接时自动关闭并显示恢复提示。
 *
 * @example
 *   useNetworkStatus()  // 挂载后自动监控，组件卸载自动清理
 */
import { onMounted, onUnmounted, ref } from 'vue'
import { ElNotification } from 'element-plus'

let instanceCount = 0
let notificationClose: (() => void) | null = null

export function useNetworkStatus() {
  const isOnline = ref(navigator.onLine)

  const showOfflineNotice = () => {
    if (notificationClose) return // 已存在
    notificationClose = ElNotification({
      title: '⚠️ 网络断开',
      message: '网络连接已断开，部分功能可能不可用',
      type: 'warning',
      duration: 0, // 不自动关闭
      position: 'top-right',
      offset: 60,
    }).close
  }

  const closeOfflineNotice = () => {
    if (notificationClose) {
      notificationClose()
      notificationClose = null
    }
    if (isOnline.value) {
      ElNotification({
        title: '✅ 网络恢复',
        message: '网络连接已恢复',
        type: 'success',
        duration: 3000,
        position: 'top-right',
        offset: 60,
      })
    }
  }

  const handleOffline = () => {
    isOnline.value = false
    showOfflineNotice()
  }

  const handleOnline = () => {
    isOnline.value = true
    closeOfflineNotice()
  }

  onMounted(() => {
    instanceCount++
    window.addEventListener('offline', handleOffline)
    window.addEventListener('online', handleOnline)
    // 挂载时检查当前状态（防止刷新时已离线）
    if (!navigator.onLine) {
      showOfflineNotice()
    }
  })

  onUnmounted(() => {
    instanceCount--
    window.removeEventListener('offline', handleOffline)
    window.removeEventListener('online', handleOnline)
    // 最后一个实例卸载时清理通知
    if (instanceCount <= 0) {
      instanceCount = 0
      closeOfflineNotice()
    }
  })

  return { isOnline }
}
