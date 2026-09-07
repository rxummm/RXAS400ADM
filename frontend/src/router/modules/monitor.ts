import type { RouteRecordRaw } from 'vue-router'

export default [
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
    path: 'monitor/server-compare',
    name: 'ServerCompare',
    component: () => import('@/views/monitor/serverCompare/index.vue'),
    meta: { title: 'menu.serverCompare', cached: true },
  },
  {
    path: 'monitor/inspection',
    name: 'Inspection',
    component: () => import('@/views/monitor/inspection/index.vue'),
    meta: { title: 'menu.inspection', cached: true },
  },
  {
    path: 'monitor/metrics',
    name: 'Metrics',
    component: () => import('@/views/monitor/metrics/index.vue'),
    meta: { title: 'menu.metrics', cached: true },
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
] satisfies RouteRecordRaw[]
