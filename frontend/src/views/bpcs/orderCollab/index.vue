<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="query.orderNo" class="w-160" :placeholder="$t('bpcs.collab.orderNo')" clearable @keyup.enter="load" />
      <el-input v-model="query.customerCode" class="w-120" :placeholder="$t('bpcs.collab.customerCode')" clearable @keyup.enter="load" />
      <el-select v-model="query.status" class="w-120" :placeholder="$t('bpcs.collab.status')" clearable>
        <el-option :label="$t('bpcs.collab.pending')" value="PENDING" />
        <el-option :label="$t('bpcs.collab.inProgress')" value="IN_PROGRESS" />
        <el-option :label="$t('bpcs.collab.completed')" value="COMPLETED" />
        <el-option :label="$t('bpcs.collab.cancelled')" value="CANCELLED" />
      </el-select>
      <el-select v-model="query.priority" class="w-100" :placeholder="$t('bpcs.collab.priority')" clearable>
        <el-option :label="$t('bpcs.collab.low')" value="LOW" />
        <el-option :label="$t('bpcs.collab.normal')" value="NORMAL" />
        <el-option :label="$t('bpcs.collab.high')" value="HIGH" />
        <el-option :label="$t('bpcs.collab.urgent')" value="URGENT" />
      </el-select>
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" @click="openCreateDialog">{{ $t('bpcs.collab.addCollab') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orderNo" :label="$t('bpcs.collab.orderNo')" width="120" />
        <el-table-column prop="customerCode" :label="$t('bpcs.collab.customerCode')" width="100" />
        <el-table-column prop="customerName" :label="$t('bpcs.collab.customerName')" min-width="120" />
        <el-table-column prop="status" :label="$t('bpcs.collab.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType((row as OrderCollaborationVO).status)" size="small">
              {{ $t('bpcs.collab.' + statusKey((row as OrderCollaborationVO).status)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" :label="$t('bpcs.collab.priority')" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType((row as OrderCollaborationVO).priority)" size="small">
              {{ $t('bpcs.collab.' + (row as OrderCollaborationVO).priority.toLowerCase()) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedTo" :label="$t('bpcs.collab.assignedTo')" width="100" />
        <el-table-column prop="dueDate" :label="$t('bpcs.collab.dueDate')" width="110" />
        <el-table-column :label="$t('common.actions')" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openNotifyDialog(row as OrderCollaborationVO)">{{ $t('bpcs.collab.sendNotification') }}</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleStatusChange(row as OrderCollaborationVO, cmd)">
              <el-button size="small" link>{{ $t('bpcs.collab.status') }}</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="PENDING">{{ $t('bpcs.collab.pending') }}</el-dropdown-item>
                  <el-dropdown-item command="IN_PROGRESS">{{ $t('bpcs.collab.inProgress') }}</el-dropdown-item>
                  <el-dropdown-item command="COMPLETED">{{ $t('bpcs.collab.completed') }}</el-dropdown-item>
                  <el-dropdown-item command="CANCELLED">{{ $t('bpcs.collab.cancelled') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" link type="danger" @click="handleDelete(row as OrderCollaborationVO)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination :total="total" v-model:current="page.current" v-model:size="page.size" @change="load" @size-change="load" />
    </div>

    <!-- ==================== 创建协同弹窗 ==================== -->
    <el-dialog v-model="createDialogVisible" :title="$t('bpcs.collab.addCollab')" width="520px" destroy-on-close>
      <el-form :model="createForm" label-width="100" ref="createFormRef" :rules="createRules">
        <el-form-item :label="$t('bpcs.collab.orderNo')" prop="orderNo">
          <el-input v-model="createForm.orderNo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.customerCode')">
          <el-input v-model="createForm.customerCode" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.customerName')">
          <el-input v-model="createForm.customerName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.priority')">
          <el-select v-model="createForm.priority" class="w-full">
            <el-option :label="$t('bpcs.collab.low')" value="LOW" />
            <el-option :label="$t('bpcs.collab.normal')" value="NORMAL" />
            <el-option :label="$t('bpcs.collab.high')" value="HIGH" />
            <el-option :label="$t('bpcs.collab.urgent')" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.assignedTo')">
          <el-input v-model="createForm.assignedTo" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.dueDate')">
          <el-date-picker v-model="createForm.dueDate" type="date" value-format="YYYY-MM-DD" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.notes')">
          <el-input v-model="createForm.notes" type="textarea" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="createSaving" @click="handleCreate">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 发送通知弹窗 ==================== -->
    <el-dialog v-model="notifyDialogVisible" :title="$t('bpcs.collab.sendNotification')" width="480px" destroy-on-close>
      <el-form :model="notifyForm" label-width="80">
        <el-form-item :label="$t('bpcs.collab.sender')">
          <el-input v-model="notifyForm.sender" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.recipient')">
          <el-input v-model="notifyForm.recipient" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.message')">
          <el-input v-model="notifyForm.message" type="textarea" :rows="3" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('bpcs.collab.channel')">
          <el-select v-model="notifyForm.channel" class="w-full">
            <el-option :label="$t('bpcs.collab.system')" value="SYSTEM" />
            <el-option :label="$t('bpcs.collab.email')" value="EMAIL" />
            <el-option :label="$t('bpcs.collab.sms')" value="SMS" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notifyDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="notifySaving" @click="handleSendNotify">{{ $t('common.send') }}</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 通知列表抽屉 ==================== -->
    <el-drawer v-model="notifDrawerVisible" :title="$t('bpcs.collab.notifications')" size="400px">
      <el-table :data="notifications" size="small" border>
        <el-table-column prop="sender" :label="$t('bpcs.collab.sender')" width="80" />
        <el-table-column prop="recipient" :label="$t('bpcs.collab.recipient')" width="80" />
        <el-table-column prop="message" :label="$t('bpcs.collab.message')" min-width="120" />
        <el-table-column prop="channel" :label="$t('bpcs.collab.channel')" width="70" />
        <el-table-column prop="isRead" :label="$t('bpcs.collab.isRead')" width="60" align="center">
          <template #default="{ row }">
            <el-tag :type="(row as CollaborationNotificationVO).isRead ? 'success' : 'warning'" size="small">
              {{ (row as CollaborationNotificationVO).isRead ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
import {
  listCollaborations, createCollaboration, updateCollabStatus, deleteCollaboration,
  sendCollabNotification, listCollabNotifications,
  type OrderCollaborationVO, type CollaborationNotificationVO
} from '@/api/bpcs'
import AppPagination from '@/components/AppPagination.vue'

defineOptions({ name: 'BpcsOrderCollab' })

const loading = ref(false)
const rows = ref<OrderCollaborationVO[]>([])
const total = ref(0)
const page = reactive({ current: 1, size: 20 })
const query = reactive({ orderNo: '', customerCode: '', status: '', priority: '' })

const createDialogVisible = ref(false)
const createSaving = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ orderNo: '', customerCode: '', customerName: '', priority: 'NORMAL', assignedTo: '', dueDate: '', notes: '' })
const createRules = reactive<FormRules>({
  orderNo: [{ required: true, message: t('common.validation.notBlank'), trigger: 'blur' }],
})

const notifyDialogVisible = ref(false)
const notifySaving = ref(false)
const notifyForm = reactive({ collaborationId: 0, sender: '', recipient: '', message: '', channel: 'SYSTEM' })

const notifDrawerVisible = ref(false)
const notifications = ref<CollaborationNotificationVO[]>([])

function statusTagType(status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' | undefined {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return undefined
}

function statusKey(status: string) {
  if (status === 'IN_PROGRESS') return 'inProgress'
  return status.toLowerCase()
}

function priorityTagType(priority: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' | undefined {
  if (priority === 'URGENT') return 'danger'
  if (priority === 'HIGH') return 'warning'
  if (priority === 'LOW') return 'info'
  return undefined
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = { current: page.current, size: page.size }
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.customerCode) params.customerCode = query.customerCode
    if (query.status) params.status = query.status
    if (query.priority) params.priority = query.priority
    const res = await listCollaborations(params)
    rows.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function resetQuery() {
  query.orderNo = ''
  query.customerCode = ''
  query.status = ''
  query.priority = ''
  page.current = 1
  load()
}

function openCreateDialog() {
  Object.assign(createForm, { orderNo: '', customerCode: '', customerName: '', priority: 'NORMAL', assignedTo: '', dueDate: '', notes: '' })
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate()
  createSaving.value = true
  try {
    await createCollaboration(createForm)
    createDialogVisible.value = false
    ElMessage.success(t('common.success'))
    load()
  } finally { createSaving.value = false }
}

async function handleStatusChange(row: OrderCollaborationVO, status: string) {
  await updateCollabStatus(row.id, status)
  ElMessage.success(t('common.success'))
  load()
}

async function handleDelete(row: OrderCollaborationVO) {
  await ElMessageBox.confirm(t('common.confirm.deleteCollaboration', { name: row.orderNo }), '', { type: 'warning' })
  await deleteCollaboration(row.id)
  ElMessage.success(t('common.success'))
  load()
}

function openNotifyDialog(row: OrderCollaborationVO) {
  Object.assign(notifyForm, { collaborationId: row.id, sender: '', recipient: row.assignedTo || '', message: '', channel: 'SYSTEM' })
  notifyDialogVisible.value = true
}

async function handleSendNotify() {
  notifySaving.value = true
  try {
    await sendCollabNotification(notifyForm)
    notifyDialogVisible.value = false
    ElMessage.success(t('common.success'))
  } finally { notifySaving.value = false }
}

onMounted(load)
</script>
