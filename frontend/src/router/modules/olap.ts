/**
 * OLAP 多维分析模块路由
 */
export default [
  {
    path: 'olap-sales',
    name: 'OlapSales',
    component: () => import('@/views/olap/sales.vue'),
    meta: { title: 'menu.olapSales', cached: true }
  },
  {
    path: 'olap-inventory',
    name: 'OlapInventory',
    component: () => import('@/views/olap/inventory.vue'),
    meta: { title: 'menu.olapInventory', cached: true }
  },
  {
    path: 'olap-purchase',
    name: 'OlapPurchase',
    component: () => import('@/views/olap/purchase.vue'),
    meta: { title: 'menu.olapPurchase', cached: true }
  }
]
