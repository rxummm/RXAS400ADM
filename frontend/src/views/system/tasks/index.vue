<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('tasks.bean')"
        clearable
        class="w-240"
        @keyup.enter="forceSearch"
      />
      <el-button type="primary" @click="onSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button @click="onSearch">
        <el-icon><Refresh /></el-icon> {{ $t('common.refresh') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedRows" size="small" border stripe class="w-full">
        <el-table-column prop="className" :label="$t('tasks.bean')" width="180" show-overflow-tooltip />
        <el-table-column prop="bean" label="Bean" width="200" show-overflow-tooltip />
        <el-table-column :label="$t('tasks.method')" width="200">
          <template #default="{ row }">{{ (row.methods || []).map((m: TaskMethodInfo) => m.method).join(', ') }}</template>
        </el-table-column>
        <el-table-column :label="$t('tasks.schedule')" min-width="200">
          <template #default="{ row }">
            <div v-for="m in row.methods" :key="m.method" class="task-line">
              <el-tag size="small" :type="m.enabled ? 'success' : 'info'">{{ m.method }}</el-tag>
              <span class="text-muted">{{ m.schedule }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onTrigger(row as TaskBeanInfo)">
              {{ $t('tasks.trigger') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTasks, triggerTask, type TaskBeanInfo, type TaskMethodInfo } from '@/api/task'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'

defineOptions({ name: 'SysTasks' })

const { t } = useI18n()

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
  resetSearch: baseResetSearch,
} = useSmartQueryTable<TaskBeanInfo>({
  fetchApi: () => listTasks().then((data) => data || []),
  frontendPage: true,
  enableCache: true,
  searchFields: ['bean', 'className'],
})

const pagedRows = computed(() => tableData.value)

const onSizeChange = () => {
  current.value = 1
}

async function onTrigger(row: TaskBeanInfo) {
  const method = row.methods?.[0]?.method
  if (!method) return
  try {
    await ElMessageBox.confirm(t('tasks.triggerConfirm', { bean: row.className, method }), t('common.tip'), { type: 'warning' })
    await triggerTask(row.bean, method)
    ElMessage.success(t('tasks.triggered'))
  } catch {
    /* cancelled */
  }
}

const resetSearch = () => {
  baseResetSearch()
}

const onSearch = () => {
  forceSearch()
}
</script>

<style scoped>
.task-line {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.8;
}
</style>