<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="custName" :label="$t('bpcs.common.customerName')" min-width="140" />
        <el-table-column prop="orderDate" :label="$t('bpcs.common.orderDate')" width="100" />
        <el-table-column prop="crHold" :label="$t('bpcs.credit.creditHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.crHold === 'Y' ? 'danger' : 'success'" size="small">{{ row.crHold }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="shipHold" :label="$t('bpcs.credit.shipHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.shipHold === 'Y' ? 'danger' : 'success'" size="small">{{ row.shipHold }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="prHold" :label="$t('bpcs.credit.priceHold')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.prHold === 'Y' ? 'warning' : 'success'" size="small">{{ row.prHold }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listCreditHolds, type CreditHoldVO } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<CreditHoldVO[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listCreditHolds() as unknown as CreditHoldVO[]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
