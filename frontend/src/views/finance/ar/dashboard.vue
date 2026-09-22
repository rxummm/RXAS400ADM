<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="section">{{ $t('ar.dashboard.title') }}</div>
      </template>
      <div class="stats">
        <el-card class="stat-card" shadow="never">
          <template #header>{{ $t('ar.dashboard.openInvoice') }}</template>
          <div class="stat-value">{{ fmtMoney(openTotal) }}</div>
          <div class="stat-sub">{{ $t('ar.dashboard.openCount') }} {{ openCount }}</div>
          <div class="stat-bar" :style="{ width: openBarPct + '%', opacity: openBarPct ? 1 : 0.25 }" />
        </el-card>
        <el-card class="stat-card" shadow="never">
          <template #header>{{ $t('ar.dashboard.overdue') }}</template>
          <div class="stat-value">{{ fmtMoney(overdueTotal) }}</div>
          <div class="stat-sub">{{ $t('ar.dashboard.overdueCount') }} {{ overdueCount }}</div>
          <div class="stat-bar overdue" :style="{ width: overdueBarPct + '%', opacity: overdueBarPct ? 1 : 0.25 }" />
        </el-card>
        <el-card class="stat-card" shadow="never">
          <template #header>{{ $t('ar.dashboard.collectionPct') }}</template>
          <div class="stat-value">{{ collectionPct.toFixed(1) }}%</div>
          <div class="stat-sub">{{ $t('ar.dashboard.totalInvoice') }} {{ totalCount }}</div>
        </el-card>
        <el-card class="stat-card" shadow="never">
          <template #header>{{ $t('ar.dashboard.aging') }}</template>
          <div class="aging-columns">
            <div v-for="col in agingCols" :key="col.bucket" class="aging-col">
              <div class="aging-label">{{ $t('ar.aging' + col.bucket) }}</div>
              <div class="aging-value">{{ fmtMoney(col.total) }}</div>
              <div class="aging-bar" :style="{ width: (col.pct || 0) + '%', opacity: col.pct ? 1 : 0.25, 'background-color': col.color }" />
            </div>
          </div>
        </el-card>
      </div>

      <div class="mt16">
        <div class="section">{{ $t('ar.dashboard.agingChart') }}</div>
        <div class="chart-wrapper">
          <ve-ring v-if="veRingData.length" :data="veRingData" :title="''" :inner-radius="70" :area="false" :center="['50%', '50%']" :outer-ring="false" :padding="25" height="280px" />
          <el-empty v-else :description="$t('common.noData')" />
        </div>
      </div>

      <div class="mt16">
        <div class="section">{{ $t('ar.dashboard.customerRanking') }}</div>
        <el-table :data="topCustomers" size="small" border stripe class="w-full">
          <el-table-column :label="$t('ar.invoice.customerName')" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">{{ (row as Record<string, unknown>).customerName || (row as Record<string, unknown>).customerCode }}</template>
          </el-table-column>
          <el-table-column :label="$t('ar.dashboard.balance')" width="150" align="right">
            <template #default="{ row }">{{ fmtMoney((row as Record<string, unknown>).balance as number | undefined) }}</template>
          </el-table-column>
          <el-table-column :label="$t('ar.dashboard.balancePct')" width="110" align="right">
            <template #default="{ row }">{{ (((row as Record<string, unknown>).balancePct as number | undefined) ?? 0).toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column :label="$t('ar.dashboard.agingStatus')" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="((row as Record<string, unknown>).agingStatus as string) === 'OVERDUE' ? 'danger' : 'success'">{{ (row as Record<string, unknown>).agingStatus as string }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'FinanceArDashboard' })

import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { fetchDashboardSummary, type ArDashboardSummary } from '@/api/ar/dashboard'

const { t } = useI18n()

const loading = ref(false)
const summary = ref<ArDashboardSummary | null>(null)

const openTotal = ref(0)
const openCount = ref(0)
const overdueTotal = ref(0)
const overdueCount = ref(0)
const collectionPct = ref(0)
const totalCount = ref(0)
const agingCols = ref<Array<{ bucket: string; total?: number; color?: string; pct?: number }>>([])
const veRingData: Array<{ name: string; value: number }> = []
const topCustomers = ref<Array<{ customerCode: string; customerName: string; balance: number; balancePct: number; agingStatus: string }>>([])

/** 计算属性：未结发票占比 */
const openBarPct = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((openCount.value / totalCount.value) * 100)
})

/** 计算属性：逾期发票占比 */
const overdueBarPct = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((overdueCount.value / totalCount.value) * 100)
})

function fmtMoney(v: number | undefined) {
  if (v == null) return '—'
  return v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function loadDashboard() {
  loading.value = true
  fetchDashboardSummary()
    .then(d => {
      summary.value = d
      openTotal.value = d.openInvoiceTotal
      openCount.value = d.openInvoiceCount
      overdueTotal.value = d.overdueTotal
      overdueCount.value = d.overdueCount
      collectionPct.value = d.collectionRate == null ? 0 : d.collectionRate * 100
      totalCount.value = d.totalInvoiceCount
      const total = d.agingSummary?.total ?? 0
      agingCols.value = (d.agingSummary?.rows ?? []).map(r => ({
        bucket: r.bucket,
        total: r.total,
        pct: total ? Math.round((r.total / total) * 100) : 0,
        color: r.color,
      }))
      veRingData.length = 0
      for (const c of agingCols.value) {
        veRingData.push({ name: t('ar.aging' + c.bucket) || c.bucket, value: c.total ?? 0 })
      }
      topCustomers.value = d.topCustomers ?? []
    })
    .catch((e: unknown) => {
      ElMessage.error(e instanceof Error ? e.message : String(e))
    })
    .finally(() => { loading.value = false })
}

onMounted(async () => {
  try {
    await loadDashboard()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
})
</script>

<style scoped>
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.stat-card {
  flex: 1;
  min-width: 200px;
  margin: 0;
  padding: 16px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
}
.stat-sub {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.stat-bar {
  height: 6px;
  background-color: var(--el-color-primary);
  border-radius: 3px;
  margin-top: 10px;
  min-width: 20px;
  transition: width 0.3s;
}
.stat-bar.overdue {
  background-color: var(--el-color-danger);
}
.aging-columns {
  display: flex;
  gap: 12px;
}
.aging-col {
  flex: 1;
  position: relative;
  padding: 8px 0;
}
.aging-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.aging-value {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}
.aging-bar {
  height: 8px;
  border-radius: 4px;
  min-width: 4px;
}
.chart-wrapper {
  height: 300px;
  padding: 10px;
}
</style>
