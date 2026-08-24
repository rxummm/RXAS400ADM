<template>
  <div class="page-container page-container--fit">
    <div class="table-wrapper">
      <el-tabs v-model="activeTab">
        <el-tab-pane v-if="canSeeTab('reports', 'reportManualTab')" :label="$t('reports.tabManual')" name="manual">
          <el-form label-width="110px" class="form-compact">
              <el-form-item :label="$t('reports.kind')">
                <el-select v-model="kind" class="w-full">
                  <el-option :label="$t('reports.kindMetrics')" value="metrics" />
                  <el-option :label="$t('reports.kindExecutions')" value="executions" />
                  <el-option :label="$t('reports.kindCapacity')" value="capacity" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('reports.format')">
                <el-radio-group v-model="format">
                  <el-radio-button value="xlsx">Excel (.xlsx)</el-radio-button>
                  <el-radio-button value="pdf">PDF</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="kind !== 'executions'" :label="$t('reports.instance')">
                <el-select v-model="instanceId" class="w-full">
                  <el-option v-for="s in systems" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('reports.days')">
                <el-input-number v-model="days" :min="1" :max="365" />
                <span class="hint">{{ $t('reports.daysHint') }}</span>
              </el-form-item>
              <el-form-item :label="$t('reports.executionType')" v-if="kind === 'executions'">
                <el-select v-model="execType" clearable class="w-full">
                  <el-option :label="$t('reports.allTypes')" value="" />
                  <el-option :label="$t('reports.typeSchedule')" value="SCHEDULE" />
                  <el-option :label="$t('reports.typeScript')" value="SCRIPT" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Download" :loading="downloading" @click="download">
                  {{ $t('reports.download') }}
                </el-button>
              </el-form-item>
            </el-form>
            <el-alert type="info" :closable="false" class="mt16">
              <template #title>
                <!-- P2-28：v-html → 插值，消除存储型 XSS 面（i18n 文案按纯文本渲染） -->
                <span>{{ $t('reports.pdfNote') }}</span>
              </template>
            </el-alert>
        </el-tab-pane>

        <el-tab-pane v-if="canSeeTab('reports', 'reportScheduleTab')" :label="$t('reports.tabSchedule')" name="schedule">
          <div class="search-bar">
            <el-button v-has-perm="'REPORT_MANAGE'" type="primary" :icon="Plus" @click="() => openCreate()">
              {{ $t('reports.scheduleAdd') }}
            </el-button>
            <el-button :icon="Refresh" @click="loadSchedules">{{ $t('common.refresh') }}</el-button>
          </div>
          <RxSkeleton type="table" :rows="5" :loading="scheduleLoading">
            <el-table :data="schedules" size="small" border>
            <el-table-column prop="name" :label="$t('reports.scheduleName')" min-width="140" />
            <el-table-column :label="$t('reports.kind')" width="110">
              <template #default="{ row }">
                <el-tag size="small">{{ reportTypeLabel(row.reportType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('reports.format')" width="80">
              <template #default="{ row }">
                <span>{{ (row.format || 'xlsx').toUpperCase() }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('reports.instance')" width="130">
              <template #default="{ row }">
                <span v-if="row.reportType !== 'executions'">{{ serverName(row.serverId) }}</span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="cronExpr" :label="$t('reports.cron')" width="120" />
            <el-table-column prop="recipients" :label="$t('reports.recipients')" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row.recipients || '-' }}</template>
            </el-table-column>
            <el-table-column :label="$t('reports.enabled')" width="80">
              <template #default="{ row }">
                <el-switch
                  v-has-perm="'REPORT_MANAGE'"
                  :model-value="row.enabled"
                  size="small"
                  @change="(v: string | number | boolean) => toggle(row as ReportSchedule, Boolean(v))"
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('reports.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastRunTime" :label="$t('reports.lastRun')" width="165">
              <template #default="{ row }">{{ row.lastRunTime || '-' }}</template>
            </el-table-column>
            <el-table-column :label="$t('reports.lastResult')" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.lastResult" :class="row.lastResult.startsWith('FAILED') ? 'text-danger' : ''">
                  {{ row.lastResult }}
                </span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.operation')" width="240" fixed="right">
              <template #default="{ row }">
                <el-button v-has-perm="'REPORT_MANAGE'" size="small" type="primary" plain :loading="runningId === row.id" @click="run(row as ReportSchedule)">
                  {{ $t('reports.runNow') }}
                </el-button>
                <el-button size="small" @click="showHistory(row as ReportSchedule)">{{ $t('reports.history') }}</el-button>
                <el-button v-has-perm="'REPORT_MANAGE'" size="small" type="warning" plain @click="openEdit(row as ReportSchedule)">{{ $t('common.edit') }}</el-button>
                <el-button v-has-perm="'REPORT_MANAGE'" size="small" type="danger" plain @click="remove(row as ReportSchedule)">{{ $t('common.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 定时任务编辑 -->
    <ReportScheduleDialog ref="scheduleDialogRef" v-model="dialogVisible" :systems="systems" @saved="loadSchedules" />

    <!-- 执行历史 -->
    <el-drawer v-model="historyVisible" :title="`${$t('reports.history')} - ${historyJob?.name || ''}`" size="620px">
      <RxSkeleton type="table" :rows="5" :loading="historyLoading">
        <el-table :data="history" size="small" border>
        <el-table-column prop="runTime" :label="$t('reports.time')" width="170" />
        <el-table-column :label="$t('reports.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" :label="$t('reports.message')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="$t('reports.fileSize')" width="110">
          <template #default="{ row }">{{ formatSize(row.fileBytes) }}</template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Reports' })
import { onMounted, ref } from 'vue'
import { Download, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { downloadReport } from '@/api/report'
import {
  deleteReportSchedule,
  executeReportSchedule,
  listReportSchedules,
  reportScheduleHistory,
  toggleReportSchedule,
  type ReportHistoryRow,
  type ReportRunResult,
  type ReportSchedule,
} from '@/api/report'
import {
  fetchSystems, type IbmiSystem
} from '@/api/as400'
import ReportScheduleDialog from './ReportScheduleDialog.vue'
import { formatSize } from '@/utils/format'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const userStore = useUserStore()

const canSeeTab = (page: string, tab: string) => userStore.canSeeTab(page, tab)

const activeTab = ref('manual')

/* ---------- 手动导出 ---------- */
const kind = ref<'metrics' | 'executions' | 'capacity'>('metrics')
const format = ref('xlsx')
const instanceId = ref<number>()
const days = ref(7)
const execType = ref<string | null>(null)
const systems = ref<IbmiSystem[]>([])
const downloading = ref(false)

const download = async () => {
  if (kind.value !== 'executions' && !instanceId.value) {
    ElMessage.warning(t('reports.selectInstance'))
    return
  }
  downloading.value = true
  try {
    const params: Record<string, unknown> = { format: format.value }
    if (kind.value === 'metrics') {
      params.instanceId = instanceId.value
      params.days = days.value
    } else if (kind.value === 'capacity') {
      params.instanceId = instanceId.value
      params.days = days.value
    } else {
      if (execType.value) params.type = execType.value
    }
    const stamp = new Date().toISOString().slice(0, 10)
    await downloadReport(kind.value, params, `report-${kind.value}-${stamp}.${format.value}`)
    ElMessage.success(t('reports.downloaded'))
  } catch {
    ElMessage.error(t('reports.downloadFailed'))
  } finally {
    downloading.value = false
  }
}

/* ---------- 定时任务 ---------- */
const schedules = ref<ReportSchedule[]>([])
const scheduleLoading = ref(false)
const runningId = ref(0)

const serverName = (id: number | null | undefined) =>
  systems.value.find((s) => s.id === id)?.name || `#${id}`

const reportTypeLabel = (type: string) => {
  if (type === 'metrics') return t('reports.kindMetrics')
  if (type === 'capacity') return t('reports.kindCapacity')
  return t('reports.kindExecutions')
}


const loadSchedules = async () => {
  scheduleLoading.value = true
  try {
    schedules.value = await listReportSchedules()
  } finally {
    scheduleLoading.value = false
  }
}

const dialogVisible = ref(false)
const scheduleDialogRef = ref<InstanceType<typeof ReportScheduleDialog>>()

const openCreate = () => {
  scheduleDialogRef.value?.openCreate()
}

const openEdit = (row: ReportSchedule) => {
  scheduleDialogRef.value?.openEdit(row)
}

const toggle = async (row: ReportSchedule, v: boolean) => {
  await toggleReportSchedule(row.id!, v)
  ElMessage.success(v ? t('reports.enabledOn') : t('reports.enabledOff'))
  await loadSchedules()
}

const run = async (row: ReportSchedule) => {
  runningId.value = row.id!
  try {
    const result: ReportRunResult = await executeReportSchedule(row.id!)
    ElMessage[result.status === 'SUCCESS' ? 'success' : 'error'](result.message)
    await loadSchedules()
  } finally {
    runningId.value = 0
  }
}

const remove = async (row: ReportSchedule) => {
  await ElMessageBox.confirm(t('reports.scheduleDeleteConfirm'), t('common.confirm'), { type: 'warning' })
  await deleteReportSchedule(row.id!)
  ElMessage.success(t('common.delete'))
  await loadSchedules()
}

const historyVisible = ref(false)
const history = ref<ReportHistoryRow[]>([])
const historyLoading = ref(false)
const historyJob = ref<ReportSchedule | null>(null)

const showHistory = async (row: ReportSchedule) => {
  historyJob.value = row
  historyVisible.value = true
  historyLoading.value = true
  try {
    history.value = await reportScheduleHistory(row.id!)
  } finally {
    historyLoading.value = false
  }
}

onMounted(async () => {
  try {
    systems.value = await fetchSystems()
    if (systems.value.length && !instanceId.value) instanceId.value = systems.value[0].id
  } catch {
    systems.value = []
  }
  const manualVisible = canSeeTab('reports', 'reportManualTab')
  const scheduleVisible = canSeeTab('reports', 'reportScheduleTab')
  if (scheduleVisible && !manualVisible) {
    activeTab.value = 'schedule'
  }
  if (scheduleVisible) {
    await loadSchedules()
  }
})
</script>

<style scoped>
/* mt16/hint/text-danger/text-muted 已收敛至 src/styles/common.css */
.form-compact { max-width: 560px; }
</style>