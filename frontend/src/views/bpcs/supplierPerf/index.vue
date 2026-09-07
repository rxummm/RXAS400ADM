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
          <template #default="{ row }">{{ row.totalPo }}</template>
          </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.supplierPerf.onTimeRate')" width="100">
          <template #default="{ row }">{{ row.onTime }}</template>
          </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.supplierPerf.onTimeRate')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.score >= 80 ? 'success' : row.score >= 50 ? 'warning' : 'danger'" size="small">{{ row.score.toFixed(1) }}%</el-tag>
          </template>
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
import { useI18n } from 'vue-i18n'
import { listSupplierScores, type SupplierScoreVO } from '@/api/bpcs'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'
const { t } = useI18n()
const cono = ref('001')
const loading = ref(false)
const rows = ref<SupplierScoreVO[]>([])
const exportColumns: ExportColumn[] = [
  { key: 'vendorName', label: t('bpcs.common.vendorName') },
  { key: 'totalPo', label: t('bpcs.common.poCount') },
  { key: 'onTime', label: t('bpcs.common.onTimeCount') },
  { key: 'score', label: t('bpcs.common.onTimeRate') },
]
function load() {
  loading.value = true
  listSupplierScores(cono.value || '001', 50).then(d => { rows.value = d }).finally(() => { loading.value = false })
}
</script>
