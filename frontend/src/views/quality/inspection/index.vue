<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('quality.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="result" :placeholder="$t('quality.result')" clearable class="search-bar__select">
        <el-option label="PASS" value="PASS" />
        <el-option label="FAIL" value="FAIL" />
        <el-option label="PARTIAL" value="PARTIAL" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="inspectionNo" :label="$t('quality.inspectionNo')" width="140" />
        <el-table-column prop="inspectionType" :label="$t('quality.inspectionType')" width="100" />
        <el-table-column prop="itemCode" :label="$t('quality.itemCode')" width="120" />
        <el-table-column prop="itemDesc" :label="$t('quality.itemDesc')" />
        <el-table-column prop="batchNo" :label="$t('quality.batchNo')" width="100" />
        <el-table-column prop="qtyInspected" :label="$t('quality.qtyInspected')" width="100" />
        <el-table-column prop="qtyAccepted" :label="$t('quality.qtyAccepted')" width="100" />
        <el-table-column prop="qtyRejected" :label="$t('quality.qtyRejected')" width="100" />
        <el-table-column prop="result" :label="$t('quality.result')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : row.result === 'FAIL' ? 'danger' : 'warning'" size="small">
              {{ row.result }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inspectionDate" :label="$t('quality.inspectionDate')" width="110" />
        <el-table-column prop="inspector" :label="$t('quality.inspector')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'QualityInspection' })

import { ref, onMounted } from 'vue'
import { searchInspections, type QualityInspection } from '@/api/quality'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')
const result = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<QualityInspection>({
  fetchApi: (params) => searchInspections({
    itemCode: itemCode.value,
    result: result.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>