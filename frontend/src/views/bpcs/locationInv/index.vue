<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="queryWarehouse" clearable :placeholder="$t('bpcs.wms.warehouse')" class="w-120" @keyup.enter="forceSearch" />
      <el-input v-model="queryBin" clearable :placeholder="$t('bpcs.location.binLocation')" class="w-130 ml8" @keyup.enter="forceSearch" />
      <el-input v-model="queryItem" clearable :placeholder="$t('bpcs.common.itemCode')" class="w-130 ml8" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch" class="ml8">{{ $t('common.search') }}</el-button>
      <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="warehouse" :label="$t('bpcs.wms.warehouse')" width="90" />
        <el-table-column prop="binLocation" :label="$t('bpcs.location.binLocation')" width="120" />
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="80" align="center" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsLocationInv' })

import { ref } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listLocations, type LocationInventory } from '@/api/bpcs'

const queryWarehouse = ref('')
const queryBin = ref('')
const queryItem = ref('')

const { pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<LocationInventory>({
  fetchApi: () => listLocations({ cono: '001', current: 1, size: 500 }),
  frontendPage: true,
  searchFields: ['warehouse', 'binLocation', 'item', 'description'],
  matchRow: (row: LocationInventory, _kw: string) => {
    const matchWh = !queryWarehouse.value || row.warehouse.toLowerCase().includes(queryWarehouse.value.toLowerCase())
    const matchBin = !queryBin.value || row.binLocation.toLowerCase().includes(queryBin.value.toLowerCase())
    const matchItem = !queryItem.value || row.item.toLowerCase().includes(queryItem.value.toLowerCase())
    return matchWh && matchBin && matchItem
  },
})

const handleReset = () => {
  queryWarehouse.value = ''
  queryBin.value = ''
  queryItem.value = ''
  forceSearch()
}
</script>
