<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="queryCono" clearable :placeholder="$t('bpcs.common.companyCode')" class="w-120" />
      <el-input v-model="queryOrno" clearable :placeholder="$t('bpcs.common.orderNo')" @keyup.enter="load" class="w-120 ml8" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="changeType" :label="$t('bpcs.orderChange.changeType')" width="120" />
        <el-table-column prop="fieldName" :label="$t('bpcs.orderChange.fieldName')" min-width="120" />
        <el-table-column prop="oldValue" :label="$t('bpcs.orderChange.oldValue')" width="140" />
        <el-table-column prop="newValue" :label="$t('bpcs.orderChange.newValue')" width="140" />
        <el-table-column prop="changedBy" :label="$t('bpcs.orderChange.changedBy')" width="100" />
        <el-table-column prop="changedTime" :label="$t('bpcs.orderChange.changedTime')" width="160" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsOrderChange' })

import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listOrderChanges } from '@/api/bpcs'
import type { OrderChange } from '@/api/bpcs'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<OrderChange[]>([])
const queryCono = ref('')
const queryOrno = ref('')

const load = async () => {
  if (!queryOrno.value) return
  loading.value = true
  try {
    const res = await listOrderChanges({ cono: queryCono.value || '001', orno: queryOrno.value })
    rows.value = res || []
  } finally {
    loading.value = false
  }
}
</script>
