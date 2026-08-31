<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="wh" :label="$t('bpcs.wms.warehouse')" width="80" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="safetyStock" :label="$t('bpcs.replenishment.safetyStock')" width="110" align="right" />
        <el-table-column prop="shortage" :label="$t('bpcs.replenishment.shortage')" width="100" align="right">
          <template #default="{ row }">
            <span class="text-danger">{{ row.shortage }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgDemand" :label="$t('bpcs.replenishment.avgDemand')" width="110" align="right" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listReplenishment, type ReplenishmentVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<ReplenishmentVO[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listReplenishment() as unknown as ReplenishmentVO[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
