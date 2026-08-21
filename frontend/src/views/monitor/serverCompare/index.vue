<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select
        v-model="selectedIds"
        multiple
        collapse-tags
        collapse-tags-tooltip
        :placeholder="$t('serverCompare.selectServers')"
        class="w-320"
      >
        <el-option
          v-for="s in servers"
          :key="s.id"
          :label="$t('serverCompare.serverLabel', { name: s.name, host: s.host, env: s.environment })"
          :value="s.id"
        />
      </el-select>
      <el-button type="primary" :icon="Search" :loading="loading" @click="compare">
        {{ $t('serverCompare.compare') }}
      </el-button>
      <span class="hint ml8">{{ $t('serverCompare.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <template v-if="rows.length">
        <RxSkeleton type="table" :rows="8" :loading="loading">
          <el-table :data="tableRows" size="small" border>
          <el-table-column :label="$t('serverCompare.metric')" width="120" fixed="left">
            <template #default="{ row }: { row: { key: string } }">{{ row.key ? $t(`serverCompare.metricNames.${row.key}`) : '' }}</template>
          </el-table-column>
          <el-table-column
            v-for="s in rows"
            :key="s.id"
            :label="`${s.name} / ${s.host}`"
            align="center"
          >
            <template #default="{ row }: { row: { key: string } }">
              <template v-if="row.key === 'status'">
                <el-tag :type="s.status === 'ONLINE' ? 'success' : 'danger'" size="small">{{ s.status }}</el-tag>
              </template>
              <template v-else-if="row.key === 'jobs'">
                {{ fmtNum(Number(s[row.key])) }}
              </template>
              <template v-else>
                <span :class="{ 'text-danger': Number(s[row.key]) >= 90 }">{{ fmtNum(Number(s[row.key])) }}</span>
              </template>
            </template>
          </el-table-column>
        </el-table>
        </RxSkeleton>
        <RxSkeleton type="card" :rows="6" :loading="loading">
        <div ref="chartRef" class="compare-chart"></div>
        </RxSkeleton>
      </template>
      <el-empty v-else-if="!loading" :description="$t('serverCompare.empty')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from '@/utils/echarts'
import { Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore } from '@/stores/as400Server'
import { fetchCompare, type CompareSnapshot } from '@/api/monitor'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const as400Store = useAs400ServerStore()

const servers = computed(() => as400Store.serverList)
const selectedIds = ref<number[]>([])
const rows = ref<CompareSnapshot[]>([])
const loading = ref(false)

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

// 指标行：status 为服务器在线状态，其余为数值指标
const tableRows = computed(() =>
  ['status', 'cpu', 'memory', 'disk', 'jobs', 'msgw', 'lckw'].map((key) => ({ key })),
)

const fmtNum = (v: number) => {
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return Math.round(n * 10) / 10
}

const compare = async () => {
  if (!selectedIds.value.length) {
    return
  }
  loading.value = true
  try {
    rows.value = await fetchCompare(selectedIds.value)
    // 等待 v-if 渲染出图表容器后再 init（否则 chartRef 为 null）
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    title: { text: t('serverCompare.chartTitle'), left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 40, left: 40, right: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: rows.value.map((s) => s.name),
    },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      {
        name: t('serverCompare.metricNames.cpu'),
        type: 'bar',
        data: rows.value.map((s) => Math.round(Number(s.cpu) * 10) / 10),
      },
      {
        name: t('serverCompare.metricNames.memory'),
        type: 'bar',
        data: rows.value.map((s) => Math.round(Number(s.memory) * 10) / 10),
      },
      {
        name: t('serverCompare.metricNames.disk'),
        type: 'bar',
        data: rows.value.map((s) => Math.round(Number(s.disk) * 10) / 10),
      },
    ],
  })
}

onMounted(async () => {
  if (!servers.value.length) {
    await as400Store.fetchServers()
  }
})

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.compare-chart {
  height: 320px;
  margin-top: 12px;
}
</style>