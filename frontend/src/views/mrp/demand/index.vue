<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="itemCode" :placeholder="$t('mrp.itemCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="status" :placeholder="$t('mrp.status')" clearable class="search-bar__select">
        <el-option label="PENDING" value="PENDING" />
        <el-option label="PLANNED" value="PLANNED" />
        <el-option label="RELEASED" value="RELEASED" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="demandNo" :label="$t('mrp.demandNo')" width="140" />
        <el-table-column prop="itemCode" :label="$t('mrp.itemCode')" width="120" />
        <el-table-column prop="itemDesc" :label="$t('mrp.itemDesc')" />
        <el-table-column prop="demandType" :label="$t('mrp.demandType')" width="120" />
        <el-table-column prop="grossRequirement" :label="$t('mrp.grossRequirement')" width="120" />
        <el-table-column prop="netRequirement" :label="$t('mrp.netRequirement')" width="120" />
        <el-table-column prop="requiredDate" :label="$t('mrp.requiredDate')" width="110" />
        <el-table-column prop="status" :label="$t('mrp.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MrpDemand' })

import { ref, onMounted } from 'vue'
import { searchMrpDemands, type MrpDemand } from '@/api/mrp'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const itemCode = ref('')
const status = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<MrpDemand>({
  fetchApi: (params) => searchMrpDemands({
    itemCode: itemCode.value,
    status: status.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>