<template>
  <div>
    <el-card shadow="never" class="mb16">
      <template #header>
        {{ $t('monitor.wsHint', { serverId: sid() }) }}
      </template>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="24" :md="8" :lg="8"><div ref="cpuChart" class="chart" /></el-col>
        <el-col :xs="24" :sm="24" :md="8" :lg="8"><div ref="memoryChart" class="chart" /></el-col>
        <el-col :xs="24" :sm="24" :md="8" :lg="8"><div ref="diskChart" class="chart" /></el-col>
      </el-row>
      <el-row :gutter="16" class="mt16">
        <el-col :span="24">
          <div class="overview">
            <el-descriptions :column="4" border>
              <el-descriptions-item :label="$t('monitor.activeJobs')">
                <span :class="{ 'num-warn': overview.jobs > 0 }">{{ overview.jobs }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('monitor.msgwJobs')">
                <span :class="{ 'num-danger': overview.msgw > 0 }">{{ overview.msgw }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('monitor.lckwJobs')">
                <span :class="{ 'num-danger': overview.lckw > 0 }">{{ overview.lckw }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('monitor.diskAsp')">
                <span :class="{ 'num-warn': overview.disk > 80 }">{{ overview.disk }}%</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="mb16">
      <template #header>
        <div class="card-header">
          <span>{{ $t('monitor.capacity') }}</span>
          <span v-if="capacity.daysToThreshold !== null && capacity.daysToThreshold !== undefined" class="capacity-warn">
            {{ $t('monitor.daysToFull', { days: capacity.daysToThreshold }) }}
          </span>
        </div>
      </template>
      <div ref="capacityChart" class="chart" />
    </el-card>

    <el-card shadow="never">
      <template #header>{{ $t('monitor.history') }}</template>
      <el-table :data="pagedMetrics" size="small" :max-height="historyMaxHeight">
        <el-table-column prop="id" :label="'ID'" width="80" />
        <el-table-column prop="metricName" :label="$t('monitor.metric')" width="120" />
        <el-table-column prop="metricValue" :label="$t('monitor.value')" width="120" />
        <el-table-column prop="collectTime" :label="$t('monitor.collectTime')" />
      </el-table>
      <AppPagination :total="metrics.length" v-model:current="metricsCurrent" v-model:size="metricsSize" @change="() => {}" @size-change="onMetricsSizeChange" />
    </el-card>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Monitor' })
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElNotification } from 'element-plus'
import * as echarts from '@/utils/echarts'
import { useStompClient } from '@/composables/useStompClient'
import { fetchCapacity, fetchMetrics, fetchOverview, type CapacityResponse, type MetricOverview, type MetricPoint } from '@/api/monitor'
import AppPagination from '@/components/AppPagination.vue'
import { useAs400ServerStore } from '@/stores/as400Server'

const { t } = useI18n()
const as400Store = useAs400ServerStore()
const { connect: connectStomp, disconnect: disconnectStomp } = useStompClient()
const cpuChart = ref<HTMLDivElement>()
const memoryChart = ref<HTMLDivElement>()
const diskChart = ref<HTMLDivElement>()
const capacityChart = ref<HTMLDivElement>()
const metrics = ref<MetricPoint[]>([])
const overview = ref<MetricOverview>({ cpu: 0, memory: 0, disk: 0, msgw: 0, lckw: 0, jobs: 0 })
const capacity = ref<CapacityResponse>({ points: [], prediction: [], daysToThreshold: null })
const metricsCurrent = ref(1)
const metricsSize = ref(10)

const historyMaxHeight = computed(() => {
  return Math.max(280, Math.floor(window.innerHeight * 0.35))
})

const pagedMetrics = computed(() =>
  metrics.value.slice((metricsCurrent.value - 1) * metricsSize.value, metricsCurrent.value * metricsSize.value),
)

const onMetricsSizeChange = () => {
  metricsCurrent.value = 1
}

let cpuInstance: echarts.ECharts | null = null
let memoryInstance: echarts.ECharts | null = null
let diskInstance: echarts.ECharts | null = null
let capacityInstance: echarts.ECharts | null = null
const cpuData = ref<number[]>([])
const memoryData = ref<number[]>([])
const diskData = ref<number[]>([])

/** 当前监控服务器 id（F-02：替代硬编码 1，随 X-AS400-Server 切换）
 * 注意不能用 `?? 1`：currentServerId 初始为 0，`0 ?? 1` 仍得 0；
 * 需在 store 未加载时回退默认服务器（defaultServer），再回退 1。 */
const sid = (): number => {
  if (as400Store.currentServerId) return as400Store.currentServerId
  const def = as400Store.serverList.find((s) => s.defaultServer)
  return def?.id || 1
}

const initCharts = () => {
  cpuInstance = echarts.init(cpuChart.value!)
  memoryInstance = echarts.init(memoryChart.value!)
  diskInstance = echarts.init(diskChart.value!)
  cpuInstance.setOption({
    title: { text: t('monitor.cpu'), left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: [] }],
  })
  memoryInstance.setOption({
    title: { text: t('monitor.memory'), left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: [] }],
  })
  diskInstance.setOption({
    title: { text: t('monitor.disk'), left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'line', smooth: true, areaStyle: {}, data: [] }],
  })
  capacityInstance = echarts.init(capacityChart.value!)
  capacityInstance.setOption({
    title: { text: t('monitor.capacity'), left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: [t('monitor.capAvg'), t('monitor.capPred')], bottom: 0 },
    xAxis: { type: 'category', data: [] },
    yAxis: { type: 'value', max: 100 },
    series: [
      { name: t('monitor.capAvg'), type: 'line', smooth: true, areaStyle: {}, data: [] },
      { name: t('monitor.capPred'), type: 'line', smooth: true, lineStyle: { type: 'dashed' }, data: [] },
    ],
  })
}

const pushCpu = (v: number) => {
  cpuData.value.push(v)
  if (cpuData.value.length > 30) cpuData.value.shift()
  cpuInstance?.setOption({
    xAxis: { data: cpuData.value.map((_, i) => i + 1) },
    series: [{ data: cpuData.value }],
  })
}

const pushMemory = (v: number) => {
  memoryData.value.push(v)
  if (memoryData.value.length > 30) memoryData.value.shift()
  memoryInstance?.setOption({
    xAxis: { data: memoryData.value.map((_, i) => i + 1) },
    series: [{ data: memoryData.value }],
  })
}

const pushDisk = (v: number) => {
  diskData.value.push(v)
  if (diskData.value.length > 30) diskData.value.shift()
  diskInstance?.setOption({
    xAxis: { data: diskData.value.map((_, i) => i + 1) },
    series: [{ data: diskData.value }],
  })
}

let wsNotifyClose: (() => void) | null = null

const connectSocket = () => {
  cpuData.value = []
  memoryData.value = []
  diskData.value = []
  connectStomp(
    (client) => {
      // 连接/重连成功：关闭断线提示
      if (wsNotifyClose) {
        wsNotifyClose()
        wsNotifyClose = null
      }
      client.subscribe(`/topic/monitor/${sid()}`, (message) => {
      try {
        const metric = JSON.parse(message.body) as MetricPoint
        if (metric.metricName === 'CPU') pushCpu(metric.metricValue)
        if (metric.metricName === 'MEMORY') pushMemory(metric.metricValue)
        if (metric.metricName === 'DISK') pushDisk(metric.metricValue)
        if (metric.metricName === 'MSGW') overview.value.msgw = metric.metricValue
        if (metric.metricName === 'LCKW') overview.value.lckw = metric.metricValue
      } catch {
        // WS message parse error (non-critical)
      }
    })
    },
    () => {
      // 断线回调：显示重连提示（不自动关闭）
      if (!wsNotifyClose) {
        wsNotifyClose = ElNotification({
          title: '⚡ 实时连接断开',
          message: '正在尝试重新连接…',
          type: 'warning',
          duration: 0,
          position: 'bottom-right',
        }).close
      }
    },
  )
}

const loadCapacity = async () => {
  try {
    const data = await fetchCapacity(sid(), 60)
    capacity.value = data
    const points = data.points || []
    const pred = data.prediction || []
    const dates = points.map((p: { date: string }) => p.date)
    const predDates = pred.map((p: { date: string }) => p.date)
    capacityInstance?.setOption({
      xAxis: { data: [...dates, ...predDates] },
      series: [
        { data: [...points.map((p: { avg: number }) => p.avg), ...predDates.map(() => null)] },
        { data: [...dates.map(() => null), ...pred.map((p: { value: number }) => p.value)] },
      ],
    })
  } catch {
    /* interceptor 已提示错误 */
  }
}

const load = async () => {
  // P2-23：轮询调用带 noDedupe，避免与其它同 URL 请求互相取消
  try {
    const data = await fetchOverview(sid(), true)
    overview.value = data
    pushCpu(data.cpu ?? 0)
    pushMemory(data.memory ?? 0)
    pushDisk(data.disk ?? 0)
  } catch {
    /* 后端未启动时忽略 */
  }
  try {
    metrics.value = await fetchMetrics(sid(), 30, true)
  } catch {
    /* interceptor 已提示错误 */
  }
}

// P2：监控轮询定时器句柄——模块级变量（组件卸载即清理，无全局泄漏）
let pollTimer: number | undefined

// P3：窗口 resize 时联动 4 个 ECharts（与 topology 页一致的响应式行为）
const onResize = () => {
  cpuInstance?.resize()
  memoryInstance?.resize()
  diskInstance?.resize()
  capacityInstance?.resize()
}

onMounted(async () => {
  initCharts()
  // 先确保服务器列表已加载（selector 挂载前 store 可能为空，currentServerId 为 0）
  await as400Store.fetchServers()
  load()
  loadCapacity()
  connectSocket()
  pollTimer = window.setInterval(load, 10000)
  window.addEventListener('resize', onResize)
})

watch(
  () => as400Store.currentServerId,
  () => {
    // 服务器切换后：重订阅 WS 频道 + 重新拉取概览/容量/历史（F-02 修复）
    load()
    loadCapacity()
    connectSocket()
  },
)

onBeforeUnmount(() => {
  disconnectStomp()
  wsNotifyClose?.()
  if (pollTimer !== undefined) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
  window.removeEventListener('resize', onResize)
  cpuInstance?.dispose()
  memoryInstance?.dispose()
  diskInstance?.dispose()
  capacityInstance?.dispose()
})
</script>

<style scoped>
/* mb16/card-header 已收敛至 src/styles/common.css */
.chart {
  height: 280px;
}
.overview {
  padding-top: 8px;
}
.num-warn {
  color: var(--color-warning);
  font-weight: 600;
}
.num-danger {
  color: var(--color-danger);
  font-weight: 600;
}
.capacity-warn {
  color: var(--color-danger);
  font-size: 13px;
}
</style>