<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
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
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsSupplierScore' })

import { ref, onMounted } from 'vue'
import { listSupplierScores, type SupplierScoreVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<SupplierScoreVO[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listSupplierScores()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
