<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input v-model="query.item" class="w-150" :placeholder="$t('bpcs.line.item')" clearable @keyup.enter="load" />
      <el-input v-model="query.fromDate" class="w-130" :placeholder="$t('bpcs.inventoryHistory.fromDate')" clearable @keyup.enter="load" />
      <el-input v-model="query.toDate" class="w-130" :placeholder="$t('bpcs.inventoryHistory.toDate')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="rows"
        :columns="exportColumns"
        :title="$t('bpcs.menu.inventoryHistory')"
        :export-url="BPCS_EXPORT.supplyChain('history')"
        :query-params="query"
      />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" />
        <el-table-column prop="warehouse" :label="$t('bpcs.label.wh')" width="80" />
        <el-table-column prop="type" :label="$t('bpcs.invoice.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'RCV' ? 'success' : row.type === 'ISS' ? 'danger' : 'info'" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventoryHistory.quantity')" width="90">
          <template #default="{ row }">{{ row.quantity }}</template>
          </el-table-column>
        <el-table-column prop="referenceNo" :label="$t('bpcs.inventoryHistory.refNo')" min-width="130" />
        <el-table-column prop="date" :label="$t('bpcs.inventoryHistory.dateRange')" width="100" />
        <el-table-column prop="time" :label="$t('bpcs.inventoryHistory.refType')" width="80" />
        <el-table-column prop="userId" :label="$t('bpcs.inventoryHistory.userId')" width="90" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsInventoryHistory' })
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getInventoryHistory, type InventoryHistory } from '@/api/bpcs'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'
const { t } = useI18n()
const loading = ref(false)
const rows = ref<InventoryHistory[]>([])
const query = reactive({ cono: '001', item: '', fromDate: '', toDate: '' })
const exportColumns: ExportColumn[] = [
  { key: 'item', label: t('bpcs.common.itemCode') },
  { key: 'warehouse', label: t('bpcs.common.warehouse') },
  { key: 'type', label: t('bpcs.common.transactionType') },
  { key: 'quantity', label: t('bpcs.common.quantity') },
  { key: 'referenceNo', label: t('bpcs.common.referenceNo') },
  { key: 'date', label: t('bpcs.common.date') },
]
function load() {
  loading.value = true
  const p: Record<string, string | number> = { cono: query.cono || '001', limit: 200 }
  if (query.item) p.item = query.item
  if (query.fromDate) p.fromDate = query.fromDate
  if (query.toDate) p.toDate = query.toDate
  getInventoryHistory(p).then(d => { rows.value = d }).finally(() => { loading.value = false })
}
</script>
