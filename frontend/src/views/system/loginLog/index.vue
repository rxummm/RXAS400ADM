<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="username"
        :placeholder="$t('loginLog.username')"
        clearable
        class="w-160"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="action"
        :placeholder="$t('loginLog.action')"
        clearable
        class="w-180"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon> {{ $t('common.refresh') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border stripe class="w-full">
        <el-table-column prop="userName" :label="$t('loginLog.username')" width="120" />
        <el-table-column prop="action" :label="$t('loginLog.action')" width="180">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTag(row.action)">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" :label="$t('loginLog.ip')" width="140" />
        <el-table-column prop="target" :label="$t('loginLog.target')" width="180" show-overflow-tooltip />
        <el-table-column prop="detail" :label="$t('loginLog.detail')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdTime" :label="$t('loginLog.time')" width="170">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size"
        @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listAuditLogs, type AuditLog } from '@/api/audit'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

/**
 * P2-30：审计「模块」过滤值是后端数据值（@OperateLog(module="登录安全") 写入 rx_audit_log 的中文字段），
 * 不能做 i18n（en-US 下会过滤不到数据），统一为常量 + 注释说明。
 */
const LOGIN_SECURITY_MODULE = '登录安全'

defineOptions({ name: 'LoginLog' })

const username = ref('')
const action = ref('')

const {
  pagedData,
  loading,
  total,
  current,
  size,
  handleSearch,
  resetSearch,
  handlePageChange,
  handleSizeChange,
  handleRefresh,
} = useSmartQueryTable<AuditLog>({
  fetchApi: (params) => listAuditLogs({
    ...params,
    module: LOGIN_SECURITY_MODULE,
    username: username.value || undefined,
    action: action.value || undefined,
  }),
  defaultSize: 20,
  buildParams: (base) => ({
    ...base,
    module: LOGIN_SECURITY_MODULE,
    username: username.value || undefined,
    action: action.value || undefined,
  }),
})

const actionTag = (a: string) =>
  a?.includes('SUCCESS') ? 'success' : a?.includes('FAILED') ? 'danger' : 'info'

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}
</script>