<template>
  <el-dialog v-model="visible" :title="title" width="460px" @closed="emit('closed')">
    <el-form :model="form" :label-width="'80px'">
      <el-form-item :label="t('users.username')">
        <el-input v-model="form.username" :disabled="isEdit" />
      </el-form-item>
      <el-form-item :label="t('users.password')" :required="!isEdit">
        <el-input v-model="form.password" type="password" show-password
                  :placeholder="isEdit ? t('users.resetPasswordHint') : t('users.createPasswordHint')" />
      </el-form-item>
      <el-form-item :label="t('users.email')">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item :label="t('users.roles')">
        <el-select v-model="form.roleIds as any" multiple class="w-full" :placeholder="t('users.roles')">
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName || r.roleCode" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isEdit" :label="t('common.status')">
        <el-switch
          v-model="form.status"
          active-value="ACTIVE"
          inactive-value="DISABLED"
          :active-text="t('users.statusActive')"
          :inactive-text="t('users.statusDisabled')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { createUser, updateUser, type UserStatus } from '@/api/user'
import type { SysRole } from '@/api/role'
import { checkPasswordStrength } from '@/utils/passwordPolicy'

const props = defineProps<{
  modelValue: boolean
  editData?: { id: number; username: string; email: string; roles: SysRole[]; status: UserStatus } | null
  roles: SysRole[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'saved': []
  'closed': []
}>()

const { t } = useI18n()
const saving = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => !!props.editData?.id)
const title = computed(() => isEdit.value ? t('users.edit') : t('users.create'))

const form = reactive({
  username: '',
  password: '',
  email: '',
  roleIds: [] as number[],
  status: 'ACTIVE' as UserStatus,
})

watch(() => props.editData, (data) => {
  if (data) {
    form.username = data.username
    form.password = ''
    form.email = data.email || ''
    form.roleIds = (data.roles || []).map((r) => r.id).filter((id): id is number => id != null)
    form.status = data.status
  } else {
    form.username = ''
    form.password = ''
    form.email = ''
    form.roleIds = []
    form.status = 'ACTIVE'
  }
}, { immediate: true })

const handleSubmit = async () => {
  if (!form.username) {
    ElMessage.warning(t('users.username'))
    return
  }
  // P2-3：前端与后端共用同一密码策略（utils/passwordPolicy.ts）
  if (form.password && !checkPasswordStrength(form.password).valid) {
    ElMessage.warning(t('users.passwordPolicy'))
    return
  }
  saving.value = true
  try {
    if (props.editData?.id) {
      await updateUser(props.editData.id, {
        password: form.password || undefined,
        email: form.email,
        status: form.status,
        roleIds: form.roleIds,
      })
      ElMessage.success(t('users.updateSuccess'))
    } else {
      // P2-3：创建必填密码（至少 8 位含字母数字，后端统一校验），不再用 123456 兜底
      if (!form.password) {
        ElMessage.warning(t('users.passwordRequired'))
        return
      }
      await createUser({
        username: form.username,
        password: form.password,
        email: form.email,
        roleIds: form.roleIds,
      })
      ElMessage.success(t('users.createSuccess'))
    }
    emit('saved')
    visible.value = false
  } finally {
    saving.value = false
  }
}
</script>