<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="`${$t('common.keyword')}: ${$t('subsystems.name')} / ${$t('subsystems.description')}`"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="240"
      @force-search="handleRefresh"
      @reset="resetSearch"
    />

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column prop="SUBSYSTEM_NAME" :label="$t('subsystems.name')" min-width="130" />
        <el-table-column prop="SUBSYSTEM_DESCRIPTION" :label="$t('subsystems.description')" min-width="180" />
        <el-table-column :label="$t('subsystems.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.STATUS === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.STATUS }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="NUMBER_OF_ACTIVE_JOBS" :label="$t('subsystems.activeJobs')" width="110" />
        <el-table-column prop="MAXIMUM_ACTIVE_JOBS" :label="$t('subsystems.maxJobs')" width="110" />
        <el-table-column prop="SUBSYSTEM_LIBRARY" :label="$t('subsystems.library')" min-width="100" />
        <el-table-column :label="$t('common.operation')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-has-perm="'SUBSYSTEM_MANAGE'"
              v-if="row.STATUS !== 'ACTIVE'"
              size="small"
              type="success"
              plain
              :loading="opName === row.SUBSYSTEM_NAME"
              @click="start(row)"
            >
              {{ $t('subsystems.start') }}
            </el-button>
            <el-button
              v-has-perm="'SUBSYSTEM_MANAGE'"
              v-else
              size="small"
              type="danger"
              plain
              :loading="opName === row.SUBSYSTEM_NAME"
              @click="end(row)"
            >
              {{ $t('subsystems.end') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && filteredData.length === 0" :description="$t('subsystems.empty')" />
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Subsystems' })
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QueryBar from '@/components/QueryBar.vue'
import { useI18n } from 'vue-i18n'
import { endSubsystem, listSubsystems, startSubsystem, type SubsystemRow } from '@/api/subsystem'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const opName = ref('')

const {
  filteredData,
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
  fetchData,
} = useSmartQueryTable<SubsystemRow>({
  fetchApi: () => listSubsystems(),
  frontendPage: true,
  enableCache: true,
  searchFields: ['SUBSYSTEM_NAME', 'SUBSYSTEM_DESCRIPTION', 'SUBSYSTEM_LIBRARY'],
})

const start = async (row: { SUBSYSTEM_NAME?: string }) => {
  if (!row.SUBSYSTEM_NAME) return
  opName.value = row.SUBSYSTEM_NAME
  try {
    const result = await startSubsystem(row.SUBSYSTEM_NAME)
    ElMessage.success(result?.message)
    await fetchData({}, true)
  } finally {
    opName.value = ''
  }
}

const end = async (row: { SUBSYSTEM_NAME?: string }) => {
  try {
    await ElMessageBox.confirm(
      t('subsystems.endConfirm', { name: row.SUBSYSTEM_NAME }),
      t('common.confirm'),
      { type: 'warning' },
    )
  } catch {
    return
  }
  if (!row.SUBSYSTEM_NAME) return
  opName.value = row.SUBSYSTEM_NAME
  try {
    const result = await endSubsystem(row.SUBSYSTEM_NAME)
    ElMessage.success(result?.message)
    await fetchData({}, true)
  } finally {
    opName.value = ''
  }
}
</script>

<style scoped>
/* card-header 已收敛至 src/styles/common.css */
</style>