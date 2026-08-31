<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-descriptions :title="$t('bpcs.orderReport.title')" :column="2" border size="small">
        <el-descriptions-item :label="$t('bpcs.orderReport.reportType')">Summary</el-descriptions-item>
        <el-descriptions-item :label="$t('bpcs.orderReport.dateRange')">2026-01 ~ 2026-08</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
const load = () => {}
</script>
