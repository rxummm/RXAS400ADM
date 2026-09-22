<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="equipmentId" :placeholder="$t('tpm.equipmentId')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="equipmentId" :label="$t('tpm.equipmentId')" width="120" />
        <el-table-column prop="calcDate" :label="$t('tpm.calcDate')" width="110" />
        <el-table-column prop="availability" :label="$t('tpm.availability')" width="120" />
        <el-table-column prop="performance" :label="$t('tpm.performance')" width="120" />
        <el-table-column prop="quality" :label="$t('tpm.quality')" width="100" />
        <el-table-column prop="oee" :label="$t('tpm.oee')" width="100">
          <template #default="{ row }">
            <el-progress :percentage="row.oee" :status="row.oee >= 80 ? 'success' : row.oee >= 60 ? 'warning' : 'exception'" />
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TpmOee' })

import { ref, onMounted } from 'vue'
import { searchOee, type OeeRecord } from '@/api/tpm'
import AppPagination from '@/components/AppPagination.vue'

const equipmentId = ref('')
const rows = ref<OeeRecord[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await searchOee({ equipmentId: equipmentId.value, current: current.value, size: size.value })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
