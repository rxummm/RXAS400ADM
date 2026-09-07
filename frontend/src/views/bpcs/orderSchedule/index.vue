<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-date-picker v-model="dateRange" type="daterange" class="w-240" :start-placeholder="$t('bpcs.orderSchedule.startDate')" :end-placeholder="$t('bpcs.orderSchedule.endDate')" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="startDate" :label="$t('bpcs.orderSchedule.startDate')" width="110" />
        <el-table-column prop="endDate" :label="$t('bpcs.orderSchedule.endDate')" width="110" />
        <el-table-column prop="progress" :label="$t('bpcs.orderSchedule.progress')" width="120" align="center">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :stroke-width="14" :text-inside="true" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderSchedule' })

import { ref } from 'vue'
import request from '@/api/request'

interface ScheduleRow {
  id: number
  cono: string
  orno: string
  cust: string
  startDate: string
  endDate: string
  progress: number
  priority: number
  createdBy: string
}

const dateRange = ref<[Date, Date] | null>(null)
const loading = ref(false)
const rows = ref<ScheduleRow[]>([])

async function load() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0].toISOString().slice(0, 10).replace(/-/g, '')
      params.endDate = dateRange.value[1].toISOString().slice(0, 10).replace(/-/g, '')
    }
    rows.value = await request.get<ScheduleRow[]>('/bpcs/orderSchedule', { params })
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}
</script>
