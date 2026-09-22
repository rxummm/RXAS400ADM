<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="lotno" :placeholder="$t('bpcs.wms.lotno')" clearable class="search-bar__input" @keyup.enter="onFilterChange" />
      <el-button type="primary" @click="onFilterChange">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="originData" :columns="exportColumns" :title="$t('bpcs.wms.batchTracking')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" width="120" />
        <el-table-column prop="itdsc" :label="$t('bpcs.description')" />
        <el-table-column prop="qty" :label="$t('bpcs.quantity')" width="80" />
        <el-table-column prop="lotno" :label="$t('bpcs.wms.lotno')" width="120" />
        <el-table-column prop="trndate" :label="$t('bpcs.wms.trndate')" width="100" />
        <el-table-column prop="ittyp" :label="$t('bpcs.wms.trntype')" width="80" />
        <el-table-column prop="trnqty" :label="$t('bpcs.wms.trnqty')" width="80" />
        <el-table-column prop="refno" :label="$t('bpcs.wms.refno')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'WmsBatchTracking' })

import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchBatches, type BatchTracking } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'
import AppPagination from '@/components/AppPagination.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

const { t } = useI18n()

const lotno = ref('')
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') },
  { key: 'trndate', label: t('bpcs.wms.trndate') },
  { key: 'ittyp', label: t('bpcs.wms.trntype') },
  { key: 'trnqty', label: t('bpcs.wms.trnqty') },
  { key: 'refno', label: t('bpcs.wms.refno') }
])

const {
  pagedData, loading, originData, total, current, size,
  handlePageChange, handleSizeChange, fetchData,
} = useSmartQueryTable<BatchTracking>({
  fetchApi: (params) => searchBatches({ cono: '001', lotno: lotno.value || undefined, current: params.current, size: params.size }),
  frontendPage: false,
  enableCache: true,
})

function onFilterChange() {
  current.value = 1
  void fetchData({}, true)
}

onMounted(() => void fetchData())
</script>
