<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input v-model="query.pono" class="w-150" :placeholder="$t('bpcs.purchase.pono')" clearable @keyup.enter="load" />
      <el-input v-model="query.vendor" class="w-150" :placeholder="$t('bpcs.purchase.vendor')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="rows"
        :columns="exportColumns"
        :title="$t('bpcs.menu.purchaseReceiving')"
        :export-url="BPCS_EXPORT.supplyChain('receiving')"
        :query-params="query"
      />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="pono" :label="$t('bpcs.purchase.pono')" min-width="140" />
        <el-table-column prop="vendorName" :label="$t('bpcs.purchase.vendor')" min-width="160" />
        <el-table-column prop="orderDate" :label="$t('bpcs.label.orderDate')" width="110" />
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" />
        <el-table-column prop="itemDesc" :label="$t('bpcs.line.itemDesc')" min-width="140" show-overflow-tooltip />
        <el-table-column align="right" :label="$t('bpcs.purchaseReceiving.orderQty')" width="80">
          <template #default="{ row }">{{ row.qtyOrdered }}</template>
          </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.purchaseReceiving.receivedQty')" width="80">
          <template #default="{ row }">{{ row.qtyReceived }}</template>
          </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.purchaseReceiving.openQty')" width="80">
          <template #default="{ row }"><span class="text-danger">{{ row.qtyOpen }}</span></template>
        </el-table-column>
        <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsPurchaseReceiving' })
import { reactive, ref } from 'vue'
import { fetchPurchaseReceiving, type PurchaseReceiving } from '@/api/supplyChain'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'
const loading = ref(false)
const rows = ref<PurchaseReceiving[]>([])
const query = reactive({ cono: '001', pono: '', vendor: '' })
const exportColumns: ExportColumn[] = [
  { key: 'pono', label: '采购单号' },
  { key: 'vendorName', label: '供应商' },
  { key: 'orderDate', label: '订单日期' },
  { key: 'item', label: '物料号' },
  { key: 'itemDesc', label: '物料描述' },
  { key: 'qtyOrdered', label: '已订购' },
  { key: 'qtyReceived', label: '已收货' },
  { key: 'qtyOpen', label: '未结量' },
]
function load() {
  loading.value = true
  const p: Record<string, string | number> = { cono: query.cono || '001', limit: 200 }
  if (query.pono) p.pono = query.pono
  if (query.vendor) p.vendor = query.vendor
  fetchPurchaseReceiving(p).then(d => { rows.value = d }).finally(() => { loading.value = false })
}
</script>
