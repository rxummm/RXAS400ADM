<template>
  <el-dialog v-model="visible" :title="$t('profile.changePassword')" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
    <el-descriptions :column="1" size="small" class="mb16">
      <el-descriptions-item :label="$t('profile.username')">{{ username }}</el-descriptions-item>
      <el-descriptions-item :label="$t('profile.email')">{{ email || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="$t('profile.permissions')">
        {{ permissionCount }}
      </el-descriptions-item>
    </el-descriptions>
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="var(--rx-form-label-width)">
      <el-form-item :label="$t('profile.oldPassword')" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="$t('profile.newPassword')" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="$t('profile.confirmPassword')" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getProfile, changePassword } from '@/api/auth'
import { MIN_PASSWORD_LENGTH, checkPasswordStrength } from '@/utils/passwordPolicy'

const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const username = ref('')
const email = ref('')
const permissionCount = ref(0)

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const formRules = computed(() => ({
  oldPassword: [{ required: true, message: () => t('profile.oldRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: () => t('profile.newRequired'), trigger: 'blur' },
    { min: MIN_PASSWORD_LENGTH, message: () => t('profile.newLength'), trigger: 'blur' },
    {
      // P2-3：与后端 PasswordPolicy 共用同一规则（utils/passwordPolicy.ts）
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) => {
        if (v && !checkPasswordStrength(v).valid) {
          cb(new Error(t('profile.newComplexity')))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      validator: (_r: unknown, v: string, cb: (e?: Error) => void) => {
        if (v !== form.newPassword) {
          cb(new Error(t('profile.confirmMismatch')))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
}))

async function open() {
  visible.value = true
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  try {
    const profile = await getProfile()
    username.value = profile.username || ''
    email.value = profile.email || ''
    permissionCount.value = profile.permissions?.length || 0
  } catch {
    /* ignore */
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await changePassword(form.oldPassword, form.newPassword)
    ElMessage.success(t('profile.success'))
    visible.value = false
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
