<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="`${$t('common.keyword')}: ${$t('schedule.name')} / ${$t('schedule.command')} / Cron`"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="240"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <template #right>
        <el-button type="primary" :icon="Plus" @click="() => openCreate()">{{ $t('schedule.create') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column prop="name" :label="$t('schedule.name')" min-width="140" />
        <el-table-column :label="$t('schedule.type')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.scheduleType === 'SQL' ? 'warning' : 'success'" size="small">{{ row.scheduleType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serverId" label="Server" width="80" />
        <el-table-column prop="command" :label="$t('schedule.command')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="cronExpr" label="Cron" width="130" />
        <el-table-column :label="$t('schedule.enabled')" width="90">
          <template #default="{ row }">
            <el-switch
              v-has-perm="'SCHEDULE_MANAGE'"
              :model-value="row.enabled"
              size="small"
              @change="(v: string | number | boolean) => toggle(row as JobSchedule, Boolean(v))"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('schedule.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastRunTime" :label="$t('schedule.lastRun')" width="170">
          <template #default="{ row }">{{ row.lastRunTime || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('schedule.lastResult')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.lastResult" :class="resultClass(row.status)">
              {{ resultText(row.status, row.lastResult) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'SCHEDULE_MANAGE'" size="small" type="primary" plain :loading="runningId === row.id" @click="run(row as JobSchedule)">
              {{ $t('schedule.runNow') }}
            </el-button>
            <el-button size="small" @click="showHistory(row as JobSchedule)">{{ $t('schedule.history') }}</el-button>
            <el-button v-has-perm="'SCHEDULE_MANAGE'" size="small" type="warning" plain @click="openEdit(row as JobSchedule)">{{ $t('common.edit') }}</el-button>
            <el-button v-has-perm="'SCHEDULE_MANAGE'" size="small" type="danger" plain :loading="removeLoading === row.id" @click="confirmRemove(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-sm)" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="var(--rx-form-label-width)">
        <el-form-item :label="$t('schedule.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('schedule.server')" prop="serverId">
          <el-select v-model="form.serverId" class="w-full">
            <el-option v-for="s in servers" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('schedule.type')" prop="scheduleType">
          <el-radio-group v-model="form.scheduleType">
            <el-radio-button value="CL">CL</el-radio-button>
            <el-radio-button value="SQL">SQL</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('schedule.command')" prop="command">
          <el-input v-model="form.command" type="textarea" :rows="4" :placeholder="$t('schedule.commandHint')" />
        </el-form-item>
        <el-form-item label="Cron" prop="cronExpr">
          <el-input v-model="form.cronExpr" placeholder="0 0 6 * * ?" />
          <div class="cron-hint">{{ $t('schedule.cronHint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('schedule.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="historyVisible" :title="`${$t('schedule.history')} - ${historyJob?.name || ''}`" size="620px">
      <RxSkeleton type="table" :rows="5" :loading="historyLoading">
          <el-table :data="history" size="small" border>
        <el-table-column prop="runTime" :label="$t('schedule.time')" width="170" />
        <el-table-column :label="$t('schedule.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('schedule.message')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ resultText(row.status, row.message) }}</template>
        </el-table-column>
        <el-table-column prop="costMs" :label="$t('schedule.cost')" width="90" />
      </el-table>
      </RxSkeleton>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Schedules' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { Plus } from '@element-plus/icons-vue'
import QueryBar from '@/components/QueryBar.vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import {
  createSchedule,
  deleteSchedule,
  executeSchedule,
  listSchedules,
  scheduleHistory,
  toggleSchedule,
  updateSchedule,
  type JobSchedule,
  type JobScheduleRequest,
  type ScheduleHistoryRow,
  type ScheduleRunResult,
} from '@/api/schedule'

const { t } = useI18n()
const as400Store = useAs400ServerStore()
const servers = ref<As400Server[]>([])
const runningId = ref(0)

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
  fetchData,
} = useSmartQueryTable<JobSchedule>({
  fetchApi: () => listSchedules(),
  frontendPage: true,
  enableCache: true,
  searchFields: ['name', 'command', 'cronExpr'],
})

type ScheduleForm = JobScheduleRequest & { id?: number }

const {
  dialogVisible,
  dialogTitle,
  loading: saving,
  formRef,
  form,
  rules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<ScheduleForm>({
  defaultForm: () => ({
    name: '',
    serverId: servers.value[0]?.id || 0,
    scheduleType: 'CL',
    command: '',
    cronExpr: '0 0 6 * * ?',
    enabled: true,
  }),
  rules: {
    name: [{ required: true, message: () => t('common.required'), trigger: 'blur' }],
    serverId: [{ required: true, message: () => t('common.required'), trigger: 'change' }],
    command: [{ required: true, message: () => t('common.required'), trigger: 'blur' }],
    cronExpr: [{ required: true, message: () => t('common.required'), trigger: 'blur' }],
  },
  saveApi: async (isEdit, data) => {
    const payload: JobScheduleRequest = {
      name: data.name,
      serverId: data.serverId,
      scheduleType: data.scheduleType,
      command: data.command,
      cronExpr: data.cronExpr,
      enabled: data.enabled,
    }
    if (isEdit && data.id) await updateSchedule(data.id, payload)
    else await createSchedule(payload)
  },
  onSuccess: () => fetchData({}, true),
  i18nPrefix: 'schedule',
})

const historyVisible = ref(false)
const history = ref<ScheduleHistoryRow[]>([])
const historyLoading = ref(false)
const historyJob = ref<JobSchedule | null>(null)

const statusType = (s: string) => {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

/** W1：后端 message 只存原始信息，成功/失败前缀按 status 在此渲染（对齐 N1/N2） */
const resultText = (status: string, msg?: string | null) => {
  const prefix = status === 'SUCCESS' ? t('schedule.execSuccess') : t('schedule.execFailed')
  return msg ? `${prefix}: ${msg}` : prefix
}

/** 失败结果标红（放 helper 避免模板内字符串字面量被 class 扫描器误判） */
const resultClass = (status: string) => (status === 'FAILED' ? 'text-danger' : '')

const load = () => fetchData({}, true)

const toggle = async (row: JobSchedule, v: boolean) => {
  await toggleSchedule(row.id, v)
  ElMessage.success(v ? t('schedule.enabledOn') : t('schedule.enabledOff'))
  await load()
}

const run = async (row: JobSchedule) => {
  runningId.value = row.id
  try {
    const result: ScheduleRunResult = await executeSchedule(row.id)
    ElMessage[result.status === 'SUCCESS' ? 'success' : 'error'](resultText(result.status, result.message))
    await load()
  } finally {
    runningId.value = 0
  }
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: JobSchedule) => deleteSchedule(row.id),
  onSuccess: load,
  confirmMessage: 'schedule.deleteConfirm',
})

const showHistory = async (row: JobSchedule) => {
  historyJob.value = row
  historyVisible.value = true
  historyLoading.value = true
  try {
    history.value = await scheduleHistory(row.id)
  } finally {
    historyLoading.value = false
  }
}

onMounted(async () => {
  servers.value = await as400Store.fetchServers()
  await load()
})
</script>

<style scoped>
/* card-header/text-danger/text-muted 已收敛至 src/styles/common.css */
.cron-hint {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
