<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="equipmentNo" :placeholder="$t('tpm.equipmentNo')" clearable class="search-bar__input" @keyup.enter="forceSearch" />
      <el-select v-model="status" :placeholder="$t('tpm.status')" clearable class="search-bar__select">
        <el-option label="ACTIVE" value="ACTIVE" />
        <el-option label="INACTIVE" value="INACTIVE" />
        <el-option label="MAINTENANCE" value="MAINTENANCE" />
        <el-option label="SCRAPPED" value="SCRAPPED" />
      </el-select>
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" size="small" border>
        <el-table-column prop="equipmentNo" :label="$t('tpm.equipmentNo')" width="140" />
        <el-table-column prop="equipmentName" :label="$t('tpm.equipmentName')" width="200" />
        <el-table-column prop="equipmentType" :label="$t('tpm.equipmentType')" width="120" />
        <el-table-column prop="manufacturer" :label="$t('tpm.manufacturer')" width="150" />
        <el-table-column prop="location" :label="$t('tpm.location')" width="120" />
        <el-table-column prop="department" :label="$t('tpm.department')" width="120" />
        <el-table-column prop="status" :label="$t('tpm.status')" width="100" />
        <el-table-column prop="responsiblePerson" :label="$t('tpm.responsiblePerson')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TpmEquipment' })

import { ref, onMounted } from 'vue'
import { searchEquipment, type Equipment } from '@/api/tpm'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'

const equipmentNo = ref('')
const status = ref('')

const { tableData, loading, current, size, total, forceSearch } = useSmartQueryTable<Equipment>({
  fetchApi: (params) => searchEquipment({
    equipmentNo: equipmentNo.value,
    status: status.value,
    current: params.current,
    size: params.size,
  }),
})

onMounted(() => forceSearch())
</script>