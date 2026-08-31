<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="searchRma" class="w-120" :placeholder="$t('bpcs.rma.rmaNo')" @keyup.enter="load" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="rmaNo" :label="$t('bpcs.rma.rmaNo')" width="140" />
        <el-table-column prop="orno" :label="$t('bpcs.common.orderNo')" width="140" />
        <el-table-column prop="item" :label="$t('bpcs.common.itemCode')" width="120" />
        <el-table-column prop="reason" :label="$t('bpcs.rma.reason')" min-width="160" />
        <el-table-column prop="status" :label="$t('bpcs.rma.rmaStatus')" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const searchRma = ref('')
const loading = ref(false)
const rows = ref<{ rmaNo: string; orno: string; item: string; reason: string; status: string }[]>([])
const load = () => { rows.value = [] }
</script>
