<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" :placeholder="$t('bpcs.label.cono')" clearable />
      <el-input v-model="cust" class="w-200" :placeholder="$t('bpcs.label.customer')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div v-if="overview" class="table-wrapper">
      <!-- 客户基础信息卡片 -->
      <el-card shadow="never" class="mb16">
        <template #header>
          <span class="font-bold">{{ overview.customerName }} ({{ overview.cust }})</span>
        </template>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item :label="$t('bpcs.customerOverview.address')">{{ overview.address }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.customerOverview.city')">{{ overview.city }}, {{ overview.state }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.customerOverview.phone')">{{ overview.phone }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.customerOverview.contact')">{{ overview.contact }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.customerOverview.terms')">{{ overview.termsCode }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.customerOverview.salesRep')">{{ overview.salesRep }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- KPI 卡片 -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ overview.totalOrders }}</div>
            <div class="text-muted">{{ $t('bpcs.customerOverview.totalOrders') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count text-warning">{{ overview.openOrders }}</div>
            <div class="text-muted">{{ $t('bpcs.customerOverview.openOrders') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ formatMoney(overview.totalRevenue) }}</div>
            <div class="text-muted">{{ $t('bpcs.customerOverview.totalRevenue') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ formatMoney(overview.creditLimit) }}</div>
            <div class="text-muted">{{ $t('bpcs.customerOverview.creditLimit') }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最近订单 -->
      <el-card shadow="never" class="mb16">
        <template #header>
          <span>{{ $t('bpcs.customerOverview.recentOrders') }}</span>
        </template>
        <el-table :data="overview.recentOrders" size="small" border>
          <el-table-column prop="orderNo" :label="$t('bpcs.label.orno')" width="130" />
          <el-table-column prop="orderDate" :label="$t('bpcs.label.orderDate')" width="110" />
          <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
          <el-table-column prop="lineCount" :label="$t('bpcs.label.lineCount')" width="80" align="right" />
          <el-table-column prop="orderTotal" :label="$t('bpcs.customerOverview.orderTotal')" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.orderTotal) }}</template>
          </el-table-column>
          <el-table-column prop="headerStatus" :label="$t('common.status')" width="80">
            <template #default="{ row }">
              <el-tag :type="(row.headerStatus === '8' ? 'success' : row.headerStatus === '0' ? 'info' : 'info') as 'success' | 'info'" size="small">
                {{ row.headerStatus === '8' ? $t('bpcs.stage.closed') : row.headerStatus === '0' ? $t('bpcs.stage.created') : row.headerStatus }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 逾期发票 -->
      <el-card shadow="never" v-if="overview.overdueInvoices.length > 0">
        <template #header>
          <span class="text-danger">{{ $t('bpcs.customerOverview.overdueInvoices') }} ({{ overview.overdueInvoices.length }})</span>
        </template>
        <el-table :data="overview.overdueInvoices" size="small" border>
          <el-table-column prop="invoiceNo" :label="$t('bpcs.orderDetail.invoiceNo')" width="130" />
          <el-table-column prop="invoiceDate" :label="$t('bpcs.customerOverview.invoiceDate')" width="110" />
          <el-table-column prop="invoiceAmount" :label="$t('bpcs.customerOverview.invoiceAmount')" width="120" align="right">
            <template #default="{ row }">
              <span class="text-danger font-bold">{{ formatMoney(row.invoiceAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderNo" :label="$t('bpcs.label.orno')" width="130" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsCustomerOverview' })

import { ref } from 'vue'
import { getCustomerOverview, type CustomerOverview } from '@/api/bpcs'

const cono = ref('001')
const cust = ref('')
const loading = ref(false)
const overview = ref<CustomerOverview | null>(null)

async function load() {
  if (!cust.value) return
  loading.value = true
  try {
    overview.value = await getCustomerOverview({ cono: cono.value, cust: cust.value })
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}

function formatMoney(val: number | null | undefined) {
  if (val == null) return '—'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'USD' }).format(val)
}
</script>
