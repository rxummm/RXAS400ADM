<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="query.pono"
        class="w-200"
        :placeholder="$t('bpcs.purchase.ponoPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-input
        v-model="query.vendor"
        class="w-200"
        :placeholder="$t('bpcs.purchase.vendorPlaceholder')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <!-- ── 表格 ─────────────────────────────────────────── -->
    <div class="table-wrapper">
      <el-table
        :data="orders"
        v-loading="loading"
        size="small"
        border
        highlight-current-row
        @row-click="openDetail"
      >
        <el-table-column prop="pono" :label="$t('bpcs.purchase.pono')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="vendorName" :label="$t('bpcs.purchase.vendor')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderDate" :label="$t('bpcs.purchase.orderDate')" width="110" />
        <el-table-column prop="reqDate" :label="$t('bpcs.label.reqDate')" width="110" />
        <el-table-column align="right" :label="$t('bpcs.purchase.totalAmount')" width="120">
          <template #default="{ row }">{{ fmtAmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column align="center" :label="$t('bpcs.purchase.lineCount')" width="70">
          <template #default="{ row }">{{ row.lineCount }}</template>
        </el-table-column>
        <el-table-column :label="$t('bpcs.purchase.status')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">
              {{ $t(row.statusKey) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row as BpcsPurchaseOrder)">
              {{ $t('common.detail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      <el-empty v-if="searched && !loading && orders.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- ── 详情抽屉 ──────────────────────────────────── -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('bpcs.purchase.detailTitle') + ' · ' + (detailOrder?.pono || '')"
      size="600px"
      :close-on-click-modal="false"
    >
      <template v-if="detailOrder">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.purchase.pono')">{{ detailOrder.pono }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.purchase.status')">
            <el-tag size="small" :type="statusTagType(detailOrder.status)">
              {{ $t(detailOrder.statusKey) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.purchase.vendor')">
            {{ detailOrder.vendorName || detailOrder.vendor || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.purchase.totalAmount')">
            {{ fmtAmt(detailOrder.totalAmount) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.purchase.orderDate')">
            {{ detailOrder.orderDate || '—' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.reqDate')">
            {{ detailOrder.reqDate || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.purchase.linesTitle') }}</h4>
        <el-table :data="detailOrder.lines" size="small" border>
          <el-table-column prop="lineNo" :label="$t('bpcs.line.orln')" width="60" />
          <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="100" show-overflow-tooltip />
          <el-table-column prop="itemDesc" :label="$t('bpcs.line.itemDesc')" min-width="120" show-overflow-tooltip />
          <el-table-column align="right" :label="$t('bpcs.purchase.qtyOrdered')" width="80">
            <template #default="{ row }">{{ row.qtyOrdered ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.purchase.qtyReceived')" width="80">
            <template #default="{ row }">{{ row.qtyReceived ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.price')" width="90">
            <template #default="{ row }">{{ fmtAmt(row.unitPrice) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { searchPurchases, type BpcsPurchaseOrder } from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'
import { formatMoney } from '@/utils/format'

defineOptions({ name: 'BpcsPurchase' })

const query = reactive({ pono: '', vendor: '' })
const loading = ref(false)
const searched = ref(false)
const orders = ref<BpcsPurchaseOrder[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const detailVisible = ref(false)
const detailOrder = ref<BpcsPurchaseOrder | null>(null)

function search() {
  loading.value = true
  searched.value = true
  const params: Record<string, string | number> = { current: current.value, size: size.value }
  if (query.pono.trim()) params.pono = query.pono.trim()
  if (query.vendor.trim()) params.vendor = query.vendor.trim()
  searchPurchases(params)
    .then(data => {
      orders.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function openDetail(row: BpcsPurchaseOrder) {
  detailOrder.value = row
  detailVisible.value = true
}

const fmtAmt = formatMoney

function statusTagType(status: number): 'info' | 'primary' | 'success' | 'warning' {
  return (['info', 'primary', 'success', 'warning'] as const)[status] ?? 'info'
}
</script>
