<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="cono" :placeholder="$t('common.cono')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-input v-model="period" :placeholder="$t('olap.period')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="cono" :label="$t('common.cono')" width="80" />
        <el-table-column prop="customerCode" :label="$t('olap.customerCode')" width="120" />
        <el-table-column prop="customerGroup" :label="$t('olap.customerGroup')" width="120" />
        <el-table-column prop="totalRevenue" :label="$t('olap.totalRevenue')" width="120" />
        <el-table-column prop="totalQuantity" :label="$t('olap.totalQuantity')" width="100" />
        <el-table-column prop="orderCount" :label="$t('olap.orderCount')" width="100" />
        <el-table-column prop="avgOrderValue" :label="$t('olap.avgOrderValue')" width="120" />
        <el-table-column prop="period" :label="$t('olap.period')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'OlapSales' })

import { ref, onMounted } from 'vue'
import { getSalesSummary, type OlapSalesSummary } from '@/api/olap'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const cono = ref('001')
const period = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<OlapSalesSummary>({
  fetchApi: (params) => getSalesSummary({
    cono: cono.value,
    period: period.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>