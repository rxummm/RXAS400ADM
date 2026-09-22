<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" clearable :placeholder="$t('bpcs.common.companyCode')" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="orderDate" :label="$t('bpcs.common.orderDate')" width="100" />
        <el-table-column prop="reqDate" :label="$t('bpcs.common.reqDate')" width="100" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'HOLD' ? 'danger' : 'success'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="backorderLines" :label="$t('bpcs.analytics.backorderLines')" width="110" align="center" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsAnomaly' })

import { ref } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { detectAnomalies, type OrderAnomaly } from '@/api/bpcs'

const cono = ref('001')

const { pagedData, loading, total, current, size, forceSearch, resetSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<OrderAnomaly>({
  fetchApi: () => detectAnomalies({ cono: cono.value }),
  frontendPage: true,
  searchFields: ['orno', 'cust', 'custName', 'status'],
})
</script>
