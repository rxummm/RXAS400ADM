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
    </div>

    <div class="table-wrapper">
      <el-table :data="filteredProfiles" v-loading="loading" size="small" border>
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
        <el-table-column :label="$t('common.operation')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'USER_PROFILE_VIEW'" type="primary" link size="small" @click="openDetail(row as UserProfileListRow)">
              {{ $t('common.detail') }}
            </el-button>
            <el-button v-has-perm="'USER_PROFILE_UPDATE'" type="warning" link size="small" @click="openEdit(row as UserProfileListRow)">
              {{ $t('common.edit') }}
            </el-button>
            <el-popconfirm
              :title="$t('userProfileManagement.deleteConfirm')"
              @confirm="handleDelete(row as UserProfileListRow)"
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
      <el-empty v-if="!loading && filteredProfiles.length === 0" :description="$t('common.noData')" />
    </div>

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

import { ref, computed, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  fetchUserProfileList,
  getUserProfileDetail,
  createUserProfile,
  updateUserProfile,
  deleteUserProfile,
  type UserProfileListRow,
  type UserProfileDetail
} from '@/api/userProfileManagement'

const { t } = useI18n()
const loading = ref(false)
const keyword = ref('')
const profiles = ref<UserProfileListRow[]>([])

const filteredProfiles = computed(() => {
  if (!keyword.value) return profiles.value
  const kw = keyword.value.toUpperCase()
  return profiles.value.filter(p => p.USER_NAME.toUpperCase().includes(kw))
})

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
  fetchUserProfileList()
    .then(data => { profiles.value = data })
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

function openEdit(row: UserProfileListRow) {
  editUserName.value = row.USER_NAME
  Object.assign(editForm, {
    description: row.TEXT_DESCRIPTION || '',
    groupProfile: row.GROUP_PROFILE || '*NONE',
    status: row.STATUS || '*ENABLED',
    initialMenu: '*SIGNOFF',
    specialAuthorities: [],
    newPassword: ''
  })
  editVisible.value = true
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

async function handleDelete(row: UserProfileListRow) {
  try {
    await deleteUserProfile(row.USER_NAME)
    ElMessage.success(t('userProfileManagement.deleteSuccess'))
    load()
  } catch {
    /* interceptor handles error */
  }
}

load()
</script>

<style scoped>
.step-content {
  margin-top: 24px;
  min-height: 200px;
}
</style>
