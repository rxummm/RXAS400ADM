import type { RouteRecordRaw } from 'vue-router'

export default [
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
    path: 'source',
    name: 'Source',
    component: () => import('@/views/Source.vue'),
    meta: { title: 'menu.source', cached: true, cacheName: 'SourceManager' },
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
    path: 'data-areas',
    name: 'DataAreas',
    component: () => import('@/views/data/dataAreas/index.vue'),
    meta: { title: 'menu.dataAreas', cached: true },
  },
  {
    path: 'as400/system-health',
    name: 'SystemHealth',
    component: () => import('@/views/as400/systemHealth/index.vue'),
    meta: { title: 'menu.systemHealth', cached: true },
  },
  {
    path: 'as400/backup-monitor',
    name: 'BackupMonitor',
    component: () => import('@/views/as400/backupMonitor/index.vue'),
    meta: { title: 'menu.backupMonitor', cached: true },
  },
  {
    path: 'as400/system-value-compliance',
    name: 'SystemValueCompliance',
    component: () => import('@/views/as400/systemValueCompliance/index.vue'),
    meta: { title: 'menu.systemValueCompliance', cached: true },
  },
  {
    path: 'as400/user-profiles',
    name: 'UserProfileManagement',
    component: () => import('@/views/as400/userProfiles/index.vue'),
    meta: { title: 'menu.userProfileManagement', cached: true },
  },
  {
    path: 'audit',
    name: 'Audit',
    component: () => import('@/views/audit/index.vue'),
    meta: { title: 'menu.audit', cached: true },
  },
] satisfies RouteRecordRaw[]
