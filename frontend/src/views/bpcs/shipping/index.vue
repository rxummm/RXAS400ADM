<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="lhno"
        class="w-200"
        :placeholder="$t('bpcs.shipping.lhnoPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
      <span class="hint">{{ $t('bpcs.shipping.hint') }}</span>
    </div>

    <!-- ── 看板四列 ───────────────────────────────────── -->
    <div class="kanban-board" v-loading="loading">
      <div
        v-for="col in kanbanColumns"
        :key="col.status"
        class="kanban-column"
      >
        <div class="kanban-column-header" :style="{ borderTopColor: col.color }">
          <span class="column-title">{{ $t(col.titleKey) }}</span>
          <el-tag size="small" :type="col.tagType" class="ml4">{{ col.items.length }}</el-tag>
        </div>
        <div class="kanban-column-body">
          <div
            v-for="load in col.items"
            :key="load.lhno"
            class="kanban-card"
            @click="openDetail(load)"
          >
            <div class="kanban-card-header">
              <span class="card-title">{{ load.lhno }}</span>
              <el-tag size="small" :type="col.tagType">{{ load.lineCount }} {{ $t('bpcs.shipping.lines') }}</el-tag>
            </div>
            <div class="card-body">
              <div class="card-row" v-if="load.carrier">
                <span class="card-label">{{ $t('bpcs.shipping.carrier') }}</span>
                <span>{{ load.carrier }}</span>
              </div>
              <div class="card-row" v-if="load.destination">
                <span class="card-label">{{ $t('bpcs.label.shipTo') }}</span>
                <span>{{ load.destination }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">{{ $t('bpcs.shipping.shipDate') }}</span>
                <span>{{ load.shipDate || '—' }}</span>
              </div>
              <div class="card-row" v-if="load.orderNos.length">
                <span class="card-label">{{ $t('bpcs.shipping.orders') }}</span>
                <span>{{ load.orderNos.join(', ') }}</span>
              </div>
              <div class="card-row">
                <span class="card-label">{{ $t('bpcs.shipping.weight') }}</span>
                <span>{{ load.weight > 0 ? load.weight.toFixed(1) + ' kg' : '—' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="col.items.length === 0" :image-size="48" description="" />
        </div>
      </div>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
    </div>

    <!-- ── 详情抽屉 ──────────────────────────────────── -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('bpcs.shipping.detailTitle') + ' · ' + (detailLoad?.lhno || '')"
      size="480px"
      :close-on-click-modal="false"
    >
      <template v-if="detailLoad">
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item :label="$t('bpcs.shipping.lhno')">{{ detailLoad.lhno }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.shipping.status')">
            <el-tag size="small" :type="statusTagType(detailLoad.status)">
              {{ $t(detailLoad.statusKey) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.shipping.carrier')">
            {{ detailLoad.carrier || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.shipTo')">
            {{ detailLoad.destination || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.shipping.shipDate')">
            {{ detailLoad.shipDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.shipping.weight')">
            {{ detailLoad.weight > 0 ? detailLoad.weight.toFixed(1) + ' kg' : '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.shipping.ordersTitle') }}</h4>
        <el-table :data="detailLoad.orderNos.map(o => ({ orno: o }))" size="small" border>
          <el-table-column prop="orno" :label="$t('bpcs.label.orno')" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { searchLoads, type BpcsLoad } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsShipping' })

const lhno = ref('')
const loading = ref(false)
const loads = ref<BpcsLoad[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const detailVisible = ref(false)
const detailLoad = ref<BpcsLoad | null>(null)

interface KanbanColumn {
  status: number
  titleKey: string
  color: string
  tagType: 'info' | 'primary' | 'warning' | 'success'
  items: BpcsLoad[]
}

const kanbanColumns = computed<KanbanColumn[]>(() => {
  const cols: KanbanColumn[] = [
    { status: 0, titleKey: 'bpcs.loadStatus.planned', color: '#909399', tagType: 'info', items: [] },
    { status: 1, titleKey: 'bpcs.loadStatus.firmed', color: 'var(--color-primary)', tagType: 'primary', items: [] },
    { status: 2, titleKey: 'bpcs.loadStatus.released', color: 'var(--el-color-warning)', tagType: 'warning', items: [] },
    { status: 3, titleKey: 'bpcs.loadStatus.dispatched', color: 'var(--color-success)', tagType: 'success', items: [] },
  ]
  for (const load of loads.value) {
    const col = cols.find(c => c.status === load.status)
    if (col) col.items.push(load)
  }
  return cols
})

function search() {
  loading.value = true
  const params: Record<string, string | number> = { current: current.value, size: size.value }
  if (lhno.value.trim()) params.lhno = lhno.value.trim()
  searchLoads(params)
    .then(data => {
      loads.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function openDetail(load: BpcsLoad) {
  detailLoad.value = load
  detailVisible.value = true
}

function statusTagType(status: number): 'info' | 'primary' | 'warning' | 'success' {
  return (['info', 'primary', 'warning', 'success'] as const)[status] ?? 'info'
}
</script>

<style scoped>
.kanban-board {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-x: auto;
}
.kanban-column {
  flex: 1;
  min-width: 240px;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.kanban-column-header {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-top: 3px solid;
  background: var(--el-fill-color-lighter);
  font-weight: 600;
  font-size: 13px;
}
.column-title {
  flex: 1;
}
.kanban-column-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kanban-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.kanban-card:hover {
  box-shadow: var(--shadow-sm);
}
.kanban-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.card-title {
  font-weight: 600;
  font-size: 13px;
}
.card-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.card-row {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: var(--text-regular);
}
.card-label {
  color: var(--text-secondary);
  min-width: 56px;
  flex-shrink: 0;
}
</style>
