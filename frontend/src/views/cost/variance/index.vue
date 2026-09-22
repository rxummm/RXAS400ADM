<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('cost.itemCode')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-select v-model="varianceType" :placeholder="$t('cost.varianceType')" clearable class="search-bar__select">
        <el-option label="FAVORABLE" value="FAVORABLE" />
        <el-option label="UNFAVORABLE" value="UNFAVORABLE" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="varianceNo" :label="$t('cost.varianceNo')" width="140" />
        <el-table-column prop="itemCode" :label="$t('cost.itemCode')" width="120" />
        <el-table-column prop="costComponent" :label="$t('cost.costComponent')" width="120" />
        <el-table-column prop="standardCost" :label="$t('cost.standardCost')" width="120" />
        <el-table-column prop="actualCost" :label="$t('cost.actualCost')" width="120" />
        <el-table-column prop="varianceAmount" :label="$t('cost.varianceAmount')" width="120" />
        <el-table-column prop="variancePct" :label="$t('cost.variancePct')" width="100" />
        <el-table-column prop="varianceType" :label="$t('cost.varianceType')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.varianceType === 'FAVORABLE' ? 'success' : 'danger'" size="small">
              {{ row.varianceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" :label="$t('cost.period')" width="100" />
        <el-table-column prop="status" :label="$t('cost.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CostVariance' })

import { ref, onMounted } from 'vue'
import { searchCostVariances, type CostVariance } from '@/api/cost'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')
const varianceType = ref('')
const rows = ref<CostVariance[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await searchCostVariances({ itemCode: itemCode.value, varianceType: varianceType.value, current: current.value, size: size.value })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
