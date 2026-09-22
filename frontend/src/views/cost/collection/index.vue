<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="collectionNo" :placeholder="$t('cost.collectionNo')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="costType" :placeholder="$t('cost.costType')" clearable class="search-bar__select">
        <el-option label="MATERIAL" value="MATERIAL" />
        <el-option label="LABOR" value="LABOR" />
        <el-option label="OVERHEAD" value="OVERHEAD" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="collectionNo" :label="$t('cost.collectionNo')" width="140" />
        <el-table-column prop="costType" :label="$t('cost.costType')" width="100" />
        <el-table-column prop="costObjectType" :label="$t('cost.costObjectType')" width="120" />
        <el-table-column prop="costObjectNo" :label="$t('cost.costObjectNo')" width="120" />
        <el-table-column prop="period" :label="$t('cost.period')" width="100" />
        <el-table-column prop="materialCost" :label="$t('cost.materialCost')" width="120" />
        <el-table-column prop="laborCost" :label="$t('cost.laborCost')" width="120" />
        <el-table-column prop="overheadCost" :label="$t('cost.overheadCost')" width="120" />
        <el-table-column prop="totalCost" :label="$t('cost.totalCost')" width="120" />
        <el-table-column prop="status" :label="$t('cost.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CostCollection' })

import { ref, onMounted } from 'vue'
import { searchCostCollections, type CostCollection } from '@/api/cost'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const collectionNo = ref('')
const costType = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<CostCollection>({
  fetchApi: (params) => searchCostCollections({
    collectionNo: collectionNo.value,
    costType: costType.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>