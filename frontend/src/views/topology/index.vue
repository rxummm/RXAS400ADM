<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="library" :placeholder="$t('topology.library')" clearable @keyup.enter="load" />
      <el-button type="primary" :icon="Search" @click="load">{{ $t('common.search') }}</el-button>
      <el-tag v-for="(color, type) in typeColors" :key="type" size="small" :color="color" effect="plain" class="legend">
        {{ type }}
      </el-tag>
      <span class="hint">{{ $t('topology.hint') }}</span>
    </div>

    <el-card shadow="never">
      <RxSkeleton type="card" :rows="8" :loading="loading">
      <div ref="chartRef" class="chart" />
      <el-empty v-if="!loading && nodes.length === 0" :description="$t('topology.empty')" />
      </RxSkeleton>
    </el-card>

    <el-drawer v-model="drawerVisible" :title="currentName" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="$t('objects.name')">{{ detail.OBJECT_NAME }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.type')">{{ detail.OBJECT_TYPE }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.library')">{{ detail.OBJECT_LIBRARY }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.size')">{{ formatSize(detail.OBJECT_SIZE) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.created')">{{ detail.OBJECT_CREATION_TIMESTAMP || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.changed')">{{ detail.OBJECT_CHANGE_TIMESTAMP || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('objects.text')">{{ detail.OBJECT_TEXT_DESCRIPTION || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="refTab" class="mt16">
          <el-tab-pane v-if="userStore.canSeeTab('topology', 'refIn')" :label="$t('objects.refIn')" name="IN">
            <el-table :data="pagedRefIn" size="small" border>
              <el-table-column prop="OBJECT_NAME" :label="$t('objects.name')" min-width="110" />
              <el-table-column prop="OBJECT_TYPE" :label="$t('objects.type')" width="90" />
            </el-table>
            <AppPagination :total="refIn.length" v-model:current="refInCurrent" v-model:size="refSize" @change="() => {}" @size-change="onRefSizeChange" />
          </el-tab-pane>
          <el-tab-pane v-if="userStore.canSeeTab('topology', 'refOut')" :label="$t('objects.refOut')" name="OUT">
            <el-table :data="pagedRefOut" size="small" border>
              <el-table-column prop="REF_OBJ_NAME" :label="$t('objects.name')" min-width="110" />
              <el-table-column prop="REF_OBJ_TYPE" :label="$t('objects.type')" width="90" />
            </el-table>
            <AppPagination :total="refOut.length" v-model:current="refOutCurrent" v-model:size="refSize" @change="() => {}" @size-change="onRefSizeChange" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import * as echarts from '@/utils/echarts'
import { topologyGraph, type TopologyLink, type TopologyNode } from '@/api/topology'
import { objectDetail, objectReferences, type ObjectDetail, type ObjectReference } from '@/api/object'
import { formatSize } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const userStore = useUserStore()
const library = ref('APP')
const loading = ref(false)
const nodes = ref<TopologyNode[]>([])
const links = ref<TopologyLink[]>([])
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const typeColors: Record<string, string> = {
  PGM: '#1677ff',
  SRVPGM: '#722ed1',
  MODULE: '#13c2c2',
  FILE: '#fa8c16',
  MSGF: '#909399',
}

const drawerVisible = ref(false)
const currentName = ref('')
const currentLib = ref('APP')
const currentObj = ref('')
const detail = ref<ObjectDetail | null>(null)
const refTab = ref('IN')
const refIn = ref<ObjectReference[]>([])
const refOut = ref<ObjectReference[]>([])
const refInCurrent = ref(1)
const refOutCurrent = ref(1)
const refSize = ref(10)

const pagedRefIn = computed(() =>
  refIn.value.slice((refInCurrent.value - 1) * refSize.value, refInCurrent.value * refSize.value),
)
const pagedRefOut = computed(() =>
  refOut.value.slice((refOutCurrent.value - 1) * refSize.value, refOutCurrent.value * refSize.value),
)

const onRefSizeChange = () => {
  refInCurrent.value = 1
  refOutCurrent.value = 1
}

const render = () => {
  if (!chart) return
  chart.setOption({
    tooltip: { formatter: (p: { dataType?: string; data?: { name?: string; type?: string } }) => (p.dataType === 'node' ? `${p.data?.name}<br/>${p.data?.type}` : '') },
    legend: [{ bottom: 0 }],
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        categories: Object.keys(typeColors).map((t) => ({
          name: t,
          itemStyle: { color: typeColors[t] },
        })),
        data: nodes.value.map((n) => ({
          id: n.id,
          name: n.name,
          type: n.type,
          category: n.type,
          symbolSize: n.type === 'FILE' || n.type === 'MSGF' ? 26 : 34,
          itemStyle: { color: (n.type && typeColors[n.type]) || '#1677ff' },
        })),
        links: links.value.map((l) => ({ source: l.source, target: l.target })),
        force: { repulsion: 320, edgeLength: [60, 140], gravity: 0.1 },
        label: { show: true, position: 'right', fontSize: 11 },
        lineStyle: { color: 'source', curveness: 0.12 },
        emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
      },
    ],
  }, true)
}

const load = async () => {
  loading.value = true
  try {
    const data = await topologyGraph(library.value)
    nodes.value = data.nodes || []
    links.value = data.links || []
    if (!chart) {
      chart = echarts.init(chartRef.value!)
      chart.on('click', (params: echarts.ECElementEvent) => {
        if (params?.dataType === 'node' && params?.data) {
          openNode(params.data as { id?: string })
        }
      })
      window.addEventListener('resize', onResize)
    }
    render()
  } catch {
    nodes.value = []
    links.value = []
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

const openNode = async (node: { id?: string; name?: string; type?: string }) => {
  const parts = String(node.id ?? '').split('.')
  currentLib.value = parts.length > 1 && parts[0] ? parts[0] : library.value
  currentObj.value = parts.length > 1 ? parts.slice(1).join('.') : (node.name || '')
  currentName.value = node.name || ''
  drawerVisible.value = true
  detail.value = null
  refIn.value = []
  refOut.value = []
  try {
    detail.value = await objectDetail(currentLib.value, currentObj.value)
  } catch {
    detail.value = null
    console.warn('[topology] objectDetail failed')
  }
  try {
    const [inList, outList] = await Promise.all([
      objectReferences(currentLib.value, currentObj.value, 'IN'),
      objectReferences(currentLib.value, currentObj.value, 'OUT'),
    ])
    refIn.value = inList || []
    refOut.value = outList || []
  } catch {
    refIn.value = []
    refOut.value = []
    console.warn('[topology] objectReferences failed')
  }
}

const onResize = () => chart?.resize()

onMounted(load)
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
/* 公共样式（toolbar/mb16/mt16）已收敛至 src/styles/common.css */
.legend {
  border: none;
  color: var(--text-regular);
}
.chart {
  height: 620px;
}
</style>