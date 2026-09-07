<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="activeTab" @change="load">
        <el-option value="otif" :label="$t('bpcs.otif.title')" />
        <el-option value="atp" :label="$t('bpcs.atp.title')" />
        <el-option value="disruption" :label="$t('bpcs.disruption.title')" />
        <el-option value="crossNode" :label="$t('bpcs.crossNode.title')" />
      </el-select>
    </div>
    <div class="table-wrapper" v-loading="loading">
      <!-- OTIF Panel -->
      <template v-if="activeTab === 'otif' && otifData">
        <el-row :gutter="16" class="mb16">
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.otif.otifRate')" :value="otifData.summary.otifRate" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.otif.fillRate')" :value="otifData.summary.fillRatePct" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.otif.avgLeadTime')" :value="otifData.summary.avgLeadTimeDays" :suffix="$t('common.unit.days')" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.otif.disruptions')" :value="otifData.summary.totalDisruptions" />
          </el-col>
        </el-row>
        <el-row :gutter="16" class="mb16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.otif.monthlyTrend') }}</template>
              <div ref="otifTrendRef" class="h-300"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.otif.byCustomer') }}</template>
              <el-table :data="otifData.byCustomer" size="small" border max-height="300">
                <el-table-column prop="partyName" :label="$t('bpcs.common.customer')" min-width="150" />
                <el-table-column prop="otifRate" :label="$t('bpcs.otif.otifRate')" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.otifRate >= 90 ? 'success' : row.otifRate >= 80 ? 'warning' : 'danger'" size="small">
                      {{ row.otifRate.toFixed(1) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="totalOrders" :label="$t('bpcs.otif.totalShipments')" width="80" />
                <el-table-column prop="rating" :label="$t('bpcs.otif.rating')" width="70" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- ATP Panel -->
      <template v-if="activeTab === 'atp' && atpData">
        <el-row :gutter="16" class="mb16">
          <el-col :span="4">
            <el-statistic :title="$t('bpcs.atp.totalItems')" :value="atpData.summary.totalItems" />
          </el-col>
          <el-col :span="5">
            <el-statistic :title="$t('bpcs.atp.atpSufficient')" :value="atpData.summary.atpSufficient">
              <template #suffix><span class="text-success">✓</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="5">
            <el-statistic :title="$t('bpcs.atp.atpShortage')" :value="atpData.summary.atpShortage">
              <template #suffix><span class="text-danger">✗</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="5">
            <el-statistic :title="$t('bpcs.atp.overallFillRate')" :value="atpData.summary.overallFillRate" suffix="%" />
          </el-col>
          <el-col :span="5">
            <el-statistic :title="$t('bpcs.atp.avgPromiseDays')" :value="atpData.summary.avgPromiseDays" />
          </el-col>
        </el-row>
        <el-row :gutter="16" class="mb16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.atp.timePhased') }}</template>
              <div ref="atpTrendRef" class="h-300"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.atp.linePromises') }}</template>
              <el-table :data="atpData.linePromises" size="small" border max-height="300">
                <el-table-column prop="orno" :label="$t('bpcs.atp.orno')" width="120" />
                <el-table-column prop="item" :label="$t('bpcs.atp.item')" width="100" />
                <el-table-column prop="requestedQty" :label="$t('bpcs.atp.requestedQty')" width="70" />
                <el-table-column prop="requestedDate" :label="$t('bpcs.atp.requestedDate')" width="100" />
                <el-table-column prop="earliestDate" :label="$t('bpcs.atp.earliestDate')" width="100" />
                <el-table-column prop="promiseStatus" :label="$t('common.status')" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.promiseStatus === 'CONFIRMED' ? 'success' : row.promiseStatus === 'PARTIAL' ? 'warning' : 'danger'" size="small">
                      {{ $t('bpcs.atp.' + row.promiseStatus.toLowerCase()) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <!-- ATP vs OTIF Deviation -->
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.atp.deviation') }}</template>
              <el-table :data="atpData.deviations" size="small" border>
                <el-table-column prop="item" :label="$t('bpcs.atp.item')" width="100" />
                <el-table-column prop="itemDesc" :label="$t('common.description')" min-width="130" />
                <el-table-column prop="atpAccuracy" :label="$t('bpcs.atp.atpAccuracy')" width="90">
                  <template #default="{ row }">
                    <span :class="row.atpAccuracy >= 85 ? 'text-success' : 'text-danger'">{{ row.atpAccuracy.toFixed(1) }}%</span>
                  </template>
                </el-table-column>
                <el-table-column prop="otifRate" :label="$t('bpcs.atp.otifRate')" width="90">
                  <template #default="{ row }">{{ row.otifRate.toFixed(1) }}%</template>
                </el-table-column>
                <el-table-column prop="deviationPct" :label="$t('bpcs.atp.deviationPct')" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.deviationPct > 5 ? 'danger' : row.deviationPct > 0 ? 'warning' : 'success'" size="small">
                      {{ row.deviationPct > 0 ? '+' : '' }}{{ row.deviationPct.toFixed(1) }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="totalPromises" :label="$t('bpcs.atp.totalPromises')" width="80" />
                <el-table-column prop="rootCause" :label="$t('bpcs.atp.rootCause')" min-width="160" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.atp.deviation') }} — {{ $t('bpcs.atp.recommendation') }}</template>
              <el-table :data="atpData.deviations" size="small" border>
                <el-table-column prop="partyName" :label="$t('bpcs.common.customer')" width="120" />
                <el-table-column prop="fulfilledOnTime" :label="$t('bpcs.atp.fulfilledOnTime')" width="80" />
                <el-table-column prop="recommendation" :label="$t('bpcs.atp.recommendation')" min-width="200" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- Disruption Panel -->
      <template v-if="activeTab === 'disruption' && disruptionData">
        <el-row :gutter="16" class="mb16">
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.disruption.activeAlerts')" :value="disruptionData.summary.activeAlerts" />
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.disruption.critical')" :value="disruptionData.summary.criticalCount">
              <template #suffix><span class="text-danger">●</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.disruption.warning')" :value="disruptionData.summary.warningCount">
              <template #suffix><span class="text-warning">●</span></template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic :title="$t('bpcs.disruption.info')" :value="disruptionData.summary.infoCount" />
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.disruption.events') }}</template>
              <el-table :data="disruptionData.events" size="small" border max-height="350">
                <el-table-column prop="severity" :label="$t('common.status')" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.severity === 'CRITICAL' ? 'danger' : row.severity === 'WARNING' ? 'warning' : 'info'" size="small">
                      {{ row.severity }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="title" :label="$t('common.description')" min-width="200" />
                <el-table-column prop="affectedItem" :label="$t('bpcs.common.itemCode')" width="100" />
                <el-table-column prop="detectedTime" :label="$t('common.time')" width="140" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.disruption.riskItems') }}</template>
              <el-table :data="disruptionData.riskItems" size="small" border max-height="350">
                <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="100" />
                <el-table-column prop="itemDesc" :label="$t('common.description')" min-width="120" />
                <el-table-column prop="riskLevel" label="Risk" width="70">
                  <template #default="{ row }">
                    <el-tag :type="row.riskLevel === 'HIGH' ? 'danger' : 'warning'" size="small">{{ row.riskLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="daysOfSupply" :label="$t('bpcs.disruption.daysOfSupply')" width="70" />
                <el-table-column prop="recommendation" :label="$t('bpcs.disruption.recommendation')" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- Cross-Node Panel -->
      <template v-if="activeTab === 'crossNode' && crossNodeData">
        <el-row :gutter="16" class="mb16">
          <el-col :span="24">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.crossNode.nodes') }}</template>
              <el-table :data="crossNodeData.nodes" size="small" border>
                <el-table-column prop="nodeId" :label="$t('col.id')" width="60" />
                <el-table-column prop="nodeName" :label="$t('bpcs.common.warehouse')" width="120" />
                <el-table-column prop="nodeType" :label="$t('bpcs.crossNode.nodeType')" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.nodeType === 'FACTORY' ? 'warning' : undefined" size="small">
                      {{ row.nodeType === 'FACTORY' ? $t('bpcs.crossNode.factory') : $t('bpcs.crossNode.warehouse') }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="totalItems" :label="$t('bpcs.crossNode.totalItems')" width="80" />
                <el-table-column prop="totalOnHand" :label="$t('bpcs.crossNode.totalOnHand')" width="90" />
                <el-table-column prop="capacityPct" :label="$t('bpcs.crossNode.capacityPct')" width="100">
                  <template #default="{ row }">
                    <el-progress :percentage="row.capacityPct" :color="row.capacityPct > 80 ? '#F56C6C' : '#67C23A'" :stroke-width="14" :text-inside="true" />
                  </template>
                </el-table-column>
                <el-table-column prop="alertCount" :label="$t('bpcs.crossNode.alertCount')" width="80" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="16" class="mb16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.crossNode.imbalances') }}</template>
              <el-table :data="crossNodeData.imbalances" size="small" border>
                <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="100" />
                <el-table-column prop="itemDesc" :label="$t('common.description')" min-width="120" />
                <el-table-column prop="imbalanceIndex" :label="$t('bpcs.crossNode.imbalanceIndex')" width="90">
                  <template #default="{ row }">
                    <span :class="row.imbalanceIndex > 0.8 ? 'text-danger' : 'text-success'">{{ (row.imbalanceIndex * 100).toFixed(0) }}%</span>
                  </template>
                </el-table-column>
                <el-table-column prop="recommendation" :label="$t('bpcs.disruption.recommendation')" min-width="200" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>{{ $t('bpcs.crossNode.heatmap') }}</template>
              <div ref="heatmapRef" class="h-280"></div>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsControlTower' })

import { ref, onMounted, nextTick } from 'vue'
import { useECharts, type ECOption } from '@/composables/useECharts'
import { useI18n } from 'vue-i18n'
import { CHART_COLORS } from '@/constants/chart'
import {
  getOtifTracking, getDisruptionAlerts, getCrossNodeInventory, getAtpOverview,
  type OtifResult, type DisruptionResult, type CrossNodeResult, type AtpResult
} from '@/api/bpcs'

const { t } = useI18n()
const activeTab = ref('otif')
const loading = ref(false)

const otifData = ref<OtifResult | null>(null)
const atpData = ref<AtpResult | null>(null)
const disruptionData = ref<DisruptionResult | null>(null)
const crossNodeData = ref<CrossNodeResult | null>(null)

const otifTrendRef = ref<HTMLDivElement>()
const atpTrendRef = ref<HTMLDivElement>()
const heatmapRef = ref<HTMLDivElement>()

const otifTrendChart = useECharts(otifTrendRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: '%', min: 60, max: 100 },
  series: [
    { name: t('bpcs.otif.otifRate'), type: 'line', smooth: true, data: [], itemStyle: { color: CHART_COLORS.primary }, areaStyle: { opacity: 0.1 } },
  ],
}))

const atpTrendChart = useECharts(atpTrendRef, (): ECOption => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: 'Qty' },
  series: [
    { name: t('bpcs.atp.onHand'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.success } },
    { name: t('bpcs.atp.plannedReceipt'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.primary } },
    { name: t('bpcs.atp.committedDemand'), type: 'bar', data: [], itemStyle: { color: CHART_COLORS.warning } },
    { name: t('bpcs.atp.cumAtpQty'), type: 'line', smooth: true, data: [], itemStyle: { color: CHART_COLORS.danger }, lineStyle: { width: 2 } },
  ],
}))

const heatmapChart = useECharts(heatmapRef, (): ECOption => ({
  tooltip: { position: 'top' },
  grid: { top: 10, bottom: 40, left: 80, right: 20 },
  xAxis: { type: 'category', data: [], splitArea: { show: true } },
  yAxis: { type: 'category', data: [], splitArea: { show: true } },
  visualMap: { min: 0, max: 500, calculable: true, orient: 'horizontal', left: 'center', bottom: 0, itemWidth: 12, itemHeight: 100 },
  series: [{ type: 'heatmap', data: [], label: { show: true } }],
}))

async function load() {
  loading.value = true
  try {
    if (activeTab.value === 'otif') {
      const data = await getOtifTracking({ months: 6 })
      otifData.value = data
      await nextTick()
      otifTrendChart.setOption({
        xAxis: { data: data.monthlyTrend.map(t => t.ym) },
        series: [{ data: data.monthlyTrend.map(t => t.otifRate) }],
      })
    } else if (activeTab.value === 'atp') {
      const data = await getAtpOverview({ weeks: 8 })
      atpData.value = data
      await nextTick()
      atpTrendChart.setOption({
        xAxis: { data: data.timePhased.map(p => p.period) },
        series: [
          { data: data.timePhased.map(p => p.onHand) },
          { data: data.timePhased.map(p => p.plannedReceipt) },
          { data: data.timePhased.map(p => p.committedDemand) },
          { data: data.timePhased.map(p => p.cumAtpQty) },
        ],
      })
    } else if (activeTab.value === 'disruption') {
      disruptionData.value = await getDisruptionAlerts({ limit: 20 })
    } else if (activeTab.value === 'crossNode') {
      const data = await getCrossNodeInventory()
      crossNodeData.value = data
      await nextTick()
      const hmData: [number, number, number][] = []
      data.heatmap.data.forEach((row, yi) => {
        row.forEach((val, xi) => { hmData.push([xi, yi, val]) })
      })
      heatmapChart.setOption({
        xAxis: { data: data.heatmap.warehouses },
        yAxis: { data: data.heatmap.items },
        series: [{ data: hmData }],
      })
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
