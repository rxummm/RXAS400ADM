<template>
  <div class="page-container">
    <div class="search-bar">
      <span class="section">{{ $t('compose.title') }}</span>
    </div>
    <el-card>
      <el-form :model="form" label-width="100px" ref="formRef" :rules="rules" v-loading="sending">
        <el-form-item :label="$t('compose.recipients')" prop="recipients">
          <div class="flex gap8 w-full">
            <el-input v-model="form.recipients" :placeholder="$t('compose.recipientsHint')" class="flex-1" />
            <el-select v-model="selectedGroup" :placeholder="$t('compose.fromGroup')" clearable
              @change="handleGroupSelect" class="w-200">
              <el-option v-for="g in groups" :key="g.id" :label="g.groupName + ' (' + (g.memberCount || 0) + ')'" :value="g.id" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item :label="$t('compose.subject')" prop="subject">
          <el-input v-model="form.subject" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('compose.body')" prop="text">
          <el-input v-model="form.text" type="textarea" :rows="10" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('compose.priority')">
          <el-select v-model="form.priority" class="w-200">
            <el-option :label="$t('compose.normal')" value="NORMAL" />
            <el-option :label="$t('compose.high')" value="HIGH" />
            <el-option :label="$t('compose.low')" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSend" :loading="sending">{{ $t('compose.send') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listAllEmailGroups, listGroupMembers, sendEmail } from '@/api/email'
import type { EmailGroup } from '@/api/email'

defineOptions({ name: 'Compose' })

const { t } = useI18n()
const sending = ref(false)
const formRef = ref<FormInstance>()
const groups = ref<EmailGroup[]>([])
const selectedGroup = ref<number | null>(null)

const form = ref({
  recipients: '',
  subject: '',
  text: '',
  priority: 'NORMAL',
})

const rules: FormRules = {
  recipients: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
  subject: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
  text: [{ required: true, message: () => t('validation.notBlank'), trigger: 'blur' }],
}

onMounted(async () => {
  groups.value = await listAllEmailGroups()
})

async function handleGroupSelect(groupId: number | null) {
  if (!groupId) return
  const members = await listGroupMembers(groupId)
  const emails = members.filter(m => m.enabled === 1).map(m => m.email)
  const existing = form.value.recipients.split(/[;, ]+/).filter(Boolean)
  const merged = [...new Set([...existing, ...emails])]
  form.value.recipients = merged.join(', ')
}

async function handleSend() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  sending.value = true
  try {
    await sendEmail(form.value)
    ElMessage.success(t('compose.sendSuccess'))
    form.value = { recipients: '', subject: '', text: '', priority: 'NORMAL' }
    selectedGroup.value = null
  } finally {
    sending.value = false
  }
}
</script>