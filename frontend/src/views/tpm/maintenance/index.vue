<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="planName" :placeholder="$t('tpm.planName')" clearable class="search-bar__input" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="planNo" :label="$t('tpm.planNo')" width="140" />
        <el-table-column prop="planName" :label="$t('tpm.planName')" width="200" />
        <el-table-column prop="maintenanceType" :label="$t('tpm.maintenanceType')" width="120" />
        <el-table-column prop="cycleDays" :label="$t('tpm.cycleDays')" width="100" />
        <el-table-column prop="nextDueDate" :label="$t('tpm.nextDueDate')" width="110" />
        <el-table-column prop="responsiblePerson" :label="$t('tpm.responsiblePerson')" width="100" />
        <el-table-column prop="status" :label="$t('tpm.status')" width="100" />
      </el-table>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TpmMaintenance' })

import { ref, onMounted } from 'vue'
import { searchMaintenancePlans, type MaintenancePlan } from '@/api/tpm'
import AppPagination from '@/components/AppPagination.vue'

const planName = ref('')
const rows = ref<MaintenancePlan[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await searchMaintenancePlans({ planName: planName.value, current: current.value, size: size.value })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
