/**
 * MRP 物料需求计划模块路由
 */
export default [
  {
    path: 'mrp-bom',
    name: 'MrpBom',
    component: () => import('@/views/mrp/bom/index.vue'),
    meta: { title: 'menu.mrpBom', cached: true }
  },
  {
    path: 'mrp-demand',
    name: 'MrpDemand',
    component: () => import('@/views/mrp/demand/index.vue'),
    meta: { title: 'menu.mrpDemand', cached: true }
  },
  {
    path: 'mrp-recommendation',
    name: 'MrpRecommendation',
    component: () => import('@/views/mrp/recommendation/index.vue'),
    meta: { title: 'menu.mrpRecommendation', cached: true }
  }
]
