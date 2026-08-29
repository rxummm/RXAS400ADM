<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input v-model="query.orno" class="w-200" :placeholder="$t('bpcs.label.orderNo')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div v-if="tracking" class="table-wrapper">
      <el-card shadow="never" class="mb16">
        <template #header>
          <div class="flex-row-center">
            <span>{{ $t('bpcs.orderTracking.orderPrefix') }}{{ tracking.cono }}-{{ tracking.orno }}</span>
            <el-tag size="small" type="info" class="ml8">{{ tracking.statusLabel }}</el-tag>
            <span class="ml8 text-muted">{{ tracking.custNo }} {{ tracking.custName || '' }}</span>
          </div>
        </template>
        <el-table :data="tracking.lines" size="small" border>
          <el-table-column prop="item" :label="$t('bpcs.line.item')" min-width="120" />
          <el-table-column prop="itemDesc" :label="$t('bpcs.line.itemDesc')" min-width="140" show-overflow-tooltip />
          <el-table-column align="right" :label="$t('bpcs.line.qtyOrdered')" width="70">
          <template #default="{ row }">{{ row.qtyOrdered }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.qtyAllocated')" width="70">
          <template #default="{ row }">{{ row.qtyAllocated }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.qtyShipped')" width="70">
          <template #default="{ row }">{{ row.qtyShipped }}</template>
          </el-table-column>
          <el-table-column align="right" :label="$t('bpcs.line.qtyInvoiced')" width="70">
          <template #default="{ row }">{{ row.qtyInvoiced }}</template>
          </el-table-column>
          <el-table-column prop="shipDate" :label="$t('bpcs.shipping.shipDate')" width="110" />
          <el-table-column prop="shipStatus" :label="$t('bpcs.shipping.status')" width="90" />
        </el-table>
      </el-card>
      <el-timeline class="mt16">
        <el-timeline-item type="primary" :hollow="true">{{ $t('bpcs.orderTracking.stepOrder') }}</el-timeline-item>
        <el-timeline-item :type="stageReached(0) ? 'success' : 'info'" :hollow="!stageReached(0)">{{ $t('bpcs.orderTracking.stepAllocate') }}</el-timeline-item>
        <el-timeline-item :type="stageReached(1) ? 'success' : 'info'" :hollow="!stageReached(1)">{{ $t('bpcs.orderTracking.stepPick') }}</el-timeline-item>
        <el-timeline-item :type="stageReached(2) ? 'success' : 'info'" :hollow="!stageReached(2)">{{ $t('bpcs.orderTracking.stepShip') }}</el-timeline-item>
        <el-timeline-item :type="stageReached(3) ? 'success' : 'info'" :hollow="!stageReached(3)">{{ $t('bpcs.orderTracking.stepInvoice') }}</el-timeline-item>
      </el-timeline>
    </div>
    <el-empty v-if="!loading && searched && !tracking" :description="$t('common.noData')" />
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderTracking' })
import { reactive, ref } from 'vue'
import { fetchOrderTracking, type OrderTracking } from '@/api/supplyChain'
const loading = ref(false)
const searched = ref(false)
const tracking = ref<OrderTracking | null>(null)
const query = reactive({ cono: '001', orno: '' })
function load() {
  if (!query.orno) return
  loading.value = true
  searched.value = true
  fetchOrderTracking(query.cono || '001', query.orno)
    .then(d => { tracking.value = d })
    .finally(() => { loading.value = false })
}
function stageReached(idx: number) {
  if (!tracking.value) return false
  const line = tracking.value.lines[0]
  if (!line) return false
  return [line.qtyAllocated > 0, line.qtyShipped > 0, line.qtyInvoiced > 0, tracking.value.statusLabel === 'Closed'][idx]
}
</script>
