/**
 * 统一 localStorage 管理（参照旧项目 composables/useStorage.js，TypeScript 化）
 * - 所有 key 集中在 STORAGE_KEYS，带 rxas400_ 命名空间，避免散落冲突
 * - Token 使用 Base64 + 异或混淆存储，避免明文泄露
 * - 提供安全 get/set/remove、命名空间 key 工厂、前缀遍历/批量清理
 */

export const STORAGE_KEYS = {
  TOKEN: 'rxas400_token',
  REFRESH_TOKEN: 'rxas400_refresh_token',
  TOKEN_EXPIRY: 'rxas400_token_expiry',
  USERNAME: 'rxas400_username',
  LOCALE: 'rxas400_locale',
  THEME: 'rxas400_theme',
  THEME_COLOR: 'rxas400_theme_color',
  TAGS: 'rxas400_tags',
  AS400_SERVER: 'rxas400_as400_server',
  NOTICE_READ: 'rxas400_notice_read',
} as const

/** Token 混淆密钥（仅防明文，非加密） */
const TOKEN_XOR_KEY = 0xa3

function encodeToken(raw: string): string {
  const bytes = new TextEncoder().encode(raw)
  const xored = bytes.map((b) => b ^ TOKEN_XOR_KEY)
  return btoa(String.fromCharCode(...xored))
}

function decodeToken(encoded: string): string {
  try {
    const xored = Uint8Array.from(atob(encoded), (c) => c.charCodeAt(0))
    const bytes = xored.map((b) => b ^ TOKEN_XOR_KEY)
    return new TextDecoder().decode(bytes)
  } catch {
    return encoded // 兼容旧版明文 token
  }
}

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key: string, value: string) {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* storage full / private mode */
  }
}

function safeRemove(key: string) {
  try {
    localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}

export interface StorageHandle {
  get(): string | null
  getJson<T = unknown>(fallback?: T): T | null
  set(value: string): void
  setJson(value: unknown): void
  remove(): void
}

/** 单一 key 的存储句柄（类型化） */
export function useStorage(key: string): StorageHandle {
  const get = (): string | null => {
    const raw = safeGet(key)
    if (raw === null) return null
    return key === STORAGE_KEYS.TOKEN ? decodeToken(raw) : raw
  }
  return {
    get,
    getJson<T = unknown>(fallback?: T): T | null {
      // P3：不依赖 this（解构 useStorage(...) 后取 getJson 也正确）
      const raw = get()
      if (raw === null || raw === '') return fallback ?? null
      try {
        return JSON.parse(raw) as T
      } catch {
        return fallback ?? null
      }
    },
    set(value: string) {
      if (key === STORAGE_KEYS.TOKEN && value) {
        safeSet(key, encodeToken(value))
      } else {
        safeSet(key, value)
      }
    },
    setJson(value: unknown) {
      safeSet(key, JSON.stringify(value))
    },
    remove() {
      safeRemove(key)
    },
  }
}

/** 命名空间 key 工厂：基 key + 动态后缀（如标签页按路径隔离） */
export function useNamespacedKey(baseKey: string, suffix: string): string {
  const safeSuffix = suffix.replace(/[^a-zA-Z0-9_\-:./]/g, '_')
  return `${baseKey}:${safeSuffix}`
}

/** 按前缀枚举所有 key（如清理某命名空间） */
export function getKeysByPrefix(prefix: string): string[] {
  const keys: string[] = []
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(prefix)) keys.push(k)
    }
  } catch {
    /* ignore */
  }
  return keys
}

/** 按前缀批量删除 */
export function removeKeysByPrefix(prefix: string) {
  getKeysByPrefix(prefix).forEach(safeRemove)
}
