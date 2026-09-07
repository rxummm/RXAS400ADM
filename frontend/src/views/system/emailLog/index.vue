<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-select v-model="filterChannel" clearable :placeholder="$t('emailLog.channel')" class="w-150">
        <el-option :label="$t('emailLog.channelAlert')" value="ALERT" />
        <el-option :label="$t('emailLog.channelReport')" value="REPORT" />
        <el-option :label="$t('emailLog.channelManual')" value="MANUAL" />
      </el-select>
      <el-select v-model="filterStatus" clearable :placeholder="$t('emailLog.status')" class="w-150">
        <el-option :label="$t('emailLog.success')" value="SUCCESS" />
        <el-option :label="$t('emailLog.failed')" value="FAILED" />
      </el-select>
      <el-input v-model="keyword" clearable :placeholder="$t('common.search')" @keyup.enter="load" class="w-200" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="createdTime" :label="$t('common.createdTime')" width="170" />
        <el-table-column prop="subject" :label="$t('emailLog.subject')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="recipients" :label="$t('emailLog.recipients')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="channel" :label="$t('emailLog.channel')" width="100" />
        <el-table-column prop="status" :label="$t('emailLog.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attachmentName" :label="$t('emailLog.attachment')" width="150" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="showDetail(row as EmailLog)">{{ $t('common.detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="load" />
    </div>

    <el-dialog v-model="detailVisible" :title="$t('emailLog.detail')" width="500px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item :label="$t('emailLog.subject')">{{ detail?.subject }}</el-descriptions-item>
        <el-descriptions-item :label="$t('emailLog.recipients')">{{ detail?.recipients }}</el-descriptions-item>
        <el-descriptions-item :label="$t('emailLog.channel')">{{ detail?.channel }}</el-descriptions-item>
        <el-descriptions-item :label="$t('emailLog.status')">{{ detail?.status }}</el-descriptions-item>
        <el-descriptions-item :label="$t('emailLog.errorMsg')" v-if="detail?.errorMessage">
          <span class="text-danger">{{ detail?.errorMessage }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('emailLog.attachment')">{{ detail?.attachmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('common.createdTime')">{{ detail?.createdTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listEmailLogs, getEmailLog } from '@/api/email'
import type { EmailLog } from '@/api/email'

defineOptions({ name: 'EmailLog' })

const loading = ref(false)
const rows = ref<EmailLog[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const filterChannel = ref('')
const filterStatus = ref('')
const keyword = ref('')

const detailVisible = ref(false)
const detail = ref<EmailLog | null>(null)

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const res = await listEmailLogs({
      current: current.value,
      size: size.value,
      channel: filterChannel.value || undefined,
      status: filterStatus.value || undefined,
      keyword: keyword.value || undefined,
    })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function showDetail(row: EmailLog) {
  detail.value = await getEmailLog(row.id)
  detailVisible.value = true
}
</script>