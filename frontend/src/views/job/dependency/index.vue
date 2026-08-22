<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadGraph">
        {{ $t('jobDependency.title') }}
      </el-button>
      <el-tag size="large" type="info">{{ $t('jobDependency.nodeCount') }}: {{ graph.nodes.length }}</el-tag>
      <el-tag size="large" type="warning" class="ml8">{{ $t('jobDependency.edgeCount') }}: {{ graph.links.length }}</el-tag>
      <span class="hint ml8">{{ $t('jobDependency.mockNote') }}</span>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="card" :rows="8" :loading="loading">
      <div ref="chartRef" class="graph"></div>
      <el-empty v-if="!loading && !graph.nodes.length" :description="$t('common.noData')" />
      </RxSkeleton>
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'JobDependency' })
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from '@/utils/echarts'
import { Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { fetchJobDependency, type DependencyGraph } from '@/api/jobDependency'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()

const loading = ref(false)
const graph = reactive<DependencyGraph>({ nodes: [], links: [] })
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const loadGraph = async () => {
  loading.value = true
  try {
    const data = await fetchJobDependency()
    graph.nodes = data?.nodes || []
    graph.links = data?.links || []
    render()
  } finally {
    loading.value = false
  }
}

const render = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const categories = [
    { name: 'PGM' },
    { name: 'FILE' },
  ]
  chart.setOption({
    title: { text: t('jobDependency.title'), left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {},
    legend: { bottom: 0, data: ['PGM', 'FILE'] },
    animationDurationUpdate: 1000,
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        categories,
        label: { show: true, position: 'right', fontSize: 12 },
        force: { repulsion: 200, edgeLength: 100 },
        data: graph.nodes.map((n) => ({
          id: n.id,
          name: n.name,
          symbolSize: n.type === 'PGM' ? 40 : 28,
          category: n.type === 'PGM' ? 0 : 1,
          itemStyle: { color: n.type === 'PGM' ? '#409eff' : '#67c23a' },
        })),
        links: graph.links.map((l) => ({ source: l.source, target: l.target })),
      },
    ],
  })
}

onMounted(loadGraph)

onBeforeUnmount(() => {
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.graph {
  height: calc(100vh - 260px);
  min-height: 400px;
}
</style>