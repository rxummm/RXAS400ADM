<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="queryStatus" clearable :placeholder="$t('bpcs.common.status')" class="w-120">
        <el-option :label="$t('bpcs.rma.status.pending')" value="PENDING" />
        <el-option :label="$t('bpcs.rma.status.approved')" value="APPROVED" />
        <el-option :label="$t('bpcs.rma.status.rejected')" value="REJECTED" />
        <el-option :label="$t('bpcs.rma.status.completed')" value="COMPLETED" />
      </el-select>
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="rmaNo" :label="$t('bpcs.rma.rmaNo')" width="140" />
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="itemDesc" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="qty" :label="$t('bpcs.common.quantity')" width="80" align="center" />
        <el-table-column prop="reason" :label="$t('bpcs.rma.reason')" min-width="160" />
        <el-table-column prop="status" :label="$t('bpcs.rma.rmaStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('bpcs.rma.createdTime')" width="160" />
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsRma' })

import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listRma } from '@/api/bpcs'
import type { Rma } from '@/api/bpcs'

const { t } = useI18n()
const loading = ref(false)
const rows = ref<Rma[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const queryStatus = ref('')

const getStatusType = (status: string | null) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    COMPLETED: 'info'
  }
  return (map[status || ''] || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

const load = async () => {
  loading.value = true
  try {
    const res = await listRma({ status: queryStatus.value || undefined })
    rows.value = res || []
    total.value = (res || []).length
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryStatus.value = ''
  current.value = 1
  load()
}

load()
</script>
