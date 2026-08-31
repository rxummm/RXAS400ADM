<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="orderDate" :label="$t('bpcs.common.orderDate')" width="100" />
        <el-table-column prop="reqDate" :label="$t('bpcs.common.reqDate')" width="100" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CLOSED' ? 'success' : row.status === 'HOLD' ? 'danger' : 'primary'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pendingLines" :label="$t('bpcs.kanban.pending')" width="90" align="center">
          <template #default="{ row }">
            <span class="text-danger">{{ row.pendingLines }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="partialLines" :label="$t('bpcs.kanban.partial')" width="90" align="center">
          <template #default="{ row }">
            <span style="color: var(--el-color-warning)">{{ row.partialLines }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shippedLines" :label="$t('bpcs.kanban.shipped')" width="90" align="center">
          <template #default="{ row }">
            <span class="text-success">{{ row.shippedLines }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listKanbanOrders, type KanbanVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<KanbanVO[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listKanbanOrders() as unknown as KanbanVO[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
