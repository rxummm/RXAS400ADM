<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="documentNo" :placeholder="$t('edi.documentNo')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="documentType" :placeholder="$t('edi.documentType')" clearable class="search-bar__select">
        <el-option label="850" value="850" />
        <el-option label="855" value="855" />
        <el-option label="856" value="856" />
        <el-option label="810" value="810" />
      </el-select>
      <el-select v-model="direction" :placeholder="$t('edi.direction')" clearable class="search-bar__select">
        <el-option label="INBOUND" value="INBOUND" />
        <el-option label="OUTBOUND" value="OUTBOUND" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="documentNo" :label="$t('edi.documentNo')" width="140" />
        <el-table-column prop="partnerCode" :label="$t('edi.partnerCode')" width="120" />
        <el-table-column prop="partnerName" :label="$t('edi.partnerName')" width="150" />
        <el-table-column prop="documentType" :label="$t('edi.documentType')" width="80" />
        <el-table-column prop="direction" :label="$t('edi.direction')" width="100" />
        <el-table-column prop="status" :label="$t('edi.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('edi.createdTime')" width="160" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'EdiDocument' })

import { ref, onMounted } from 'vue'
import { searchEdiDocuments, type EdiDocument } from '@/api/edi'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const documentNo = ref('')
const documentType = ref('')
const direction = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<EdiDocument>({
  fetchApi: (params) => searchEdiDocuments({
    documentNo: documentNo.value,
    documentType: documentType.value,
    direction: direction.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>