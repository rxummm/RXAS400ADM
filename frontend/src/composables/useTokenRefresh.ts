/**
 * Token 主动刷新 composable：在 token 过期前 N 秒弹出确认弹窗，
 * 用户确认后刷新 token，取消或超时则跳登录页。
 *
 * 实现要点（零轮询方案）：
 * - 登录时存储 token 过期时间（当前时间 + expireMs）
 * - 计算精确触发时间：tokenExpiry - WARN_SECONDS
 * - 使用单次 setTimeout 定时，到点弹窗（零轮询，零资源消耗）
 * - 确认 → refresh → 重新计算 setTimeout
 * - 取消/超时 → 清 token → 跳登录
 * - 刷新成功后自动重置定时器
 */
import { ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import router from '@/router'
import i18n from '@/i18n'
import { useUserStore } from '@/stores/user'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'

/** 过期前多少秒弹窗 */
const WARN_SECONDS = 30

const tokenStore = useStorage(STORAGE_KEYS.TOKEN)
const tokenExpiryStore = useStorage(STORAGE_KEYS.TOKEN_EXPIRY)

/** 全局状态：是否正在显示刷新弹窗（防止重复弹出） */
const isShowing = ref(false)

/** 主定时器 ID（setTimeout 返回值） */
let refreshTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 启动 token 主动刷新定时器。
 * 计算精确触发时间，单次 setTimeout 到点弹窗。
 * 在 layout/index.vue 的 onMounted 中调用。
 */
export function startTokenRefreshTimer() {
  stopTokenRefreshTimer()
  scheduleNext()
}

/**
 * 计算下一次弹窗的精确时间并设置 setTimeout。
 * 核心逻辑：delayMs = tokenExpiry - now - WARN_SECONDS * 1000
 */
function scheduleNext() {
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }

  if (!tokenStore.get()) return // 未登录，不设置定时器

  const expiryStr = tokenExpiryStore.get()
  if (!expiryStr) return // 无过期时间，不设置定时器
  const expiry = Number(expiryStr)
  if (isNaN(expiry)) return

  const now = Date.now()
  const triggerAt = expiry - WARN_SECONDS * 1000 // 过期前 30 秒触发
  const delayMs = triggerAt - now

  if (delayMs <= 0) {
    // 已经过了触发时间（可能 token 快过期了），立即弹窗或处理过期
    if (now >= expiry) {
      // token 已过期
      handleExpired()
    } else {
      // token 还没过期但已不足 30 秒，立即弹窗
      showRefreshDialog()
    }
    return
  }

  // 设置精确延时（最多 2^31 - 1 ≈ 24.8 天，24 小时的 token 完全够用）
  refreshTimer = setTimeout(() => {
    refreshTimer = null
    if (!tokenStore.get()) return // 未登录（可能已登出）
    if (isShowing.value) return // 弹窗已显示

    const currentExpiry = Number(tokenExpiryStore.get() || '0')
    if (Date.now() >= currentExpiry) {
      handleExpired()
    } else {
      showRefreshDialog()
    }
  }, delayMs)
}

/** 停止 token 主动刷新定时器 */
export function stopTokenRefreshTimer() {
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
}

/**
 * 显示刷新确认弹窗（阻塞式）
 */
async function showRefreshDialog() {
  if (isShowing.value) return
  isShowing.value = true

  const { t } = i18n.global

  try {
    await ElMessageBox.confirm(
      t('tokenRefresh.message', { seconds: WARN_SECONDS }),
      t('tokenRefresh.title'),
      {
        confirmButtonText: t('tokenRefresh.confirm'),
        cancelButtonText: t('tokenRefresh.cancel'),
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        type: 'warning',
        distinguishCancelAndClose: true,
      },
    )

    // 用户点确认 → 刷新 token
    await refreshAndContinue()
  } catch (action) {
    if (action === 'cancel' || action === 'close' || action === undefined) {
      // 用户点取消 / 右上角 X / 超时 → 跳登录
      handleLogout()
    } else {
      handleLogout()
    }
  } finally {
    isShowing.value = false
  }
}

/**
 * 刷新 token 并继续
 */
async function refreshAndContinue() {
  const { t } = i18n.global

  try {
    const userStore = useUserStore()
    const success = await userStore.tryRefreshToken()

    if (success) {
      // 刷新成功 → 重新计算定时器（tryRefreshToken 已更新 tokenExpiryStore）
      scheduleNext()
    } else {
      // Refresh token 也过期了
      ElMessage.error(t('tokenRefresh.refreshFailed'))
      handleLogout()
    }
  } catch {
    ElMessage.error(t('tokenRefresh.refreshFailed'))
    handleLogout()
  }
}

/**
 * 处理 token 已过期（直接跳登录）
 */
function handleExpired() {
  const { t } = i18n.global
  ElMessage.warning(t('tokenRefresh.expired'))
  handleLogout()
}

/**
 * 处理登出（清 token + 跳登录）
 */
function handleLogout() {
  try {
    const userStore = useUserStore()
    userStore.logout()
  } catch {
    tokenStore.remove()
    tokenExpiryStore.remove()
  }
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 导出 isShowing 供外部检测（可选）
export function isTokenRefreshDialogShowing() {
  return isShowing.value
}
