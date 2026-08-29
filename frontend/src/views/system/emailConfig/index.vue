<template>
  <div class="page-container">
    <div class="search-bar">
      <span class="section">{{ $t('emailConfig.title') }}</span>
    </div>
    <el-card>
      <el-form :model="form" label-width="120px" v-loading="loading">
        <el-form-item :label="$t('emailConfig.smtpHost')">
          <el-input v-model="form.smtpHost" :placeholder="$t('emailConfig.smtpHostHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.smtpPort')">
          <el-input v-model="form.smtpPort" :placeholder="$t('emailConfig.smtpPortHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.smtpTimeout')">
          <el-input v-model="form.smtpTimeout" :placeholder="$t('emailConfig.smtpTimeoutHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.smtpUser')">
          <el-input v-model="form.smtpUser" :placeholder="$t('emailConfig.smtpUserHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.smtpPass')">
          <el-input v-model="form.smtpPass" type="password" show-password :placeholder="$t('emailConfig.smtpPassHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.smtpFrom')">
          <el-input v-model="form.smtpFrom" :placeholder="$t('emailConfig.smtpFromHint')" class="w-full" />
        </el-form-item>
        <el-form-item :label="$t('emailConfig.defaultRecipients')">
          <el-input v-model="form.defaultTo" :placeholder="$t('emailConfig.defaultRecipientsHint')" class="w-full" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">{{ $t('common.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt16">
      <template #header>
        <span>{{ $t('emailConfig.testSend') }}</span>
      </template>
      <el-form :inline="true">
        <el-form-item :label="$t('emailConfig.testRecipient')">
          <el-input v-model="testEmail" :placeholder="$t('emailConfig.testRecipientHint')" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleTestSend" :loading="testLoading">{{ $t('emailConfig.sendTest') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { listEmailConfigs, updateEmailConfigs, testSendEmail } from '@/api/email'

defineOptions({ name: 'EmailConfig' })

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const testLoading = ref(false)
const testEmail = ref('')

const form = ref({
  smtpHost: '',
  smtpPort: '465',
  smtpTimeout: '5000',
  smtpUser: '',
  smtpPass: '',
  smtpFrom: '',
  defaultTo: '',
})

const KEY_MAP: Record<string, string> = {
  smtpHost: 'host',
  smtpPort: 'port',
  smtpTimeout: 'timeout',
  smtpUser: 'user',
  smtpPass: 'pass',
  smtpFrom: 'from',
  defaultTo: 'to',
}

onMounted(async () => {
  loading.value = true
  try {
    const list = await listEmailConfigs()
    for (const item of list) {
      const key = Object.keys(KEY_MAP).find(k => KEY_MAP[k] === item.configKey)
      if (key) {
        ;(form.value as Record<string, string>)[key] = item.configValue || ''
      }
    }
  } finally {
    loading.value = false
  }
})

async function handleSave() {
  saving.value = true
  try {
    const configs: Record<string, string> = {}
    for (const [field, configKey] of Object.entries(KEY_MAP)) {
      configs[configKey] = (form.value as Record<string, string>)[field] || ''
    }
    await updateEmailConfigs(configs)
    ElMessage.success(t('common.operationSuccess'))
  } finally {
    saving.value = false
  }
}

async function handleTestSend() {
  if (!testEmail.value) return
  testLoading.value = true
  try {
    await testSendEmail(testEmail.value)
    ElMessage.success(t('emailConfig.testSendSuccess'))
  } finally {
    testLoading.value = false
  }
}
</script>