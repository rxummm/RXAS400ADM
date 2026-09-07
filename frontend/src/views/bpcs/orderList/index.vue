<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="search" />
      <el-input v-model="query.orno" class="w-150" :placeholder="$t('bpcs.label.orderNo')" clearable @keyup.enter="search" />
      <el-input v-model="query.cust" class="w-150" :placeholder="$t('bpcs.label.customer')" clearable @keyup.enter="search" />
      <el-input v-model="query.fromDate" class="w-130" :placeholder="$t('bpcs.orderList.fromDate')" clearable @keyup.enter="search" />
      <el-input v-model="query.toDate" class="w-130" :placeholder="$t('bpcs.orderList.toDate')" clearable @keyup.enter="search" />
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
      <ExportDropdown
        :data="orders"
        :columns="exportColumns"
        :title="$t('bpcs.menu.orderList')"
        :export-url="BPCS_EXPORT.supplyChain('orders')"
        :query-params="query"
      />
    </div>

    <div class="table-wrapper">
      <el-table :data="orders" v-loading="loading" size="small" border>
        <el-table-column prop="cono" :label="$t('bpcs.label.cono')" width="80" />
        <el-table-column prop="orno" :label="$t('bpcs.label.orderNo')" min-width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToOrder(row as BpcsOrderHeader)">{{ row.orno }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="customerNo" :label="$t('bpcs.label.customer')" width="90" />
        <el-table-column prop="orderDate" :label="$t('bpcs.label.orderDate')" width="110" />
        <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
        <el-table-column prop="lineCount" :label="$t('bpcs.sales.lineCount')" width="60" align="center" />
        <el-table-column :label="$t('bpcs.shipping.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="stageTagType(row.currentStageIndex)">{{ row.timeline.find((t: TimelineNode) => t.current)?.nameKey || row.currentStageIndex }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="raw.chsts" :label="$t('col.chsts')" width="80" />
      </el-table>
      <el-empty v-if="!loading && orders.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderList' })

import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { getOrderHeader, type BpcsOrderHeader, type TimelineNode } from '@/api/bpcs'
import { BPCS_EXPORT } from '@/api/bpcs'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const orders = ref<BpcsOrderHeader[]>([])
const query = reactive({ cono: '001', orno: '', cust: '', fromDate: '', toDate: '' })

const exportColumns: ExportColumn[] = [
  { key: 'cono', label: t('bpcs.common.companyCode') },
  { key: 'orno', label: t('bpcs.common.orderNo') },
  { key: 'custNo', label: t('bpcs.common.customerCode') },
  { key: 'custName', label: t('bpcs.common.customerName') },
  { key: 'orderDate', label: t('bpcs.common.orderDate') },
  { key: 'reqDate', label: t('bpcs.common.reqDate') },
  { key: 'lineCount', label: t('bpcs.common.lineCount') },
  { key: 'statusLabel', label: t('bpcs.common.status') },
]

function search() {
  if (!query.orno) return
  loading.value = true
  getOrderHeader(query.cono || '001', query.orno)
    .then(data => { orders.value = data ? [data] : [] })
    .finally(() => { loading.value = false })
}

function goToOrder(row: BpcsOrderHeader) {
  router.push({ path: '/bpcs-order', query: { cono: row.cono, orno: row.orno } })
}

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
function stageTagType(idx: number): TagType {
  if (idx >= 4) return 'info'
  if (idx >= 3) return 'success'
  if (idx >= 1) return 'primary'
  return 'info'
}
</script>
