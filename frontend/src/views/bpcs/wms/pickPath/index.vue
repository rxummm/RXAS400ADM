<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="whse" :placeholder="$t('bpcs.wms.whse')" clearable class="search-bar__input" @keyup.enter="onFilterChange" />
      <el-button type="primary" @click="onFilterChange">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="originData" :columns="exportColumns" :title="$t('bpcs.wms.pickPath')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border row-key="binno">
        <el-table-column type="index" :label="$t('bpcs.wms.pickOrder')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
        <el-table-column prop="item" :label="$t('bpcs.label.item')" width="120" />
        <el-table-column prop="itdsc" :label="$t('bpcs.description')" />
        <el-table-column prop="qty" :label="$t('bpcs.quantity')" width="80" />
        <el-table-column prop="lotno" :label="$t('bpcs.wms.lotno')" width="120" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'WmsPickPath' })

import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchBinInventory, type BinInventory } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'
import AppPagination from '@/components/AppPagination.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

const { t } = useI18n()

const whse = ref('')
const exportColumns = computed(() => [
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') }
])

const {
  pagedData, loading, originData, total, current, size,
  handlePageChange, handleSizeChange, fetchData,
} = useSmartQueryTable<BinInventory>({
  fetchApi: async (params) => {
    const res = await searchBinInventory({ cono: '001', whse: whse.value || undefined, current: params.current, size: params.size })
    res.records.sort((a: BinInventory, b: BinInventory) => a.binno.localeCompare(b.binno))
    return res
  },
  frontendPage: false,
  enableCache: true,
})

function onFilterChange() {
  current.value = 1
  void fetchData({}, true)
}

onMounted(() => void fetchData())
</script>
