<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="poNo" :label="$t('bpcs.po.poNo')" width="140" />
        <el-table-column prop="vendor" :label="$t('bpcs.po.vendorCode')" width="100" />
        <el-table-column prop="vendorName" :label="$t('bpcs.po.vendorName')" min-width="160" />
        <el-table-column prop="orderDate" :label="$t('bpcs.po.orderDate')" width="100" />
        <el-table-column prop="receivedDate" :label="$t('bpcs.po.receivedDate')" width="100" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'CLOSED' ? 'success' : row.onHold ? 'danger' : 'primary'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lineCount" :label="$t('bpcs.po.lineCount')" width="90" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listPoLifecycle, type PoLifecycleVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<PoLifecycleVO[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listPoLifecycle() as unknown as PoLifecycleVO[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
