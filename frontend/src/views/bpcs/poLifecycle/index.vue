<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="poNo" :label="$t('bpcs.po.poNo')" width="140" />
        <el-table-column prop="vendor" :label="$t('bpcs.po.vendorCode')" width="100" />
        <el-table-column prop="vendorName" :label="$t('bpcs.po.vendorName')" min-width="160" />
        <el-table-column prop="orderDate" :label="$t('bpcs.po.orderDate')" width="100" />
        <el-table-column prop="receivedDate" :label="$t('bpcs.po.receivedDate')" width="100" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CLOSED' ? 'success' : row.onHold ? 'danger' : 'primary'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lineCount" :label="$t('bpcs.po.lineCount')" width="90" align="center" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsPoLifecycle' })

import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listPoLifecycle, type PoLifecycleVO } from '@/api/bpcs'

const { pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<PoLifecycleVO>({
  fetchApi: () => listPoLifecycle({}),
  frontendPage: true,
  searchFields: ['poNo', 'vendor', 'vendorName', 'status'],
})
</script>
