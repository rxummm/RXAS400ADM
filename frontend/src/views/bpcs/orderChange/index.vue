<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="searchOrno" class="w-120" :placeholder="$t('bpcs.common.orderNo')" @keyup.enter="load" />
      <el-button type="primary" @click="load" class="ml8">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="changeDate" :label="$t('bpcs.orderChange.changeDate')" width="120" />
        <el-table-column prop="changeType" :label="$t('bpcs.orderChange.changeType')" width="120" />
        <el-table-column prop="field" :label="$t('bpcs.common.description')" min-width="120" />
        <el-table-column prop="oldValue" :label="$t('bpcs.orderChange.oldValue')" width="120" />
        <el-table-column prop="newValue" :label="$t('bpcs.orderChange.newValue')" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const searchOrno = ref('')
const loading = ref(false)
const rows = ref<{ changeDate: string; changeType: string; field: string; oldValue: string; newValue: string }[]>([])
const load = () => { rows.value = [] }
</script>
