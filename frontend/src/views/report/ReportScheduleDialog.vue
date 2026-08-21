<template>
  <el-dialog v-model="visible" :title="form.id ? $t('reports.scheduleEdit') : $t('reports.scheduleAdd')" width="560px">
    <el-form :model="form" label-width="110px">
      <el-form-item :label="$t('reports.scheduleName')" required>
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item :label="$t('reports.kind')" required>
        <el-select v-model="form.reportType" class="w-full">
          <el-option :label="$t('reports.kindMetrics')" value="metrics" />
          <el-option :label="$t('reports.kindExecutions')" value="executions" />
          <el-option :label="$t('reports.kindCapacity')" value="capacity" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('reports.format')" required>
        <el-radio-group v-model="form.format">
          <el-radio-button value="xlsx">Excel (.xlsx)</el-radio-button>
          <el-radio-button value="pdf">PDF</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.reportType !== 'executions'" :label="$t('reports.instance')" required>
        <el-select v-model="form.serverId" class="w-full">
          <el-option v-for="s in systems" :key="s.id" :label="`${s.name} (${s.host})`" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.reportType !== 'executions'" :label="$t('reports.days')">
        <el-input-number v-model="form.days" :min="1" :max="365" />
      </el-form-item>
      <el-form-item :label="$t('reports.cron')" required>
        <el-input v-model="form.cronExpr" placeholder="0 0 6 * * ?" />
        <div class="cron-hint">{{ $t('reports.cronHint') }}</div>
      </el-form-item>
      <el-form-item :label="$t('reports.recipients')" required>
        <el-input v-model="form.recipients" type="textarea" :rows="2" :placeholder="$t('reports.recipientsHint')" />
      </el-form-item>
      <el-form-item :label="$t('reports.enabled')">
        <el-switch v-model="form.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { createReportSchedule, updateReportSchedule, type ReportSchedule } from '@/api/report'
import type { IbmiSystem } from '@/api/as400'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  systems: IbmiSystem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const saving = ref(false)
const form = ref<ReportSchedule & { id?: number }>({
  name: '',
  reportType: 'metrics',
  format: 'xlsx',
  serverId: undefined,
  days: 7,
  cronExpr: '0 0 6 * * ?',
  recipients: '',
  enabled: true,
})

function openCreate() {
  form.value = {
    name: '',
    reportType: 'metrics',
    format: 'xlsx',
    serverId: props.systems[0]?.id,
    days: 7,
    cronExpr: '0 0 6 * * ?',
    recipients: '',
    enabled: true,
  }
  visible.value = true
}

function openEdit(row: ReportSchedule) {
  form.value = {
    id: row.id,
    name: row.name,
    reportType: row.reportType,
    format: row.format || 'xlsx',
    serverId: row.serverId ?? undefined,
    days: row.days || 7,
    cronExpr: row.cronExpr,
    recipients: row.recipients || '',
    enabled: row.enabled !== false,
  }
  visible.value = true
}

async function save() {
  if (!form.value.name || !form.value.cronExpr || !form.value.recipients) {
    ElMessage.warning(t('reports.scheduleRequired'))
    return
  }
  if (form.value.reportType !== 'executions' && !form.value.serverId) {
    ElMessage.warning(t('reports.selectInstance'))
    return
  }
  saving.value = true
  try {
    const { id: _id, ...payload }: ReportSchedule = { ...form.value }
    if (form.value.reportType === 'executions') payload.serverId = null
    if (form.value.id) {
      await updateReportSchedule(form.value.id, payload)
    } else {
      await createReportSchedule(payload)
    }
    ElMessage.success(t('common.save'))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ openCreate, openEdit })
</script>

<style scoped>
.cron-hint {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>