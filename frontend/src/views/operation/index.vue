<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" :placeholder="$t('common.keyword')" clearable @keyup.enter="forceSearch" class="w-200" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="tableData" size="small" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="operationType" :label="$t('operation.type')" width="140" />
          <el-table-column prop="status" :label="$t('operation.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="currentStep" :label="$t('operation.currentStep')" width="160" />
          <el-table-column prop="riskLevel" :label="$t('operation.riskLevel')" width="120">
            <template #default="{ row }">
              <el-tag :type="riskType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requestedBy" :label="$t('operation.requestedBy')" width="120" />
          <el-table-column prop="requestedAt" :label="$t('operation.requestedAt')" width="170" />
          <el-table-column :label="$t('common.operation')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDetail(row as OperationVO)">
                {{ $t('common.detail') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </RxSkeleton>
      <AppPagination
        v-model:current="current"
        v-model:size="size"
        :total="total"
        @change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="detailVisible" :title="$t('operation.detail')" width="600px">
      <OperationProgress
        v-if="currentOp"
        :operation="currentOp"
        @cancel="handleCancel"
        @retry="handleRetry"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { getOperation, cancelOperation, retryOperation, listOperations } from '@/api/operation'
import type { OperationVO } from '@/api/operation'
import RxSkeleton from '@/components/RxSkeleton.vue'
import AppPagination from '@/components/AppPagination.vue'
import OperationProgress from './OperationProgress.vue'

defineOptions({ name: 'OperationIndex' })

const keyword = ref('')
const detailVisible = ref(false)
const currentOp = ref<OperationVO | null>(null)

const {
  tableData, loading, current, size, total, forceSearch, handlePageChange, handleSizeChange, fetchData
} = useSmartQueryTable<OperationVO>({
  fetchApi: async (params) => {
    const data = await listOperations(params.current ?? 1, params.size ?? 20)
    return { records: data.records, total: data.total }
  },
  frontendPage: false
})

function statusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    SUCCESS: 'success',
    RUNNING: 'info',
    FAILED: 'danger',
    CANCELLED: 'info',
    RETRYING: 'warning',
    REQUESTED: 'info'
  }
  return map[status] || 'info'
}

function riskType(level: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    READ: 'success',
    WRITE: 'info',
    DESTRUCTIVE: 'warning',
    CRITICAL: 'danger',
    BREAK_GLASS: 'danger'
  }
  return map[level] || 'info'
}

function viewDetail(op: OperationVO) {
  currentOp.value = op
  detailVisible.value = true
}

async function handleCancel() {
  if (!currentOp.value) return
  await cancelOperation(currentOp.value.id)
  detailVisible.value = false
  fetchData()
}

async function handleRetry() {
  if (!currentOp.value) return
  const updated = await retryOperation(currentOp.value.id)
  currentOp.value = updated
  fetchData()
}
</script>