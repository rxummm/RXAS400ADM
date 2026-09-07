<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="reportType" :placeholder="$t('bpcs.orderReport.reportType')" class="w-160">
        <el-option :label="$t('bpcs.orderReport.type.fulfillment')" value="fulfillment" />
        <el-option :label="$t('bpcs.orderReport.type.otd')" value="otd" />
        <el-option :label="$t('bpcs.orderReport.type.anomaly')" value="anomaly" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" class="ml8" :start-placeholder="$t('common.startDate')" :end-placeholder="$t('common.endDate')" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-card v-if="reportType === 'fulfillment'" shadow="never">
        <template #header>{{ $t('bpcs.orderReport.fulfillmentTitle') }}</template>
        <div ref="fulfillmentChartRef" class="h-400" />
      </el-card>
      <el-card v-else-if="reportType === 'otd'" shadow="never">
        <template #header>{{ $t('bpcs.orderReport.otdTitle') }}</template>
        <div ref="otdChartRef" class="h-400" />
      </el-card>
      <el-card v-else shadow="never">
        <template #header>{{ $t('bpcs.orderReport.anomalyTitle') }}</template>
        <div ref="anomalyChartRef" class="h-400" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderReport' })

import { ref, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from '@/utils/echarts'

const { t } = useI18n()

const reportType = ref('fulfillment')
const dateRange = ref<[Date, Date] | null>(null)
const fulfillmentChartRef = ref<HTMLElement>()
const otdChartRef = ref<HTMLElement>()
const anomalyChartRef = ref<HTMLElement>()

let fulfillmentChart: ReturnType<typeof echarts.init> | null = null
let otdChart: ReturnType<typeof echarts.init> | null = null
let anomalyChart: ReturnType<typeof echarts.init> | null = null

const initCharts = () => {
  if (reportType.value === 'fulfillment' && fulfillmentChartRef.value) {
    if (!fulfillmentChart) {
      fulfillmentChart = echarts.init(fulfillmentChartRef.value)
    }
    fulfillmentChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: [t('bpcs.orderReport.lineFillRate'), t('bpcs.orderReport.orderFillRate')] },
      xAxis: { type: 'category', data: [t('common.january'), t('common.february'), t('common.march'), t('common.april'), t('common.may'), t('common.june')] },
      yAxis: { type: 'value', max: 100 },
      series: [
        { name: t('bpcs.orderReport.lineFillRate'), type: 'line', data: [92, 94, 91, 95, 93, 96] },
        { name: t('bpcs.orderReport.orderFillRate'), type: 'line', data: [88, 90, 87, 91, 89, 92] }
      ]
    })
  }
  if (reportType.value === 'otd' && otdChartRef.value) {
    if (!otdChart) {
      otdChart = echarts.init(otdChartRef.value)
    }
    otdChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: [t('bpcs.orderReport.onTime'), t('bpcs.orderReport.early'), t('bpcs.orderReport.late')] },
      xAxis: { type: 'category', data: [t('common.january'), t('common.february'), t('common.march'), t('common.april'), t('common.may'), t('common.june')] },
      yAxis: { type: 'value', max: 100 },
      series: [
        { name: t('bpcs.orderReport.onTime'), type: 'bar', stack: 'total', data: [70, 72, 68, 75, 71, 74] },
        { name: t('bpcs.orderReport.early'), type: 'bar', stack: 'total', data: [15, 14, 16, 13, 15, 14] },
        { name: t('bpcs.orderReport.late'), type: 'bar', stack: 'total', data: [15, 14, 16, 12, 14, 12] }
      ]
    })
  }
  if (reportType.value === 'anomaly' && anomalyChartRef.value) {
    if (!anomalyChart) {
      anomalyChart = echarts.init(anomalyChartRef.value)
    }
    anomalyChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: t('bpcs.orderReport.anomalyType'),
        type: 'pie',
        radius: '50%',
        data: [
          { value: 35, name: t('bpcs.orderReport.creditHold') },
          { value: 25, name: t('bpcs.orderReport.stockShortage') },
          { value: 20, name: t('bpcs.orderReport.priceHold') },
          { value: 15, name: t('bpcs.orderReport.shippingDelay') },
          { value: 5, name: t('bpcs.orderReport.other') }
        ]
      }]
    })
  }
}

const load = () => {
  nextTick(initCharts)
}

watch(reportType, () => {
  nextTick(initCharts)
})

onMounted(() => {
  nextTick(initCharts)
})
</script>
