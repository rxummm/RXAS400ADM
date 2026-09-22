<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="whse" :placeholder="$t('bpcs.wms.whse')" clearable class="search-bar__input" @keyup.enter="onFilterChange" />
      <el-button type="primary" @click="onFilterChange">{{ $t('common.search') }}</el-button>
      <ExportDropdown :data="originData" :columns="exportColumns" :title="$t('bpcs.wms.binInventory')" />
    </div>
    <div class="table-wrapper">
      <el-table :data="pagedData" v-loading="loading" size="small" border>
        <el-table-column prop="whse" :label="$t('bpcs.wms.whse')" width="80" />
        <el-table-column prop="binno" :label="$t('bpcs.wms.binno')" width="120" />
        <el-table-column prop="bintype" :label="$t('bpcs.wms.bintype')" width="80" />
        <el-table-column prop="status" :label="$t('bpcs.wms.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'O' ? 'primary' : row.status === 'F' ? 'success' : 'info'" size="small">
              {{ row.status === 'O' ? $t('bpcs.wms.occupied') : row.status === 'F' ? $t('bpcs.wms.freeTag') : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" :label="$t('bpcs.wms.capacity')" width="80" />
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
defineOptions({ name: 'WmsBinInventory' })

import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { searchBinInventory, type BinInventory } from '@/api/wms'
import ExportDropdown from '@/components/ExportDropdown.vue'
import AppPagination from '@/components/AppPagination.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

const { t } = useI18n()

const whse = ref('')
const exportColumns = computed(() => [
  { key: 'whse', label: t('bpcs.wms.whse') },
  { key: 'binno', label: t('bpcs.wms.binno') },
  { key: 'bintype', label: t('bpcs.wms.bintype') },
  { key: 'status', label: t('bpcs.wms.status') },
  { key: 'item', label: t('bpcs.label.item') },
  { key: 'itdsc', label: t('bpcs.description') },
  { key: 'qty', label: t('bpcs.quantity') },
  { key: 'lotno', label: t('bpcs.wms.lotno') }
])

const {
  pagedData, loading, originData, total, current, size,
  handlePageChange, handleSizeChange, fetchData,
} = useSmartQueryTable<BinInventory>({
  fetchApi: (params) => searchBinInventory({ cono: '001', whse: whse.value || undefined, current: params.current, size: params.size }),
  frontendPage: false,
  enableCache: true,
})

function onFilterChange() {
  current.value = 1
  void fetchData({}, true)
}

onMounted(() => void fetchData())
</script>
