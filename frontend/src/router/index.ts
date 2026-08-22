import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import { useUserStore, type MenuItem } from '@/stores/user'
import Layout from '@/layout/index.vue'
import i18n from '@/i18n'

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
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: 'menu.dashboard' },
        },
        {
          path: 'monitor',
          name: 'Monitor',
          component: () => import('@/views/Monitor.vue'),
          meta: { title: 'menu.monitor', cached: true },
        },
        {
          path: 'monitor/alert-rules',
          name: 'AlertRules',
          component: () => import('@/views/monitor/alertRules/index.vue'),
          meta: { title: 'menu.alertRules', cached: true },
        },
        {
          path: 'jobs',
          name: 'Jobs',
          component: () => import('@/views/job/index.vue'),
          meta: { title: 'menu.jobs', cached: true },
        },
        {
          path: 'query',
          name: 'Query',
          component: () => import('@/views/query/index.vue'),
          meta: { title: 'menu.query', cached: true },
        },
        {
          path: 'assets',
          name: 'Assets',
          component: () => import('@/views/assets/index.vue'),
          meta: { title: 'menu.assets', cached: true },
        },
        {
          path: 'objects',
          name: 'Objects',
          component: () => import('@/views/objects/index.vue'),
          meta: { title: 'menu.objects', cached: true },
        },
        {
          path: 'schedules',
          name: 'Schedules',
          component: () => import('@/views/schedule/index.vue'),
          meta: { title: 'menu.schedules', cached: true },
        },
        {
          path: 'scripts',
          name: 'Scripts',
          component: () => import('@/views/scripts/index.vue'),
          meta: { title: 'menu.scripts', cached: true },
        },
        {
          path: 'ifs',
          name: 'Ifs',
          component: () => import('@/views/ifs/index.vue'),
          meta: { title: 'menu.ifs', cached: true },
        },
        {
          path: 'subsystems',
          name: 'Subsystems',
          component: () => import('@/views/subsystems/index.vue'),
          meta: { title: 'menu.subsystems', cached: true },
        },
        {
          path: 'executions',
          name: 'Executions',
          component: () => import('@/views/executions/index.vue'),
          meta: { title: 'menu.executions', cached: true },
        },
        {
          path: 'pf',
          name: 'Pf',
          component: () => import('@/views/pf/index.vue'),
          meta: { title: 'menu.pf', cached: true },
        },
        {
          path: 'audit',
          name: 'Audit',
          component: () => import('@/views/audit/index.vue'),
          meta: { title: 'menu.audit', cached: true },
        },
        {
          path: 'health',
          name: 'Health',
          component: () => import('@/views/health/index.vue'),
          meta: { title: 'menu.health', cached: true },
        },
        {
          path: 'topology',
          name: 'Topology',
          component: () => import('@/views/topology/index.vue'),
          meta: { title: 'menu.topology', cached: true },
        },
        {
          path: 'table-fields',
          name: 'TableFields',
          component: () => import('@/views/data/tableFields/index.vue'),
          meta: { title: 'menu.tableFields', cached: true },
        },
        {
          path: 'message-files',
          name: 'MessageFiles',
          component: () => import('@/views/data/messageFiles/index.vue'),
          meta: { title: 'menu.messageFiles', cached: true },
        },
        {
          path: 'sysvals',
          name: 'Sysvals',
          component: () => import('@/views/data/sysvals/index.vue'),
          meta: { title: 'menu.sysvals', cached: true },
        },
        {
          path: 'server-compare',
          name: 'ServerCompare',
          component: () => import('@/views/monitor/serverCompare/index.vue'),
          meta: { title: 'menu.serverCompare', cached: true },
        },
        {
          path: 'inspection',
          name: 'Inspection',
          component: () => import('@/views/monitor/inspection/index.vue'),
          meta: { title: 'menu.inspection', cached: true },
        },
        {
          path: 'metrics',
          name: 'Metrics',
          component: () => import('@/views/monitor/metrics/index.vue'),
          meta: { title: 'menu.metrics', cached: true },
        },
        {
          path: 'job-sla',
          name: 'JobSla',
          component: () => import('@/views/job/sla/index.vue'),
          meta: { title: 'menu.jobSla', cached: true },
        },
        {
          path: 'job-dependency',
          name: 'JobDependency',
          component: () => import('@/views/job/dependency/index.vue'),
          meta: { title: 'menu.jobDependency', cached: true },
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('@/views/report/index.vue'),
          meta: { title: 'menu.reports', cached: true },
        },
        {
          path: 'docs',
          name: 'Docs',
          component: () => import('@/views/docs/index.vue'),
          meta: { title: 'menu.docs', cached: true },
        },
        {
          path: 'flowcharts',
          name: 'Flowcharts',
          component: () => import('@/views/flowcharts/index.vue'),
          meta: { title: 'menu.flowcharts', cached: true },
        },
        {
          path: 'source',
          name: 'Source',
          component: () => import('@/views/Source.vue'),
          meta: { title: 'menu.source', cached: true, cacheName: 'SourceManager' },
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/system/Users.vue'),
          meta: { title: 'menu.users', cached: true },
        },
        {
          path: 'roles',
          name: 'Roles',
          component: () => import('@/views/system/roles/index.vue'),
          meta: { title: 'menu.roles', cached: true },
        },
        {
          path: 'menus',
          name: 'Menus',
          component: () => import('@/views/system/menus/index.vue'),
          meta: { title: 'menu.menus', cached: true },
        },
        {
          path: 'system/config',
          name: 'SysConfig',
          component: () => import('@/views/system/config/index.vue'),
          meta: { title: 'menu.config', cached: true },
        },
        {
          path: 'system/i18n',
          name: 'SysI18n',
          component: () => import('@/views/system/i18n/index.vue'),
          meta: { title: 'menu.i18n', cached: true },
        },
        {
          path: 'system/login-log',
          name: 'LoginLog',
          component: () => import('@/views/system/loginLog/index.vue'),
          meta: { title: 'menu.loginLog', cached: true },
        },
        {
          path: 'system/cache',
          name: 'SysCache',
          component: () => import('@/views/system/cache/index.vue'),
          meta: { title: 'menu.cache', cached: true },
        },
        {
          path: 'system/tasks',
          name: 'SysTasks',
          component: () => import('@/views/system/tasks/index.vue'),
          meta: { title: 'menu.tasks', cached: true },
        },
        {
          path: 'system/ip-rules',
          name: 'IpRules',
          component: () => import('@/views/system/ipRules/index.vue'),
          meta: { title: 'menu.ipRules', cached: true },
        },
        {
          path: 'system/webhooks',
          name: 'Webhooks',
          component: () => import('@/views/system/webhooks/index.vue'),
          meta: { title: 'menu.webhooks', cached: true },
        },
        {
          path: 'system/permission-request',
          name: 'PermissionRequest',
          component: () => import('@/views/system/permissionRequest/index.vue'),
          meta: { title: 'menu.permissionRequest', cached: true },
        },
        {
          path: 'system/notices',
          name: 'Notice',
          component: () => import('@/views/system/notice/index.vue'),
          meta: { title: 'menu.notices', cached: true },
        },
        {
          path: 'system/notifications',
          name: 'Notifications',
          component: () => import('@/views/system/notifications/index.vue'),
          meta: { title: 'menu.notifications', cached: true },
        },
        {
          path: 'system/permissions',
          name: 'SysPermissions',
          component: () => import('@/views/system/permissions/index.vue'),
          // P3-1：cacheName 与组件 defineOptions.name('Permissions') 对齐，否则 keep-alive 缓存静默失效
          meta: { title: 'menu.permissions', cached: true, cacheName: 'Permissions' },
        },
        {
          path: 'system/dict',
          name: 'SysDict',
          component: () => import('@/views/system/dict/index.vue'),
          // P2-24：cacheName 与组件 defineOptions.name('DictManage') 对齐，否则 keep-alive 缓存静默失效
          meta: { title: 'menu.dict', cached: true, cacheName: 'DictManage' },
        },
        {
          path: 'system/params',
          name: 'SysParams',
          component: () => import('@/views/system/params/index.vue'),
          // P2-24：cacheName 与组件 defineOptions.name('Params') 对齐
          meta: { title: 'menu.params', cached: true, cacheName: 'Params' },
        },
        {
          path: 'region',
          name: 'Region',
          component: () => import('@/views/tool/region/index.vue'),
          meta: { title: 'menu.region', cached: true, cacheName: 'RegionManage' },
        },
        {
          path: 'calendar',
          name: 'Calendar',
          component: () => import('@/views/calendar/index.vue'),
          meta: { title: 'menu.calendar', cached: true },
        },
        {
          path: 'biz-data',
          name: 'BizData',
          component: () => import('@/views/biz/data/index.vue'),
          meta: { title: 'menu.bizData', cached: true },
        },
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