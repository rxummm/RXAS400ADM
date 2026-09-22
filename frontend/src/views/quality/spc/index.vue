<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('quality.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="itemCode" :label="$t('quality.itemCode')" width="120" />
        <el-table-column prop="qualityChar" :label="$t('quality.qualityChar')" width="150" />
        <el-table-column prop="sampleDate" :label="$t('quality.sampleDate')" width="110" />
        <el-table-column prop="subgroupNo" :label="$t('quality.subgroupNo')" width="80" />
        <el-table-column prop="mean" :label="$t('quality.mean')" width="100" />
        <el-table-column prop="range" :label="$t('quality.range')" width="100" />
        <el-table-column prop="ucl" :label="$t('quality.ucl')" width="100" />
        <el-table-column prop="cl" :label="$t('quality.cl')" width="100" />
        <el-table-column prop="lcl" :label="$t('quality.lcl')" width="100" />
        <el-table-column prop="isOutOfControl" :label="$t('quality.outOfControl')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isOutOfControl ? 'danger' : 'success'" size="small">
              {{ row.isOutOfControl ? $t('quality.yes') : $t('quality.no') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'QualitySpc' })

import { ref, onMounted } from 'vue'
import { searchSpc, type SpcRecord } from '@/api/quality'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<SpcRecord>({
  fetchApi: (params) => searchSpc({
    itemCode: itemCode.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>