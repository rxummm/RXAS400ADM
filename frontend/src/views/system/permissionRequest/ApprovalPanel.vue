<template>
  <div>
    <div class="search-bar">
      <el-select v-model="filterStatus" :placeholder="$t('permissionRequest.status')" clearable class="w-130">
        <el-option :label="$t('permissionRequest.stPending')" value="PENDING" />
        <el-option :label="$t('permissionRequest.stApproved')" value="APPROVED" />
        <el-option :label="$t('permissionRequest.stRejected')" value="REJECTED" />
      </el-select>
      <el-input
        v-model="filterKeyword"
        :placeholder="$t('permissionRequest.reviewKeyword')"
        clearable
        class="w-180"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
    </div>
    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border stripe class="w-full">
        <el-table-column prop="username" label="User" width="110" />
        <el-table-column :label="$t('permissionRequest.appliedMenus')" min-width="240">
          <template #default="{ row }">
            <el-tag
              v-for="name in parseNames(row.menuNames)"
              :key="name"
              size="small"
              type="info"
              class="mr4"
            >
              {{ name }}
            </el-tag>
            <span v-if="!row.menuNames && row.permissionCode" class="text-muted">{{ row.permissionCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" :label="$t('permissionRequest.reason')" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('permissionRequest.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('permissionRequest.time')" min-width="190">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" @click="onApprove(row)">
                {{ $t('permissionRequest.approve') }}
              </el-button>
              <el-button link type="danger" size="small" @click="onReject(row)">
                {{ $t('permissionRequest.reject') }}
              </el-button>
            </template>
            <span v-else class="text-muted">{{ row.approveComment || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  listPermissionRequests,
  getPermissionRequestPendingCount,
  approvePermissionRequest,
  rejectPermissionRequest,
  parseJsonArray,
  type PermissionRequest,
} from '@/api/permission'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useUserStore } from '@/stores/user'

const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const rows = ref<PermissionRequest[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const filterStatus = ref<string>()
const filterKeyword = ref('')
const pendingCount = ref(0)

const parseNames = (raw?: string) => parseJsonArray(raw)

const formatTime = (time?: string) => {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

const statusTag = (s?: string) => (s === 'PENDING' ? 'warning' : s === 'APPROVED' ? 'success' : 'danger')
const statusLabel = (s?: string) =>
  s === 'PENDING' ? t('permissionRequest.stPending') : s === 'APPROVED' ? t('permissionRequest.stApproved') : t('permissionRequest.stRejected')

async function load() {
  loading.value = true
  try {
    const data = await listPermissionRequests({
      current: current.value,
      size: size.value,
      status: filterStatus.value,
      keyword: filterKeyword.value || undefined,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadPendingCount() {
  if (!userStore.hasPermission('SYS_PERMISSION_REQUEST')) {
    pendingCount.value = 0
    return
  }
  try {
    const data = await getPermissionRequestPendingCount()
    pendingCount.value = data.count || 0
  } catch {
    /* interceptor 已提示错误 */
  }
}

const reset = () => {
  filterStatus.value = undefined
  filterKeyword.value = ''
  current.value = 1
  load()
}

async function onApprove(row: PermissionRequest) {
  try {
    await ElMessageBox.confirm(t('permissionRequest.approveConfirm'), t('common.tip'), { type: 'warning' })
    if (row.id) await approvePermissionRequest(row.id)
    ElMessage.success(t('common.updateSuccess'))
    load()
    loadPendingCount()
  } catch {
    /* cancelled */
  }
}

async function onReject(row: PermissionRequest) {
  try {
    const { value } = await ElMessageBox.prompt(t('permissionRequest.comment'), t('permissionRequest.reject'), { type: 'warning' })
    if (row.id) await rejectPermissionRequest(row.id, value || undefined)
    ElMessage.success(t('common.updateSuccess'))
    load()
    loadPendingCount()
  } catch {
    /* cancelled */
  }
}

function init() {
  load()
  loadPendingCount()
}

defineExpose({ init, pendingCount })
</script>