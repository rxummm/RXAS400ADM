<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="warehouse" :label="$t('bpcs.wms.warehouse')" width="90" />
        <el-table-column prop="binLocation" :label="$t('bpcs.location.binLocation')" width="120" />
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="description" :label="$t('bpcs.common.description')" min-width="160" />
        <el-table-column prop="qtyOnHand" :label="$t('bpcs.inventory.onHand')" width="100" align="right" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="80" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'BpcsLocationInv' })

import { ref, onMounted } from 'vue'
import { listLocations, type LocationInventory } from '@/api/bpcs'

const loading = ref(false)
const rows = ref<LocationInventory[]>([])

const load = async () => {
  loading.value = true
  try {
    rows.value = await listLocations()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
