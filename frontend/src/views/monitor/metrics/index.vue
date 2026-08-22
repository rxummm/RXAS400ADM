<template>
  <div class="page-container page-container--fit">
    <!-- 健康状态卡片 -->
    <div class="search-bar">
      <el-tag :type="healthStatus === 'UP' ? 'success' : healthStatus === 'DOWN' ? 'danger' : 'info'" size="large">
        {{ $t('monitor.healthStatusLabel') }}: {{ healthStatus || '...' }}
      </el-tag>
      <el-tag v-if="as400Health" :type="as400Health === 'UP' ? 'success' : 'danger'" size="large">
        AS400: {{ as400Health }}
      </el-tag>
      <el-button @click="refresh" :loading="loading" type="primary" size="small">
        {{ $t('common.refresh') }}
      </el-button>
    </div>

    <!-- 指标概览 -->
    <div class="table-wrapper">
      <el-row :gutter="16">
        <el-col :span="6" v-for="card in summaryCards" :key="card.title">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-card__title">{{ card.title }}</div>
            <div class="metric-card__value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="metric-card__desc">{{ card.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- JVM 内存图表 -->
    <div class="table-wrapper">
      <h3 class="section">JVM Memory</h3>
      <div ref="jvmMemoryChart" class="chart-container"></div>
    </div>

    <!-- HTTP 请求图表 -->
    <div class="table-wrapper">
      <h3 class="section">HTTP Requests</h3>
      <div ref="httpChart" class="chart-container"></div>
    </div>

    <!-- AS400 服务器指标 -->
    <div class="table-wrapper" v-if="as400Metrics.length">
      <h3 class="section">AS400 Servers</h3>
      <el-table :data="as400Metrics" size="small" border>
        <el-table-column prop="name" :label="$t('monitor.serverName')" />
        <el-table-column prop="value" :label="$t('monitor.value')" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Metrics' })
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  fetchHealth,
  fetchMetricDetail,
  type ActuatorHealth,
  type MetricDetail,
} from '@/api/metrics'
import { formatSize } from '@/utils/format'

const loading = ref(false)
const healthStatus = ref('')
const as400Health = ref('')
const jvmMemoryChart = ref<HTMLElement>()
const httpChart = ref<HTMLElement>()
let jvmChart: echarts.ECharts | null = null
let httpChartInstance: echarts.ECharts | null = null

const summaryCards = ref([
  { title: 'JVM Memory Used', value: '-', color: '#409eff', desc: 'heap' },
  { title: 'JVM Threads', value: '-', color: '#67c23a', desc: 'live' },
  { title: 'HTTP Requests', value: '-', color: '#e6a23c', desc: 'total' },
  { title: 'AS400 Servers', value: '-', color: '#909399', desc: 'configured' }
])

const as400Metrics = ref<{ name: string; value: string }[]>([])

async function refresh() {
  loading.value = true
  try {
    await fetchHealthData()
    await fetchJvmMetrics()
    await fetchHttpMetrics()
    await fetchAs400Metrics()
  } finally {
    loading.value = false
  }
}

async function fetchHealthData() {
  try {
    const data: ActuatorHealth = await fetchHealth()
    healthStatus.value = data?.status || 'UNKNOWN'
    const as400 = data?.components?.as400
    if (as400) {
      as400Health.value = as400.status || 'UNKNOWN'
    }
  } catch {
    healthStatus.value = 'UNREACHABLE'
  }
}

async function fetchJvmMetrics() {
  try {
    const heap = await fetchMetricDetail('jvm.memory.used')
    const heapUsed = heap?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    summaryCards.value[0].value = formatSize(heapUsed)

    const heapMaxDetail = await fetchMetricDetail('jvm.memory.max')
    const heapMax = heapMaxDetail?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0

    const threads = await fetchMetricDetail('jvm.threads.live')
    const threadCount = threads?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    summaryCards.value[1].value = String(Math.round(threadCount))

    await nextTick()
    renderJvmChart(heapUsed, heapMax)
  } catch { /* ignore */ }
}

async function fetchHttpMetrics() {
  try {
    const data: MetricDetail = await fetchMetricDetail('http.server.requests')
    const count = data?.measurements?.find((m) => m.statistic === 'COUNT')?.value ?? 0
    summaryCards.value[2].value = String(Math.round(count))

    await nextTick()
    renderHttpChart(data)
  } catch { /* ignore */ }
}

async function fetchAs400Metrics() {
  try {
    const total = await fetchMetricDetail('as400.servers.total')
    const enabled = await fetchMetricDetail('as400.servers.enabled')
    const totalCount = total?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    const enabledCount = enabled?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    summaryCards.value[3].value = `${enabledCount}/${totalCount}`
    as400Metrics.value = [
      { name: 'Total Servers', value: String(totalCount) },
      { name: 'Enabled Servers', value: String(enabledCount) }
    ]
  } catch { /* ignore */ }
}

function renderJvmChart(heapUsed: number, heapMax: number) {
  if (!jvmMemoryChart.value) return
  if (!jvmChart) {
    jvmChart = echarts.init(jvmMemoryChart.value)
  }
  // F10：原实现把「已用字节」当分母（/1GB），百分比严重失真；改为 已用 / 上限
  const usedPct = heapMax > 0 ? Math.round((heapUsed / heapMax) * 100) : 0
  jvmChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'gauge',
      detail: { formatter: '{value}%' },
      data: [{ value: Math.min(usedPct, 100), name: 'Heap Usage' }],
      axisLine: { lineStyle: { width: 15, color: [[0.3, '#67c23a'], [0.7, '#e6a23c'], [1, '#f56c6c']] } }
    }]
  })
}

function renderHttpChart(data: MetricDetail) {
  if (!httpChart.value) return
  if (!httpChartInstance) {
    httpChartInstance = echarts.init(httpChart.value)
  }
  const tags = data?.availableTags?.find((t) => t.tag === 'outcome')
  const outcomes = tags?.values || []
  httpChartInstance.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: outcomes.map((o: string) => ({ name: o, value: 1 })),
      emphasis: { itemStyle: { shadowBlur: 10 } }
    }]
  })
}



onMounted(() => refresh())

onBeforeUnmount(() => {
  jvmChart?.dispose()
  httpChartInstance?.dispose()
})
</script>

<style scoped>
.metric-card {
  text-align: center;
  margin-bottom: 16px;
}
.metric-card__title {
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
  margin-bottom: 8px;
}
.metric-card__value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 4px;
}
.metric-card__desc {
  font-size: 11px;
  color: var(--color-text-placeholder, #c0c4cc);
}
.section {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}
.chart-container {
  height: 300px;
  width: 100%;
}
</style>
