<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('quality.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-input v-model="batchNo" :placeholder="$t('quality.batchNo')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="itemCode" :label="$t('quality.itemCode')" width="120" />
        <el-table-column prop="batchNo" :label="$t('quality.batchNo')" width="100" />
        <el-table-column prop="traceType" :label="$t('quality.traceType')" width="120" />
        <el-table-column prop="sourceType" :label="$t('quality.sourceType')" width="120" />
        <el-table-column prop="sourceNo" :label="$t('quality.sourceNo')" width="120" />
        <el-table-column prop="targetType" :label="$t('quality.targetType')" width="120" />
        <el-table-column prop="targetNo" :label="$t('quality.targetNo')" width="120" />
        <el-table-column prop="relationship" :label="$t('quality.relationship')" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'QualityTraceability' })

import { ref, onMounted } from 'vue'
import { tracePaged, type TraceabilityChain } from '@/api/quality'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')
const batchNo = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<TraceabilityChain>({
  fetchApi: (params) => tracePaged({
    itemCode: itemCode.value || undefined,
    batchNo: batchNo.value || undefined,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>