<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="cono"
        class="w-120"
        :placeholder="$t('bpcs.label.cono')"
        clearable
        @keyup.enter="load"
      />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <!-- 履行率卡片 -->
    <div class="table-wrapper">
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ fulfillmentStats?.totalLines ?? '—' }}</div>
            <div class="text-muted">{{ $t('bpcs.analytics.totalLines') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count" :class="fulfillmentStats && fulfillmentStats.lineFillRate >= 80 ? 'text-success' : 'text-danger'">
              {{ fulfillmentStats?.lineFillRate != null ? fulfillmentStats.lineFillRate + '%' : '—' }}
            </div>
            <div class="text-muted">{{ $t('bpcs.analytics.lineFillRate') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count" :class="fulfillmentStats && fulfillmentStats.qtyFillRate >= 80 ? 'text-success' : 'text-danger'">
              {{ fulfillmentStats?.qtyFillRate != null ? fulfillmentStats.qtyFillRate + '%' : '—' }}
            </div>
            <div class="text-muted">{{ $t('bpcs.analytics.qtyFillRate') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count text-danger">{{ fulfillmentStats?.backorderLines ?? '—' }}</div>
            <div class="text-muted">{{ $t('bpcs.analytics.backorderLines') }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- OTD 卡片 -->
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ otdStats?.totalDelivered ?? '—' }}</div>
            <div class="text-muted">{{ $t('bpcs.analytics.totalDelivered') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count" :class="otdStats && otdStats.onTimeRate >= 90 ? 'text-success' : 'text-danger'">
              {{ otdStats?.onTimeRate != null ? otdStats.onTimeRate + '%' : '—' }}
            </div>
            <div class="text-muted">{{ $t('bpcs.analytics.onTimeRate') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count">{{ otdStats?.early ?? '—' }}</div>
            <div class="text-muted">{{ $t('bpcs.analytics.earlyDelivery') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="count text-danger">{{ otdStats?.late ?? '—' }}</div>
            <div class="text-muted">{{ $t('bpcs.analytics.lateDelivery') }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Backorder 表格 -->
      <el-tabs v-model="activeTab" class="mb16">
        <el-tab-pane :label="$t('bpcs.analytics.backorderDetail')" name="backorder">
          <el-table :data="backorderLines" v-loading="loading" size="small" border>
            <el-table-column prop="orno" :label="$t('bpcs.label.orno')" min-width="120" />
            <el-table-column prop="orln" :label="$t('bpcs.label.orln')" width="80" />
            <el-table-column prop="item" :label="$t('bpcs.label.item')" min-width="120" />
            <el-table-column prop="itemDesc" :label="$t('bpcs.label.itemDesc')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="qtyOrdered" :label="$t('bpcs.analytics.ordered')" width="90" align="right" />
            <el-table-column prop="qtyShipped" :label="$t('bpcs.analytics.shipped')" width="90" align="right" />
            <el-table-column prop="qtyOpen" :label="$t('bpcs.analytics.open')" width="90" align="right">
              <template #default="{ row }">
                <span class="text-danger font-bold">{{ row.qtyOpen }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerNo" :label="$t('bpcs.label.customer')" width="100" />
            <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('bpcs.analytics.backorderByItem')" name="byItem">
          <el-table :data="backorderByItem" v-loading="loading" size="small" border>
            <el-table-column prop="item" :label="$t('bpcs.label.item')" min-width="120" />
            <el-table-column prop="itemDesc" :label="$t('bpcs.label.itemDesc')" min-width="160" show-overflow-tooltip />
            <el-table-column prop="backorderCount" :label="$t('bpcs.analytics.boCount')" width="120" align="right" />
            <el-table-column prop="totalBackorderQty" :label="$t('bpcs.analytics.boQty')" width="120" align="right">
              <template #default="{ row }">
                <span class="text-danger font-bold">{{ row.totalBackorderQty }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('bpcs.analytics.otdByCustomer')" name="otdCustomer">
          <el-table :data="otdByCustomer" v-loading="loading" size="small" border>
            <el-table-column prop="customerNo" :label="$t('bpcs.label.customer')" width="100" />
            <el-table-column prop="customerName" :label="$t('bpcs.label.customerName')" min-width="160" />
            <el-table-column prop="totalOrders" :label="$t('bpcs.analytics.totalOrders')" width="100" align="right" />
            <el-table-column prop="onTimeOrders" :label="$t('bpcs.analytics.onTimeOrders')" width="100" align="right" />
            <el-table-column prop="otdRate" :label="$t('bpcs.analytics.otdRate')" width="100" align="right">
              <template #default="{ row }">
                <span :class="row.otdRate >= 90 ? 'text-success' : 'text-danger'">
                  {{ row.otdRate }}%
                </span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'OrderAnalytics' })

import { ref, onMounted } from 'vue'
import {
  getFulfillmentStats,
  getBackorderLines,
  getBackorderByItem,
  getOtdStats,
  getOtdByCustomer,
  type FulfillmentStats,
  type BackorderLine,
  type BackorderByItem,
  type OtdStats,
  type OtdByCustomer
} from '@/api/bpcs'

const cono = ref('001')
const loading = ref(false)
const activeTab = ref('backorder')

const fulfillmentStats = ref<FulfillmentStats | null>(null)
const backorderLines = ref<BackorderLine[]>([])
const backorderByItem = ref<BackorderByItem[]>([])
const otdStats = ref<OtdStats | null>(null)
const otdByCustomer = ref<OtdByCustomer[]>([])

async function load() {
  loading.value = true
  try {
    const [fStats, boLines, boItems, oStats, oCustomers] = await Promise.all([
      getFulfillmentStats(cono.value),
      getBackorderLines({ cono: cono.value, size: 50 }),
      getBackorderByItem(cono.value, 20),
      getOtdStats(cono.value),
      getOtdByCustomer(cono.value, 20)
    ])
    fulfillmentStats.value = fStats
    backorderLines.value = boLines
    backorderByItem.value = boItems
    otdStats.value = oStats
    otdByCustomer.value = oCustomers
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
