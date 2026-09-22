<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-date-picker v-model="dateRange" type="daterange" class="w-360" :start-placeholder="$t('bpcs.orderSchedule.startDate')" :end-placeholder="$t('bpcs.orderSchedule.endDate')" value-format="YYYYMMDD" />
      <el-button type="primary" @click="forceSearch" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="startDate" :label="$t('bpcs.orderSchedule.startDate')" width="110" />
        <el-table-column prop="endDate" :label="$t('bpcs.orderSchedule.endDate')" width="110" />
        <el-table-column prop="progress" :label="$t('bpcs.orderSchedule.progress')" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="14" :text-inside="true" />
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderSchedule' })

import { ref } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listOrderSchedule, type OrderScheduleVO } from '@/api/bpcs'

const dateRange = ref<[string, string] | null>(null)

const { tableData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<OrderScheduleVO>({
  fetchApi: (params) => {
    const qp: Record<string, unknown> = { current: params.current, size: params.size }
    if (dateRange.value) {
      qp.startDate = dateRange.value[0]
      qp.endDate = dateRange.value[1]
    }
    return listOrderSchedule(qp)
  },
  frontendPage: false,
})
</script>
