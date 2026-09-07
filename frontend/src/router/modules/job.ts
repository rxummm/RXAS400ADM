import type { RouteRecordRaw } from 'vue-router'

export default [
  {
    path: 'jobs',
    name: 'Jobs',
    component: () => import('@/views/job/index.vue'),
    meta: { title: 'menu.jobs', cached: true },
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
] satisfies RouteRecordRaw[]
