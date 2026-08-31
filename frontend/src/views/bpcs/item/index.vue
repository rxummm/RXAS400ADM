<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        class="w-200"
        :placeholder="$t('bpcs.item.searchPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
      <ExportDropdown
        :data="items"
        :columns="exportColumns"
        :title="$t('bpcs.menu.items')"
        :export-url="BPCS_EXPORT.items"
        :query-params="{ item: keyword }"
      />
    </div>

    <div class="item-layout">
      <!-- ── 左侧：物料列表 ──────────────────────────── -->
      <div class="item-list-panel">
        <div class="item-list" v-loading="loading">
          <div
            v-for="it in items"
            :key="it.item"
            class="item-entry"
            :class="{ active: selectedItem?.item === it.item }"
            @click="selectItem(it)"
          >
            <div class="item-name">{{ it.description || it.item }}</div>
            <div class="item-code">{{ it.item }} · {{ it.uom || '—' }}</div>
          </div>
          <el-empty v-if="!loading && items.length === 0" :image-size="60" />
        </div>
        <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      </div>

      <!-- ── 右侧：物料详情 Tab ──────────────────────── -->
      <div class="item-detail-panel">
        <template v-if="detail">
          <!-- 物料头卡片 -->
          <el-card shadow="never" class="mb16">
            <template #header>
              <div class="flex-row-center">
                <span class="mr4">{{ detail.description || '—' }}</span>
                <el-tag size="small" type="info">{{ detail.item }}</el-tag>
                <el-tag v-if="detail.category" size="small" class="ml8">{{ detail.category }}</el-tag>
              </div>
            </template>
            <el-descriptions :column="4" size="small">
              <el-descriptions-item :label="$t('bpcs.item.uom')">{{ detail.uom || '—' }}</el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.item.unitCost')">{{ fmtMoney(detail.unitCost) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.item.listPrice')">{{ fmtMoney(detail.listPrice) }}</el-descriptions-item>
              <el-descriptions-item :label="$t('bpcs.item.weight')">{{ detail.weight != null ? detail.weight + ' kg' : '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 多 Tab 详情 -->
          <el-tabs v-model="activeTab" type="border-card">
            <!-- Tab 1: 库存 -->
            <el-tab-pane :label="$t('bpcs.item.tabInventory')" name="inventory">
              <div class="inv-summary mb16">
                <div class="inv-stat">
                  <span class="inv-stat-value">{{ detail.totalOnHand }}</span>
                  <span class="inv-stat-label">{{ $t('bpcs.inventory.onHand') }}</span>
                </div>
                <div class="inv-stat">
                  <span class="inv-stat-value">{{ detail.totalAllocated }}</span>
                  <span class="inv-stat-label">{{ $t('bpcs.inventory.allocated') }}</span>
                </div>
                <div class="inv-stat inv-stat--accent">
                  <span class="inv-stat-value">{{ detail.totalAvailable }}</span>
                  <span class="inv-stat-label">{{ $t('bpcs.inventory.available') }}</span>
                </div>
              </div>
              <el-table :data="detail.warehouses" size="small" border>
                <el-table-column prop="wh" :label="$t('bpcs.label.wh')" width="80" />
                <el-table-column prop="location" :label="$t('bpcs.inventory.location')" width="100" />
                <el-table-column align="right" :label="$t('bpcs.inventory.onHand')" width="90">
                  <template #default="{ row }">{{ row.onHand }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.allocated')" width="90">
                  <template #default="{ row }">{{ row.allocated }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.onOrder')" width="90">
                  <template #default="{ row }">{{ row.onOrder }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.inventory.available')" width="90">
                  <template #default="{ row }">
                    <span :class="{ 'text-danger': row.available < 0 }">{{ row.available }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Tab 2: 采购历史 -->
            <el-tab-pane :label="$t('bpcs.item.tabPurchase')" name="purchase">
              <el-table :data="detail.recentPurchases" size="small" border>
                <el-table-column prop="pono" :label="$t('bpcs.purchase.pono')" min-width="140" show-overflow-tooltip />
                <el-table-column prop="vendorName" :label="$t('bpcs.purchase.vendor')" min-width="160" show-overflow-tooltip />
                <el-table-column prop="orderDate" :label="$t('bpcs.purchase.orderDate')" width="110" />
                <el-table-column align="right" :label="$t('bpcs.purchase.qtyOrdered')" width="80">
                  <template #default="{ row }">{{ row.qty ?? '—' }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.line.price')" width="100">
                  <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.recentPurchases.length" :description="$t('common.noData')" :image-size="60" />
            </el-tab-pane>

            <!-- Tab 3: 销售历史 -->
            <el-tab-pane :label="$t('bpcs.item.tabSales')" name="sales">
              <el-table :data="detail.recentSales" size="small" border>
                <el-table-column prop="orno" :label="$t('bpcs.label.orno')" min-width="140" show-overflow-tooltip />
                <el-table-column prop="custName" :label="$t('bpcs.label.customer')" min-width="160" show-overflow-tooltip />
                <el-table-column prop="orderDate" :label="$t('bpcs.purchase.orderDate')" width="110" />
                <el-table-column align="right" :label="$t('bpcs.line.qtyOrdered')" width="80">
                  <template #default="{ row }">{{ row.qty ?? '—' }}</template>
                </el-table-column>
                <el-table-column align="right" :label="$t('bpcs.line.price')" width="100">
                  <template #default="{ row }">{{ fmtMoney(row.unitPrice) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!detail.recentSales.length" :description="$t('common.noData')" :image-size="60" />
            </el-tab-pane>

            <!-- Tab 4: 基本信息 -->
            <el-tab-pane :label="$t('bpcs.item.tabBasic')" name="basic">
              <el-descriptions :column="2" size="small" border>
                <el-descriptions-item :label="$t('bpcs.line.item')">{{ detail.item }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.line.itemDesc')">{{ detail.description || '—' }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.uom')">{{ detail.uom || '—' }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.category')">{{ detail.category || '—' }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.unitCost')">{{ fmtMoney(detail.unitCost) }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.listPrice')">{{ fmtMoney(detail.listPrice) }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.weight')">{{ detail.weight != null ? detail.weight + ' kg' : '—' }}</el-descriptions-item>
                <el-descriptions-item :label="$t('bpcs.item.shelfLife')">{{ detail.shelfLife != null ? detail.shelfLife + ' 天' : '—' }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else :description="$t('bpcs.item.selectHint')" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchItems, getItemDetail, type BpcsItem, BPCS_EXPORT } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'
import ExportDropdown from '@/components/ExportDropdown.vue'
import type { ExportColumn } from '@/components/ExportButton.vue'

defineOptions({ name: 'BpcsItem' })

const keyword = ref('')
const loading = ref(false)
const items = ref<BpcsItem[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const selectedItem = ref<BpcsItem | null>(null)
const detail = ref<BpcsItem | null>(null)
const activeTab = ref('inventory')

const exportColumns: ExportColumn[] = [
  { key: 'item', label: '物料号' },
  { key: 'description', label: '描述' },
  { key: 'uom', label: '单位' },
  { key: 'category', label: '分类' },
  { key: 'unitCost', label: '单位成本' },
  { key: 'listPrice', label: '列表价' },
  { key: 'totalOnHand', label: '在手量' },
  { key: 'totalAllocated', label: '已分配' },
  { key: 'totalAvailable', label: '可用量' },
]

function search() {
  loading.value = true
  const params: Record<string, string | number> = { current: current.value, size: size.value }
  const kw = keyword.value.trim()
  if (kw) {
    if (/^[A-Z]/.test(kw)) params.item = kw
    else params.desc = kw
  }
  searchItems(params)
    .then(data => {
      items.value = data.records
      total.value = data.total
      if (data.records.length === 1) selectItem(data.records[0])
    })
    .finally(() => { loading.value = false })
}

function selectItem(item: BpcsItem) {
  selectedItem.value = item
  activeTab.value = 'inventory'
  // 加载详情（含库存/采购/销售）
  loading.value = true
  getItemDetail(item.item)
    .then(d => { detail.value = d })
    .finally(() => { loading.value = false })
}

function fmtMoney(v: number | null): string {
  return v == null ? '—' : v.toLocaleString(undefined, { minimumFractionDigits: 2 })
}
</script>

<style scoped>
.item-layout {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}
.item-list-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.item-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}
.item-entry {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
}
.item-entry:hover {
  background: var(--el-fill-color-light);
}
.item-entry.active {
  background: var(--el-color-primary-light-9);
  border-left: 3px solid var(--color-primary);
}
.item-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.item-code {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.item-detail-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
.inv-summary {
  display: flex;
  gap: 24px;
  padding: 8px 0;
}
.inv-stat {
  text-align: center;
}
.inv-stat--accent .inv-stat-value {
  color: var(--color-primary);
}
.inv-stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
}
.inv-stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
