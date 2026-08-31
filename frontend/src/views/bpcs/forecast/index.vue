<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="selectedItem" :placeholder="$t('bpcs.common.itemCode')" clearable class="w-200">
        <el-option v-for="item in itemOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.forecast.demandForecast') }}</template>
            <div ref="forecastChartRef" class="w-full" style="height: 320px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.forecast.replenishmentSuggestions') }}</template>
            <div ref="replenishChartRef" class="w-full" style="height: 320px"></div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.forecast.stockLevel') }}</template>
            <div ref="stockChartRef" class="w-full" style="height: 280px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.forecast.accuracyMetrics') }}</template>
            <div class="p16">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item :label="$t('bpcs.forecast.mape')">{{ metrics.mape }}%</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.forecast.bias')">{{ metrics.bias }}%</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.forecast.gmAbc')">{{ metrics.gmAbc }}%</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.forecast.gmXyz')">{{ metrics.gmXyz }}%</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { useI18n } from 'vue-i18n'
import { getForecast, getForecastItemOptions, type ForecastResult } from '@/api/bpcs'

const { t } = useI18n()

const selectedItem = ref('')
const itemOptions = ref<{ value: string; label: string }[]>([])
const loading = ref(false)

const metrics = reactive({ mape: 0, bias: 0, gmAbc: 0, gmXyz: 0 })

const forecastChartRef = ref<HTMLDivElement>()
const replenishChartRef = ref<HTMLDivElement>()
const stockChartRef = ref<HTMLDivElement>()

const forecastChart = useECharts(forecastChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: t('bpcs.forecast.units') },
  series: [
    { name: t('bpcs.forecast.actual'), type: 'bar', data: [], itemStyle: { color: '#409EFF' } },
    { name: t('bpcs.forecast.forecast'), type: 'line', smooth: true, data: [], lineStyle: { type: 'dashed', color: '#F56C6C' }, itemStyle: { color: '#F56C6C' } },
    { name: t('bpcs.forecast.upperBound'), type: 'line', data: [], lineStyle: { opacity: 0.3 }, symbol: 'none' },
    { name: t('bpcs.forecast.lowerBound'), type: 'line', data: [], lineStyle: { opacity: 0.3 }, symbol: 'none', areaStyle: { opacity: 0.05 } },
  ],
}))

const replenishChart = useECharts(replenishChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: t('bpcs.forecast.reorderQty') },
  series: [
    { name: t('bpcs.forecast.currentStock'), type: 'bar', stack: 'total', data: [], itemStyle: { color: '#67C23A' } },
    { name: t('bpcs.forecast.suggestedOrder'), type: 'bar', stack: 'total', data: [], itemStyle: { color: '#E6A23C' } },
  ],
}))

const stockChart = useECharts(stockChartRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: t('bpcs.forecast.stockLevel') },
    { type: 'value', name: t('bpcs.forecast.daysOfSupply'), position: 'right' },
  ],
  series: [
    { name: t('bpcs.inventory.onHand'), type: 'bar', data: [], itemStyle: { color: '#409EFF' } },
    { name: t('bpcs.alert.safetyStock'), type: 'line', yAxisIndex: 1, data: [], lineStyle: { type: 'dashed', color: '#F56C6C' }, itemStyle: { color: '#F56C6C' } },
  ],
}))

async function loadItems() {
  const items = await getForecastItemOptions({ limit: 50 }) as unknown as { value: string; label: string }[]
  itemOptions.value = items
}

async function load() {
  loading.value = true
  try {
    const data = await getForecast({ cono: '001', item: selectedItem.value || undefined, months: 6 }) as unknown as ForecastResult
    // Update metrics
    Object.assign(metrics, data.metrics)
    // Update forecast chart
    nextTick(() => {
      const yms = data.monthlyDemand.map(d => d.ym)
      forecastChart.setOption({
        xAxis: { data: yms },
        series: [
          { data: data.monthlyDemand.map(d => d.actual || null) },
          { data: data.monthlyDemand.map(d => d.forecast) },
          { data: data.monthlyDemand.map(d => d.upperBound) },
          { data: data.monthlyDemand.map(d => d.lowerBound) },
        ],
      })
      // Replenish chart
      replenishChart.setOption({
        xAxis: { data: data.replenishSuggestions.map(s => s.item) },
        series: [
          { data: data.replenishSuggestions.map(s => s.currentStock) },
          { data: data.replenishSuggestions.map(s => s.suggestedOrder) },
        ],
      })
      // Stock chart
      const stockYms = data.stockLevels.map(s => s.ym)
      stockChart.setOption({
        xAxis: { data: stockYms },
        series: [
          { data: data.stockLevels.map(s => s.onHand) },
          { data: data.stockLevels.map(s => s.safetyStock) },
        ],
      })
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadItems(); load() })
</script>
