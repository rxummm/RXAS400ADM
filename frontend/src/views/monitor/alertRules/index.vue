<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" :placeholder="$t('common.keyword')" clearable class="w-200" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" v-has-perm="'ALERT_MANAGE'" :icon="Plus" @click="() => openCreate()">
        {{ $t('alertRules.add') }}
      </el-button>
      <el-button :icon="Refresh" @click="forceSearch">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('alertRules.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="tableData" size="small" border>
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
              @change="(v: string | number | boolean) => toggle(row as AlertRule, Boolean(v))"
            />
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('alertRules.description')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ descText(row as AlertRule) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'ALERT_MANAGE'" size="small" type="warning" plain @click="openEdit(row as AlertRule)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'ALERT_MANAGE'" size="small" type="danger" plain :loading="removeLoading === row.id" @click="confirmRemove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="forceSearch" @size-change="forceSearch" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-sm)" :close-on-click-modal="false">
      <el-form :model="form" label-width="var(--rx-form-label-width-wide)">
        <el-form-item :label="$t('alertRules.metric')" required>
          <el-select v-model="form.metricName" filterable allow-create default-first-option class="w-full">
            <el-option v-for="m in metricItems" :key="m.itemKey" :label="m.itemValue" :value="m.itemKey" />
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
            <el-option v-for="d in levelItems" :key="d.itemKey" :label="d.itemValue" :value="d.itemKey" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alertRules.server')">
          <el-select v-model="form.serverId as number | null" class="w-full" clearable :placeholder="$t('alertRules.allServers')" @clear="form.serverId = null">
            <el-option v-for="s in servers" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('alertRules.channel')">
          <el-select v-model="form.channel" class="w-full">
            <el-option v-for="d in channelItems" :key="d.itemKey" :label="d.itemValue" :value="d.itemKey" />
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
        <el-button type="primary" :loading="saving" @click="onSubmit">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'AlertRules' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { useDict } from '@/composables/useDict'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore, type As400Server } from '@/stores/as400Server'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
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

/** W3：种子规则 description 存 code（cpuCritical 等），映射 i18n；用户自建规则存原文直接展示。 */
const descText = (row: AlertRule) => {
  const key = `alertRules.descriptions.${row.description}`
  return te(key) ? t(key) : (row.description || '-')
}
const as400Store = useAs400ServerStore()

const { items: metricItems } = useDict('METRIC_TYPE')
const { items: levelItems } = useDict('ALERT_LEVEL')
const { items: channelItems, getTagType, getLabel } = useDict('ALERT_CHANNEL')
const servers = ref<As400Server[]>([])

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
} = useSmartQueryTable<AlertRule>({
  fetchApi: async () => {
    const data = await listAlertRules()
    return data as AlertRule[]
  },
  frontendPage: true,
  autoFetch: false,
  searchFields: ['metricName', 'description', 'level'],
  showRefresh: false,
})

const channelType = (ch: string) => getTagType(ch)

const channelLabel = (ch: string) => getLabel(ch)

const serverName = (id: number) => servers.value.find((s) => s.id === id)?.name || `#${id}`

const load = () => forceSearch()

type AlertRuleForm = {
  id?: number
  metricName: string
  operator: string
  threshold: number
  durationSeconds: number
  level: string
  channel: string
  serverId: number | null
  description: string
  enabled: boolean
}

const {
  dialogVisible,
  dialogTitle,
  loading: saving,
  form,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<AlertRuleForm>({
  defaultForm: () => ({
    metricName: 'CPU',
    operator: '>',
    threshold: 90,
    durationSeconds: 300,
    level: 'WARNING',
    channel: 'ALL',
    serverId: null,
    description: '',
    enabled: true,
  }),
  saveApi: async (isEdit, data) => {
    const { id: _id, ...payload } = data
    if (isEdit && data.id) await updateAlertRule(data.id, payload)
    else await createAlertRule(payload)
  },
  onSuccess: () => load(),
  i18nPrefix: 'alertRules',
  validate: false,
})

const toggle = async (row: AlertRule, v: boolean) => {
  await toggleAlertRule(row.id!, v)
  ElMessage.success(v ? t('alertRules.enabledOn') : t('alertRules.enabledOff'))
  await load()
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: AlertRule) => deleteAlertRule(row.id!),
  onSuccess: load,
  confirmMessage: 'alertRules.deleteConfirm',
})

onMounted(async () => {
  try {
    servers.value = await as400Store.fetchServers()
    await load()
  } catch (e: unknown) {
    ElMessage.error((e instanceof Error ? e.message : null) || t('common.loadFailed'))
  }
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
