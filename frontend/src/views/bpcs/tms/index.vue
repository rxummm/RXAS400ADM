<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button-group>
        <el-button :type="activeTab === 'routes' ? 'primary' : ''" @click="switchTab('routes')">{{ $t('bpcs.tms.routePlan') }}</el-button>
        <el-button :type="activeTab === 'carriers' ? 'primary' : ''" @click="switchTab('carriers')">{{ $t('bpcs.tms.carrierComparison') }}</el-button>
        <el-button :type="activeTab === 'freight' ? 'primary' : ''" @click="switchTab('freight')">{{ $t('bpcs.tms.freightAnalysis') }}</el-button>
        <el-button :type="activeTab === 'tracking' ? 'primary' : ''" @click="switchTab('tracking')">{{ $t('bpcs.tms.deliveryTracking') }}</el-button>
      </el-button-group>
    </div>
    <div class="table-wrapper" v-loading="loading">
      <!-- Route Plans -->
      <template v-if="activeTab === 'routes'">
        <el-table :data="tmsData?.routePlans || []" size="small" border>
          <el-table-column prop="routeId" :label="$t('bpcs.tms.routeId')" width="80" />
          <el-table-column prop="origin" :label="$t('bpcs.tms.origin')" width="80" />
          <el-table-column prop="destination" :label="$t('bpcs.tms.destination')" width="80" />
          <el-table-column prop="distanceKm" :label="$t('bpcs.tms.distance')" width="90" />
          <el-table-column prop="estimatedHours" :label="$t('bpcs.tms.estHours')" width="80" />
          <el-table-column prop="carrier" :label="$t('bpcs.tms.carrier')" width="90" />
          <el-table-column prop="estimatedCost" :label="$t('bpcs.tms.estCost')" width="100">
            <template #default="{ row }">¥{{ row.estimatedCost.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('bpcs.tms.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'IN_TRANSIT' ? undefined : 'info'" size="small">
                {{ row.status === 'COMPLETED' ? $t('bpcs.tms.completed') : row.status === 'IN_TRANSIT' ? $t('bpcs.tms.inTransit') : $t('bpcs.tms.pending') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="orderNos" :label="$t('bpcs.tms.orderNos')" min-width="150">
            <template #default="{ row }">{{ row.orderNos.join(', ') }}</template>
          </el-table-column>
        </el-table>
      </template>

      <!-- Carrier Comparison -->
      <template v-if="activeTab === 'carriers'">
        <el-table :data="tmsData?.carrierComparisons || []" size="small" border>
          <el-table-column prop="carrierName" :label="$t('bpcs.tms.carrier')" width="120" />
          <el-table-column prop="rating" :label="$t('bpcs.tms.rating')" width="70" />
          <el-table-column prop="totalShipments" :label="$t('bpcs.tms.totalShipments')" width="100" />
          <el-table-column prop="onTimeRate" :label="$t('bpcs.tms.onTimeRate')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.onTimeRate >= 90 ? 'success' : row.onTimeRate >= 80 ? 'warning' : 'danger'" size="small">
                {{ row.onTimeRate.toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="avgCostPerKg" :label="$t('bpcs.tms.avgCostKg')" width="120">
            <template #default="{ row }">¥{{ row.avgCostPerKg.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="avgTransitDays" :label="$t('bpcs.tms.avgTransit')" width="100">
            <template #default="{ row }">{{ row.avgTransitDays }} {{ $t('common.unit.days') }}</template>
          </el-table-column>
          <el-table-column prop="serviceLevel" :label="$t('bpcs.tms.serviceLevel')" width="80" />
          <el-table-column prop="recommended" :label="$t('bpcs.tms.recommended')" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.recommended" type="success" size="small">★ {{ $t('bpcs.tms.recommended') }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- Freight Analysis -->
      <template v-if="activeTab === 'freight' && tmsData?.freightAnalysis">
        <el-row :gutter="16" class="mb16">
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.tms.totalCost')" :value="tmsData.freightAnalysis.totalFreightCost" prefix="¥" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.tms.avgCostShipment')" :value="tmsData.freightAnalysis.avgCostPerShipment" prefix="¥" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.tms.avgCostKg2')" :value="tmsData.freightAnalysis.avgCostPerKg" prefix="¥" suffix="/kg" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.tms.costChange')" :value="tmsData.freightAnalysis.costChangePct" suffix="%" />
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.tms.carrierShare') }}</template>
              <div ref="carrierPieRef" class="h-280"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.tms.monthlyTrend') }}</template>
              <div ref="freightTrendRef" class="h-280"></div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- Delivery Tracking -->
      <template v-if="activeTab === 'tracking'">
        <el-table :data="tmsData?.deliveryTrackings || []" size="small" border row-key="loadNo" @expand-change="handleExpand">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="p16">
                <el-timeline v-if="row.events && row.events.length">
                  <el-timeline-item v-for="(evt, i) in row.events" :key="i" :timestamp="evt.timestamp" placement="top">
                    <strong>{{ evt.location }}</strong> - {{ evt.event }}<br />
                    <span class="text-muted">{{ evt.detail }}</span>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else :description="$t('bpcs.tms.trackingEvents') + ': -'" :image-size="60" />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="loadNo" :label="$t('bpcs.tms.loadNo')" width="100" />
          <el-table-column prop="orderNo" :label="$t('bpcs.tms.orderNo')" width="100" />
          <el-table-column prop="carrier" :label="$t('bpcs.tms.carrier')" width="90" />
          <el-table-column prop="origin" :label="$t('bpcs.tms.origin2')" width="60" />
          <el-table-column prop="destination" :label="$t('bpcs.tms.dest2')" width="60" />
          <el-table-column prop="shipDate" :label="$t('bpcs.tms.shipDate')" width="100" />
          <el-table-column prop="estimatedArrival" :label="$t('bpcs.tms.estArrival')" width="100" />
          <el-table-column prop="actualArrival" :label="$t('bpcs.tms.actArrival')" width="100">
            <template #default="{ row }">{{ row.actualArrival || '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('common.status')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'DELIVERED' ? 'success' : row.status === 'IN_TRANSIT' ? undefined : 'info'" size="small">
                {{ row.statusKey }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="signedBy" :label="$t('bpcs.tms.signedBy')" width="80">
            <template #default="{ row }">{{ row.signedBy || '-' }}</template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsTms' })

import { ref, onMounted, nextTick } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { useI18n } from 'vue-i18n'
import { CHART_COLORS } from '@/constants/chart'
import { getTmsAll, type TmsLiteResult } from '@/api/bpcs'

const { t } = useI18n()
const activeTab = ref('routes')
const loading = ref(false)
const tmsData = ref<TmsLiteResult | null>(null)

const carrierPieRef = ref<HTMLDivElement>()
const freightTrendRef = ref<HTMLDivElement>()

const carrierPieChart = useECharts(carrierPieRef, (): ECOption => ({
  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
  series: [{ type: 'pie', radius: ['40%', '70%'], data: [] }],
}))

const freightTrendChart = useECharts(freightTrendRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: '¥' },
    { type: 'value', name: t('bpcs.tms.totalShipments'), position: 'right' },
  ],
  series: [
    { name: t('bpcs.tms.totalCost'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.primary } },
    { name: t('bpcs.tms.totalShipments'), type: 'line', yAxisIndex: 1, data: [], itemStyle: { color: CHART_COLORS.success } },
  ],
}))

function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'freight' && tmsData.value?.freightAnalysis) {
    nextTick(renderFreightCharts)
  }
}

function renderFreightCharts() {
  const fa = tmsData.value?.freightAnalysis
  if (!fa) return
  carrierPieChart.setOption({
    series: [{ data: fa.carrierShares.map(s => ({ name: s.carrier, value: s.totalCost })) }],
  })
  freightTrendChart.setOption({
    xAxis: { data: fa.monthlyTrends.map(m => m.ym) },
    series: [
      { data: fa.monthlyTrends.map(m => m.totalCost) },
      { data: fa.monthlyTrends.map(m => m.shipmentCount) },
    ],
  })
}

function handleExpand() {}

async function load() {
  loading.value = true
  try {
    const data = await getTmsAll({ cono: '001', months: 6 })
    tmsData.value = data
    if (activeTab.value === 'freight') {
      await nextTick()
      renderFreightCharts()
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
