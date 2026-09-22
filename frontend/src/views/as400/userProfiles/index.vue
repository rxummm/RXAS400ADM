<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('userProfileManagement.searchPlaceholder')"
        clearable
        @keyup.enter="load"
      />
      <el-button type="primary" :loading="loading" @click="load">
        {{ $t('common.search') }}
      </el-button>
      <el-button v-has-perm="'USER_PROFILE_CREATE'" type="primary" @click="openCreate">
        {{ $t('userProfileManagement.create') }}
      </el-button>
      <el-button
        v-if="selectedRows.length > 0"
        v-has-perm="'USER_PROFILE_DELETE'"
        type="danger"
        plain
        @click="openBatchDelete"
      >
        {{ $t('userProfileManagement.batchDelete') }} ({{ selectedRows.length }})
      </el-button>
      <el-button
        v-has-perm="'USER_PROFILE_VIEW'"
        type="info"
        plain
        @click="openStats"
      >
        {{ $t('userProfileManagement.deleteStats') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table
          :data="profiles"
          size="small"
          border
          @selection-change="handleSelectionChange"
        >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="USER_NAME" :label="$t('userProfileManagement.userName')" min-width="140" />
        <el-table-column prop="STATUS" :label="$t('userProfileManagement.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.STATUS === '*ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.STATUS }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="GROUP_PROFILE" :label="$t('userProfileManagement.groupProfile')" width="120" />
        <el-table-column prop="TEXT_DESCRIPTION" :label="$t('userProfileManagement.description')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="LAST_USED_DATE" :label="$t('userProfileManagement.lastUsed')" width="120" />
        <el-table-column :label="$t('common.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'USER_PROFILE_VIEW'" type="primary" link size="small" @click="openDetail(row as UserProfileListRow)">
              {{ $t('common.detail') }}
            </el-button>
            <el-button v-has-perm="'USER_PROFILE_UPDATE'" type="warning" link size="small" @click="openEdit(row as UserProfileListRow)">
              {{ $t('common.edit') }}
            </el-button>
            <el-popconfirm
              :title="$t('userProfileManagement.deleteConfirm')"
              @confirm="openDeleteDialog(row as UserProfileListRow)"
            >
              <template #reference>
                <el-button v-has-perm="'USER_PROFILE_DELETE'" type="danger" link size="small">
                  {{ $t('common.delete') }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="load" @size-change="load" />
      <el-empty v-if="!loading && profiles.length === 0" :description="$t('common.noData')" />
    </div>

    <!-- 删除原因对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      :title="$t('userProfileManagement.deleteTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="deleteFormRef" :model="deleteForm" :rules="deleteRules" label-width="120px">
        <el-form-item :label="$t('userProfileManagement.deletionType')" prop="deletionType">
          <el-select v-model="deleteForm.deletionType" class="w-full" :placeholder="$t('userProfileManagement.deletionTypeRequired')">
            <el-option
              v-for="opt in deletionTypeOptions"
              :key="opt.value"
              :label="$t(opt.label)"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.deleteReason')" prop="deleteReason">
          <el-select
            v-model="deleteForm.deleteReason"
            class="w-full"
            :placeholder="$t('userProfileManagement.deleteReasonPlaceholder')"
            filterable
            allow-create
            default-first-option
          >
            <el-option
              v-for="opt in deleteReasonOptions"
              :key="opt.value"
              :label="$t(opt.key)"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.customReason')">
          <el-input
            v-model="deleteForm.customReason"
            :placeholder="$t('userProfileManagement.customReasonPlaceholder')"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="deleting" @click="confirmDelete">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量删除对话框 -->
    <el-dialog
      v-model="batchDeleteVisible"
      :title="$t('userProfileManagement.batchDeleteTitle')"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="hint mb16">
        {{ $t('userProfileManagement.batchDeleteHint', { count: selectedRows.length }) }}
      </div>
      <el-form ref="batchDeleteFormRef" :model="batchDeleteForm" :rules="batchDeleteRules" label-width="120px">
        <el-form-item :label="$t('userProfileManagement.deletionType')" prop="deletionType">
          <el-select v-model="batchDeleteForm.deletionType" class="w-full" :placeholder="$t('userProfileManagement.deletionTypeRequired')">
            <el-option
              v-for="opt in deletionTypeOptions"
              :key="opt.value"
              :label="$t(opt.label)"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.deleteReason')" prop="deleteReason">
          <el-select
            v-model="batchDeleteForm.deleteReason"
            class="w-full"
            :placeholder="$t('userProfileManagement.deleteReasonPlaceholder')"
            filterable
            allow-create
            default-first-option
          >
            <el-option
              v-for="opt in deleteReasonOptions"
              :key="opt.value"
              :label="$t(opt.key)"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDeleteVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="batchDeleting" @click="confirmBatchDelete">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 删除统计报表对话框 -->
    <el-dialog
      v-model="statsVisible"
      :title="$t('userProfileManagement.deleteStatsTitle')"
      width="900px"
    >
      <el-row :gutter="16" class="mb16">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value stat-total">{{ stats.totalDeletes }}</div>
            <div class="stat-label">{{ $t('userProfileManagement.totalDeletes') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value stat-manual">{{ stats.manualDeletes }}</div>
            <div class="stat-label">{{ $t('userProfileManagement.manualDeletes') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value stat-inactive">{{ stats.inactiveDeletes }}</div>
            <div class="stat-label">{{ $t('userProfileManagement.inactiveDeletes') }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value stat-resigned">{{ stats.resignedDeletes }}</div>
            <div class="stat-label">{{ $t('userProfileManagement.resignedDeletes') }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider>{{ $t('userProfileManagement.recentRecords') }}</el-divider>
      <el-table :data="stats.recentRecords" size="small" border v-loading="statsLoading">
        <el-table-column prop="userName" :label="$t('userProfileManagement.userName')" width="140" />
        <el-table-column prop="operator" :label="$t('userProfileManagement.operator')" width="120" />
        <el-table-column prop="deleteReason" :label="$t('userProfileManagement.deleteReason')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="deletionType" :label="$t('userProfileManagement.deletionType')" width="120">
          <template #default="{ row }">
            <el-tag :type="deletionTypeTagType(row.deletionType)" size="small">
              {{ deletionTypeLabel(row.deletionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('userProfileManagement.createdTime')" width="180" />
      </el-table>
      <template #footer>
        <el-button @click="statsVisible = false">{{ $t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- 创建向导弹窗 -->
    <el-dialog
      v-model="createVisible"
      :title="$t('userProfileManagement.createTitle')"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step :title="$t('userProfileManagement.step1Title')" />
        <el-step :title="$t('userProfileManagement.step2Title')" />
        <el-step :title="$t('userProfileManagement.step3Title')" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
          <el-form-item :label="$t('userProfileManagement.userName')" prop="userName">
            <el-input v-model="createForm.userName" :placeholder="$t('userProfileManagement.userNamePlaceholder')" maxlength="10" />
          </el-form-item>
          <el-form-item :label="$t('userProfileManagement.description')">
            <el-input v-model="createForm.description" :placeholder="$t('userProfileManagement.descriptionPlaceholder')" maxlength="50" />
          </el-form-item>
          <el-form-item :label="$t('userProfileManagement.groupProfile')">
            <el-select v-model="createForm.groupProfile" filterable class="w-full">
              <el-option label="*NONE" value="*NONE" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('userProfileManagement.password')" prop="password">
            <el-input v-model="createForm.password" type="password" show-password :placeholder="$t('userProfileManagement.passwordPlaceholder')" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 权限配置 -->
      <div v-show="currentStep === 1" class="step-content">
        <el-form :model="createForm" label-width="120px">
          <el-form-item :label="$t('userProfileManagement.initialMenu')">
            <el-select v-model="createForm.initialMenu" filterable class="w-full">
              <el-option label="*SIGNOFF" value="*SIGNOFF" />
              <el-option :label="$t('userProfileManagement.mainMenu')" value="MAINMENU" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('userProfileManagement.specialAuthorities')">
            <el-checkbox-group v-model="createForm.specialAuthorities">
              <el-checkbox label="*ALLOBJ" />
              <el-checkbox label="*SAVRST" />
              <el-checkbox label="*SERVICE" />
              <el-checkbox label="*SECADM" />
            </el-checkbox-group>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 3: 邮件通知 -->
      <div v-show="currentStep === 2" class="step-content">
        <el-form :model="createForm" label-width="140px">
          <el-form-item :label="$t('userProfileManagement.emailNotification')">
            <el-switch v-model="createForm.emailNotification" active-value="YES" inactive-value="NO" />
          </el-form-item>
          <el-form-item v-if="createForm.emailNotification === 'YES'" :label="$t('userProfileManagement.recipientEmail')" prop="recipientEmail">
            <el-input v-model="createForm.recipientEmail" type="email" :placeholder="$t('userProfileManagement.recipientEmailPlaceholder')" />
          </el-form-item>
        </el-form>

        <el-divider>{{ $t('userProfileManagement.preview') }}</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('userProfileManagement.userName')">{{ createForm.userName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('userProfileManagement.description')">{{ createForm.description }}</el-descriptions-item>
          <el-descriptions-item :label="$t('userProfileManagement.groupProfile')">{{ createForm.groupProfile }}</el-descriptions-item>
          <el-descriptions-item :label="$t('userProfileManagement.initialMenu')">{{ createForm.initialMenu }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button v-if="currentStep > 0" @click="currentStep--">{{ $t('common.previous') }}</el-button>
        <el-button v-if="currentStep < 2" type="primary" @click="nextStep">{{ $t('common.next') }}</el-button>
        <el-button v-if="currentStep === 2" type="primary" :loading="creating" @click="handleCreate">
          {{ $t('common.confirm') }}
        </el-button>
        <el-button @click="createVisible = false">{{ $t('common.cancel') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="$t('userProfileManagement.detailTitle')"
      width="600px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="$t('userProfileManagement.userName')">{{ detailData.userName }}</el-descriptions-item>
        <el-descriptions-item :label="$t('userProfileManagement.status')">
          <el-tag :type="detailData.status === '*ENABLED' ? 'success' : 'danger'" size="small">
            {{ detailData.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('userProfileManagement.groupProfile')">{{ detailData.groupProfile }}</el-descriptions-item>
        <el-descriptions-item :label="$t('userProfileManagement.description')">{{ detailData.description }}</el-descriptions-item>
        <el-descriptions-item :label="$t('userProfileManagement.initialMenu')">{{ detailData.initialMenu }}</el-descriptions-item>
        <el-descriptions-item :label="$t('userProfileManagement.lastUsed')">{{ detailData.lastUsedDate }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="$t('userProfileManagement.editTitle')"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item :label="$t('userProfileManagement.description')">
          <el-input v-model="editForm.description" maxlength="50" />
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.groupProfile')">
          <el-select v-model="editForm.groupProfile" filterable class="w-full">
            <el-option label="*NONE" value="*NONE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.status')">
          <el-select v-model="editForm.status" class="w-full">
            <el-option label="*ENABLED" value="*ENABLED" />
            <el-option label="*DISABLED" value="*DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.initialMenu')">
          <el-select v-model="editForm.initialMenu" filterable class="w-full">
            <el-option label="*SIGNOFF" value="*SIGNOFF" />
            <el-option :label="$t('userProfileManagement.mainMenu')" value="MAINMENU" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.specialAuthorities')">
          <el-checkbox-group v-model="editForm.specialAuthorities">
            <el-checkbox label="*ALLOBJ" />
            <el-checkbox label="*SAVRST" />
            <el-checkbox label="*SERVICE" />
            <el-checkbox label="*SECADM" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="$t('userProfileManagement.newPassword')">
          <el-input v-model="editForm.newPassword" type="password" show-password :placeholder="$t('userProfileManagement.newPasswordPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="editing" @click="handleEdit">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'UserProfileManagement' })

import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import {
  fetchUserProfileList,
  getUserProfileDetail,
  createUserProfile,
  updateUserProfile,
  deleteUserProfile,
  batchDeleteUserProfiles,
  getDeleteStats,
  type UserProfileListRow,
  type UserProfileDetail,
  type UserProfileDeleteStats
} from '@/api/userProfileManagement'

const { t } = useI18n()
const loading = ref(false)
const keyword = ref('')
const profiles = ref<UserProfileListRow[]>([])
const current = ref(1)
const size = ref(20)
const total = ref(0)

// 批量选择
const selectedRows = ref<UserProfileListRow[]>([])

function handleSelectionChange(rows: UserProfileListRow[]) {
  selectedRows.value = rows
}

// 删除原因对话框
const deleteDialogVisible = ref(false)
const deleting = ref(false)
const deleteFormRef = ref<FormInstance>()
const deleteUserName = ref('')
const deleteForm = reactive({
  deletionType: '',
  deleteReason: '',
  customReason: ''
})
const deleteRules: FormRules = {
  deletionType: [{ required: true, message: t('userProfileManagement.deletionTypeRequired'), trigger: 'change' }],
  deleteReason: [{ required: true, message: t('userProfileManagement.deleteReasonRequired'), trigger: 'change' }]
}

const deletionTypeOptions = [
  { value: 'MANUAL', label: 'userProfileManagement.deletionTypeManual' },
  { value: 'INACTIVE_90D', label: 'userProfileManagement.deletionTypeInactive' },
  { value: 'RESIGNED', label: 'userProfileManagement.deletionTypeResigned' }
]

const deleteReasonOptions = [
  { key: 'userProfileManagement.deleteReasonManual', value: '管理员手动删除' },
  { key: 'userProfileManagement.deleteReasonInactive', value: '超过90天未登录' },
  { key: 'userProfileManagement.deleteReasonResigned', value: '用户已离职' },
  { key: 'userProfileManagement.deleteReasonSecurity', value: '安全策略清理' },
]

function openDeleteDialog(row: UserProfileListRow) {
  deleteUserName.value = row.USER_NAME
  deleteForm.deletionType = ''
  deleteForm.deleteReason = ''
  deleteForm.customReason = ''
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  deleteFormRef.value?.validate(async valid => {
    if (!valid) return
    deleting.value = true
    try {
      const reason = deleteForm.customReason
        ? `${deleteForm.deleteReason} - ${deleteForm.customReason}`
        : deleteForm.deleteReason
      await deleteUserProfile(deleteUserName.value, {
        deleteReason: reason,
        deletionType: deleteForm.deletionType
      })
      ElMessage.success(t('userProfileManagement.deleteSuccess'))
      deleteDialogVisible.value = false
      load()
    } catch {
      /* interceptor handles error */
    } finally {
      deleting.value = false
    }
  })
}

// 批量删除对话框
const batchDeleteVisible = ref(false)
const batchDeleting = ref(false)
const batchDeleteFormRef = ref<FormInstance>()
const batchDeleteForm = reactive({
  deletionType: '',
  deleteReason: ''
})
const batchDeleteRules: FormRules = {
  deletionType: [{ required: true, message: t('userProfileManagement.deletionTypeRequired'), trigger: 'change' }],
  deleteReason: [{ required: true, message: t('userProfileManagement.deleteReasonRequired'), trigger: 'change' }]
}

function openBatchDelete() {
  batchDeleteForm.deletionType = ''
  batchDeleteForm.deleteReason = ''
  batchDeleteVisible.value = true
}

async function confirmBatchDelete() {
  batchDeleteFormRef.value?.validate(async valid => {
    if (!valid) return
    batchDeleting.value = true
    try {
      const result = await batchDeleteUserProfiles({
        userNames: selectedRows.value.map(r => r.USER_NAME),
        deleteReason: batchDeleteForm.deleteReason,
        deletionType: batchDeleteForm.deletionType
      })
      ElMessage.success(t('userProfileManagement.batchDeleteSuccess', { count: result.successCount }))
      batchDeleteVisible.value = false
      selectedRows.value = []
      load()
    } catch {
      /* interceptor handles error */
    } finally {
      batchDeleting.value = false
    }
  })
}

// 删除统计对话框
const statsVisible = ref(false)
const statsLoading = ref(false)
const stats = ref<UserProfileDeleteStats>({
  totalDeletes: 0,
  manualDeletes: 0,
  inactiveDeletes: 0,
  resignedDeletes: 0,
  recentRecords: []
})

function openStats() {
  statsVisible.value = true
  loadStats()
}

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await getDeleteStats()
  } catch {
    /* ignore */
  } finally {
    statsLoading.value = false
  }
}

function deletionTypeTagType(type: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (type) {
    case 'MANUAL': return 'warning'
    case 'INACTIVE_90D': return 'danger'
    case 'RESIGNED': return 'danger'
    default: return 'info'
  }
}

function deletionTypeLabel(type: string): string {
  switch (type) {
    case 'MANUAL': return t('userProfileManagement.deletionTypeManual')
    case 'INACTIVE_90D': return t('userProfileManagement.deletionTypeInactive')
    case 'RESIGNED': return t('userProfileManagement.deletionTypeResigned')
    default: return type
  }
}

// 创建向导
const createVisible = ref(false)
const currentStep = ref(0)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  userName: '',
  password: '',
  description: '',
  groupProfile: '*NONE',
  initialMenu: '*SIGNOFF',
  specialAuthorities: [] as string[],
  emailNotification: 'NO',
  recipientEmail: ''
})
const createRules: FormRules = {
  userName: [{ required: true, message: t('userProfileManagement.userNameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('userProfileManagement.passwordRequired'), trigger: 'blur' }]
}

// 详情
const detailVisible = ref(false)
const detailData = ref<UserProfileDetail>({
  userName: '',
  status: '',
  groupProfile: '',
  description: '',
  initialMenu: '',
  specialAuthorities: [],
  lastUsedDate: '',
  passwordExpireDate: ''
})

// 编辑
const editVisible = ref(false)
const editing = ref(false)
const editFormRef = ref<FormInstance>()
const editUserName = ref('')
const editForm = reactive({
  description: '',
  groupProfile: '*NONE',
  status: '*ENABLED',
  initialMenu: '*SIGNOFF',
  specialAuthorities: [] as string[],
  newPassword: ''
})
const editRules: FormRules = {}

function load() {
  loading.value = true
  const params: Record<string, unknown> = { current: current.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  fetchUserProfileList(params)
    .then(data => { profiles.value = data.records; total.value = data.total })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function openCreate() {
  currentStep.value = 0
  Object.assign(createForm, {
    userName: '',
    password: '',
    description: '',
    groupProfile: '*NONE',
    initialMenu: '*SIGNOFF',
    specialAuthorities: [],
    emailNotification: 'NO',
    recipientEmail: ''
  })
  createVisible.value = true
}

function nextStep() {
  if (currentStep.value === 0) {
    createFormRef.value?.validate(valid => {
      if (valid) currentStep.value++
    })
  } else {
    currentStep.value++
  }
}

async function handleCreate() {
  creating.value = true
  try {
    await createUserProfile(createForm)
    ElMessage.success(t('userProfileManagement.createSuccess'))
    createVisible.value = false
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    creating.value = false
  }
}

async function openDetail(row: UserProfileListRow) {
  try {
    detailData.value = await getUserProfileDetail(row.USER_NAME)
    detailVisible.value = true
  } catch {
    /* interceptor handles error */
  }
}

async function openEdit(row: UserProfileListRow) {
  try {
    const detail = await getUserProfileDetail(row.USER_NAME)
    editUserName.value = row.USER_NAME
    Object.assign(editForm, {
      description: detail.description || '',
      groupProfile: detail.groupProfile || '*NONE',
      status: detail.status || '*ENABLED',
      initialMenu: detail.initialMenu || '*SIGNOFF',
      specialAuthorities: detail.specialAuthorities || [],
      newPassword: ''
    })
    editVisible.value = true
  } catch {
    /* interceptor handles error */
  }
}

async function handleEdit() {
  editing.value = true
  try {
    await updateUserProfile(editUserName.value, editForm)
    ElMessage.success(t('userProfileManagement.editSuccess'))
    editVisible.value = false
    load()
  } catch {
    /* interceptor handles error */
  } finally {
    editing.value = false
  }
}

load()
</script>

<style scoped>
.step-content {
  margin-top: 24px;
  min-height: 200px;
}

/* 统计卡片颜色修饰符（基础样式由 common.css .stat-card/.stat-value/.stat-label 提供） */
.stat-total { color: var(--el-color-primary); }
.stat-manual { color: var(--el-color-warning); }
.stat-inactive { color: var(--el-color-danger); }
.stat-resigned { color: var(--el-color-danger); }
</style>