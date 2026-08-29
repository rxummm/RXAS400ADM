<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="fromYm"
        class="w-140"
        :placeholder="$t('bpcs.sales.fromYm')"
        clearable
        @keyup.enter="load"
      />
      <el-input
        v-model="toYm"
        class="w-140"
        :placeholder="$t('bpcs.sales.toYm')"
        clearable
        @keyup.enter="load"
      />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <!-- ── 汇总卡片 ──────────────────────────────────── -->
    <div v-if="trend" class="summary-cards mb16">
      <div class="summary-card">
        <div class="summary-value">{{ fmtMoney(trend.totalRevenue) }}</div>
        <div class="summary-label">{{ $t('bpcs.sales.totalRevenue') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ trend.totalOrders }}</div>
        <div class="summary-label">{{ $t('bpcs.sales.totalOrders') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ trend.totalLines }}</div>
        <div class="summary-label">{{ $t('bpcs.sales.totalLines') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ trend.months.length }}</div>
        <div class="summary-label">{{ $t('bpcs.sales.months') }}</div>
      </div>
    </div>

    <!-- ── ECharts 图表 ─────────────────────────────────── -->
    <div class="table-wrapper">
      <div ref="chartRef" class="chart-container" v-loading="loading" />
      <el-empty v-if="!loading && (!trend || trend.months.length === 0)" :description="$t('common.noData')" />
    </div>

    <!-- ── 月度明细表格 ─────────────────────────────────── -->
    <div v-if="trend?.months.length" class="table-wrapper mt16">
      <el-table :data="trend.months" size="small" border>
        <el-table-column prop="ym" :label="$t('bpcs.sales.month')" width="100" />
        <el-table-column align="right" :label="$t('bpcs.sales.revenue')" width="120">
          <template #default="{ row }">{{ fmtMoney(row.revenue) }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.sales.orderCount')" width="80">
          <template #default="{ row }">{{ row.orderCount }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.sales.lineCount')" width="80">
          <template #default="{ row }">{{ row.lineCount }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.sales.avgOrderValue')" width="120">
          <template #default="{ row }">
            {{ row.orderCount > 0 ? fmtMoney((row.revenue ?? 0) / row.orderCount) : '—' }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from '@/utils/echarts'
import { getSalesTrend, type BpcsSalesTrend } from '@/api/bpcs'
import { formatMoney } from '@/utils/format'
import { cssVar } from '@/utils/cssVar'

defineOptions({ name: 'BpcsSales' })

const fromYm = ref('')
const toYm = ref('')
const loading = ref(false)
const trend = ref<BpcsSalesTrend | null>(null)
const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const onResize = () => chartInstance?.resize()

onMounted(() => {
  window.addEventListener('resize', onResize)
  load()
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartInstance?.dispose()
  chartInstance = null
})

function load() {
  loading.value = true
  const params: Record<string, string> = {}
  if (fromYm.value.trim()) params.fromYm = fromYm.value.trim()
  if (toYm.value.trim()) params.toYm = toYm.value.trim()
  getSalesTrend(params)
    .then(data => {
      trend.value = data
      nextTick(() => renderChart(data))
    })
    .finally(() => { loading.value = false })
}

function renderChart(data: BpcsSalesTrend) {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const labels = data.months.map(m => m.ym)
  const revenues = data.months.map(m => m.revenue ?? 0)
  const orders = data.months.map(m => m.orderCount)

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [t('bpcs.sales.revenue'), t('bpcs.sales.orderCount')], top: 4 },
    grid: { left: 60, right: 60, bottom: 30, top: 50 },
    xAxis: { type: 'category', data: labels },
    yAxis: [
      { type: 'value', name: t('bpcs.sales.revenue'), position: 'left' },
      { type: 'value', name: t('bpcs.sales.orderCount'), position: 'right' },
    ],
    series: [
      {
        name: t('bpcs.sales.revenue'),
        type: 'bar',
        data: revenues,
        itemStyle: { color: cssVar('--el-color-primary') },
        barMaxWidth: 40,
      },
      {
        name: t('bpcs.sales.orderCount'),
        type: 'line',
        yAxisIndex: 1,
        data: orders,
        smooth: true,
        itemStyle: { color: cssVar('--el-color-success') },
      },
    ],
  })
}

const fmtMoney = (v: number | null | undefined) => formatMoney(v, 0)
const { t } = useI18n()
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
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.chart-container {
  width: 100%;
  height: 360px;
}
</style>
