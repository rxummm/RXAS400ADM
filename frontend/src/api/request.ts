import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAs400ServerStore } from '@/stores/as400Server'
import { useUserStore } from '@/stores/user'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'
import { getActivePinia } from 'pinia'
// P3：拦截器提示文案走全局 i18n（模块加载期不调用，仅回调运行时求值，无循环依赖）
import i18n from '@/i18n'

/** D2: 错误码到 i18n key 的映射（code -> common.error.XXX） */
const ERROR_CODE_I18N_MAP: Record<number, string> = {
  // 通用 HTTP 级
  400: 'common.error.BAD_REQUEST',
  401: 'common.error.UNAUTHORIZED',
  403: 'common.error.FORBIDDEN',
  404: 'common.error.NOT_FOUND',
  500: 'common.error.INTERNAL_ERROR',
  // 用户/角色 (10000+)
  10001: 'common.error.USER_NOT_FOUND',
  10002: 'common.error.USERNAME_EXISTS',
  10003: 'common.error.LOGIN_FAILED',
  10004: 'common.error.LOGIN_LOCKED',
  10005: 'common.error.LOGIN_TOO_MANY',
  // AS400 实例 (20000+)
  20001: 'common.error.AS400_SERVER_NOT_FOUND',
  20002: 'common.error.AS400_SERVER_DISABLED',
  20003: 'common.error.AS400_CONNECTION_FAILED',
  20004: 'common.error.AS400_COMMAND_FAILED',
  20005: 'common.error.AS400_SERVER_REQUIRED',
  20007: 'common.error.AS400_SQL_FAILED',
  20006: 'common.error.AS400_HOST_NOT_CONFIGURED',
  // 发布 (30000+)
  30001: 'common.error.DEPLOY_NOT_FOUND',
  30002: 'common.error.DEPLOY_STATUS_INVALID',
  30003: 'common.error.APPROVAL_NOT_FOUND',
  30004: 'common.error.DEPLOY_ROLLBACK_FAILED',
  30005: 'common.error.DEPLOY_CONCURRENT',
  // 监控/告警 (40000+)
  40001: 'common.error.MONITOR_COLLECT_FAILED',
  40002: 'common.error.ALERT_RULE_INVALID',
  40003: 'common.error.ALERT_RULE_NOT_FOUND',
  40004: 'common.error.ALERT_EVENT_NOT_FOUND',
  // 系统/管理 (50000+)
  50001: 'common.error.SYSTEM_USER_OPERATION',
  50002: 'common.error.SYSTEM_NOTICE_NOT_FOUND',
  50003: 'common.error.SYSTEM_REQUEST_NOT_FOUND',
  50004: 'common.error.SYSTEM_SCHEDULER_LOCKED',
  // 报表/SQL (60000+)
  60001: 'common.error.REPORT_GENERATE_FAILED',
  60002: 'common.error.SQL_READONLY_REQUIRED',
  // 编译 (70000+) 错误码已随 V56 下线删除，号段保留勿复用
  // 源文件 (80000+)
  80001: 'common.error.SOURCE_NOT_FOUND',
  80002: 'common.error.SOURCE_READ_FAILED',
  80003: 'common.error.FILE_PATH_INVALID',
  // 作业/子系统 (90000+)
  90001: 'common.error.JOB_NOT_FOUND',
  90002: 'common.error.JOB_INVALID_STATUS',
  90003: 'common.error.NAME_REQUIRED',
  // 角色 (110000+)
  110001: 'common.error.ROLE_NOT_FOUND',
  110002: 'common.error.ROLE_CODE_EXISTS',
  110003: 'common.error.ROLE_ADMIN_PROTECTED',
  // 密码/编译 (120000+)
  120001: 'common.error.PASSWORD_POLICY_VIOLATION',
}

/** D2: 根据错误码获取 i18n 文案，未命中则返回 null */
function resolveErrorI18n(code: number): string | null {
  const key = ERROR_CODE_I18N_MAP[code]
  if (!key) return null
  const msg = i18n.global.t(key)
  return msg !== key ? msg : null
}

const tokenStore = useStorage(STORAGE_KEYS.TOKEN)

/** API 基地址（vite 代理或独立后端），统一入口避免三处重复（request/blobClient/auth） */
export const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  // 后端 ApiResponse.success() 无参时 data 为 null（对齐后端泛型类），调用方需自行收窄
  data: T | null
}

const instance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

// 重复请求取消：同 method+url+params 的请求，只保留最后一次（列表页快速翻页/搜索防竞态）
// P2-23：noDedupe=true 的请求跳过取消逻辑（轮询/多调用方共享 URL 场景，避免互相 abort）
const pendingRequests = new Map<string, AbortController>()

/** 请求配置扩展：noDedupe=true 时跳过重复请求取消（轮询/多调用方共享 URL） */
export interface RequestConfig extends AxiosRequestConfig {
  noDedupe?: boolean
}

function buildRequestKey(config: AxiosRequestConfig): string {
  return `${config.method}:${config.url}:${JSON.stringify(config.params ?? '')}:${JSON.stringify(config.data ?? '')}`
}

function isCancelError(error: unknown): boolean {
  return axios.isCancel(error) || (error instanceof Error && (error.name === 'CanceledError' || error.message === 'ERR_CANCELED'))
}

instance.interceptors.request.use((config) => {
  // 统一走 useStorage（token 为混淆存储，读取时解码）
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // W2：透传前端语言，后端 Bean Validation 消息按 Accept-Language 本地化（zh-CN/en-US）
  config.headers['Accept-Language'] = String(i18n.global.locale.value)

  // 注入当前选择的 AS400 服务器（多服务器环境管理）
  try {
    const pinia = getActivePinia()
    if (pinia) {
      const as400Store = useAs400ServerStore()
      if (as400Store.currentServerId) {
        config.headers['X-AS400-Server'] = String(as400Store.currentServerId)
      }
    }
  } catch {
    /* Pinia 未初始化时忽略 */
  }

  // 仅取消幂等读请求（GET/HEAD）；写请求不取消，避免误伤提交
  // P2-23：noDedupe=true（轮询等）不参与取消
  if ((config.method === 'get' || config.method === 'head') && !(config as RequestConfig).noDedupe) {
    const key = buildRequestKey(config)
    const prev = pendingRequests.get(key)
    if (prev) {
      prev.abort()
      pendingRequests.delete(key)
    }
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(key, controller)
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    if (response.config) {
      pendingRequests.delete(buildRequestKey(response.config))
    }
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 0) {
      const i18nMsg = resolveErrorI18n(res.code)
      ElMessage.error(i18nMsg || res.message || i18n.global.t('common.requestFailed'))
      return Promise.reject(new Error(res.message))
    }
    // 解包：返回 AxiosResponse 形状但 data 已替换为业务数据（res.data），
    // 配合下方 instance.get<T>.then(r => r.data)，T 即解包后的业务数据类型
    return { ...response, data: res.data }
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.config) {
      pendingRequests.delete(buildRequestKey(error.config))
    }
    // 重复请求被主动取消：静默处理，不提示（调用方 finally 会复位 loading）。
    // 附加 noop catch 标记已处理，避免“Uncaught (in promise) CanceledError”控制台噪音。
    if (isCancelError(error)) {
      const rejection = Promise.reject(error)
      rejection.catch(() => {})
      return rejection
    }
    const status = error.response?.status
    if (status === 401) {
      // P2: 主动刷新机制已上线（useTokenRefresh），401 仅作为兜底
      // 清 token + 跳登录（useTokenRefresh 未覆盖的边界情况）
      const isLoginPage = router.currentRoute.value.path === '/login'
      try {
        const pinia = getActivePinia()
        if (pinia) useUserStore().logout()
        else tokenStore.remove()
      } catch {
        tokenStore.remove()
      }
      if (!isLoginPage) {
        router.push('/login')
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || i18n.global.t('common.networkError'))
    }
    return Promise.reject(error)
  },
)

/**
 * 创建带 noDedupe 标记的请求配置（P2-23）。
 * 轮询/多调用方共享 URL 场景下传 noDedupe=true，避免同 URL 请求互相 abort。
 *
 * @example
 * fetchOverview(id, { noDedupe: true })
 * fetchMetrics(id, { params: { limit: 50 }, noDedupe: true })
 */
export function withNoDedupe(config: AxiosRequestConfig = {}): RequestConfig {
  return { ...config, noDedupe: true }
}

/**
 * 响应拦截器已将 data 解包为业务数据（{ ...response, data: res.data }），
 * 这里用 axios 泛型 instance.get<T> + .then(r => r.data) 让 T 直接落到业务类型，
 * 不再需要 as unknown as 断言（2026-08-15 重构，消除断言链）。
 */
const request = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get<T>(url, config).then((r) => r.data)
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post<T>(url, data, config).then((r) => r.data)
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put<T>(url, data, config).then((r) => r.data)
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete<T>(url, config).then((r) => r.data)
  },
}

export default request