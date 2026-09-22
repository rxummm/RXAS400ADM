import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'procurement-po',
    name: 'ProcurementPo',
    component: () => import('@/views/procurement/po/index.vue'),
    meta: { title: 'menu.procurementPo', cached: true },
  },
  {
    path: 'procurement-approval',
    name: 'ProcurementApproval',
    component: () => import('@/views/procurement/approval/index.vue'),
    meta: { title: 'menu.procurementApproval', cached: true },
  },
] satisfies RouteRecordRaw[]