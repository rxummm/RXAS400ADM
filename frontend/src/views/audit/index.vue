<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('audit.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="180"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-input v-model="filters.module" :placeholder="$t('audit.module')" clearable class="w-130" />
      <el-input v-model="filters.username" :placeholder="$t('audit.username')" clearable class="w-130" />
      <el-input v-model="filters.action" :placeholder="$t('audit.action')" clearable class="w-160" />
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="module" :label="$t('audit.module')" width="110" />
        <el-table-column prop="action" :label="$t('audit.action')" min-width="170" />
        <el-table-column prop="userName" :label="$t('audit.username')" width="120" />
        <el-table-column prop="target" :label="$t('audit.target')" min-width="140" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="detail" :label="$t('audit.detail')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createdTime" :label="$t('audit.time')" width="170" />
      </el-table>
      </RxSkeleton>
      <AppPagination
        :total="total"
        v-model:current="current"
        v-model:size="size"
        @change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { listAuditLogs, type AuditLog } from '@/api/audit'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import QueryBar from '@/components/QueryBar.vue'

defineOptions({ name: 'Audit' })

const filters = reactive({ module: '', username: '', action: '' })

// 服务端分页 + 3 分钟查询缓存；关键词驱动已加载记录的实时前端模糊匹配（module/username/action 仍走后端过滤）
const {
  pagedData: rows,
  loading,
  keyword,
  current,
  size,
  total,
  isFromCache,
  dataSourceTick,
  handleRefresh,
  resetSearch,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<AuditLog>({
  fetchApi: (params) =>
    listAuditLogs({
      current: params.current,
      size: params.size,
      module: filters.module || undefined,
      username: filters.username || undefined,
      action: filters.action || undefined,
      keyword: params.keyword || undefined,
    }),
  frontendPage: false,
  enableCache: true,
  searchFields: ['module', 'action', 'userName', 'target', 'ip', 'detail'],
})
</script>

<style scoped>
/* toolbar/pager 已收敛至 src/styles/common.css */
</style>