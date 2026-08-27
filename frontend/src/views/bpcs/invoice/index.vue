<template>
  <div class="page-container page-container--fit">
    <!-- ── 搜索区 ───────────────────────────────────────── -->
    <div class="search-bar">
      <el-input
        v-model="orno"
        class="w-200"
        :placeholder="$t('bpcs.label.orno')"
        clearable
        @keyup.enter="search"
      />
      <el-button type="primary" :loading="loading" @click="search">
        {{ $t('common.search') }}
      </el-button>
    </div>

    <!-- ── Tab 切换（在制/历史） ────────────────────────── -->
    <div class="table-wrapper">
      <el-tabs v-model="activeTab" @tab-change="search">
        <el-tab-pane :label="$t('bpcs.invoice.activeTab')" name="active" />
        <el-tab-pane :label="$t('bpcs.invoice.historyTab')" name="history" />
      </el-tabs>

      <!-- 汇总卡片 -->
      <div v-if="invoices.length" class="summary-row mb16">
        <div class="summary-item">
          <span class="summary-value">{{ invoices.length }}</span>
          <span class="summary-label">{{ $t('bpcs.invoice.invoiceCount') }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ formatMoney(grandTotal) }}</span>
          <span class="summary-label">{{ $t('bpcs.invoice.totalAmount') }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ formatMoney(grandTax) }}</span>
          <span class="summary-label">{{ $t('bpcs.invoice.totalTax') }}</span>
        </div>
      </div>

      <!-- 发票时间轴 -->
      <div v-loading="loading">
        <el-timeline v-if="invoices.length">
          <el-timeline-item
            v-for="inv in invoices"
            :key="inv.invNo"
            :type="timelineType(inv.status)"
            :hollow="inv.status === 'cancelled'"
            :timestamp="inv.invDate || undefined"
            placement="top"
          >
            <el-card shadow="never" class="invoice-card" @click="openDetail(inv)">
              <div class="invoice-header">
                <span class="invoice-no">{{ inv.invNo }}</span>
                <el-tag size="small" :type="statusTagType(inv.status)">
                  {{ $t(inv.statusKey) }}
                </el-tag>
              </div>
              <div class="invoice-meta">
                <span>{{ $t('bpcs.label.orno') }}: {{ inv.orno || '—' }}</span>
                <span class="ml8">{{ $t('bpcs.label.customer') }}: {{ inv.custName || inv.cust || '—' }}</span>
              </div>
              <div class="invoice-amounts">
                <span>{{ $t('bpcs.invoice.totalAmount') }}: <b>{{ formatMoney(inv.totalAmount) }}</b></span>
                <span class="ml8">{{ $t('bpcs.invoice.lineCount') }}: {{ inv.lineCount }}</span>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
        <el-empty v-if="!loading && invoices.length === 0" :description="$t('common.noData')" />
      </div>
    </div>

    <!-- ── 发票行详情抽屉 ──────────────────────────────── -->
    <el-drawer
      v-model="detailVisible"
      :title="$t('bpcs.invoice.detailTitle') + ' · ' + (detailInv?.invNo || '')"
      size="560px"
      :close-on-click-modal="false"
    >
      <template v-if="detailInv">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item :label="$t('bpcs.invoice.invNo')">{{ detailInv.invNo }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.invoice.status')">
            <el-tag size="small" :type="statusTagType(detailInv.status)">{{ $t(detailInv.statusKey) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.orno')">{{ detailInv.orno || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.label.customer')">{{ detailInv.custName || detailInv.cust || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.invoice.invDate')">{{ detailInv.invDate || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.invoice.totalAmount')">{{ formatMoney(detailInv.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('bpcs.invoice.taxAmount')">{{ formatMoney(detailInv.taxAmount) }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section">{{ $t('bpcs.invoice.linesTitle') }}</h4>
        <el-table :data="detailInv.lines" size="small" border>
          <el-table-column prop="lineNo" :label="$t('bpcs.line.orln')" width="60" />
          <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="100" show-overflow-tooltip />
          <el-table-column prop="itemDesc" :label="$t('bpcs.line.itemDesc')" min-width="120" show-overflow-tooltip />
          <el-table-column align="right" :label="$t('bpcs.line.qtyOrdered')" width="80">
            <template #default="{ row }">{{ row.qty ?? '—' }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.price')" width="90">
            <template #default="{ row }">{{ fmtAmt(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.invoice.lineAmount')" width="100">
            <template #default="{ row }">{{ fmtAmt(row.lineAmount) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { searchInvoices, type BpcsInvoice } from '@/api/bpcs'
import { formatMoney } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsInvoice' })

const activeTab = ref('active')
const orno = ref('')
const loading = ref(false)
const invoices = ref<BpcsInvoice[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const detailVisible = ref(false)
const detailInv = ref<BpcsInvoice | null>(null)

const grandTotal = computed(() => invoices.value.reduce((s, i) => s + (i.totalAmount ?? 0), 0))
const grandTax = computed(() => invoices.value.reduce((s, i) => s + (i.taxAmount ?? 0), 0))

function search() {
  loading.value = true
  const params: Record<string, string | number> = { tab: activeTab.value, current: current.value, size: size.value }
  if (orno.value.trim()) params.orno = orno.value.trim()
  searchInvoices(params)
    .then(data => {
      invoices.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function openDetail(inv: BpcsInvoice) {
  detailInv.value = inv
  detailVisible.value = true
}

const fmtAmt = formatMoney
function timelineType(status: string | null): 'primary' | 'success' | 'info' | 'danger' {
  if (status === 'posted') return 'success'
  if (status === 'cancelled') return 'danger'
  return 'primary'
}
function statusTagType(status: string | null): 'success' | 'danger' | 'primary' {
  if (status === 'posted') return 'success'
  if (status === 'cancelled') return 'danger'
  return 'primary'
}
</script>

<style scoped>
.summary-row {
  display: flex;
  gap: 24px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
}
.invoice-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.invoice-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.invoice-no {
  font-weight: 600;
  font-size: 14px;
}
.invoice-meta, .invoice-amounts {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
