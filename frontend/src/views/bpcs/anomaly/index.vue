<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" clearable :placeholder="$t('bpcs.common.companyCode')" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
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
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsAnomaly' })

import { ref, onMounted } from 'vue'
import { detectAnomalies, type OrderAnomaly } from '@/api/bpcs'

const cono = ref('001')
const loading = ref(false)
const rows = ref<OrderAnomaly[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await detectAnomalies(cono.value)
  } finally {
    loading.value = false
  }
}

const reset = () => {
  cono.value = '001'
  load()
}

onMounted(load)
</script>
