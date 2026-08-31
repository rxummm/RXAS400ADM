<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" :placeholder="$t('bpcs.cono')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="rows" :columns="exportColumns" :title="$t('bpcs.wms.overview')" export-url="/api/v1/bpcs/export/supply-chain/kpi" />
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="100" />
        <el-table-column prop="whname" :label="$t('bpcs.wms.whname')" />
        <el-table-column prop="totalBins" :label="$t('bpcs.wms.totalBins')" width="100" />
        <el-table-column prop="occupied" :label="$t('bpcs.wms.occupied')" width="100" />
        <el-table-column prop="free" :label="$t('bpcs.wms.free')" width="100" />
        <el-table-column :label="$t('bpcs.wms.occupancy')" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="row.totalBins > 0 ? Math.round(row.occupied / row.totalBins * 100) : 0"
              :status="getProgressStatus(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { warehouseSummary, type WarehouseSummary } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'

const { t } = useI18n()

const cono = ref('001')
const rows = ref<WarehouseSummary[]>([])
const loading = ref(false)
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'whname', label: t('bpcs.wms.whname') },
  { key: 'totalBins', label: t('bpcs.wms.totalBins') },
  { key: 'occupied', label: t('bpcs.wms.occupied') },
  { key: 'free', label: t('bpcs.wms.free') }
])

function getProgressStatus(row: Record<string, unknown>) {
  const totalBins = (row.totalBins as number) || 0
  const occupied = (row.occupied as number) || 0
  const pct = totalBins > 0 ? occupied / totalBins : 0
  if (pct > 0.9) return 'exception'
  if (pct > 0.7) return 'warning'
  return 'success'
}

async function load() {
  loading.value = true
  try {
    rows.value = await warehouseSummary(cono.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
