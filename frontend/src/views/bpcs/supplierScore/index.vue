<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="vendor" :label="$t('bpcs.supplier.vendorCode')" width="100" />
        <el-table-column prop="vendorName" :label="$t('bpcs.supplier.vendorName')" min-width="160" />
        <el-table-column prop="totalPo" :label="$t('bpcs.supplier.totalPo')" width="90" align="center" />
        <el-table-column prop="onTime" :label="$t('bpcs.supplier.onTime')" width="90" align="center" />
        <el-table-column prop="score" :label="$t('bpcs.supplier.score')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.score >= 90 ? 'success' : row.score >= 70 ? 'warning' : 'danger'" size="small">{{ row.score }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsSupplierScore' })

import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listSupplierScores, type SupplierScoreVO } from '@/api/bpcs'

const { pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<SupplierScoreVO>({
  fetchApi: () => listSupplierScores({}),
  frontendPage: true,
  searchFields: ['vendor', 'vendorName'],
})
</script>
