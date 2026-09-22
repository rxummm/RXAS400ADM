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
import operationRoutes from './modules/operation'
import procurementRoutes from './modules/procurement'
import financeRoutes from './modules/finance'
import approvalRoutes from './modules/approval'
import qualityRoutes from './modules/quality'
import costRoutes from './modules/cost'
import mrpRoutes from './modules/mrp'
import tpmRoutes from './modules/tpm'
import ediRoutes from './modules/edi'
import olapRoutes from './modules/olap'

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
        ...operationRoutes,
        ...procurementRoutes,
        ...financeRoutes,
        ...approvalRoutes,
        ...qualityRoutes,
        ...costRoutes,
        ...mrpRoutes,
        ...tpmRoutes,
        ...ediRoutes,
        ...olapRoutes,
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

router.beforeEach(async (to) => {
  NProgress.start()
  const userStore = useUserStore()
  if (!userStore.token) {
    if (to.path !== '/login') {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    return
  }
  if (to.path === '/login') {
    return { path: '/dashboard' }
  }
  if (!userStore.menus.length) {
    try {
      await userStore.fetchMenus()
    } catch {
      userStore.logout()
      return { path: '/login' }
    }
  }
  const allowed = collectMenuPaths(userStore.menus)
  if (!allowed.includes(to.path) && !to.meta.public) {
    ElMessage.error(i18n.global.t('common.noPermission'))
    return { path: '/dashboard' }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
