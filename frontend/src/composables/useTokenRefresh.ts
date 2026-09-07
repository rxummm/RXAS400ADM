import { useStorage, STORAGE_KEYS } from './useStorage'
import { refreshAccessToken } from '@/api/auth'

const tokenStore = useStorage(STORAGE_KEYS.TOKEN)
const refreshTokenStore = useStorage(STORAGE_KEYS.REFRESH_TOKEN)
const tokenExpiryStore = useStorage(STORAGE_KEYS.TOKEN_EXPIRY)

let refreshTimer: ReturnType<typeof setTimeout> | null = null

/**
 * P3：跨 Tab Token 同步。Tab A 刷新 Token 后通过 BroadcastChannel 通知其他 Tab
 * 重新读取 localStorage 中的最新 Token，避免旧 Token 导致 401 循环。
 */
const TOKEN_CHANNEL = 'rxas400-token-sync'
let tokenChannel: BroadcastChannel | null = null

function getChannel(): BroadcastChannel | null {
  if (typeof BroadcastChannel === 'undefined') return null
  if (!tokenChannel) {
    tokenChannel = new BroadcastChannel(TOKEN_CHANNEL)
    tokenChannel.onmessage = (ev) => {
      if (ev.data === 'token-updated') {
        // 其他 Tab 刷新了 Token，重新读取本地 storage 同步 Pinia 状态
        syncTokenFromStorage()
      }
    }
  }
  return tokenChannel
}

function syncTokenFromStorage() {
  const newToken = tokenStore.get()
  const newRefresh = refreshTokenStore.get()
  const newExpiry = tokenExpiryStore.get()
  // 通知 Pinia store 更新（延迟导入避免循环依赖）
  import('@/stores/user').then(({ useUserStore }) => {
    const userStore = useUserStore()
    if (newToken) userStore.$patch({ token: newToken })
    // 重新调度刷新定时器
    scheduleRefresh()
  })
}

function broadcastTokenUpdate() {
  getChannel()?.postMessage('token-updated')
}

function getExpiryMs(): number | null {
  const raw = tokenExpiryStore.get()
  if (!raw) return null
  const ms = Number(raw)
  return Number.isFinite(ms) ? ms : null
}

function scheduleRefresh() {
  clearRefreshTimer()

  const expiryMs = getExpiryMs()
  if (!expiryMs) return

  const now = Date.now()
  const bufferMs = 5 * 60 * 1000
  const delay = expiryMs - now - bufferMs

  if (delay <= 0) {
    void doRefresh()
    return
  }

  refreshTimer = setTimeout(() => {
    void doRefresh()
  }, Math.min(delay, 2147483647))
}

async function doRefresh() {
  const refreshToken = refreshTokenStore.get()
  if (!refreshToken) return

  try {
    const res = await refreshAccessToken(refreshToken)
    tokenStore.set(res.token)
    refreshTokenStore.set(res.refreshToken)
    if (res.expireMs) {
      tokenExpiryStore.set(String(res.expireMs))
    }
    broadcastTokenUpdate()
    scheduleRefresh()
  } catch {
    // Refresh failed — will be caught by 401 interceptor on next request
  }
}

function clearRefreshTimer() {
  if (refreshTimer !== null) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
}

export function startTokenRefreshTimer() {
  scheduleRefresh()
}

export function stopTokenRefreshTimer() {
  clearRefreshTimer()
  tokenChannel?.close()
  tokenChannel = null
}
