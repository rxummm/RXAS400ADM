<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="orderDate" :label="$t('bpcs.common.orderDate')" width="100" />
        <el-table-column prop="crHold" :label="$t('bpcs.credit.creditHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.crHold === 'Y' ? 'danger' : 'success'" size="small">{{ row.crHold }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shipHold" :label="$t('bpcs.credit.shipHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.shipHold === 'Y' ? 'danger' : 'success'" size="small">{{ row.shipHold }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="prHold" :label="$t('bpcs.credit.priceHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.prHold === 'Y' ? 'warning' : 'success'" size="small">{{ row.prHold }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsCreditHold' })

import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listCreditHolds, type CreditHoldVO } from '@/api/bpcs'

const { pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<CreditHoldVO>({
  fetchApi: () => listCreditHolds({}),
  frontendPage: true,
  searchFields: ['orno', 'cust', 'custName'],
})
</script>
