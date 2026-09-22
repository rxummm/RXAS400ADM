import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'approval-center',
    name: 'ApprovalCenter',
    component: () => import('@/views/approval/index.vue'),
    meta: { title: 'menu.approvalCenter', cached: true },
  },
] satisfies RouteRecordRaw[]
