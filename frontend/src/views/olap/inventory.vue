<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" :placeholder="$t('common.cono')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-input v-model="warehouse" :placeholder="$t('olap.warehouse')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="cono" :label="$t('common.cono')" width="80" />
        <el-table-column prop="warehouse" :label="$t('olap.warehouse')" width="100" />
        <el-table-column prop="itemCode" :label="$t('olap.itemCode')" width="120" />
        <el-table-column prop="onHandQty" :label="$t('olap.onHandQty')" width="100" />
        <el-table-column prop="availableQty" :label="$t('olap.availableQty')" width="100" />
        <el-table-column prop="stockValue" :label="$t('olap.stockValue')" width="120" />
        <el-table-column prop="period" :label="$t('olap.period')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'OlapInventory' })

import { ref, onMounted } from 'vue'
import { getInventorySummary, type OlapInventorySummary } from '@/api/olap'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const cono = ref('001')
const warehouse = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<OlapInventorySummary>({
  fetchApi: (params) => getInventorySummary({
    cono: cono.value,
    warehouse: warehouse.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>