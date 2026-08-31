<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="never">
            <div class="section">{{ $t('bpcs.transport.totalShipments') }}</div>
            <div class="count">{{ summary.totalShipments }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="section">{{ $t('bpcs.transport.onTimeRate') }}</div>
            <div class="count text-success">{{ summary.onTimeRate }}%</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="section">{{ $t('bpcs.transport.avgTransit') }}</div>
            <div class="count">{{ summary.avgTransitDays }} {{ $t('common.unit.days') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never">
            <div class="section">{{ $t('bpcs.transport.costPerOrder') }}</div>
            <div class="count">${{ summary.costPerOrder }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.transport.weeklyShipments') }}</template>
            <div ref="barChartRef" class="w-full" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.transport.statusBreakdown') }}</template>
            <div ref="pieChartRef" class="w-full" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16" class="mt16">
        <el-col :span="24">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.transport.costTrend') }}</template>
            <div ref="lineChartRef" class="w-full" style="height: 300px"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { listShipments, type ShipmentVO } from '@/api/bpcs'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(false)
const rows = ref<ShipmentVO[]>([])
const summary = reactive({
  totalShipments: 0,
  onTimeRate: 0,
  avgTransitDays: 0,
  costPerOrder: 0,
})

const barChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const lineChartRef = ref<HTMLDivElement>()

const barChart = useECharts(barChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
  },
  yAxis: { type: 'value', name: t('bpcs.transport.shipments') },
  series: [{
    type: 'bar',
    data: [32, 28, 35, 40, 38, 15, 8],
    itemStyle: { borderRadius: [4, 4, 0, 0] },
  }],
}))

const pieChart = useECharts(pieChartRef, (): ECOption => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}: {c} ({d}%)' },
    data: [
      { value: 120, name: t('bpcs.transport.delivered') },
      { value: 18, name: t('bpcs.transport.inTransit') },
      { value: 8, name: t('bpcs.transport.delayed') },
      { value: 10, name: t('bpcs.transport.pending') },
    ],
  }],
}))

const lineChart = useECharts(lineChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: {
    type: 'category',
    data: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
  },
  yAxis: { type: 'value', name: '$' },
  series: [
    {
      name: t('bpcs.transport.avgCost'),
      type: 'line',
      smooth: true,
      data: [12.5, 11.8, 13.2, 12.0, 11.5, 12.5],
      areaStyle: { opacity: 0.15 },
    },
    {
      name: t('bpcs.transport.fuelSurcharge'),
      type: 'line',
      smooth: true,
      data: [2.1, 2.3, 2.5, 2.2, 1.9, 2.0],
    },
  ],
}))

const load = async () => {
  loading.value = true
  try {
    rows.value = await listShipments('001', 200) as unknown as ShipmentVO[]
    summary.totalShipments = rows.value.length
    summary.onTimeRate = 92.3
    summary.avgTransitDays = 3.2
    summary.costPerOrder = 12.5
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
