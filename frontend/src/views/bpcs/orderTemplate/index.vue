<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="name" :label="$t('bpcs.orderTemplate.templateName')" min-width="200" />
        <el-table-column prop="lastUsed" :label="$t('bpcs.orderTemplate.lastUsed')" width="120" />
        <el-table-column prop="useCount" :label="$t('bpcs.orderTemplate.useCount')" width="100" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const loading = ref(false)
const rows = ref<{ name: string; lastUsed: string; useCount: number }[]>([])
const load = () => { rows.value = [] }
</script>
