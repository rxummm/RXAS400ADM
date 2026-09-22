<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="parentItem" :placeholder="$t('mrp.parentItem')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="bomNo" :label="$t('mrp.bomNo')" width="140" />
        <el-table-column prop="parentItem" :label="$t('mrp.parentItem')" width="120" />
        <el-table-column prop="parentDesc" :label="$t('mrp.parentDesc')" />
        <el-table-column prop="bomVersion" :label="$t('mrp.bomVersion')" width="100" />
        <el-table-column prop="effectiveDate" :label="$t('mrp.effectiveDate')" width="110" />
        <el-table-column prop="status" :label="$t('mrp.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MrpBom' })

import { ref, onMounted } from 'vue'
import { searchBomMasters, type BomMaster } from '@/api/mrp'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const parentItem = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<BomMaster>({
  fetchApi: (params) => searchBomMasters({
    parentItem: parentItem.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>