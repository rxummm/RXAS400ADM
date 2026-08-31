<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="sourceOrder" class="w-120" :placeholder="$t('bpcs.orderCopy.sourceOrder')" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="cust" :label="$t('bpcs.common.customerCode')" width="100" />
        <el-table-column prop="status" :label="$t('bpcs.common.status')" width="90" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const sourceOrder = ref('')
const loading = ref(false)
const rows = ref<{ orno: string; cust: string; status: string }[]>([])
const load = () => { rows.value = [] }
</script>
