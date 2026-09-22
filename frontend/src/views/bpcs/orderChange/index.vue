<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="queryCono" clearable :placeholder="$t('bpcs.common.companyCode')" class="w-120" />
      <el-input v-model="queryOrno" clearable :placeholder="$t('bpcs.common.orderNo')" @keyup.enter="forceSearch" class="w-150 ml8" />
      <el-button type="primary" @click="forceSearch" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="changeType" :label="$t('bpcs.orderChange.changeType')" width="120" />
        <el-table-column prop="fieldName" :label="$t('bpcs.orderChange.fieldName')" min-width="120" />
        <el-table-column prop="oldValue" :label="$t('bpcs.orderChange.oldValue')" width="140" />
        <el-table-column prop="newValue" :label="$t('bpcs.orderChange.newValue')" width="140" />
        <el-table-column prop="changedBy" :label="$t('bpcs.orderChange.changedBy')" width="100" />
        <el-table-column prop="changedTime" :label="$t('bpcs.orderChange.changedTime')" width="160" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderChange' })

import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listOrderChanges } from '@/api/bpcs'
import type { OrderChange } from '@/api/bpcs'

const { t } = useI18n()
const queryCono = ref('001')
const queryOrno = ref('')

const { tableData, loading, total, current, size, forceSearch, handlePageChange, handleSizeChange } = useSmartQueryTable<OrderChange>({
  fetchApi: (params) => {
    if (!queryOrno.value) {
      ElMessage.warning(t('bpcs.orderChange.pleaseInputOrderNo'))
      return Promise.resolve({ records: [], total: 0 })
    }
    return listOrderChanges({ cono: queryCono.value || '001', orno: queryOrno.value, current: params.current, size: params.size })
  },
  frontendPage: false,
  searchFields: ['orno', 'changeType', 'fieldName'],
})
</script>
