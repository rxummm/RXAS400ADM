/**
 * 质量追溯模块路由
 */
export default [
  {
    path: 'quality-inspection',
    name: 'QualityInspection',
    component: () => import('@/views/quality/inspection/index.vue'),
    meta: { title: 'menu.qualityInspection', cached: true }
  },
  {
    path: 'quality-ncr',
    name: 'QualityNcr',
    component: () => import('@/views/quality/ncr/index.vue'),
    meta: { title: 'menu.qualityNcr', cached: true }
  },
  {
    path: 'quality-spc',
    name: 'QualitySpc',
    component: () => import('@/views/quality/spc/index.vue'),
    meta: { title: 'menu.qualitySpc', cached: true }
  },
  {
    path: 'quality-traceability',
    name: 'QualityTraceability',
    component: () => import('@/views/quality/traceability/index.vue'),
    meta: { title: 'menu.qualityTraceability', cached: true }
  }
]
