<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input v-model="query.item" class="w-150" :placeholder="$t('bpcs.line.item')" clearable @keyup.enter="load" />
      <el-input v-model="query.fromDate" class="w-130" :placeholder="$t('bpcs.inventoryHistory.fromDate')" clearable @keyup.enter="load" />
      <el-input v-model="query.toDate" class="w-130" :placeholder="$t('bpcs.inventoryHistory.toDate')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
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
import { fetchInventoryHistory, type InventoryHistory } from '@/api/supplyChain'
const loading = ref(false)
const rows = ref<InventoryHistory[]>([])
const query = reactive({ cono: '001', item: '', fromDate: '', toDate: '' })
function load() {
  loading.value = true
  const p: Record<string, string | number> = { cono: query.cono || '001', limit: 200 }
  if (query.item) p.item = query.item
  if (query.fromDate) p.fromDate = query.fromDate
  if (query.toDate) p.toDate = query.toDate
  fetchInventoryHistory(p).then(d => { rows.value = d }).finally(() => { loading.value = false })
}
</script>
