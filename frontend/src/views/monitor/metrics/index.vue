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

    <!-- AS400 系统资源趋势 -->
    <div class="table-wrapper" v-if="selectedServerId">
      <h3 class="section">{{ $t('monitor.as400ResourceTrends') }}</h3>
      <el-select v-model="selectedServerId" :placeholder="$t('scripts.selectServer')" class="w-200 mb8">
        <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <div ref="trendChart" class="chart-container"></div>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { GaugeChart, PieChart } from 'echarts/charts'
import * as echarts from '@/utils/echarts'
import { cssVar } from '@/utils/cssVar'
import { useECharts, type ECOption } from '@/composables/useECharts'
import {
  fetchHealth,
  fetchMetricDetail,
  fetchMetricsHistory,
  type ActuatorHealth,
  type MetricDetail,
  type MetricHistory,
} from '@/api/metrics'
import { fetchSystems, type IbmiSystem } from '@/api/as400'
import { formatSize } from '@/utils/format'

const { t } = useI18n()

// gauge/pie 不在 @/utils/echarts 的全局注册清单内，此处按需补注册（维持按需打包，不引全量包）
echarts.use([GaugeChart, PieChart])

const loading = ref(false)
const healthStatus = ref('')
const as400Health = ref('')
const jvmMemoryChart = ref<HTMLDivElement>()
const httpChart = ref<HTMLDivElement>()
const trendChart = ref<HTMLDivElement>()

const servers = ref<IbmiSystem[]>([])
const selectedServerId = ref<number | undefined>()

// 概览卡片数值（title/desc 文案由下方 summaryCards computed 响应语言切换）
const jvmMemoryUsedValue = ref('-')
const jvmThreadsValue = ref('-')
const httpRequestsValue = ref('-')
const as400ServersValue = ref('-')

const summaryCards = computed(() => [
  { title: t('monitor.jvmMemoryUsed'), value: jvmMemoryUsedValue.value, color: 'var(--el-color-primary)', desc: t('monitor.jvmMemoryUsedHint') },
  { title: t('monitor.jvmThreads'), value: jvmThreadsValue.value, color: 'var(--el-color-success)', desc: t('monitor.jvmThreadsHint') },
  { title: t('monitor.httpRequests'), value: httpRequestsValue.value, color: 'var(--el-color-warning)', desc: t('monitor.httpRequestsHint') },
  { title: t('monitor.as400Servers'), value: as400ServersValue.value, color: 'var(--el-color-info)', desc: t('monitor.as400ServersHint') }
])

const as400Metrics = ref<{ name: string; value: string }[]>([])

async function refresh() {
  loading.value = true
  try {
    await fetchHealthData()
    await fetchJvmMetrics()
    await fetchHttpMetrics()
    await fetchAs400Metrics()
    await loadServers()
  } finally {
    loading.value = false
  }
}

async function loadServers() {
  try {
    servers.value = await fetchSystems()
    if (servers.value.length > 0 && !selectedServerId.value) {
      selectedServerId.value = servers.value[0].id
    }
  } catch {
    /* ignored */
  }
}

async function fetchAs400Trend() {
  if (!selectedServerId.value) return
  try {
    const history = await fetchMetricsHistory(selectedServerId.value, 50)
    // trend 容器位于 v-if 内，等 DOM 挂载后由 useECharts 的 setOption 兜底初始化
    await nextTick()
    if (history.length > 0) {
      trendCtl.setOption(buildTrendOption(history))
    }
  } catch {
    /* ignored */
  }
}

watch(selectedServerId, () => {
  fetchAs400Trend()
})

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
    jvmMemoryUsedValue.value = formatSize(heapUsed)

    const heapMaxDetail = await fetchMetricDetail('jvm.memory.max')
    const heapMax = heapMaxDetail?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0

    const threads = await fetchMetricDetail('jvm.threads.live')
    const threadCount = threads?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    jvmThreadsValue.value = String(Math.round(threadCount))

    await nextTick()
    jvmCtl.setOption(buildJvmOption(heapUsed, heapMax))
  } catch { /* ignore */ }
}

async function fetchHttpMetrics() {
  try {
    const data: MetricDetail = await fetchMetricDetail('http.server.requests')
    const count = data?.measurements?.find((m) => m.statistic === 'COUNT')?.value ?? 0
    httpRequestsValue.value = String(Math.round(count))

    await nextTick()
    const tags = data?.availableTags?.find((tag) => tag.tag === 'outcome')
    httpCtl.setOption(buildHttpOption(tags?.values || []))
  } catch { /* ignore */ }
}

async function fetchAs400Metrics() {
  try {
    const total = await fetchMetricDetail('as400.servers.total')
    const enabled = await fetchMetricDetail('as400.servers.enabled')
    const totalCount = total?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    const enabledCount = enabled?.measurements?.find((m) => m.statistic === 'VALUE')?.value ?? 0
    as400ServersValue.value = `${enabledCount}/${totalCount}`
    as400Metrics.value = [
      { name: t('monitor.totalServers'), value: String(totalCount) },
      { name: t('monitor.enabledServers'), value: String(enabledCount) }
    ]
  } catch { /* ignore */ }
}

// ---- 三图基准配置（getter 惰性求值；颜色沿用 cssVar 取值不变）----

function buildJvmOption(heapUsed: number, heapMax: number): ECOption {
  // F10：原实现把「已用字节」当分母（/1GB），百分比严重失真；改为 已用 / 上限
  const usedPct = heapMax > 0 ? Math.round((heapUsed / heapMax) * 100) : 0
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'gauge',
      detail: { formatter: '{value}%' },
      data: [{ value: Math.min(usedPct, 100), name: t('monitor.heapUsage') }],
      axisLine: { lineStyle: { width: 15, color: [[0.3, cssVar('--color-success')], [0.7, cssVar('--color-warning')], [1, cssVar('--color-danger')]] } }
    }]
  }
}

function buildHttpOption(outcomes: string[]): ECOption {
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: outcomes.map((o) => ({ name: o, value: 1 })),
      emphasis: { itemStyle: { shadowBlur: 10 } }
    }]
  }
}

function buildTrendOption(history: MetricHistory[]): ECOption {
  // 按指标类型分组
  const cpuData = history.filter(m => m.metricName === 'CPU').reverse()
  const memoryData = history.filter(m => m.metricName === 'MEMORY').reverse()
  const diskData = history.filter(m => m.metricName === 'DISK').reverse()

  const times = cpuData.map(m => m.collectTime?.slice(11, 16) || '')

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [t('monitor.trendCpu'), t('monitor.trendMemory'), t('monitor.trendDisk')] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: t('monitor.trendCpu'), type: 'line', data: cpuData.map(m => m.metricValue), smooth: true },
      { name: t('monitor.trendMemory'), type: 'line', data: memoryData.map(m => m.metricValue), smooth: true },
      { name: t('monitor.trendDisk'), type: 'line', data: diskData.map(m => m.metricValue), smooth: true },
    ]
  }
}

// init / window resize / dispose / v-if 延迟挂载兜底全交由 useECharts 托管；
// 注：composable 未处理 keep-alive onActivated 重绘，原实现亦无此逻辑，故不额外补充
const jvmCtl = useECharts(jvmMemoryChart, () => buildJvmOption(0, 0))
const httpCtl = useECharts(httpChart, () => buildHttpOption([]))
const trendCtl = useECharts(trendChart, () => buildTrendOption([]))

onMounted(() => refresh())
</script>

<style scoped>
.chart-container {
  height: 300px;
  width: 100%;
}
</style>
