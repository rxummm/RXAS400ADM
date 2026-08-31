<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="query.item"
        class="w-200"
        :placeholder="$t('bpcs.inventory.itemPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-input
        v-model="query.desc"
        class="w-200"
        :placeholder="$t('bpcs.inventory.descPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-input
        v-model="query.wh"
        class="w-120"
        :placeholder="$t('bpcs.label.wh')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
      <ExportDropdown
        :data="items"
        :columns="exportColumns"
        :title="$t('bpcs.menu.inventory')"
        :export-url="BPCS_EXPORT.inventory"
        :query-params="query"
      />
    </div>

    <!-- ── 汇总卡片 ──────────────────────────────────── -->
    <div v-if="items.length" class="summary-cards mb16">
      <div class="summary-card">
        <div class="summary-value">{{ totalOnHand }}</div>
        <div class="summary-label">{{ $t('bpcs.inventory.totalOnHand') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ totalAllocated }}</div>
        <div class="summary-label">{{ $t('bpcs.inventory.totalAllocated') }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-value">{{ totalOnOrder }}</div>
        <div class="summary-label">{{ $t('bpcs.inventory.totalOnOrder') }}</div>
      </div>
      <div class="summary-card summary-card--accent">
        <div class="summary-value">{{ totalAvailable }}</div>
        <div class="summary-label">{{ $t('bpcs.inventory.totalAvailable') }}</div>
      </div>
    </div>

    <!-- ── 物料表格（可展开仓库明细） ─────────────────── -->
    <div class="table-wrapper">
      <el-table
        :data="items"
        v-loading="loading"
        size="small"
        border
        row-key="item"
        :expand-row-keys="expandedKeys"
        @expand-change="handleExpand"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <el-table :data="row.warehouses" size="small" border>
                <el-table-column prop="wh" :label="$t('bpcs.label.wh')" width="80" />
                <el-table-column prop="location" :label="$t('bpcs.inventory.location')" width="100" />
                <el-table-column align="right" :label="$t('bpcs.inventory.onHand')" width="90">
                  <template #default="{ row: w }">{{ w.onHand }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.allocated')" width="90">
                  <template #default="{ row: w }">{{ w.allocated }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.onOrder')" width="90">
                  <template #default="{ row: w }">{{ w.onOrder }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.available')" width="90">
                  <template #default="{ row: w }">
                    <span :class="{ 'text-danger': w.available < 0 }">{{ w.available }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="description" :label="$t('bpcs.line.itemDesc')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="uom" :label="$t('bpcs.inventory.uom')" width="60" />
        <el-table-column align="right" :label="$t('bpcs.inventory.onHand')" width="90">
          <template #default="{ row }">{{ row.totalOnHand }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.allocated')" width="90">
          <template #default="{ row }">{{ row.totalAllocated }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.onOrder')" width="90">
          <template #default="{ row }">{{ row.totalOnOrder }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.available')" width="90">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.totalAvailable < 0 }">
              {{ row.totalAvailable }}
            </span>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.unitCost')" width="100">
          <template #default="{ row }">
            {{ row.unitCost != null ? row.unitCost.toLocaleString(undefined, { minimumFractionDigits: 2 }) : '—' }}
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.inventory.whCount')" width="60">
          <template #default="{ row }">{{ row.warehouses?.length || 0 }}</template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
    </div>

    <el-empty v-if="searched && !loading && items.length === 0" :description="$t('common.noData')" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchInventory, type BpcsInventory, BPCS_EXPORT } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'

const { t } = useI18n()

defineOptions({ name: 'BpcsInventory' })

const query = reactive({ item: '', desc: '', wh: '' })
const loading = ref(false)
const searched = ref(false)
const items = ref<BpcsInventory[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const expandedKeys = ref<string[]>([])

const exportColumns = computed<ExportColumn[]>(() => [
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'description', label: t('bpcs.line.itemDesc') },
  { key: 'uom', label: t('bpcs.inventory.uom') },
  { key: 'totalOnHand', label: t('bpcs.inventory.totalOnHand') },
  { key: 'totalAllocated', label: t('bpcs.inventory.totalAllocated') },
  { key: 'totalOnOrder', label: t('bpcs.inventory.totalOnOrder') },
  { key: 'totalAvailable', label: t('bpcs.inventory.totalAvailable') },
  { key: 'unitCost', label: t('bpcs.inventory.unitCost') },
])

const totalOnHand = computed(() => items.value.reduce((s, i) => s + i.totalOnHand, 0))
const totalAllocated = computed(() => items.value.reduce((s, i) => s + i.totalAllocated, 0))
const totalOnOrder = computed(() => items.value.reduce((s, i) => s + i.totalOnOrder, 0))
const totalAvailable = computed(() => items.value.reduce((s, i) => s + i.totalAvailable, 0))

function search() {
  loading.value = true
  searched.value = true
  expandedKeys.value = []
  const params: Record<string, string | number> = { current: current.value, size: size.value }
  if (query.item.trim()) params.item = query.item.trim()
  if (query.desc.trim()) params.desc = query.desc.trim()
  if (query.wh.trim()) params.wh = query.wh.trim()
  searchInventory(params)
    .then(data => {
      items.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function handleExpand(_row: BpcsInventory, expanded: BpcsInventory[] | boolean) {
  if (Array.isArray(expanded)) {
    expandedKeys.value = expanded.map(r => r.item)
  }
}
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
.summary-card--accent {
  border-color: var(--color-primary);
  background: var(--el-color-primary-light-9);
}
.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-card--accent .summary-value {
  color: var(--color-primary);
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.expand-content {
  padding: 8px 48px;
}
</style>
