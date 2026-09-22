<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="forceSearch" />
      <el-button type="primary" :loading="loading" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="originData"
        :columns="exportColumns"
        :title="$t('bpcs.menu.abcAnalysis')"
        :export-url="BPCS_EXPORT.supplyChain('abc')"
        :query-params="{ cono }"
      />
    </div>
    <div v-if="originData.length" class="summary-cards mb16">
      <div class="summary-card summary-card--a"><div class="summary-value">{{ classCount('A') }}</div><div class="summary-label">{{ $t('bpcs.abcAnalysis.classA') }}</div></div>
      <div class="summary-card summary-card--b"><div class="summary-value">{{ classCount('B') }}</div><div class="summary-label">{{ $t('bpcs.abcAnalysis.classB') }}</div></div>
      <div class="summary-card summary-card--c"><div class="summary-value">{{ classCount('C') }}</div><div class="summary-label">{{ $t('bpcs.abcAnalysis.classC') }}</div></div>
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" />
        <el-table-column prop="itemDesc" :label="$t('bpcs.line.itemDesc')" min-width="140" show-overflow-tooltip />
        <el-table-column align="right" :label="$t('bpcs.inventoryHistory.quantity')" width="80">
          <template #default="{ row }">{{ row.totalQty }}</template>
          </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.abcAnalysis.totalValue')" width="120">
          <template #default="{ row }">{{ row.stockValue.toFixed(0) }}</template>
          </el-table-column>
        <el-table-column :label="$t('bpcs.abcAnalysis.classA')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.abcClass === 'A' ? 'danger' : row.abcClass === 'B' ? 'warning' : 'info'" size="small" effect="dark">{{ row.abcClass }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsAbcAnalysis' })
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { getAbcXyzMatrix, type AbcXyzItem } from '@/api/bpcs'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'
const { t } = useI18n()
const cono = ref('001')
const { originData, pagedData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<AbcXyzItem>({
  fetchApi: () => getAbcXyzMatrix({ cono: cono.value || '001', current: 1, size: 500 }),
  frontendPage: true,
  searchFields: ['item', 'itemDesc', 'abcClass'],
})
const exportColumns: ExportColumn[] = [
  { key: 'item', label: t('bpcs.common.itemCode') },
  { key: 'itemDesc', label: t('bpcs.common.description') },
  { key: 'totalQty', label: t('bpcs.common.quantity') },
  { key: 'stockValue', label: t('bpcs.inventory.stockValue') },
  { key: 'abcClass', label: t('bpcs.common.category') },
]
const classCount = (c: string) => originData.value.filter(i => i.abcClass === c).length
</script>
<style scoped>
.summary-cards { display: flex; gap: 16px; }
.summary-card { flex: 1; background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 16px; text-align: center; }
.summary-card--a { border-color: var(--color-danger); background: var(--el-color-danger-light-9); }
.summary-card--b { border-color: var(--color-warning); background: var(--el-color-warning-light-9); }
.summary-card--c { border-color: var(--color-info); background: var(--el-color-info-light-9); }
.summary-card--a .summary-value { color: var(--color-danger); }
.summary-card--b .summary-value { color: var(--color-warning); }
.summary-card--c .summary-value { color: var(--color-info); }
</style>
