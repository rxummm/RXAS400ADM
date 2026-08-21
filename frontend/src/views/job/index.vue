<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('common.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="200"
      @force-search="refreshJobs"
      @reset="resetJobs"
    >
      <template #right>
        <el-button :icon="ChatDotRound" @click="loadMessages">{{ $t('jobs.messages') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <el-tabs v-model="section">
        <!-- 活动作业 -->
        <el-tab-pane v-if="userStore.canSeeTab('jobs', 'jobsTab')" :label="$t('jobs.jobsTab')" name="jobs">
          <div class="mb8">
            <el-radio-group v-model="tab" size="small" @change="onTabChange">
              <el-radio-button value="all">{{ $t('jobs.all') }}</el-radio-button>
              <el-radio-button value="msgw">{{ $t('jobs.msgw') }}</el-radio-button>
              <el-radio-button value="lckw">{{ $t('jobs.lckw') }}</el-radio-button>
            </el-radio-group>
          </div>
          <RxSkeleton type="table" :rows="8" :loading="loading">
            <el-table :data="pagedJobs" size="small" empty-text="">
            <template #empty>
              <el-empty :description="$t('jobs.empty')" :image-size="80" />
            </template>
            <el-table-column prop="jobName" :label="$t('jobs.jobName')" min-width="140" />
            <el-table-column prop="jobUser" :label="$t('jobs.jobUser')" width="110" />
            <el-table-column prop="jobNumber" :label="$t('jobs.jobNumber')" width="100" />
            <el-table-column prop="jobStatus" :label="$t('jobs.jobStatus')" width="90">
              <template #default="{ row }: { row: JobInfo }">
                <el-tag :type="statusType(row.jobStatus)" size="small">{{ row.jobStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="jobProgram" :label="$t('jobs.jobProgram')" min-width="120" />
            <el-table-column prop="cpuTime" :label="$t('jobs.cpuTime')" width="90" />
            <el-table-column prop="temporaryStorage" :label="$t('jobs.tempStorage')" width="110" />
            <el-table-column :label="$t('common.operation')" width="280" fixed="right">
              <template #default="{ row }: { row: JobInfo }">
                <el-button size="small" @click="openLog(row)">{{ $t('jobs.log') }}</el-button>
                <el-button
                  v-has-perm="'JOB_END'"
                  v-if="row.jobStatus === 'HELD'"
                  size="small"
                  type="warning"
                  plain
                  @click="handleRelease(row)"
                >
                  {{ $t('jobs.release') }}
                </el-button>
                <el-button v-has-perm="'JOB_END'" v-else size="small" type="warning" plain @click="handleHold(row)">
                  {{ $t('jobs.hold') }}
                </el-button>
                <el-button
                  v-has-perm="'JOB_END'"
                  v-if="row.jobStatus === 'MSGW'"
                  size="small"
                  type="primary"
                  plain
                  @click="handleReply(row)"
                >
                  {{ $t('jobs.reply') }}
                </el-button>
                <el-button v-has-perm="'JOB_END'" size="small" type="danger" plain @click="handleEnd(row)">
                  {{ $t('jobs.end') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
          <AppPagination :total="jobsTotal" v-model:current="jobsCurrent" v-model:size="jobsSize" @change="handlePageChange" @size-change="handleSizeChange" />
        </el-tab-pane>

        <!-- 作业队列 -->
        <el-tab-pane v-if="userStore.canSeeTab('jobs', 'queuesTab')" :label="$t('jobs.queuesTab')" name="queues">
          <RxSkeleton type="table" :rows="5" :loading="queuesLoading">
            <el-table :data="pagedQueues" size="small" border>
            <el-table-column prop="JOB_QUEUE_NAME" :label="$t('jobs.queueName')" min-width="130" />
            <el-table-column prop="JOB_QUEUE_LIBRARY" :label="$t('jobs.queueLibrary')" min-width="110" />
            <el-table-column prop="JOB_QUEUE_STATUS" :label="$t('jobs.queueStatus')" width="110">
              <template #default="{ row }: { row: JobQueueInfo }">
                <el-tag :type="row.JOB_QUEUE_STATUS === 'RELEASED' ? 'danger' : 'success'" size="small">
                  {{ row.JOB_QUEUE_STATUS }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="NUMBER_OF_JOBS" :label="$t('jobs.queueJobs')" width="100" />
            <el-table-column prop="JOB_QUEUE_TYPE" :label="$t('jobs.queueType')" width="120" />
          </el-table>
          </RxSkeleton>
          <el-empty v-if="!queuesLoading && queuesTotal === 0" :description="$t('jobs.empty')" />
          <AppPagination :total="queuesTotal" v-model:current="queuesCurrent" v-model:size="queuesSize" @change="onQueuesPageChange" @size-change="onQueuesSizeChange" />
        </el-tab-pane>

        <!-- SPOOL 文件 -->
        <el-tab-pane v-if="userStore.canSeeTab('jobs', 'spoolTab')" :label="$t('jobs.spoolTab')" name="spool">
          <div class="search-bar">
            <el-input v-model="spoolJobName" :placeholder="$t('jobs.jobName')" clearable class="w-150" @keyup.enter="handleSpoolSearch" />
            <el-input v-model="spoolJobUser" :placeholder="$t('jobs.jobUser')" clearable class="w-130" @keyup.enter="handleSpoolSearch" />
            <el-input v-model="spoolJobNumber" :placeholder="$t('jobs.jobNumber')" clearable class="w-120" @keyup.enter="handleSpoolSearch" />
            <el-button type="primary" @click="handleSpoolSearch">{{ $t('common.search') }}</el-button>
            <el-button :icon="Refresh" @click="handleSpoolRefresh">{{ $t('common.refresh') }}</el-button>
            <el-tag v-if="spoolIsFromCache" size="small" type="info">{{ $t('common.fromCache') }}</el-tag>
          </div>
          <RxSkeleton type="table" :rows="5" :loading="spoolLoading">
            <el-table :data="pagedSpools" size="small" border>
            <el-table-column prop="SPOOLED_FILE_NAME" :label="$t('jobs.spoolName')" min-width="130" />
            <el-table-column prop="JOB_NAME" :label="$t('jobs.jobName')" min-width="110" />
            <el-table-column prop="JOB_USER" :label="$t('jobs.jobUser')" width="100" />
            <el-table-column prop="JOB_NUMBER" :label="$t('jobs.jobNumber')" width="90" />
            <el-table-column prop="OUTPUT_QUEUE" :label="$t('jobs.outputQueue')" min-width="110" />
            <el-table-column prop="SPOOLED_FILE_STATUS" :label="$t('jobs.spoolStatus')" width="110">
              <template #default="{ row }: { row: SpoolFile }">
                <el-tag :type="row.SPOOLED_FILE_STATUS === 'HELD' ? 'warning' : 'success'" size="small">
                  {{ row.SPOOLED_FILE_STATUS }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="NUMBER_OF_PAGES" :label="$t('jobs.spoolPages')" width="90" />
            <el-table-column prop="USER_DATA" :label="$t('jobs.spoolUserData')" width="100" />
          </el-table>
          </RxSkeleton>
          <el-empty v-if="!spoolLoading && spoolTotal === 0" :description="$t('jobs.empty')" />
          <AppPagination :total="spoolTotal" v-model:current="spoolsCurrent" v-model:size="spoolsSize" @change="onSpoolsPageChange" @size-change="onSpoolsSizeChange" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 作业日志 Drawer -->
    <el-drawer v-model="logVisible" :title="$t('jobs.logDrawer')" size="55%">
      <el-table :data="logRows" size="small" max-height="560">
        <el-table-column prop="ORDINAL_POSITION" :label="$t('jobs.ordinal')" width="60" />
        <el-table-column prop="MESSAGE_ID" :label="$t('jobs.messageId')" width="110" />
        <el-table-column prop="MESSAGE_TYPE" :label="$t('jobs.messageType')" width="130" />
        <el-table-column prop="MESSAGE_TEXT" :label="$t('jobs.messageText')" min-width="220" />
        <el-table-column prop="MESSAGE_TIMESTAMP" :label="$t('jobs.timestamp')" width="170" />
      </el-table>
      <el-empty v-if="logRows.length === 0" :description="$t('jobs.noLog')" />
    </el-drawer>

    <!-- MSGW 待应答消息 Drawer -->
    <el-drawer v-model="msgVisible" :title="$t('jobs.messagesTitle')" size="60%">
      <el-table :data="msgwMsgs" size="small" max-height="560">
        <el-table-column prop="JOB_NAME" :label="$t('jobs.jobName')" min-width="130" />
        <el-table-column prop="JOB_USER" :label="$t('jobs.jobUser')" width="110" />
        <el-table-column prop="MESSAGE_ID" :label="$t('jobs.messageId')" width="110" />
        <el-table-column prop="MESSAGE_TYPE" :label="$t('jobs.messageType')" width="120" />
        <el-table-column prop="MESSAGE_TEXT" :label="$t('jobs.messageText')" min-width="220" />
        <el-table-column prop="REPLY_STATUS" :label="$t('jobs.replyStatus')" width="100">
          <template #default="{ row }: { row: MsgwMessage }">
            <el-tag v-if="row.REPLY_STATUS === 'MSGW'" type="danger" size="small">MSGW</el-tag>
            <span v-else>{{ row.REPLY_STATUS || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="100" fixed="right">
          <template #default="{ row }: { row: MsgwMessage }">
            <el-button size="small" type="primary" plain @click="handleReply(row)">
              {{ $t('jobs.reply') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="msgwMsgs.length === 0" :description="$t('jobs.noLog')" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import QueryBar from '@/components/QueryBar.vue'
import { useUserStore } from '@/stores/user'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { normalizeJobIdentity } from '@/utils/jobIdentity'
import {
  endJob,
  fetchJobLog,
  fetchJobQueues,
  fetchJobs,
  fetchLckwJobs,
  fetchMsgwJobs,
  fetchMsgwMessages,
  fetchSpoolFiles,
  holdJob,
  releaseJob,
  replyMsg,
  type JobInfo,
  type MsgwMessage,
  type SpoolFile,
  type JobQueueInfo,
  type JobLogRow,
} from '@/api/job'

const { t } = useI18n()
const userStore = useUserStore()
const section = ref('jobs')
const tab = ref<'all' | 'msgw' | 'lckw'>('all')

// ==================== 活动作业（按 all/msgw/lckw 视图隔离缓存） ====================
const {
  pagedData: pagedJobs,
  loading,
  keyword,
  current: jobsCurrent,
  size: jobsSize,
  total: jobsTotal,
  isFromCache,
  dataSourceTick,
  handlePageChange,
  handleSizeChange,
  fetchData: fetchJobsData,
} = useSmartQueryTable<JobInfo>({
  fetchApi: () =>
    tab.value === 'msgw' ? fetchMsgwJobs() : tab.value === 'lckw' ? fetchLckwJobs() : fetchJobs(),
  frontendPage: true,
  enableCache: true,
  searchFields: ['jobName', 'jobUser', 'jobNumber', 'jobProgram', 'jobStatus'],
})

const onTabChange = () => {
  jobsCurrent.value = 1
  void fetchJobsData({ view: tab.value })
}
const refreshJobs = () => fetchJobsData({ view: tab.value }, true)

const resetJobs = () => {
  keyword.value = ''
  jobsCurrent.value = 1
  void fetchJobsData({ view: tab.value })
}

// ==================== 作业队列 ====================
const {
  pagedData: pagedQueues,
  loading: queuesLoading,
  total: queuesTotal,
  current: queuesCurrent,
  size: queuesSize,
  fetchData: fetchQueuesData,
} = useSmartQueryTable<JobQueueInfo>({
  fetchApi: () => fetchJobQueues(),
  frontendPage: true,
  enableCache: true,
  autoFetch: false,
  searchFields: ['JOB_QUEUE_NAME', 'JOB_QUEUE_LIBRARY', 'JOB_QUEUE_STATUS', 'JOB_QUEUE_TYPE'],
})
const onQueuesPageChange = () => {}
const onQueuesSizeChange = () => {
  queuesCurrent.value = 1
}

// ==================== SPOOL 文件（服务端按作业过滤） ====================
const spoolJobName = ref('')
const spoolJobUser = ref('')
const spoolJobNumber = ref('')
const {
  pagedData: pagedSpools,
  loading: spoolLoading,
  total: spoolTotal,
  current: spoolsCurrent,
  size: spoolsSize,
  isFromCache: spoolIsFromCache,
  handleSearch: handleSpoolSearch,
  handleRefresh: handleSpoolRefresh,
  fetchData: fetchSpoolData,
} = useSmartQueryTable<SpoolFile>({
  fetchApi: (params) =>
    fetchSpoolFiles({
      jobName: (params.jobName as string) || undefined,
      jobUser: (params.jobUser as string) || undefined,
      jobNumber: (params.jobNumber as string) || undefined,
    }),
  frontendPage: true,
  enableCache: true,
  autoFetch: false,
  buildParams: () => ({
    jobName: spoolJobName.value || undefined,
    jobUser: spoolJobUser.value || undefined,
    jobNumber: spoolJobNumber.value || undefined,
  }),
  searchFields: ['SPOOLED_FILE_NAME', 'JOB_NAME', 'JOB_USER', 'OUTPUT_QUEUE', 'USER_DATA'],
})
const onSpoolsPageChange = () => {}
const onSpoolsSizeChange = () => {
  spoolsCurrent.value = 1
}

const logVisible = ref(false)
const logRows = ref<JobLogRow[]>([])
const msgVisible = ref(false)
const msgwMsgs = ref<MsgwMessage[]>([])

const statusType = (s: string) => {
  if (s === 'MSGW' || s === 'LCKW') return 'danger'
  if (s === 'RUN' || s === 'ACTIVE') return 'success'
  if (s === 'HELD') return 'warning'
  return 'info'
}

watch(section, (s) => {
  if (s === 'queues') void fetchQueuesData()
  if (s === 'spool') void fetchSpoolData()
})

const openLog = async (row: JobInfo) => {
  logVisible.value = true
  logRows.value = []
  try {
    logRows.value = await fetchJobLog(row.jobName, row.jobUser, row.jobNumber)
  } catch {
    logRows.value = []
  }
}

const loadMessages = async () => {
  msgVisible.value = true
  msgwMsgs.value = []
  try {
    msgwMsgs.value = await fetchMsgwMessages()
  } catch {
    msgwMsgs.value = []
  }
}

const confirm = async (
  msgKey: string,
  row: { jobName?: string; jobUser?: string; jobNumber?: string },
): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      t(msgKey, { job: `${row.jobName || '-'}/${row.jobUser || '-'}/${row.jobNumber || '-'}` }),
      t('common.confirm'),
      { type: 'warning' },
    )
    return true
  } catch {
    return false
  }
}

const handleEnd = async (row: JobInfo) => {
  if (!(await confirm('jobs.endConfirm', row))) return
  try {
    await endJob(row.jobName, row.jobUser, row.jobNumber)
    ElMessage.success(t('jobs.ended'))
    refreshJobs()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

const handleHold = async (row: JobInfo) => {
  try {
    await holdJob(row.jobName, row.jobUser, row.jobNumber)
    ElMessage.success(t('jobs.holdSuccess'))
    refreshJobs()
  } catch {
    /* interceptor 已提示错误 */
  }
}

const handleRelease = async (row: JobInfo) => {
  try {
    await releaseJob(row.jobName, row.jobUser, row.jobNumber)
    ElMessage.success(t('jobs.releaseSuccess'))
    refreshJobs()
  } catch {
    /* interceptor 已提示错误 */
  }
}

const handleReply = async (row: JobInfo | MsgwMessage) => {
  // 作业表行小写字段、MSGW 抽屉行大写字段（JOB_NAME…），统一归一化后再应答（utils/jobIdentity）
  const job = normalizeJobIdentity(row)
  if (!(await confirm('jobs.replyConfirm', job))) return
  try {
    await replyMsg(job.jobName, job.jobUser, job.jobNumber)
    ElMessage.success(t('jobs.replied'))
    refreshJobs()
    if (msgVisible.value) await loadMessages()
  } catch {
    /* interceptor 已提示错误 */
  }
}


</script>

<style scoped>
/* card-header/mb8/toolbar 已收敛至 src/styles/common.css */
</style>