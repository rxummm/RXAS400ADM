<template>
  <div class="page-container page-container--fit">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-input v-model="query.poNo" class="w-150" :placeholder="$t('procurement.po.poNo')" clearable @keyup.enter="search" />
      <el-input v-model="query.vendorName" class="w-150" :placeholder="$t('procurement.po.vendorName')" clearable @keyup.enter="search" />
      <el-button type="primary" :loading="loading" @click="search">{{ $t('common.search') }}</el-button>
      <el-button @click="reset">{{ $t('common.reset') }}</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <el-table :data="orders" v-loading="loading" size="small" border>
        <el-table-column prop="poNo" :label="$t('procurement.po.poNo')" min-width="160" />
        <el-table-column prop="vendorName" :label="$t('procurement.po.vendorName')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="$t('procurement.po.totalAmount')" width="130" align="right">
          <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column :label="$t('procurement.po.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ $t(row.statusKey) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('procurement.po.approvalLevel')" width="100" align="center">
          <template #default="{ row }">L{{ row.approvalLevel }}</template>
        </el-table-column>
        <el-table-column :label="$t('procurement.po.approvalStatus')" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.approvalStatus === 'APPROVED'" size="small" type="success">{{ $t('procurement.approval.actionApproved') }}</el-tag>
            <el-tag v-else-if="row.approvalStatus === 'REJECTED'" size="small" type="danger">{{ $t('procurement.approval.actionRejected') }}</el-tag>
            <el-tag v-else size="small" type="warning">{{ $t('procurement.approval.actionPending') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" :label="$t('procurement.po.createdBy')" width="100" />
        <el-table-column prop="createdTime" :label="$t('procurement.po.createdTime')" width="170">
          <template #default="{ row }">{{ row.createdTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.actions')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as PurchaseOrderVO)">{{ $t('common.detail') }}</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL'" type="success" size="small" @click="confirmApprovalAction(row as PurchaseOrderVO, 'APPROVED')">{{ $t('procurement.approval.approve') }}</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL'" type="warning" size="small" @click="confirmApprovalAction(row as PurchaseOrderVO, 'RETURNED')">{{ $t('procurement.approval.return') }}</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVAL'" type="danger" size="small" @click="confirmApprovalAction(row as PurchaseOrderVO, 'REJECTED')">{{ $t('procurement.approval.reject') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-if="total > 0" :total="total" v-model:current="current" v-model:size="size" @change="search" @size-change="search" />
      <el-empty v-if="searched && !loading && orders.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalVisible" :title="$t('procurement.approval.approve')" width="500px" :close-on-click-modal="false">
      <el-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" label-width="80px" size="small">
        <el-form-item :label="$t('procurement.po.poNo')">
          <el-tag>{{ approvalForm.poNo }}</el-tag>
        </el-form-item>
        <el-form-item :label="$t('procurement.approval.action')">
          <el-tag :type="actionTag(approvalForm.action)">{{ $t('procurement.approval.action' + approvalForm.action) }}</el-tag>
        </el-form-item>
        <el-form-item :label="$t('procurement.approval.comment')" prop="comment">
          <el-input v-model="approvalForm.comment" type="textarea" :rows="3" :placeholder="$t('procurement.approval.commentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="confirmApproval">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProcurementApproval' })

import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  pagePurchaseOrders,
  getPurchaseOrderDetail,
  handleApproval,
  type PurchaseOrderVO,
  type PurchaseOrderQuery,
} from '@/api/procurement'
import AppPagination from '@/components/AppPagination.vue'
import { formatMoney } from '@/utils/format'
import type { FormInstance } from 'element-plus'

const { t } = useI18n()

const loading = ref(false)
const searched = ref(false)
const orders = ref<PurchaseOrderVO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const query = reactive<PurchaseOrderQuery>({
  poNo: '', vendorName: '', status: 'PENDING_APPROVAL',
})

function search() {
  loading.value = true
  searched.value = true
  pagePurchaseOrders({ current: current.value, size: size.value, ...query })
    .then(data => {
      orders.value = data.records
      total.value = data.total
    })
    .finally(() => { loading.value = false })
}

function reset() {
  query.poNo = ''
  query.vendorName = ''
  query.status = 'PENDING_APPROVAL'
  current.value = 1
  search()
}

// 审批对话框
const approvalVisible = ref(false)
const saving = ref(false)
const approvalFormRef = ref<FormInstance>()
const approvalForm = reactive({
  poId: 0,
  poNo: '',
  action: '' as 'APPROVED' | 'REJECTED' | 'RETURNED',
  comment: '',
})

const approvalRules = {
  comment: [{ max: 500, message: t('common.validation.maxLength', { max: 500 }), trigger: 'blur' }],
}

function openDetail(row: PurchaseOrderVO) {
  getPurchaseOrderDetail(row.id).then(d => {
    // For detail view, could open a drawer or just show info
    ElMessage.info(`${t('procurement.po.poNo')}: ${d.poNo}, ${t('procurement.po.status')}: ${t(d.statusKey)}`)
  })
}

function confirmApprovalAction(row: PurchaseOrderVO, action: 'APPROVED' | 'REJECTED' | 'RETURNED') {
  approvalForm.poId = row.id
  approvalForm.poNo = row.poNo
  approvalForm.action = action
  approvalForm.comment = ''
  approvalVisible.value = true
}

async function confirmApproval() {
  const valid = await approvalFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await handleApproval({ poId: approvalForm.poId, action: approvalForm.action, comment: approvalForm.comment || undefined })
    ElMessage.success(t('common.updateSuccess'))
    approvalVisible.value = false
    search()
  } finally {
    saving.value = false
  }
}

// 工具
const fmtMoney = (v: number | null | undefined) => (v != null ? formatMoney(v) : '—')

function statusTag(status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    DRAFT: 'info', PENDING_APPROVAL: 'warning', APPROVED: 'success',
    SHIPPED: 'primary', RECEIVED: 'success', PAID: 'success', CANCELLED: 'danger',
  }
  return map[status] || 'info'
}

function actionTag(action: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    APPROVED: 'success', REJECTED: 'danger', RETURNED: 'warning',
  }
  return map[action] || 'info'
}

onMounted(async () => {
  try {
    await search()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : String(e))
  }
})
</script>
