<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="queryWarehouse" clearable :placeholder="$t('bpcs.wms.warehouse')" class="w-120" @keyup.enter="forceSearch" />
      <el-input v-model="queryItem" clearable :placeholder="$t('bpcs.common.itemCode')" class="w-130 ml8" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch" class="ml8">{{ $t('common.search') }}</el-button>
      <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="wh" :label="$t('bpcs.wms.warehouse')" width="80" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="safetyStock" :label="$t('bpcs.replenishment.safetyStock')" width="110" align="right" />
        <el-table-column prop="shortage" :label="$t('bpcs.replenishment.shortage')" width="100" align="right">
          <template #default="{ row }">
            <span class="text-danger">{{ row.shortage }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgDemand" :label="$t('bpcs.replenishment.avgDemand')" width="110" align="right" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsReplenishment' })

import { ref } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listReplenishment, type ReplenishmentVO } from '@/api/bpcs'

const queryWarehouse = ref('')
const queryItem = ref('')

const { pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<ReplenishmentVO>({
  fetchApi: () => listReplenishment({ cono: '001', current: 1, size: 500 }),
  frontendPage: true,
  searchFields: ['item', 'description', 'wh'],
  matchRow: (row: ReplenishmentVO, _kw: string) => {
    const matchWh = !queryWarehouse.value || row.wh.toLowerCase().includes(queryWarehouse.value.toLowerCase())
    const matchItem = !queryItem.value || row.item.toLowerCase().includes(queryItem.value.toLowerCase())
    return matchWh && matchItem
  },
})

const handleReset = () => {
  queryWarehouse.value = ''
  queryItem.value = ''
  forceSearch()
}
</script>
