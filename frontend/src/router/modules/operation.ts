import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'operations',
    name: 'Operations',
    component: () => import('@/views/operation/index.vue'),
    meta: { title: 'menu.operation', cached: true },
  },
] satisfies RouteRecordRaw[]