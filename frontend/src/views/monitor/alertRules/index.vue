<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button type="primary" v-has-perm="'ALERT_MANAGE'" :icon="Plus" @click="() => openCreate()">
        {{ $t('alertRules.add') }}
      </el-button>
      <el-button :icon="Refresh" @click="load">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('alertRules.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedRows" size="small" border>
        <el-table-column prop="metricName" :label="$t('alertRules.metric')" width="120" />
        <el-table-column :label="$t('alertRules.condition')" width="160">
          <template #default="{ row }">
            <span class="text-muted">{{ row.metricName }}</span>
            <el-tag size="small" class="mx8">{{ row.operator }}</el-tag>
            <b>{{ row.threshold }}</b>
            <span class="hint ml4">({{ row.durationSeconds || 0 }}s)</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('alertRules.level')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('alertRules.server')" width="140">
          <template #default="{ row }">
            <el-tag v-if="!row.serverId" size="small" type="info">{{ $t('alertRules.allServers') }}</el-tag>
            <template v-else>
              <span>{{ serverName(row.serverId) }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column :label="$t('alertRules.channel')" width="120">
          <template #default="{ row }">
            <el-tag :type="channelType(row.channel)" size="small">{{ channelLabel(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('alertRules.enabled')" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-has-perm="'ALERT_MANAGE'"
              :model-value="row.enabled"
              size="small"
              @change="(v: any) => toggle(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('alertRules.description')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ descText(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'ALERT_MANAGE'" size="small" type="warning" plain @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'ALERT_MANAGE'" size="small" type="danger" plain @click="remove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="rows.length" v-model:current="current" v-model:size="size" @change="() => {}" @size-change="() => { current = 1 }" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? $t('alertRules.edit') : $t('alertRules.add')" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="$t('alertRules.metric')" required>
          <el-select v-model="form.metricName" filterable allow-create default-first-option class="w-full">
            <el-option v-for="m in metricOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alertRules.condition')" required>
          <div class="flex-row">
            <el-select v-model="form.operator" class="w-90">
              <el-option label="&gt;" value=">" />
              <el-option label="&gt;=" value=">=" />
              <el-option label="&lt;" value="<" />
              <el-option label="&lt;=" value="<=" />
            </el-select>
            <el-input-number v-model="form.threshold" :min="0" :precision="1" class="ml8" />
            <span class="hint ml8">{{ $t('alertRules.thresholdUnit') }}</span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('alertRules.duration')">
          <el-input-number v-model="form.durationSeconds" :min="0" :max="86400" />
          <span class="hint ml8">{{ $t('alertRules.durationUnit') }}</span>
        </el-form-item>
        <el-form-item :label="$t('alertRules.level')" required>
          <el-select v-model="form.level" class="w-full">
            <el-option label="WARNING" value="WARNING" />
            <el-option label="CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alertRules.server')">
          <el-select v-model="form.serverId as any" class="w-full" clearable>
            <el-option :label="$t('alertRules.allServers')" :value="undefined" />
            <el-option v-for="s in servers" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alertRules.channel')">
          <el-select v-model="form.channel" class="w-full">
            <el-option :label="$t('alertRules.channelAll')" value="ALL" />
            <el-option :label="$t('alertRules.channelWebhook')" value="WEBHOOK" />
            <el-option :label="$t('alertRules.channelEmail')" value="EMAIL" />
            <el-option :label="$t('alertRules.channelNone')" value="NONE" />
          </el-select>
          <div class="hint">{{ $t('alertRules.channelHint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('alertRules.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="$t('alertRules.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'AlertRules' })
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'
import {
  createAlertRule,
  deleteAlertRule,
  listAlertRules,
  toggleAlertRule,
  updateAlertRule,
  type AlertRule,
} from '@/api/alertRules'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t, te } = useI18n()

/** W3：种子规则 description 存 code（cpuCritical 等），映射 i18n；用户自建规则存原文直接展示。
 * 不能用 $t(...) || row.description 兜底——vue-i18n 缺失 key 返回 key 本身（truthy），兜底永不生效。 */
const descText = (row: AlertRule) => {
  const key = `alertRules.descriptions.${row.description}`
  return te(key) ? t(key) : (row.description || '-')
}
const as400Store = useAs400ServerStore()

const metricOptions = ['CPU', 'MEMORY', 'DISK', 'NETWORK', 'MSGW', 'LCKW', 'PRINTER']
const servers = ref<As400Server[]>([])
const rows = ref<AlertRule[]>([])
const loading = ref(false)
const saving = ref(false)
const current = ref(1)
const size = ref(10)

const pagedRows = computed(() => rows.value.slice((current.value - 1) * size.value, current.value * size.value))

const channelType = (ch: string) => {
  if (ch === 'NONE') return 'info'
  if (ch === 'EMAIL') return 'warning'
  if (ch === 'WEBHOOK') return 'success'
  return 'primary'
}

const channelLabel = (ch: string) => {
  const key = (ch || 'ALL').toUpperCase()
  if (key === 'WEBHOOK') return t('alertRules.channelWebhook')
  if (key === 'EMAIL') return t('alertRules.channelEmail')
  if (key === 'NONE') return t('alertRules.channelNone')
  return t('alertRules.channelAll')
}

const serverName = (id: number) => servers.value.find((s) => s.id === id)?.name || `#${id}`

const load = async () => {
  loading.value = true
  try {
    rows.value = (await listAlertRules()) as AlertRule[]
  } finally {
    loading.value = false
  }
}

const emptyForm = () => ({
  id: undefined as number | undefined,
  metricName: 'CPU',
  operator: '>',
  threshold: 90,
  durationSeconds: 300,
  level: 'WARNING',
  channel: 'ALL',
  serverId: null as number | null,
  description: '',
  enabled: true,
})

const dialogVisible = ref(false)
const form = ref(emptyForm())

const openCreate = () => {
  form.value = emptyForm()
  dialogVisible.value = true
}

const openEdit = (row: AlertRule) => {
  form.value = {
    id: row.id,
    metricName: row.metricName,
    operator: row.operator,
    threshold: row.threshold,
    durationSeconds: row.durationSeconds || 0,
    level: row.level,
    channel: row.channel || 'ALL',
    serverId: row.serverId ?? null,
    description: row.description || '',
    enabled: row.enabled !== false,
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.metricName || form.value.threshold == null) {
    ElMessage.warning(t('alertRules.required'))
    return
  }
  saving.value = true
  try {
    const { id: _id, ...payload } = form.value
    if (form.value.id) {
      await updateAlertRule(form.value.id, payload)
    } else {
      await createAlertRule(payload)
    }
    ElMessage.success(t('common.save'))
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const toggle = async (row: AlertRule, v: boolean) => {
  await toggleAlertRule(row.id!, v)
  ElMessage.success(v ? t('alertRules.enabledOn') : t('alertRules.enabledOff'))
  await load()
}

const remove = async (row: AlertRule) => {
  await ElMessageBox.confirm(t('alertRules.deleteConfirm'), t('common.confirm'), { type: 'warning' })
  await deleteAlertRule(row.id!)
  ElMessage.success(t('common.delete'))
  await load()
}

onMounted(async () => {
  servers.value = await as400Store.fetchServers()
  await load()
})
</script>

<style scoped>
.flex-row {
  display: flex;
  align-items: center;
}
.mx8 {
  margin: 0 6px;
}
</style>