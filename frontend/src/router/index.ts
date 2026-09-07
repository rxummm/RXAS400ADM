import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import { useUserStore, type MenuItem } from '@/stores/user'
import Layout from '@/layout/index.vue'
import i18n from '@/i18n'

import monitorRoutes from './modules/monitor'
import jobRoutes from './modules/job'
import as400Routes from './modules/as400'
import bpcsRoutes from './modules/bpcs'
import systemRoutes from './modules/system'
import toolRoutes from './modules/tool'

NProgress.configure({
  showSpinner: false,
  speed: 400,
  minimum: 0.15,
})

/** 拍平授权菜单树，收集全部已授权路径 */
function collectMenuPaths(menus: MenuItem[]): string[] {
  const paths: string[] = []
  const walk = (list: MenuItem[]) => {
    for (const m of list || []) {
      if (m?.path) paths.push(m.path)
      if (m?.children?.length) walk(m.children)
    }
  }
  walk(menus)
  return paths
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: 'login.title' },
    },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        ...monitorRoutes,
        ...jobRoutes,
        ...as400Routes,
        ...bpcsRoutes,
        ...systemRoutes,
        ...toolRoutes,
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue'),
      meta: { title: 'common.notFound' },
    },
  ],
})

// F7：受保护路径集仅依赖静态路由表，模块级缓存一次，避免每次导航重复重建
const PROTECTED_PATHS = new Set(
  router.getRoutes()
    .filter(r => r.path.startsWith('/') && r.path !== '/' && r.components?.default)
    .map(r => r.path)
)

/**
 * Phase 3b：从后端菜单树收集 path → { cached, cacheName } 映射，
 * 合并到前端路由 meta，使 TagsView / keep-alive 可读取后端配置。
 * 首次 fetchMenus 后执行一次，后续导航复用缓存。
 */
let menuMetaMerged = false
function mergeMenuMeta(menus: MenuItem[]) {
  if (menuMetaMerged) return
  const metaMap = new Map<string, { cached?: boolean; cacheName?: string }>()
  const walk = (list: MenuItem[]) => {
    for (const m of list || []) {
      if (m?.path) {
        metaMap.set(m.path, { cached: m.cached, cacheName: m.cacheName })
      }
      if (m?.children?.length) walk(m.children)
    }
  }
  walk(menus)
  for (const route of router.getRoutes()) {
    const override = metaMap.get(route.path)
    if (override) {
      if (override.cached !== undefined) {
        route.meta.cached = override.cached
      }
      if (override.cacheName) {
        route.meta.cacheName = override.cacheName
      }
    }
  }
  menuMetaMerged = true
}

router.beforeEach(async (to) => {
  NProgress.start()
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.isLoggedIn) {
    return { path: '/login' }
  }
  if (to.path === '/login' && userStore.isLoggedIn) {
    return { path: '/' }
  }
  // 页面级访问控制：动态收集 Layout 子路由作为受保护路径集，替代硬编码 MENU_PATHS
  if (userStore.isLoggedIn && PROTECTED_PATHS.has(to.path)) {
    if (!userStore.menus.length) {
      try {
        await userStore.fetchMenus()
      } catch {
        /* 菜单加载失败（如 token 失效）：放行交由 401 拦截器处理 */
      }
    }
    // 确保 DB 翻译已加载，避免布局组件渲染时 $t() 解析到 key 原文触发 console warning
    try {
      await userStore.loadDbTranslations()
    } catch {
      /* 翻译加载失败不阻断导航 */
    }
    // Phase 3b：合并后端菜单 cached/cacheName 到路由 meta
    if (userStore.menus.length) {
      mergeMenuMeta(userStore.menus)
    }
    const menuPaths = collectMenuPaths(userStore.menus)
    const authorized = new Set(menuPaths)
    if (!authorized.has(to.path)) {
      ElMessage.warning(i18n.global.t('common.noPermission'))
      // P2-22：不要硬跳 /dashboard——若用户也无该页权限会形成死循环；
      // 改跳第一个有权限的菜单；一个都没有则放行当前页（页面自行展示空态/接口 403）
      const fallback = menuPaths.find((p) => p !== to.path)
      return fallback ? { path: fallback } : undefined
    }
  }
  const titleKey = to.meta.title as string | undefined
  document.title = titleKey
    ? `RXAS400 - ${i18n.global.t(titleKey)}`
    : 'RXAS400'
})

router.afterEach(() => {
  NProgress.done()
})

export default router
