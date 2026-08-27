<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="search" />
      <el-input v-model="query.orno" class="w-150" :placeholder="$t('bpcs.label.orderNo')" clearable @keyup.enter="search" />
      <el-input v-model="query.cust" class="w-150" :placeholder="$t('bpcs.label.customer')" clearable @keyup.enter="search" />
      <el-input v-model="query.fromDate" class="w-130" :placeholder="$t('bpcs.orderList.fromDate')" clearable @keyup.enter="search" />
      <el-input v-model="query.toDate" class="w-130" :placeholder="$t('bpcs.orderList.toDate')" clearable @keyup.enter="search" />
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="orders" v-loading="loading" size="small" border>
        <el-table-column prop="cono" :label="$t('bpcs.label.cono')" width="80" />
        <el-table-column prop="orno" :label="$t('bpcs.label.orderNo')" min-width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToOrder(row as OrderListItem)">{{ row.orno }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="custNo" :label="$t('bpcs.label.customer')" width="90" />
        <el-table-column prop="custName" :label="$t('bpcs.customer.shipName')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderDate" :label="$t('bpcs.label.orderDate')" width="110" />
        <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
        <el-table-column prop="lineCount" :label="$t('bpcs.sales.lineCount')" width="60" align="center" />
        <el-table-column :label="$t('bpcs.shipping.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="stageTagType(row.currentStageIndex)">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rawChsts" label="CHSTS" width="80" />
      </el-table>
      <el-empty v-if="!loading && orders.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderList' })

import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchOrders, type OrderListItem } from '@/api/supplyChain'

const router = useRouter()
const loading = ref(false)
const orders = ref<OrderListItem[]>([])
const query = reactive({ cono: '001', orno: '', cust: '', fromDate: '', toDate: '' })

function search() {
  loading.value = true
  const params: Record<string, string | number> = { cono: query.cono || '001' }
  if (query.orno) params.orno = query.orno
  if (query.cust) params.cust = query.cust
  if (query.fromDate) params.fromDate = query.fromDate
  if (query.toDate) params.toDate = query.toDate
  searchOrders(params)
    .then(data => { orders.value = data })
    .finally(() => { loading.value = false })
}

function goToOrder(row: OrderListItem) {
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
