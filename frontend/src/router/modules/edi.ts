/**
 * EDI/AS2 集成模块路由
 */
export default [
  {
    path: 'edi-document',
    name: 'EdiDocument',
    component: () => import('@/views/edi/document/index.vue'),
    meta: { title: 'menu.ediDocument', cached: true }
  },
  {
    path: 'edi-partner',
    name: 'EdiPartner',
    component: () => import('@/views/edi/partner/index.vue'),
    meta: { title: 'menu.ediPartner', cached: true }
  }
]
