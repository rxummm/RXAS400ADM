<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="whse" :placeholder="$t('bpcs.wms.whse')" clearable class="search-bar__input" @keyup.enter="onFilterChange" />
      <el-input v-model="item" :placeholder="$t('bpcs.label.item')" clearable class="search-bar__input" @keyup.enter="onFilterChange" />
      <el-button type="primary" @click="onFilterChange">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="originData" :columns="exportColumns" :title="$t('bpcs.wms.movementHistory')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="80" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" width="120" />
        <el-table-column prop="frombin" :label="$t('bpcs.wms.frombin')" width="100" />
        <el-table-column prop="tobin" :label="$t('bpcs.wms.tobin')" width="100" />
        <el-table-column prop="qty" :label="$t('bpcs.quantity')" width="80" />
        <el-table-column prop="ittyp" :label="$t('bpcs.wms.trntype')" width="80" />
        <el-table-column prop="trndate" :label="$t('bpcs.wms.trndate')" width="100" />
        <el-table-column prop="trntime" :label="$t('bpcs.wms.trntime')" width="80" />
        <el-table-column prop="refno" :label="$t('bpcs.wms.refno')" width="100" />
        <el-table-column prop="userid" :label="$t('bpcs.wms.userid')" width="80" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'WmsMovementHistory' })

import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchMovements, type Movement } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'
import AppPagination from '@/components/AppPagination.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

const { t } = useI18n()

const whse = ref('')
const item = ref('')
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'frombin', label: t('bpcs.wms.frombin') },
  { key: 'tobin', label: t('bpcs.wms.tobin') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'ittyp', label: t('bpcs.wms.trntype') },
  { key: 'trndate', label: t('bpcs.wms.trndate') },
  { key: 'trntime', label: t('bpcs.wms.trntime') },
  { key: 'refno', label: t('bpcs.wms.refno') },
  { key: 'userid', label: t('bpcs.wms.userid') }
])

const {
  pagedData, loading, originData, total, current, size,
  handlePageChange, handleSizeChange, fetchData,
} = useSmartQueryTable<Movement>({
  fetchApi: (params) => searchMovements({ cono: '001', whse: whse.value || undefined, item: item.value || undefined, current: params.current, size: params.size }),
  frontendPage: false,
  enableCache: true,
})

function onFilterChange() {
  current.value = 1
  void fetchData({}, true)
}

onMounted(() => void fetchData())
</script>
