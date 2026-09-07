<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border show-summary :summary-method="getSummary">
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="wh" :label="$t('bpcs.wms.warehouse')" width="80" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="unitCost" :label="$t('bpcs.stockValue.unitCost')" width="110" align="right" />
        <el-table-column prop="stockValue" :label="$t('bpcs.stockValue.stockValue')" width="130" align="right">
          <template #default="{ row }">
            <span class="text-danger">{{ formatCurrency(row.stockValue) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsStockValue' })

import { ref, onMounted } from 'vue'
import { listStockValue, type StockValueVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<StockValueVO[]>([])

const formatCurrency = (v: number) => v != null ? v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0'

const getSummary = ({ columns, data }: { columns: { property?: string }[]; data: StockValueVO[] }) => {
  return columns.map((col: { property?: string }, idx: number) => {
    if (idx === 0) return 'Total'
    if (col.property === 'stockValue') return formatCurrency(data.reduce((s: number, r: StockValueVO) => s + (r.stockValue || 0), 0))
    if (col.property === 'qtyOnHand') return String(data.reduce((s: number, r: StockValueVO) => s + (r.qtyOnHand || 0), 0))
    return ''
  })
}

const load = async () => {
  loading.value = true
  try {
    rows.value = await listStockValue()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
