<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="partnerCode" :placeholder="$t('edi.partnerCode')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="partnerType" :placeholder="$t('edi.partnerType')" clearable class="search-bar__select">
        <el-option label="SUPPLIER" value="SUPPLIER" />
        <el-option label="CUSTOMER" value="CUSTOMER" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="partnerCode" :label="$t('edi.partnerCode')" width="120" />
        <el-table-column prop="partnerName" :label="$t('edi.partnerName')" width="200" />
        <el-table-column prop="partnerType" :label="$t('edi.partnerType')" width="100" />
        <el-table-column prop="ediVersion" :label="$t('edi.ediVersion')" width="100" />
        <el-table-column prop="as2Url" :label="$t('edi.as2Url')" />
        <el-table-column prop="status" :label="$t('edi.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'EdiPartner' })

import { ref, onMounted } from 'vue'
import { searchEdiPartners, type EdiPartner } from '@/api/edi'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const partnerCode = ref('')
const partnerType = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<EdiPartner>({
  fetchApi: (params) => searchEdiPartners({
    partnerCode: partnerCode.value,
    partnerType: partnerType.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>