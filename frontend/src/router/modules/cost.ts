/**
 * 成本核算模块路由
 */
export default [
  {
    path: 'cost-collection',
    name: 'CostCollection',
    component: () => import('@/views/cost/collection/index.vue'),
    meta: { title: 'menu.costCollection', cached: true }
  },
  {
    path: 'cost-variance',
    name: 'CostVariance',
    component: () => import('@/views/cost/variance/index.vue'),
    meta: { title: 'menu.costVariance', cached: true }
  }
]
