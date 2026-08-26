<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" :placeholder="$t('common.keyword')" clearable class="w-200" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" v-has-perm="'AS400_MANAGE'" :icon="Plus" @click="() => openCreate()">
        {{ $t('assets.add') }}
      </el-button>
      <el-button :icon="Refresh" @click="forceSearch">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('assets.serverSyncHint') }}</span>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="tableData" size="small" border>
        <el-table-column prop="name" :label="$t('assets.name')" min-width="120" />
        <el-table-column prop="host" :label="$t('assets.host')" min-width="140" />
        <el-table-column prop="port" :label="$t('assets.port')" width="70" />
        <el-table-column :label="$t('assets.environment')" width="90">
          <template #default="{ row }">
            <el-tag :type="envType(row.environment)" size="small">{{ row.environment }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('assets.level')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.criticalLevel === 'CRITICAL' ? 'danger' : 'info'" size="small">
              {{ row.criticalLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('assets.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="$t('assets.username')" width="110" />
        <el-table-column :label="$t('assets.defaultServer')" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.defaultServer" size="small" type="success">{{ $t('common.yes') }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain :loading="testingId === row.id" @click="test(row as IbmiSystem)">
              {{ $t('assets.test') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="info" plain @click="openCommand(row as IbmiSystem)">
              {{ $t('assets.command') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="warning" plain @click="openEdit(row as IbmiSystem)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="danger" plain :loading="removeLoading === row.id" @click="confirmRemove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-md)" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="var(--rx-form-label-width-wide)">
        <el-form-item :label="$t('assets.name')" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('assets.host')" prop="host">
          <el-input v-model="form.host" placeholder="US400CND / 10.0.0.5" />
        </el-form-item>
        <el-form-item :label="$t('assets.port')">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item :label="$t('assets.username')">
          <el-input v-model="form.username" placeholder="QPGMR / QSECOFR" />
        </el-form-item>
        <el-form-item :label="$t('assets.password')" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? $t('assets.passwordPlaceholder') : $t('assets.passwordRequired')" />
        </el-form-item>
        <el-form-item :label="$t('assets.environment')">
          <el-select v-model="form.environment" class="w-full">
            <el-option label="PROD" value="PROD" />
            <el-option label="TEST" value="TEST" />
            <el-option label="DEV" value="DEV" />
            <el-option label="DR" value="DR" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('assets.level')">
          <el-select v-model="form.criticalLevel" class="w-full">
            <el-option label="CRITICAL" value="CRITICAL" />
            <el-option label="NORMAL" value="NORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('assets.region')">
          <el-input v-model="form.region" />
        </el-form-item>
        <el-form-item :label="$t('assets.haGroup')">
          <el-input v-model="form.haGroup" />
        </el-form-item>
        <el-form-item :label="$t('assets.defaultLibraries')">
          <el-input v-model="form.defaultLibraries" placeholder="QGPL,QTEMP" />
        </el-form-item>
        <el-form-item :label="'CCSID'">
          <el-input-number v-model="form.ccsid" :min="0" />
        </el-form-item>
        <el-form-item :label="$t('assets.sortOrder')">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item :label="$t('assets.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('assets.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item :label="$t('assets.defaultServer')">
          <el-switch v-model="form.defaultServer" />
        </el-form-item>
        <el-form-item :label="'SSL'">
          <el-switch v-model="form.sslEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 执行 CL 命令 -->
    <el-dialog v-model="commandVisible" :title="`${$t('assets.command')} - ${commandSystem?.name || ''}`" width="var(--rx-dialog-md)" :close-on-click-modal="false">
      <el-input
        v-model="command"
        type="textarea"
        :rows="4"
        placeholder="DSPLIB QGPL"
        class="mb16"
      />
      <el-divider>{{ $t('assets.commandResult') }}</el-divider>
      <pre v-if="commandResult" class="command-output">{{ commandResult }}</pre>
      <span v-else class="hint">{{ $t('assets.commandHint') }}</span>
      <template #footer>
        <el-button @click="commandVisible = false">{{ $t('common.close') }}</el-button>
        <el-button type="primary" :loading="commandRunning" :disabled="!command" @click="runCommand">
          {{ $t('assets.execute') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'Assets' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore } from '@/stores/as400Server'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  createSystem,
  deleteSystem,
  executeCommand,
  fetchSystemDetail,
  testSystem,
  updateSystem,
  type IbmiSystem,
  type CommandResult,
} from '@/api/as400'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const as400Store = useAs400ServerStore()
const testingId = ref(0)

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
} = useSmartQueryTable<IbmiSystem>({
  fetchApi: async () => {
    const data = await fetchSystemDetail()
    as400Store.refreshServers()
    return data
  },
  frontendPage: true,
  autoFetch: false,
  searchFields: ['name', 'host', 'description'],
  showRefresh: false,
})

const envType = (env: string) => {
  if (env === 'PROD') return 'danger'
  if (env === 'TEST') return 'warning'
  if (env === 'DR') return 'info'
  return 'success'
}

const load = () => forceSearch()

type AssetForm = {
  id?: number
  name: string
  host: string
  port: number
  username: string
  password: string
  environment: string
  criticalLevel: string
  region: string
  haGroup: string
  defaultLibraries: string
  ccsid: number
  sortOrder: number
  description: string
  enabled: boolean
  defaultServer: boolean
  sslEnabled: boolean
}

const {
  dialogVisible,
  dialogTitle,
  loading: saving,
  formRef,
  form,
  rules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<AssetForm>({
  defaultForm: () => ({
    name: '',
    host: '',
    port: 8470,
    username: '',
    password: '',
    environment: 'TEST',
    criticalLevel: 'NORMAL',
    region: '',
    haGroup: '',
    defaultLibraries: '',
    ccsid: 37,
    sortOrder: 0,
    description: '',
    enabled: true,
    defaultServer: false,
    sslEnabled: false,
  }),
  rules: {
    name: [{ required: true, message: () => t('assets.required'), trigger: 'blur' }],
    host: [{ required: true, message: () => t('assets.required'), trigger: 'blur' }],
    password: [{
      required: true,
      message: () => t('assets.passwordRequired'),
      trigger: 'blur',
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (!form.value.id && !value) callback(new Error(t('assets.passwordRequired')))
        else callback()
      },
    }],
  },
  saveApi: async (isEdit, data) => {
    const payload: Partial<IbmiSystem> = { ...data }
    delete payload.id
    if (payload.password) {
      payload.passwordEncrypt = payload.password
    }
    delete payload.password
    if (isEdit && data.id) await updateSystem(data.id, payload)
    else await createSystem(payload)
  },
  onSuccess: () => load(),
  i18nPrefix: 'assets',
})

const test = async (row: IbmiSystem) => {
  testingId.value = row.id
  try {
    const result: { success: boolean; message: string } = await testSystem(row.id)
    ElMessage.success(`${row.name}: ${result.message}`)
    await load()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    testingId.value = 0
  }
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: IbmiSystem) => deleteSystem(row.id),
  onSuccess: load,
  confirmMessage: 'assets.deleteConfirm',
})

const commandVisible = ref(false)
const commandSystem = ref<IbmiSystem | null>(null)
const command = ref('')
const commandRunning = ref(false)
const commandResult = ref('')

const openCommand = (row: IbmiSystem) => {
  commandSystem.value = row
  command.value = ''
  commandResult.value = ''
  commandVisible.value = true
}

const runCommand = async () => {
  if (!command.value.trim() || !commandSystem.value) return
  commandRunning.value = true
  try {
    const result: CommandResult = await executeCommand(commandSystem.value.id, command.value.trim())
    commandResult.value = result.output || JSON.stringify(result)
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    commandRunning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.command-output {
  background: var(--bg-container);
  border-radius: 4px;
  padding: 10px 12px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-regular);
}
</style>
