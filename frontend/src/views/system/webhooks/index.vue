<template>
  <div class="page-container page-container--fit">
    <el-tabs v-model="tab" class="page-tabs">
      <el-tab-pane v-if="userStore.canSeeTab('webhooks', 'configTab')" :label="$t('webhooks.configTab')" name="config">
        <div class="search-bar">
          <div class="flex-1" />
          <el-button v-has-perm="'WEBHOOK_MANAGE'" type="primary" @click="() => openCreate()">
            <el-icon><Plus /></el-icon> {{ $t('webhooks.add') }}
          </el-button>
        </div>
        <div class="table-wrapper">
          <RxSkeleton type="table" :rows="5" :loading="loading">
            <el-table :data="webhooks" size="small" border stripe class="w-full">
            <el-table-column prop="name" :label="$t('webhooks.name')" width="180" show-overflow-tooltip />
            <el-table-column prop="url" :label="$t('webhooks.url')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="description" :label="$t('webhooks.description')" min-width="150" show-overflow-tooltip />
            <el-table-column prop="enabled" :label="$t('webhooks.enabled')" width="90" align="center">
              <template #default="{ row }">
                <el-switch v-has-perm="'WEBHOOK_MANAGE'"
                  :model-value="row.enabled === 1"
                  @change="(val: string | number | boolean) => onToggle(row as WebhookConfig, Boolean(val))"
                />
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.operation')" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-has-perm="'WEBHOOK_MANAGE'" link type="primary" size="small" @click="onTest(row as WebhookConfig)">
                  {{ $t('webhooks.test') }}
                </el-button>
                <el-button v-has-perm="'WEBHOOK_MANAGE'" link type="primary" size="small" @click="openEdit(row)">
                  {{ $t('common.edit') }}
                </el-button>
                <el-button v-has-perm="'WEBHOOK_MANAGE'" link type="danger" size="small" @click="onDelete(row as WebhookConfig)">
                  {{ $t('common.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="userStore.canSeeTab('webhooks', 'logTab')" :label="$t('webhooks.logTab')" name="logs">
        <div class="search-bar">
          <el-input
            v-model="logName"
            :placeholder="$t('webhooks.logName')"
            clearable
            class="w-180"
            @keyup.enter="loadLogs"
          />
          <el-select v-model="logSuccess" :placeholder="$t('webhooks.logSuccess')" clearable class="w-120">
            <el-option :label="$t('webhooks.success')" :value="1" />
            <el-option :label="$t('webhooks.fail')" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadLogs">
            <el-icon><Search /></el-icon> {{ $t('common.search') }}
          </el-button>
          <div class="flex-1" />
          <el-button v-has-perm="'WEBHOOK_MANAGE'" type="danger" plain @click="onCleanLogs">
            <el-icon><Delete /></el-icon> {{ $t('webhooks.clearLogs') }}
          </el-button>
        </div>
        <div class="table-wrapper">
          <RxSkeleton type="table" :rows="5" :loading="logLoading">
            <el-table :data="logs" size="small" border stripe class="w-full">
            <el-table-column prop="webhookName" :label="$t('webhooks.logName')" width="160" show-overflow-tooltip />
            <el-table-column prop="title" :label="$t('webhooks.logTitle')" min-width="200" show-overflow-tooltip />
            <el-table-column prop="success" :label="$t('webhooks.logSuccess')" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.success === 1 ? 'success' : 'danger'">
                  {{ row.success === 1 ? $t('webhooks.success') : $t('webhooks.fail') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="attempts" :label="$t('webhooks.logAttempts')" width="70" align="center" />
            <el-table-column prop="errorMsg" :label="$t('webhooks.logError')" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="text-danger">{{ row.errorMsg || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdTime" :label="$t('webhooks.logTime')" width="170">
              <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
            </el-table-column>
          </el-table>
          </RxSkeleton>
          <AppPagination :total="logTotal" v-model:current="logCurrent" v-model:size="logSize" @change="loadLogs" @size-change="loadLogs" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item :label="$t('webhooks.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('webhooks.url')" prop="url">
          <el-input v-model="form.url" placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxx" />
        </el-form-item>
        <el-form-item :label="$t('webhooks.secret')">
          <el-input v-model="form.secret" show-password />
        </el-form-item>
        <el-form-item :label="$t('webhooks.description')">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item :label="$t('webhooks.enabled')">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Delete, Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWebhooks,
  listWebhookLogs,
  createWebhook,
  updateWebhook,
  deleteWebhook,
  toggleWebhook,
  testWebhook,
  cleanWebhookLogs,
  type WebhookConfig,
  type WebhookLog,
} from '@/api/webhook'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useUserStore } from '@/stores/user'
import { useFormDialog } from '@/composables/useFormDialog'

defineOptions({ name: 'Webhooks' })

const { t } = useI18n()
const userStore = useUserStore()

const tab = ref('config')
const loading = ref(false)
const webhooks = ref<WebhookConfig[]>([])

const logLoading = ref(false)
const logs = ref<WebhookLog[]>([])
const logTotal = ref(0)
const logCurrent = ref(1)
const logSize = ref(20)
const logName = ref('')
const logSuccess = ref<number>()

interface WebhookForm { id?: number; name: string; url: string; secret: string; description: string; enabled: number }

const {
  dialogVisible,
  dialogTitle,
  loading: submitLoading,
  formRef,
  form,
  rules: formRules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<WebhookForm>({
  defaultForm: () => ({ id: undefined, name: '', url: '', secret: '', description: '', enabled: 1 }),
  rules: {
    name: [{ required: true, message: () => t('webhooks.nameRequired'), trigger: 'blur' }],
    url: [{ required: true, message: () => t('webhooks.urlRequired'), trigger: 'blur' }],
  },
  createApi: (data) => createWebhook(data),
  updateApi: (id, data) => updateWebhook(Number(id), data),
  onSuccess: () => loadWebhooks(),
  i18nPrefix: 'webhooks',
})

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

async function loadWebhooks() {
  loading.value = true
  try {
    webhooks.value = (await listWebhooks()) || []
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const data = await listWebhookLogs({
      current: logCurrent.value,
      size: logSize.value,
      success: logSuccess.value,
      webhookName: logName.value || undefined,
    })
    logs.value = data.records || []
    logTotal.value = data.total || 0
  } finally {
    logLoading.value = false
  }
}

async function onToggle(row: WebhookConfig, val: boolean) {
  if (row.id) {
    await toggleWebhook(row.id, val ? 1 : 0)
    row.enabled = val ? 1 : 0
  }
}

async function onDelete(row: WebhookConfig) {
  try {
    await ElMessageBox.confirm(t('webhooks.deleteConfirm', { name: row.name }), t('common.tip'), { type: 'warning' })
    if (row.id) await deleteWebhook(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    loadWebhooks()
  } catch {
    /* cancelled */
  }
}

async function onTest(row: WebhookConfig) {
  if (!row.id) return
  try {
    const result = await testWebhook(row.id)
    if (result.success) {
      ElMessage.success(t('webhooks.testSuccess'))
    } else {
      ElMessage.error(t('webhooks.testFailed'))
    }
  } catch {
    /* request interceptor shows error */
  }
}

async function onCleanLogs() {
  try {
    await ElMessageBox.confirm(t('webhooks.clearLogsConfirm'), t('common.tip'), { type: 'warning' })
    await cleanWebhookLogs(30)
    ElMessage.success(t('common.deleteSuccess'))
    loadLogs()
  } catch {
    /* cancelled */
  }
}

onMounted(() => {
  loadWebhooks()
  loadLogs()
})
</script>

<style scoped>
.page-tabs :deep(.el-tabs__content) {
  overflow: visible;
}
</style>