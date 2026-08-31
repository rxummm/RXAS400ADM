<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-120" :placeholder="$t('bpcs.label.cono')" clearable />
      <el-input v-model="orno" class="w-200" :placeholder="$t('bpcs.label.orno')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div v-if="loaded" class="table-wrapper">
      <!-- 订单行增强详情表格 -->
      <el-table :data="lineDetails" v-loading="loading" size="small" border class="mb16">
        <el-table-column prop="orln" :label="$t('bpcs.label.orln')" width="70" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" min-width="120" />
        <el-table-column prop="itemDesc" :label="$t('bpcs.label.itemDesc')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="qtyOrdered" :label="$t('bpcs.analytics.ordered')" width="80" align="right" />
        <el-table-column prop="qtyAllocated" :label="$t('bpcs.analytics.allocated')" width="80" align="right" />
        <el-table-column prop="qtyShipped" :label="$t('bpcs.analytics.shipped')" width="80" align="right" />
        <el-table-column prop="qtyInvoiced" :label="$t('bpcs.analytics.invoiced')" width="80" align="right" />
        <el-table-column :label="$t('bpcs.label.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.statusLabel)" size="small">
              {{ $t('bpcs.orderDetail.status.' + row.statusLabel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loadNo" :label="$t('bpcs.orderDetail.loadNo')" width="100" />
        <el-table-column prop="invoiceNo" :label="$t('bpcs.orderDetail.invoiceNo')" width="110" />
      </el-table>

      <!-- 时间线 -->
      <el-card shadow="never" v-if="timeline.length > 0">
        <template #header>
          <span>{{ $t('bpcs.orderDetail.timeline') }}</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="event in timeline"
            :key="event.eventType + event.eventDate"
            :timestamp="event.eventDate"
            :type="timelineType(event.eventType)"
            placement="top"
          >
            {{ event.eventDesc }}
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'OrderDetailEnhanced' })

import { ref } from 'vue'
import { getOrderLineDetails, getOrderTimeline, type OrderLineDetail, type OrderTimelineEvent } from '@/api/bpcs'

const cono = ref('001')
const orno = ref('')
const loading = ref(false)
const loaded = ref(false)
const lineDetails = ref<OrderLineDetail[]>([])
const timeline = ref<OrderTimelineEvent[]>([])

async function load() {
  if (!orno.value) return
  loading.value = true
  try {
    const [lines, events] = await Promise.all([
      getOrderLineDetails(cono.value, orno.value),
      getOrderTimeline(cono.value, orno.value)
    ])
    lineDetails.value = lines
    timeline.value = events
    loaded.value = true
  } catch {
    /* interceptor handles error */
  } finally {
    loading.value = false
  }
}

function statusType(label: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    INVOICED: 'success', SHIPPED: 'success', PARTIAL_SHIP: 'warning',
    ALLOCATED: 'info', ORDERED: 'info', PENDING: 'info'
  }
  return map[label] || 'info'
}

function timelineType(type: string) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    CREATED: 'primary', SHIPPED: 'success', INVOICED: 'warning'
  }
  return map[type] || 'info'
}
</script>
