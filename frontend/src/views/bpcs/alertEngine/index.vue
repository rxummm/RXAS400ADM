<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-row :gutter="16" class="mb16">
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.alert.gauge') }}</template>
            <div ref="gaugeChartRef" class="w-full" style="height: 280px"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.alert.distribution') }}</template>
            <div ref="barChartRef" class="w-full" style="height: 280px"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.alert.trend') }}</template>
            <div ref="lineChartRef" class="w-full" style="height: 280px"></div>
          </el-card>
        </el-col>
      </el-row>
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="wh" :label="$t('bpcs.wms.warehouse')" width="80" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="safetyStock" :label="$t('bpcs.alert.safetyStock')" width="110" align="right" />
        <el-table-column prop="maxStock" :label="$t('bpcs.alert.maxStock')" width="100" align="right" />
        <el-table-column prop="alertType" :label="$t('bpcs.alert.type')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.alertType === 'LOW_STOCK' ? 'danger' : 'warning'" size="small">{{ row.alertType }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { listAlertRules, type AlertRuleVO } from '@/api/bpcs'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(false)
const rows = ref<AlertRuleVO[]>([])

const gaugeChartRef = ref<HTMLDivElement>()
const barChartRef = ref<HTMLDivElement>()
const lineChartRef = ref<HTMLDivElement>()

const gaugeChart = useECharts(gaugeChartRef, (): ECOption => ({
  series: [{
    type: 'gauge',
    startAngle: 200,
    endAngle: -20,
    min: 0,
    max: 100,
    splitNumber: 10,
    axisLine: {
      lineStyle: {
        width: 12,
        color: [[0.3, '#67C23A'], [0.7, '#E6A23C'], [1, '#F56C6C']],
      },
    },
    pointer: { width: 5 },
    axisTick: { distance: -12, length: 6, lineStyle: { color: '#fff', width: 1 } },
    splitLine: { distance: -14, length: 14, lineStyle: { color: '#fff', width: 2 } },
    axisLabel: { distance: 20, color: 'var(--el-text-color-secondary)', fontSize: 11 },
    detail: { valueAnimation: true, formatter: '{value}%', fontSize: 20, offsetCenter: [0, '70%'] },
    title: { offsetCenter: [0, '90%'] },
    data: [{ value: 72, name: t('bpcs.alert.riskScore') }],
  }],
}))

const barChart = useECharts(barChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: [t('bpcs.alert.lowStock'), t('bpcs.alert.overStock'), t('bpcs.alert.expiry'), t('bpcs.alert.obsolete')],
  },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar',
    data: [
      { value: 12, itemStyle: { color: '#F56C6C' } },
      { value: 5, itemStyle: { color: '#E6A23C' } },
      { value: 3, itemStyle: { color: '#409EFF' } },
      { value: 8, itemStyle: { color: '#909399' } },
    ],
    barWidth: '50%',
  }],
}))

const lineChart = useECharts(lineChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: {
    type: 'category',
    data: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: t('bpcs.alert.critical'),
      type: 'line',
      data: [5, 3, 8, 4],
      itemStyle: { color: '#F56C6C' },
    },
    {
      name: t('bpcs.alert.warning'),
      type: 'line',
      data: [12, 15, 10, 14],
      itemStyle: { color: '#E6A23C' },
    },
  ],
}))

const load = async () => {
  loading.value = true
  try {
    rows.value = await listAlertRules() as unknown as AlertRuleVO[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
