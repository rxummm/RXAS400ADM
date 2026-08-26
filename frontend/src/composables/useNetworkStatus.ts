import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const isOnline = ref(navigator.onLine)
const wasOffline = ref(false)

export function useNetworkStatus() {
  const handleOnline = () => {
    if (!isOnline.value) {
      wasOffline.value = true
      ElMessage.success('网络已恢复连接')
      setTimeout(() => { wasOffline.value = false }, 3000)
    }
    isOnline.value = true
  }

  const handleOffline = () => {
    isOnline.value = false
    ElMessage.error('网络连接已断开')
  }

  onMounted(() => {
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
  })

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  })

  return { isOnline, wasOffline }
}
