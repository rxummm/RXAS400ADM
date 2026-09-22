/**
 * TPM 设备管理模块路由
 */
export default [
  {
    path: 'tpm-equipment',
    name: 'TpmEquipment',
    component: () => import('@/views/tpm/equipment/index.vue'),
    meta: { title: 'menu.tpmEquipment', cached: true }
  },
  {
    path: 'tpm-maintenance',
    name: 'TpmMaintenance',
    component: () => import('@/views/tpm/maintenance/index.vue'),
    meta: { title: 'menu.tpmMaintenance', cached: true }
  },
  {
    path: 'tpm-oee',
    name: 'TpmOee',
    component: () => import('@/views/tpm/oee/index.vue'),
    meta: { title: 'menu.tpmOee', cached: true }
  }
]
