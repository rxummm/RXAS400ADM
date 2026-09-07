import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'reports',
    name: 'Reports',
    component: () => import('@/views/report/index.vue'),
    meta: { title: 'menu.reports', cached: true },
  },
  {
    path: 'report/builder',
    name: 'ReportBuilder',
    component: () => import('@/views/report/ReportBuilder.vue'),
    meta: { title: 'menu.reportBuilder', cached: true },
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
    path: 'mail/compose',
    name: 'Compose',
    component: () => import('@/views/mail/compose.vue'),
    meta: { title: 'menu.compose', cached: true },
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
] satisfies RouteRecordRaw[]
