<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="selectedItem" :placeholder="$t('bpcs.common.itemCode')" clearable class="w-200">
        <el-option v-for="item in itemOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input-number v-model="months" :min="3" :max="24" class="w-120" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper" v-loading="loading">
      <!-- CPFR Metrics -->
      <el-row :gutter="16" class="mb16" v-if="cpfrData">
        <el-col :span="5">
          <el-statistic :title="$t('bpcs.cpfr.forecastAccuracy')" :value="cpfrData.metrics.forecastAccuracy" suffix="%" />
        </el-col>
        <el-col :span="5">
          <el-statistic :title="$t('bpcs.cpfr.seasonalStrength')" :value="cpfrData.metrics.seasonalStrength" />
        </el-col>
        <el-col :span="5">
          <el-statistic :title="$t('bpcs.cpfr.collabAlignment')" :value="cpfrData.metrics.collaborativeAlignment" suffix="%" />
        </el-col>
        <el-col :span="5">
          <el-statistic :title="$t('bpcs.cpfr.overallScore')" :value="cpfrData.metrics.overallScore" suffix="%" />
        </el-col>
        <el-col :span="4">
          <div class="text-center p16"><span class="text-muted">{{ $t('bpcs.cpfr.dominantSeason') }}</span><br /><strong class="fs-20">{{ cpfrData.seasonal.dominantSeason }}</strong></div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mb16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.cpfr.seasonal') }}</template>
            <div ref="seasonalRef" class="h-320"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.cpfr.backtest') }}</template>
            <div ref="backtestRef" class="h-320"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.cpfr.collaborative') }}</template>
            <div ref="collabRef" class="h-320"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>{{ $t('bpcs.cpfr.backtest') }}</template>
            <el-table :data="cpfrData?.accuracyBacktest || []" size="small" border max-height="280">
              <el-table-column prop="ym" :label="$t('col.ym')" width="70" />
              <el-table-column prop="actual" :label="$t('bpcs.forecast.actual')" width="60" />
              <el-table-column prop="predicted" :label="$t('bpcs.forecast.forecast')" width="60" />
              <el-table-column prop="mapePct" :label="$t('bpcs.cpfr.accuracy')" width="60">
                <template #default="{ row }">{{ row.mapePct }}%</template>
              </el-table-column>
              <el-table-column prop="accuracyGrade" :label="$t('bpcs.cpfr.grade')" width="50">
                <template #default="{ row }">
                  <el-tag :type="row.accuracyGrade === 'A' ? 'success' : row.accuracyGrade === 'B' ? 'warning' : 'danger'" size="small">
                    {{ row.accuracyGrade }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsCpfr' })

import { ref, onMounted, nextTick } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { useI18n } from 'vue-i18n'
import { CHART_COLORS } from '@/constants/chart'
import { getCpfrAnalysis, getForecastItemOptions, type CpfrResult } from '@/api/bpcs'

const { t } = useI18n()
const selectedItem = ref('')
const months = ref(12)
const itemOptions = ref<{ value: string; label: string }[]>([])
const loading = ref(false)
const cpfrData = ref<CpfrResult | null>(null)

const seasonalRef = ref<HTMLDivElement>()
const backtestRef = ref<HTMLDivElement>()
const collabRef = ref<HTMLDivElement>()

const seasonalChart = useECharts(seasonalRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value' },
  series: [
    { name: t('bpcs.forecast.actual'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.primary } },
    { name: t('bpcs.cpfr.trend'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.success }, smooth: true },
    { name: t('bpcs.cpfr.seasonalComp'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.warning }, yAxisIndex: 0 },
  ],
}))

const backtestChart = useECharts(backtestRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: t('bpcs.forecast.units') },
    { type: 'value', name: '%', position: 'right', min: 0, max: 30 },
  ],
  series: [
    { name: t('bpcs.forecast.actual'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.primary } },
    { name: t('bpcs.forecast.forecast'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.danger }, smooth: true },
    { name: t('bpcs.cpfr.accuracy'), type: 'line', yAxisIndex: 1, data: [], itemStyle: { color: CHART_COLORS.warning }, lineStyle: { type: 'dashed' } },
  ],
}))

const collabChart = useECharts(collabRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value' },
  series: [
    { name: t('bpcs.cpfr.salesF'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.primary }, smooth: true },
    { name: t('bpcs.cpfr.marketingF'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.success }, smooth: true },
    { name: t('bpcs.cpfr.supplyF'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.warning }, smooth: true },
    { name: t('bpcs.cpfr.consensus'), type: 'line', data: [], itemStyle: { color: CHART_COLORS.danger }, lineStyle: { type: 'dashed', width: 3 }, smooth: true },
    { name: t('bpcs.cpfr.finalActual'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.info, opacity: 0.3 } },
  ],
}))

async function loadItems() {
  const items = await getForecastItemOptions({ limit: 50 })
  itemOptions.value = items
}

async function load() {
  loading.value = true
  try {
    const data = await getCpfrAnalysis({ cono: '001', item: selectedItem.value || undefined, months: months.value })
    cpfrData.value = data
    await nextTick()
    // Seasonal decomposition chart
    const comps = data.seasonal.components
    seasonalChart.setOption({
      xAxis: { data: comps.map(c => c.ym) },
      yAxis: [{ type: 'value' }],
      series: [
        { data: comps.map(c => c.actual) },
        { data: comps.map(c => c.trend) },
        { data: comps.map(c => Math.abs(c.seasonal)) },
      ],
    })
    // Backtest chart
    const bt = data.accuracyBacktest
    backtestChart.setOption({
      xAxis: { data: bt.map(b => b.ym) },
      series: [
        { data: bt.map(b => b.actual) },
        { data: bt.map(b => b.predicted) },
        { data: bt.map(b => b.mapePct) },
      ],
    })
    // Collaborative forecast chart
    const cf = data.collaborativeForecasts
    collabChart.setOption({
      xAxis: { data: cf.map(c => c.ym) },
      series: [
        { data: cf.map(c => c.salesForecast) },
        { data: cf.map(c => c.marketingForecast) },
        { data: cf.map(c => c.supplyForecast) },
        { data: cf.map(c => c.consensusForecast) },
        { data: cf.map(c => c.finalActual || null) },
      ],
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadItems(); load() })
</script>
