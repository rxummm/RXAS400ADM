<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input-number v-model="topN" :min="3" :max="20" size="small" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
    </div>

    <!-- 汇总卡片 -->
    <div v-if="analysis" class="summary-cards mb16">
      <div class="summary-card">
        <div class="summary-value">{{ fmtMoney(analysis.totalRevenue) }}</div>
        <div class="summary-label">{{ $t('bpcs.salesAnalysis.totalRevenue') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ analysis.totalOrders }}</div>
        <div class="summary-label">{{ $t('bpcs.salesAnalysis.totalOrders') }}</div>
      </div>
    </div>

    <div class="table-wrapper">
      <el-tabs v-model="activeTab">
        <!-- Top 客户 -->
        <el-tab-pane :label="$t('bpcs.salesAnalysis.topCustomers')" name="customers">
          <el-table :data="analysis?.topCustomers || []" v-loading="loading" size="small" border>
            <el-table-column type="index" width="50" :label="$t('common.index')" />
            <el-table-column prop="code" :label="$t('bpcs.label.customer')" width="100" />
            <el-table-column prop="name" :label="$t('bpcs.customer.shipName')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="orderCount" :label="$t('bpcs.salesAnalysis.orderCount')" width="100" align="center" />
            <el-table-column align="right" :label="$t('bpcs.salesAnalysis.totalAmount')" width="140">
              <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Top 物料 -->
        <el-tab-pane :label="$t('bpcs.salesAnalysis.topItems')" name="items">
          <el-table :data="analysis?.topItems || []" v-loading="loading" size="small" border>
            <el-table-column type="index" width="50" :label="$t('common.index')" />
            <el-table-column prop="code" :label="$t('bpcs.line.item')" min-width="120" />
            <el-table-column prop="name" :label="$t('bpcs.line.itemDesc')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="orderCount" :label="$t('bpcs.salesAnalysis.totalQty')" width="100" align="center" />
            <el-table-column align="right" :label="$t('bpcs.salesAnalysis.totalAmount')" width="140">
              <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <el-empty v-if="!loading && !analysis" :description="$t('common.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsSalesAnalysis' })

import { ref } from 'vue'
import { fetchSalesAnalysis, type SalesAnalysis } from '@/api/supplyChain'
import { formatMoney } from '@/utils/format'

const cono = ref('001')
const topN = ref(10)
const loading = ref(false)
const analysis = ref<SalesAnalysis | null>(null)
const activeTab = ref('customers')

function load() {
  loading.value = true
  fetchSalesAnalysis({ cono: cono.value || '001', topN: topN.value })
    .then(data => { analysis.value = data })
    .finally(() => { loading.value = false })
}

const fmtMoney = (v: number | null | undefined) => formatMoney(v, 0)
</script>

<style scoped>
.summary-cards {
  display: flex;
  gap: 16px;
}
.summary-card {
  flex: 1;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
