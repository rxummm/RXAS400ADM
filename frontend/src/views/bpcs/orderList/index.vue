<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="forceSearch" />
      <el-input v-model="query.orno" class="w-150" :placeholder="$t('bpcs.label.orderNo')" clearable @keyup.enter="forceSearch" />
      <el-input v-model="query.cust" class="w-150" :placeholder="$t('bpcs.label.customer')" clearable @keyup.enter="forceSearch" />
      <el-date-picker v-model="query.fromDate" type="date" class="w-140" :placeholder="$t('bpcs.orderList.fromDate')" value-format="YYYYMMDD" />
      <el-date-picker v-model="query.toDate" type="date" class="w-140" :placeholder="$t('bpcs.orderList.toDate')" value-format="YYYYMMDD" />
      <el-button type="primary" :loading="loading" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="tableData"
        :columns="exportColumns"
        :title="$t('bpcs.menu.orderList')"
        :export-url="BPCS_EXPORT.supplyChain('orders')"
        :query-params="query"
      />
    </div>

    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="cono" :label="$t('bpcs.label.cono')" width="80" />
        <el-table-column prop="orno" :label="$t('bpcs.label.orderNo')" min-width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToOrder(row as OrderRow)">{{ row.orno }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="custNo" :label="$t('bpcs.label.customer')" width="90" />
        <el-table-column prop="orderDate" :label="$t('bpcs.label.orderDate')" width="110" />
        <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
        <el-table-column prop="lineCount" :label="$t('bpcs.sales.lineCount')" width="60" align="center" />
        <el-table-column prop="status" :label="$t('bpcs.shipping.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chsts" :label="$t('col.chsts')" width="80" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
      <el-empty v-if="!loading && tableData.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderList' })

import { reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { searchOrderList, BPCS_EXPORT } from '@/api/bpcs'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'

interface OrderRow {
  cono: string
  orno: string
  custNo: string
  orderDate: string
  reqDate: string
  lineCount: number
  status: string
  chsts: string
}

const router = useRouter()
const { t } = useI18n()
const query = reactive({ cono: '001', orno: '', cust: '', fromDate: '', toDate: '' })

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<OrderRow>({
  fetchApi: (params) => {
    const p: Record<string, unknown> = { cono: query.cono || '001', current: params.current!, size: params.size! }
    if (query.orno) p.orno = query.orno
    if (query.cust) p.cust = query.cust
    if (query.fromDate) p.fromDate = query.fromDate
    if (query.toDate) p.toDate = query.toDate
    return searchOrderList(p) as Promise<{ records: OrderRow[]; total: number }>
  },
})

const exportColumns = computed<ExportColumn[]>(() => [
  { key: 'cono', label: t('bpcs.common.companyCode') },
  { key: 'orno', label: t('bpcs.common.orderNo') },
  { key: 'custNo', label: t('bpcs.common.customerCode') },
  { key: 'orderDate', label: t('bpcs.common.orderDate') },
  { key: 'reqDate', label: t('bpcs.common.reqDate') },
  { key: 'lineCount', label: t('bpcs.common.lineCount') },
  { key: 'status', label: t('bpcs.common.status') },
])

function goToOrder(row: OrderRow) {
  router.push({ path: '/bpcs-order', query: { cono: row.cono, orno: row.orno } })
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function statusTagType(status: string): TagType {
  if (status === 'SHIPPED' || status === 'CLOSED') return 'success'
  if (status === 'HELD') return 'warning'
  if (status === 'PARTIAL') return 'primary'
  return 'info'
}
</script>
