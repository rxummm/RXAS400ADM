<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="rows"
        :columns="exportColumns"
        :title="$t('bpcs.menu.supplierPerf')"
        :export-url="BPCS_EXPORT.supplyChain('supplier')"
        :query-params="{ cono }"
      />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="vendorName" :label="$t('bpcs.purchase.vendor')" min-width="180" />
        <el-table-column align="center" :label="$t('bpcs.supplierPerf.totalOrders')" width="100">
          <template #default="{ row }">{{ row.poCount }}</template>
          </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.supplierPerf.onTimeRate')" width="100">
          <template #default="{ row }">{{ row.onTimeCount }}</template>
          </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.supplierPerf.onTimeRate')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.onTimeRate >= 80 ? 'success' : row.onTimeRate >= 50 ? 'warning' : 'danger'" size="small">{{ row.onTimeRate.toFixed(1) }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.supplierPerf.avgPrice')" width="120">
          <template #default="{ row }">{{ row.avgPrice.toFixed(2) }}</template>
          </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsSupplierPerf' })
import { ref } from 'vue'
import { fetchSupplierPerformance, type SupplierPerf } from '@/api/supplyChain'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'
const cono = ref('001')
const loading = ref(false)
const rows = ref<SupplierPerf[]>([])
const exportColumns: ExportColumn[] = [
  { key: 'vendorName', label: '供应商名称' },
  { key: 'poCount', label: '采购单数' },
  { key: 'onTimeCount', label: '准时交付数' },
  { key: 'onTimeRate', label: '准时交付率%' },
  { key: 'avgPrice', label: '平均单价' },
]
function load() {
  loading.value = true
  fetchSupplierPerformance({ cono: cono.value || '001', limit: 50 }).then(d => { rows.value = d }).finally(() => { loading.value = false })
}
</script>
