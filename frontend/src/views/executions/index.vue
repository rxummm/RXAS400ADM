<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="`${$t('common.keyword')}: ${$t('executions.name')} / ${$t('executions.user')}`"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="200"
      @force-search="onFilterChange"
      @reset="resetSearch"
    >
      <el-select v-model="typeFilter" :placeholder="$t('executions.allTypes')" clearable class="w-140" @change="onFilterChange">
        <el-option :label="$t('executions.typeSchedule')" value="schedule" />
        <el-option :label="$t('executions.typeScript')" value="script" />
      </el-select>
      <el-select v-model="statusFilter" :placeholder="$t('executions.allStatus')" clearable class="w-130" @change="onFilterChange">
        <el-option label="SUCCESS" value="SUCCESS" />
        <el-option label="FAILED" value="FAILED" />
      </el-select>
      <template #right>
        <el-button type="primary" :icon="Download" @click="exportCsv">{{ $t('executions.export') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column :label="$t('executions.source')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 'SCHEDULE' ? 'warning' : 'primary'" size="small">
              {{ row.source === 'SCHEDULE' ? $t('executions.typeSchedule') : $t('executions.typeScript') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('executions.name')" min-width="150" />
        <el-table-column prop="type" :label="$t('executions.type')" width="70" />
        <el-table-column prop="serverId" :label="$t('executions.server')" width="80">
          <template #default="{ row }">{{ row.serverId ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="user" :label="$t('executions.user')" width="110">
          <template #default="{ row }">{{ row.user || '-' }}</template>
        </el-table-column>
        <el-table-column prop="runTime" :label="$t('executions.time')" width="170">
          <template #default="{ row }">{{ formatDate(row.runTime) || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('executions.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" :label="$t('executions.message')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="costMs" :label="$t('executions.cost')" width="90">
          <template #default="{ row }">{{ row.costMs ?? '-' }}</template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && total === 0" :description="$t('executions.empty')" />
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Executions' })
import { ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import QueryBar from '@/components/QueryBar.vue'
import { listExecutions, type ExecutionRecord } from '@/api/execution'
import { triggerBlobDownload } from '@/api/blobClient'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { formatDate } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const typeFilter = ref<string | null>(null)
const statusFilter = ref<string | null>(null)

const {
  pagedData,
  loading,
  keyword,
  current,
  size,
  total,
  isFromCache,
  dataSourceTick,
  resetSearch,
  handleRefresh,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<ExecutionRecord>({
  // 后端过滤（来源/状态/关键字）+ 分页（P1 大数据页优化）
  fetchApi: (params) => listExecutions(params),
  enableCache: true,
  buildParams: (base) => ({
    type: typeFilter.value || undefined,
    status: statusFilter.value || undefined,
    keyword: base.keyword || undefined,
    current: base.current,
    size: base.size,
  }),
})

const onFilterChange = () => {
  handleRefresh()
}

const exportCsv = async () => {
  const res = await listExecutions({
    type: typeFilter.value || undefined,
    status: statusFilter.value || undefined,
    keyword: keyword.value || undefined,
    limit: 500,
  })
  const rows = res?.records || []
  if (rows.length === 0) {
    return
  }
  const header = ['source', 'name', 'type', 'serverId', 'user', 'runTime', 'status', 'message', 'costMs']
  const lines = rows.map((r: ExecutionRecord) =>
    header.map((h) => {
      const v = r[h as keyof ExecutionRecord]
      const s = v === null || v === undefined ? '' : String(v)
      return `"${s.replace(/"/g, '""')}"`
    }).join(','),
  )
  const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  triggerBlobDownload(blob, `rxas400-executions-${new Date().toISOString().slice(0, 10)}.csv`)
}
</script>

<style scoped>
/* card-header 已收敛至 src/styles/common.css */
</style>