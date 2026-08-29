<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" clearable :placeholder="$t('common.search')" @keyup.enter="load" />
      <el-button type="primary" @click="load">{{ $t('common.search') }}</el-button>
      <el-button @click="openCreate">{{ $t('emailGroups.add') }}</el-button>
    </div>
    <div class="table-wrapper">
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="groupName" :label="$t('emailGroups.groupName')" min-width="150" />
        <el-table-column prop="description" :label="$t('common.description')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="memberCount" :label="$t('emailGroups.memberCount')" width="100" align="center" />
        <el-table-column prop="createdTime" :label="$t('common.createdTime')" width="170" />
        <el-table-column :label="$t('common.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openMembers(row as EmailGroup)">{{ $t('emailGroups.members') }}</el-button>
            <el-button size="small" link @click="openEdit(row as EmailGroup)">{{ $t('common.edit') }}</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(row as EmailGroup)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="load" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('emailGroups.edit') : $t('emailGroups.add')" width="480px">
      <el-form :model="form" label-width="100px" ref="formRef" :rules="rules">
        <el-form-item :label="$t('emailGroups.groupName')" prop="groupName">
          <el-input v-model="form.groupName" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('common.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理抽屉 -->
    <el-drawer v-model="drawerVisible" :title="currentGroup?.groupName + ' - ' + $t('emailGroups.members')" size="480px">
      <div class="mb16">
        <el-input v-model="newEmail" :placeholder="$t('emailGroups.emailHint')" class="flex-1 mr4" />
        <el-button type="primary" @click="handleAddMember" :loading="memberLoading">{{ $t('common.add') }}</el-button>
      </div>
      <el-table :data="members" size="small" border>
        <el-table-column prop="email" :label="$t('emailGroups.email')" min-width="200" />
        <el-table-column :label="$t('common.operation')" width="80">
          <template #default="{ row }">
            <el-button size="small" link type="danger" @click="handleRemoveMember(row as EmailRecipient)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listEmailGroups, createEmailGroup, updateEmailGroup, deleteEmailGroup,
  listGroupMembers, addGroupMember, removeGroupMember
} from '@/api/email'
import type { EmailGroup, EmailRecipient } from '@/api/email'

defineOptions({ name: 'EmailGroups' })

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const memberLoading = ref(false)
const rows = ref<EmailGroup[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref({ groupName: '', description: '' })
const rules: FormRules = {
  groupName: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
}

const drawerVisible = ref(false)
const currentGroup = ref<EmailGroup | null>(null)
const members = ref<EmailRecipient[]>([])
const newEmail = ref('')

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const res = await listEmailGroups({ current: current.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = { groupName: '', description: '' }
  dialogVisible.value = true
}

function openEdit(row: EmailGroup) {
  isEdit.value = true
  editId.value = row.id
  form.value = { groupName: row.groupName, description: row.description || '' }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && editId.value != null) {
      await updateEmailGroup(editId.value, form.value)
    } else {
      await createEmailGroup(form.value)
    }
    ElMessage.success(t('common.operationSuccess'))
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: EmailGroup) {
  await ElMessageBox.confirm(t('emailGroups.deleteConfirm', { name: row.groupName }), t('common.confirm'), { type: 'warning' })
  await deleteEmailGroup(row.id)
  ElMessage.success(t('common.operationSuccess'))
  load()
}

async function openMembers(row: EmailGroup) {
  currentGroup.value = row
  drawerVisible.value = true
  members.value = await listGroupMembers(row.id)
}

async function handleAddMember() {
  if (!newEmail.value || !currentGroup.value) return
  memberLoading.value = true
  try {
    await addGroupMember(currentGroup.value.id, { email: newEmail.value })
    ElMessage.success(t('common.operationSuccess'))
    newEmail.value = ''
    members.value = await listGroupMembers(currentGroup.value.id)
    load()
  } finally {
    memberLoading.value = false
  }
}

async function handleRemoveMember(row: EmailRecipient) {
  if (!currentGroup.value) return
  await removeGroupMember(currentGroup.value.id, row.id)
  ElMessage.success(t('common.operationSuccess'))
  members.value = await listGroupMembers(currentGroup.value.id)
  load()
}
</script>