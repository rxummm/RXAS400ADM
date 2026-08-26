import { useStorage, STORAGE_KEYS } from './useStorage'
import { refreshAccessToken } from '@/api/auth'

const tokenStore = useStorage(STORAGE_KEYS.TOKEN)
const refreshTokenStore = useStorage(STORAGE_KEYS.REFRESH_TOKEN)
const tokenExpiryStore = useStorage(STORAGE_KEYS.TOKEN_EXPIRY)

let refreshTimer: ReturnType<typeof setTimeout> | null = null

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
}
