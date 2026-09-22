import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'ar/invoice',
    name: 'FinanceAr',
    component: () => import('@/views/finance/ar/index.vue'),
    meta: { title: 'menu.arInvoice', cached: true },
  },
] satisfies RouteRecordRaw[]
