<template>
  <el-dialog v-model="visible" :title="title" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="var(--rx-form-label-width)">
      <el-form-item :label="$t('role.roleName')" prop="roleName">
        <el-input v-model="form.roleName" />
      </el-form-item>
      <el-form-item :label="$t('role.roleCode')" prop="roleCode">
        <el-input v-model="form.roleCode" :disabled="isEdit" placeholder="OPERATOR" />
      </el-form-item>
      <el-form-item :label="$t('role.description')">
        <el-input v-model="form.description" />
      </el-form-item>
      <div class="flex gap16">
        <el-form-item :label="$t('common.sort')">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { createRole, updateRole, type SysRole } from '@/api/role'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const title = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: undefined as number | undefined,
  roleCode: '',
  roleName: '',
  description: '',
  sort: 0,
  status: 1,
})
const form = reactive(defaultForm())

const rules = {
  roleName: [{ required: true, message: () => t('role.nameRequired'), trigger: 'blur' }],
  roleCode: [{ required: true, message: () => t('role.codeRequired'), trigger: 'blur' }],
}

function openCreate() {
  isEdit.value = false
  title.value = t('role.addRole')
  Object.assign(form, defaultForm())
  visible.value = true
}

function openEdit(row: SysRole) {
  isEdit.value = true
  title.value = t('common.edit')
  Object.assign(form, defaultForm(), {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description || '',
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  })
  visible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateRole(form.id, { ...form })
      ElMessage.success(t('common.updateSuccess'))
    } else {
      await createRole({ ...form })
      ElMessage.success(t('common.createSuccess'))
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}

defineExpose({ openCreate, openEdit })
</script>