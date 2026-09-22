<template>
  <div class="page-container page-container--fit">
    <!-- 统计卡片 -->
    <div class="approval-stats">
      <el-card class="stat-card" shadow="never">
        <div class="stat-value text-danger">{{ pendingCount }}</div>
        <div class="stat-label">{{ $t('approval.pending') }}</div>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <div class="stat-value text-success">{{ approvedCount }}</div>
        <div class="stat-label">{{ $t('approval.approved') }}</div>
      </el-card>
      <el-card class="stat-card" shadow="never">
        <div class="stat-value text-warning">{{ rejectedCount }}</div>
        <div class="stat-label">{{ $t('approval.rejected') }}</div>
      </el-card>
    </div>

    <!-- 筛选 -->
    <div class="search-bar">
      <el-select v-model="filterStatus" class="w-150" :placeholder="$t('common.statusFilter')" clearable>
        <el-option :label="$t('approval.pending')" value="PENDING" />
        <el-option :label="$t('approval.approved')" value="APPROVED" />
        <el-option :label="$t('approval.rejected')" value="REJECTED" />
      </el-select>
      <el-select v-model="filterType" class="w-150" :placeholder="$t('common.typeFilter')" clearable>
        <el-option :label="$t('approval.po')" value="PO" />
        <el-option :label="$t('approval.invoice')" value="INVOICE" />
        <el-option :label="$t('approval.operation')" value="OPERATION" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-wrapper">
      <el-table :data="approvals" v-loading="loading" size="small" border>
        <el-table-column prop="title" :label="$t('approval.title')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targetType" :label="$t('approval.targetType')" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ targetTypeLabel(row.targetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('approval.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approverName" :label="$t('approval.approver')" width="100" />
        <el-table-column prop="createdBy" :label="$t('approval.creator')" width="100" />
        <el-table-column prop="content" :label="$t('approval.content')" show-overflow-tooltip />
        <el-table-column prop="createdTime" :label="$t('common.createdTime')" width="170">
          <template #default="{ row }">{{ formatDate(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click="openAction(row as ApprovalNotification)">
              {{ $t('approval.action') }}
            </el-button>
            <el-button link type="info" size="small" @click="viewDetail(row as ApprovalNotification)">
              {{ $t('common.detail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      <el-empty v-if="searched && !loading && approvals.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 审批操作对话框 -->
    <el-dialog v-model="actionVisible" :title="$t('approval.action')" width="480px" :close-on-click-modal="false">
      <el-form ref="actionFormRef" :model="actionForm" :rules="actionRules" label-width="80px" size="small">
        <el-form-item :label="$t('approval.target')">
          <span>{{ currentApproval?.title }}</span>
        </el-form-item>
        <el-form-item :label="$t('approval.action')" prop="action">
          <el-radio-group v-model="actionForm.action">
            <el-radio value="APPROVED">{{ $t('approval.approve') }}</el-radio>
            <el-radio value="REJECTED">{{ $t('approval.reject') }}</el-radio>
            <el-radio value="RETURNED">{{ $t('approval.return') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('approval.comment')" prop="comment">
          <el-input v-model="actionForm.comment" type="textarea" :rows="3" :placeholder="$t('approval.commentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="confirmAction">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" :title="$t('approval.detail')" width="480px">
      <el-descriptions v-if="currentApproval" :column="1" size="small" border>
        <el-descriptions-item :label="$t('approval.title')">{{ currentApproval.title }}</el-descriptions-item>
        <el-descriptions-item :label="$t('approval.targetType')">{{ targetTypeLabel(currentApproval.targetType) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('approval.status')">
          <el-tag size="small" :type="statusTag(currentApproval.status)">{{ statusLabel(currentApproval.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('approval.approver')">{{ currentApproval.approverName }}</el-descriptions-item>
        <el-descriptions-item :label="$t('approval.creator')">{{ currentApproval.createdBy }}</el-descriptions-item>
        <el-descriptions-item :label="$t('common.createdTime')">{{ formatDate(currentApproval.createdTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentApproval.comment" :label="$t('approval.comment')">{{ currentApproval.comment }}</el-descriptions-item>
        <el-descriptions-item v-if="currentApproval.content" :label="$t('approval.content')">{{ currentApproval.content }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ApprovalCenter' })

import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  listPendingApprovals,
  listAllApprovals,
  approveNotification,
  countPendingApprovals,
  getApprovalStats,
  type ApprovalNotification,
  type ApprovalActionDTO,
} from '@/api/approval'
import AppPagination from '@/components/AppPagination.vue'
import { formatDate } from '@/utils/format'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()

// 状态
const loading = ref(false)
const searched = ref(false)
const approvals = ref<ApprovalNotification[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const filterStatus = ref('')
const filterType = ref('')

// 统计
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)

// 审批操作
const actionVisible = ref(false)
const saving = ref(false)
const actionFormRef = ref<FormInstance>()
const currentApproval = ref<ApprovalNotification | null>(null)
const actionForm = reactive<ApprovalActionDTO & { action: string }>({
  notificationId: 0,
  action: 'APPROVED',
  comment: '',
})
const actionRules: FormRules = {
  action: [{ required: true, message: t('common.validation.notNull'), trigger: 'change' }],
}

// 详情
const detailVisible = ref(false)

function search() {
  loading.value = true
  searched.value = true
  const promise = filterStatus.value || filterType.value
    ? listAllApprovals(current.value, size.value, filterStatus.value || undefined, filterType.value || undefined)
    : listPendingApprovals(current.value, size.value)
  promise.then(data => {
    approvals.value = data.records
    total.value = data.total
  }).finally(() => { loading.value = false })
}

function reset() {
  filterStatus.value = ''
  filterType.value = ''
  current.value = 1
  search()
}

function openAction(row: ApprovalNotification) {
  currentApproval.value = row
  actionForm.notificationId = row.id
  actionForm.action = 'APPROVED'
  actionForm.comment = ''
  actionVisible.value = true
}

async function confirmAction() {
  const valid = await actionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await approveNotification(actionForm.notificationId, {
      notificationId: actionForm.notificationId,
      action: actionForm.action as 'APPROVED' | 'REJECTED' | 'RETURNED',
      comment: actionForm.comment || undefined,
    })
    ElMessage.success(t('approval.actionSuccess'))
    actionVisible.value = false
    search()
    loadStats()
  } finally {
    saving.value = false
  }
}

function viewDetail(row: ApprovalNotification) {
  currentApproval.value = row
  detailVisible.value = true
}

// 工具
function targetTypeLabel(type: string): string {
  const map: Record<string, string> = {
    PO: t('approval.targetTypePO'),
    INVOICE: t('approval.targetTypeInvoice'),
    OPERATION: t('approval.targetTypeOperation'),
  }
  return map[type] || type
}

function statusTag(status: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: t('approval.pending'),
    APPROVED: t('approval.approved'),
    REJECTED: t('approval.rejected'),
    CANCELLED: t('approval.cancelled'),
  }
  return map[status] || status
}

async function loadStats() {
  try {
    const [pending, [approved, rejected]] = await Promise.all([
      countPendingApprovals(),
      getApprovalStats(),
    ])
    pendingCount.value = pending
    approvedCount.value = approved
    rejectedCount.value = rejected
  } catch { /* ignore */ }
}

onMounted(async () => {
  try {
    await search()
    await loadStats()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
})
</script>

<style scoped>
.approval-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  flex: 1;
  text-align: center;
  padding: 16px 0;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
}
.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 4px;
}
</style>
