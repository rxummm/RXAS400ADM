<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div v-if="kpi" class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-value">{{ kpi.totalOrders }}</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.totalOrders') }}</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-value">{{ kpi.closedOrders }}</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.closedOrders') }}</div>
      </div>
      <div class="kpi-card kpi-card--primary">
        <div class="kpi-value">{{ kpi.completionRate.toFixed(1) }}%</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.completionRate') }}</div>
      </div>
      <div class="kpi-card kpi-card--warning">
        <div class="kpi-value">{{ kpi.totalItems }}</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.totalItems') }}</div>
      </div>
      <div class="kpi-card kpi-card--info">
        <div class="kpi-value">{{ fmtMoney(kpi.inventoryValue) }}</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.inventoryValue') }}</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-value">{{ kpi.onTimeDeliveryRate.toFixed(1) }}%</div>
        <div class="kpi-label">{{ $t('bpcs.kpi.onTimeDeliveryRate') }}</div>
      </div>
    </div>
    <el-empty v-if="!loading && !kpi" :description="$t('common.noData')" />
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsKpi' })
import { ref } from 'vue'
import { getSupplyChainKpi, type SupplyChainKpi } from '@/api/bpcs'
import { formatMoney } from '@/utils/format'
const cono = ref('001')
const loading = ref(false)
const kpi = ref<SupplyChainKpi | null>(null)
const fmtMoney = (v: number) => formatMoney(v, 0)
function load() {
  loading.value = true
  getSupplyChainKpi({ cono: cono.value || '001' }).then(d => { kpi.value = d }).finally(() => { loading.value = false })
}
load()
</script>
<style scoped>
.kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.kpi-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 24px; text-align: center; }
.kpi-card--success { border-color: var(--color-success); }
.kpi-card--primary { border-color: var(--color-primary); }
.kpi-card--warning { border-color: var(--color-warning); }
.kpi-card--info { border-color: var(--color-info); }
.kpi-value { font-size: 32px; font-weight: 700; color: var(--text-primary); }
.kpi-card--success .kpi-value { color: var(--color-success); }
.kpi-card--primary .kpi-value { color: var(--color-primary); }
.kpi-card--warning .kpi-value { color: var(--color-warning); }
.kpi-card--info .kpi-value { color: var(--color-info); }
.kpi-label { font-size: 13px; color: var(--text-secondary); margin-top: 8px; }
</style>
