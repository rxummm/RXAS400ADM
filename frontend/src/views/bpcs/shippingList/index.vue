<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.cono" class="w-100" :placeholder="$t('bpcs.label.cono')" clearable @keyup.enter="load" />
      <el-input v-model="query.lhno" class="w-150" :placeholder="$t('bpcs.shipping.lhno')" clearable @keyup.enter="load" />
      <el-input v-model="query.carrier" class="w-150" :placeholder="$t('bpcs.shipping.carrier')" clearable @keyup.enter="load" />
      <el-button type="primary" :loading="loading" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="lhno" :label="$t('bpcs.shipping.lhno')" min-width="140" />
        <el-table-column :label="$t('bpcs.shipping.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="carrier" :label="$t('bpcs.shipping.carrier')" min-width="120" />
        <el-table-column prop="destination" :label="$t('bpcs.shippingList.shipToCity')" min-width="120" />
        <el-table-column prop="shipDate" :label="$t('bpcs.shipping.shipDate')" width="110" />
        <el-table-column align="center" :label="$t('bpcs.sales.lineCount')" width="70">
          <template #default="{ row }">{{ row.lineCount }}</template>
          </el-table-column>
        <el-table-column align="right" :label="$t('bpcs.shipping.weight')" width="90">
          <template #default="{ row }">{{ row.weight }}</template>
          </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" :description="$t('common.noData')" />
    </div>
  </div>
</template>
<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsShippingList' })
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchLoads, type BpcsLoad } from '@/api/bpcs'
const { t } = useI18n()
const loading = ref(false)
const rows = ref<BpcsLoad[]>([])
const query = reactive({ cono: '001', lhno: '', carrier: '' })
function load() {
  loading.value = true
  const p: Record<string, string | number> = { cono: query.cono || '001', current: 1, size: 200 }
  if (query.lhno) p.lhno = query.lhno
  if (query.carrier) p.carrier = query.carrier
  searchLoads(p).then(d => { rows.value = d.records }).finally(() => { loading.value = false })
}
const statusLabel = (s: number) => {
  const keys = ['bpcs.shippingList.statusPlanned', 'bpcs.shippingList.statusFirmed', 'bpcs.shippingList.statusReleased', 'bpcs.shippingList.statusDispatched']
  return t(keys[s] || 'bpcs.shippingList.statusUnknown')
}
const statusTypes: Array<'info' | 'primary' | 'warning' | 'success'> = ['info', 'primary', 'warning', 'success']
const statusType = (s: number) => statusTypes[s] || 'info' as const
</script>