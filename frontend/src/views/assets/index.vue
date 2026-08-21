<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" v-has-perm="'AS400_MANAGE'" :icon="Plus" @click="openCreate">
        {{ $t('assets.add') }}
      </el-button>
      <el-button :icon="Refresh" @click="load">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('assets.serverSyncHint') }}</span>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedRows" size="small" border>
        <el-table-column prop="name" :label="$t('assets.name')" min-width="120" />
        <el-table-column prop="host" :label="$t('assets.host')" min-width="140" />
        <el-table-column prop="port" :label="$t('assets.port')" width="70" />
        <el-table-column :label="$t('assets.environment')" width="90">
          <template #default="{ row }: { row: IbmiSystem }">
            <el-tag :type="envType(row.environment)" size="small">{{ row.environment }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('assets.level')" width="90">
          <template #default="{ row }: { row: IbmiSystem }">
            <el-tag :type="row.criticalLevel === 'CRITICAL' ? 'danger' : 'info'" size="small">
              {{ row.criticalLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('assets.status')" width="100">
          <template #default="{ row }: { row: IbmiSystem }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" :label="$t('assets.username')" width="110" />
        <el-table-column :label="$t('assets.defaultServer')" width="80" align="center">
          <template #default="{ row }: { row: IbmiSystem }">
            <el-tag v-if="row.defaultServer" size="small" type="success">{{ $t('common.yes') }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="300" fixed="right">
          <template #default="{ row }: { row: IbmiSystem }">
            <el-button size="small" type="primary" plain :loading="testingId === row.id" @click="test(row)">
              {{ $t('assets.test') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="info" plain @click="openCommand(row)">
              {{ $t('assets.command') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="warning" plain @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'AS400_MANAGE'" size="small" type="danger" plain @click="remove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="systems.length" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="onSizeChange" />
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? $t('assets.edit') : $t('assets.add')" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="$t('assets.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="$t('assets.host')" required>
          <el-input v-model="form.host" placeholder="US400CND / 10.0.0.5" />
        </el-form-item>
        <el-form-item :label="$t('assets.port')">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item :label="$t('assets.username')">
          <el-input v-model="form.username" placeholder="QPGMR / QSECOFR" />
        </el-form-item>
        <el-form-item :label="$t('assets.password')">
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
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 执行 CL 命令 -->
    <el-dialog v-model="commandVisible" :title="`${$t('assets.command')} - ${commandSystem?.name || ''}`" width="640px">
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore } from '@/stores/as400Server'
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
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const as400Store = useAs400ServerStore()
const systems = ref<IbmiSystem[]>([])
const loading = ref(false)
const testingId = ref(0)
const saving = ref(false)
const current = ref(1)
const size = ref(10)

const pagedRows = computed(() =>
  systems.value.slice((current.value - 1) * size.value, current.value * size.value),
)

const onSizeChange = () => {
  current.value = 1
}

const envType = (env: string) => {
  if (env === 'PROD') return 'danger'
  if (env === 'TEST') return 'warning'
  if (env === 'DR') return 'info'
  return 'success'
}

const load = async () => {
  loading.value = true
  try {
    // N2：资产清单为管理面（AS400_MANAGE），走完整 detail 接口拿到 username 等凭据字段
    // 维护结果同步到全站：管理面改动后再刷一次普通列表给顶栏选择器/报表
    systems.value = await fetchSystemDetail()
    // N2：管理面改动后强制刷新全站共享的服务器列表（顶栏选择器/报表等）
    as400Store.refreshServers()
  } finally {
    loading.value = false
  }
}

const emptyForm = () => ({
  id: undefined as number | undefined,
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
})

const dialogVisible = ref(false)
const form = ref(emptyForm())

const openCreate = () => {
  form.value = emptyForm()
  dialogVisible.value = true
}

const openEdit = (row: IbmiSystem) => {
  form.value = {
    id: row.id,
    name: row.name,
    host: row.host,
    port: row.port,
    username: row.username || '',
    password: '',
    environment: row.environment || 'TEST',
    criticalLevel: row.criticalLevel || 'NORMAL',
    region: row.region || '',
    haGroup: row.haGroup || '',
    defaultLibraries: row.defaultLibraries || '',
    ccsid: row.ccsid || 37,
    sortOrder: row.sortOrder || 0,
    description: row.description || '',
    enabled: row.enabled !== false,
    defaultServer: !!row.defaultServer,
    sslEnabled: !!row.sslEnabled,
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.host) {
    ElMessage.warning(t('assets.required'))
    return
  }
  if (!form.value.id && !form.value.password) {
    ElMessage.warning(t('assets.passwordRequired'))
    return
  }
  saving.value = true
  try {
    const payload: Partial<IbmiSystem> = { ...form.value }
    delete payload.id
    if (payload.password) {
      payload.passwordEncrypt = payload.password
    }
    delete payload.password
    if (form.value.id) {
      await updateSystem(form.value.id, payload)
    } else {
      await createSystem(payload)
    }
    ElMessage.success(t('common.save'))
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

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

const remove = async (row: IbmiSystem) => {
  await ElMessageBox.confirm(
    t('assets.deleteConfirm', { name: row.name }),
    t('common.confirm'),
    { type: 'warning' },
  )
  await deleteSystem(row.id)
  ElMessage.success(t('common.delete'))
  await load()
}

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