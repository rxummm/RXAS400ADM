import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

const isOnline = ref(navigator.onLine)
const wasOffline = ref(false)

export function useNetworkStatus() {
  const { t } = useI18n()

  const handleOnline = () => {
    if (!isOnline.value) {
      wasOffline.value = true
      ElMessage.success(t('network.restored'))
      setTimeout(() => { wasOffline.value = false }, 3000)
    }
    isOnline.value = true
  }

  const handleOffline = () => {
    isOnline.value = false
    ElMessage.error(t('network.disconnected'))
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
