<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="query.item"
        class="w-200"
        :placeholder="$t('bpcs.whReplenish.itemPlaceholder')"
        clearable
        @keyup.enter="forceSearch"
      />
      <el-input
        v-model="query.itdsc"
        class="w-200"
        :placeholder="$t('bpcs.whReplenish.itdscPlaceholder')"
        clearable
        @keyup.enter="forceSearch"
      />
      <el-checkbox v-model="query.belowSafetyOnly" class="ml8">
        {{ $t('bpcs.whReplenish.belowSafetyOnly') }}
      </el-checkbox>
      <el-button type="primary" :loading="loading" @click="forceSearch">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <el-table
        :data="tableData"
        v-loading="loading"
        size="small"
        border
        highlight-current-row
        @row-click="openDetail"
      >
        <el-table-column prop="item" :label="$t('bpcs.whReplenish.item')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="itdsc" :label="$t('bpcs.whReplenish.itdsc')" min-width="160" show-overflow-tooltip />
        <el-table-column align="right" :label="$t('bpcs.whReplenish.totalQty')" width="100">
          <template #default="{ row }">{{ row.totalQty ?? '—' }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.whReplenish.safetyStock')" width="100">
          <template #default="{ row }">{{ row.safetyStock ?? '—' }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.whReplenish.shortage')" width="100">
          <template #default="{ row }">
            <span :class="getShortageClass(row.shortage)">{{ fmtShortage(row.shortage) }}</span>
          </template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.whReplenish.avgDailyDemand')" width="110">
          <template #default="{ row }">{{ row.avgDailyDemand?.toFixed(1) ?? '—' }}</template>
        </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.whReplenish.suggestQty')" width="100">
          <template #default="{ row }">
            <span :class="row.suggestQty > 0 ? 'text-danger' : ''">{{ row.suggestQty ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.whReplenish.warehouses')" width="80">
          <template #default="{ row }">{{ row.warehouses?.length ?? 0 }}</template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
      <el-empty v-if="!loading && tableData.length === 0" :description="$t('common.noData')" />
    </div>

    <el-drawer
      v-model="detailVisible"
      :title="$t('bpcs.whReplenish.detailTitle') + ' · ' + (detailItem?.item || '')"
      size="650px"
      :close-on-click-modal="false"
    >
      <template v-if="detailItem">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.whReplenish.item')">{{ detailItem.item }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.itdsc')">{{ detailItem.itdsc || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.totalQty')">{{ detailItem.totalQty ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.safetyStock')">{{ detailItem.safetyStock ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.shortage')">
            <span :class="getShortageClass(detailItem.shortage)">{{ fmtShortage(detailItem.shortage) }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.avgDailyDemand')">{{ detailItem.avgDailyDemand?.toFixed(1) ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.whReplenish.suggestQty')">
            <span :class="(detailItem.suggestQty ?? 0) > 0 ? 'text-danger' : ''">{{ detailItem.suggestQty ?? '—' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section mt16">{{ $t('bpcs.whReplenish.warehouses') }}</h4>
        <el-table :data="detailItem.warehouses" size="small" border>
          <el-table-column prop="wh" :label="$t('bpcs.whReplenish.wh')" width="80" />
          <el-table-column align="right" :label="$t('bpcs.whReplenish.qtyOnHand')" width="90">
            <template #default="{ row }">{{ row.qtyOnHand ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.whReplenish.qtyAllocated')" width="90">
            <template #default="{ row }">{{ row.qtyAllocated ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.whReplenish.qtyAvailable')" width="90">
            <template #default="{ row }">{{ row.qtyAvailable ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.whReplenish.qtyOnOrder')" width="90">
            <template #default="{ row }">{{ row.qtyOnOrder ?? '—' }}</template>
          </el-table-column>
          <el-table-column :label="$t('bpcs.whReplenish.lastTxnDate')" width="100">
            <template #default="{ row }">{{ row.lastTxnDate || '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.whReplenish.pct')" width="70">
            <template #default="{ row }">{{ row.pct != null ? row.pct + '%' : '—' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { searchWarehouseReplenish, type WarehouseReplenishItem } from '@/api/bpcs'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsWhReplenish' })

const query = reactive({ item: '', itdsc: '', belowSafetyOnly: false })
const detailVisible = ref(false)
const detailItem = ref<WarehouseReplenishItem | null>(null)

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<WarehouseReplenishItem>({
  fetchApi: (params) => {
    const p: Record<string, string | number | boolean> = { current: params.current!, size: params.size! }
    if (query.item.trim()) p.item = query.item.trim()
    if (query.itdsc.trim()) p.itdsc = query.itdsc.trim()
    if (query.belowSafetyOnly) p.belowSafetyOnly = true
    return searchWarehouseReplenish(p)
  },
})

function openDetail(row: WarehouseReplenishItem) {
  detailItem.value = row
  detailVisible.value = true
}

function fmtShortage(val: number | null): string {
  if (val == null) return '—'
  if (val <= 0) return '0'
  return '+' + val
}

function getShortageClass(val: number | null): string {
  if (val == null || val <= 0) return ''
  return 'text-danger'
}
</script>
