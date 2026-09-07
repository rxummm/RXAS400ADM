import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  as400Login as as400LoginApi,
  login as loginApi,
  getMenu,
  revokeToken,
  refreshAccessToken,
  type LoginResponse,
  type MenuNode,
  type MenuResponse,
  type TabItem as ApiTabItem,
} from '@/api/auth'
import { fetchTranslations } from '@/api/i18n'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'
import { clearTablePageCache } from '@/composables/useTablePage'
import { useAs400ServerStore } from '@/stores/as400Server'
import i18n from '@/i18n'

export type MenuItem = MenuNode

export type TabItem = ApiTabItem

interface UserState {
  token: string
  username: string
  permissions: string[]
  menus: MenuItem[]
  tabs: TabItem[]
}

const tokenStore = useStorage(STORAGE_KEYS.TOKEN)
const refreshTokenStore = useStorage(STORAGE_KEYS.REFRESH_TOKEN)
const tokenExpiryStore = useStorage(STORAGE_KEYS.TOKEN_EXPIRY)
const usernameStore = useStorage(STORAGE_KEYS.USERNAME)

/**
 * P3：菜单拉取并发去重。登录后 applyLogin 异步发起 fetchMenus，路由守卫/布局挂载
 * 会在 menus 尚未就绪时再次触发——这里把并发调用合并为同一个 Promise，避免登录时
 * 重复请求 /auth/menu（同一个 SPA 会话内只允许一个在途请求）。
 */
let menusFetchPromise: Promise<void> | null = null
let i18nFetchPromise: Promise<void> | null = null

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: tokenStore.get() || '',
    username: usernameStore.get() || '',
    permissions: [],
    menus: [],
    tabs: [],
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    hasPermission: (state) => (code: string) => state.permissions.includes(code),
    /**
     * Tab 可见性（menu_type=4 授权模型）：
     * - 未建模的 tab → 默认可见（true）
     * - 已建模但 status=0（停用）→ 对所有人隐藏（含 ADMIN，与页面菜单 status 语义一致）
     * - 已建模且 perms 为空 → 默认可见
     * - 已建模且有 perms → 需用户已获该权限码才可见（管理员可在菜单管理设置 perms 控制）
     */
    canSeeTab:
      (state) =>
      (page: string, tabTitle: string) => {
        const tab = state.tabs.find((t) => t.page === page && t.title === tabTitle)
        if (!tab) return true
        if (tab.status === 0) return false
        return !tab.perms || state.permissions.includes(tab.perms)
      },
  },

  actions: {
    async login(username: string, password: string) {
      const data: LoginResponse = await loginApi(username, password)
      this.applyLogin(data)
    },
    async as400Login(serverId: number, username: string, password: string) {
      const data: LoginResponse = await as400LoginApi(serverId, username, password)
      this.applyLogin(data)
    },
    applyLogin(data: LoginResponse) {
      this.token = data.token
      this.username = data.username
      this.permissions = data.permissions || []
      tokenStore.set(data.token)
      if (data.refreshToken) refreshTokenStore.set(data.refreshToken)
      if (data.expireMs) {
        tokenExpiryStore.set(String(Date.now() + data.expireMs))
      }
      usernameStore.set(data.username)
      void this.fetchMenus()
      void this.loadDbTranslations()
    },
    /** P2: 用 refresh token 换取新 access token，成功后更新 store */
    async tryRefreshToken(): Promise<boolean> {
      const rt = refreshTokenStore.get()
      if (!rt) return false
      try {
        const data = await refreshAccessToken(rt)
        this.token = data.token
        tokenStore.set(data.token)
        if (data.refreshToken) {
          refreshTokenStore.set(data.refreshToken)
        }
        // 更新过期时间（从后端获取 expireMs，或使用默认 24 小时）
        tokenExpiryStore.set(String(Date.now() + (data.expireMs || 86400000)))
        return true
      } catch {
        return false
      }
    },
    async fetchMenus() {
      if (menusFetchPromise) return menusFetchPromise
      menusFetchPromise = this.loadMenus().finally(() => {
        menusFetchPromise = null
      })
      return menusFetchPromise
    },
    async loadMenus() {
      try {
        const data: MenuResponse = await getMenu()
        this.menus = data?.menus || []
        // 菜单权限码（页面/按钮/Tab perms），供 hasPermission / v-has-perm / canSeeTab 判断
        this.permissions = data?.perms || []
        this.tabs = data?.tabs || []
      } catch {
        this.menus = []
        this.permissions = []
        this.tabs = []
      }
    },
    /** Load translations from DB (rx_i18n) and merge into i18n messages（并发去重，与 fetchMenus 同模式） */
    async loadDbTranslations() {
      if (i18nFetchPromise) return i18nFetchPromise
      i18nFetchPromise = this._doLoadDbTranslations().finally(() => {
        i18nFetchPromise = null
      })
      return i18nFetchPromise
    },
    async _doLoadDbTranslations() {
      try {
        const lang = i18n.global.locale.value as string
        const data = await fetchTranslations(lang)
        if (data && typeof data === 'object') {
          i18n.global.mergeLocaleMessage(lang, data)
        }
      } catch {
        // Admin users see warning to remind DB translation maintenance
        if (this.permissions.includes('I18N_MANAGE')) {
          ElMessage.warning(i18n.global.t('common.dbTranslationFailed'))
        }
      }
    },
    logout() {
      // P2-1：尽力把当前 token 加入服务端吊销名单（失败不影响本地登出）
      const token = this.token
      if (token) void revokeToken(token)
      this.token = ''
      this.username = ''
      this.permissions = []
      this.menus = []
      this.tabs = []
      tokenStore.remove()
      refreshTokenStore.remove()
      tokenExpiryStore.remove()
      usernameStore.remove()
      // P3-2：重置 AS400 服务器选择，避免登出后残留旧会话服务器（并清 X-AS400-Server 头来源）
      useAs400ServerStore().reset()
      // P2-25：清模块级表格缓存，避免同 SPA 会话内下一个用户读到前一用户列表数据
      clearTablePageCache()
    },
  },
})