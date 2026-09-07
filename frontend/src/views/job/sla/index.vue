<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button v-has-perm="'SLA_MANAGE'" type="success" :icon="Plus" @click="() => openCreate()">
        {{ $t('jobSla.addRule') }}
      </el-button>
      <el-button :icon="Refresh" @click="loadAll">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('jobSla.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <div class="section">{{ $t('jobSla.rules') }}</div>
      <RxSkeleton type="table" :rows="5" :loading="loading">
        <el-table :data="slaRules" size="small" border>
        <el-table-column prop="jobName" :label="$t('jobSla.jobName')" min-width="140" />
        <el-table-column prop="scheduleName" :label="$t('jobSla.scheduleName')" min-width="160">
          <template #default="{ row }">{{ row.scheduleName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="expectedDurationSec" :label="$t('jobSla.expectedSec')" width="120" align="right">
          <template #default="{ row }">{{ row.expectedDurationSec }} s</template>
        </el-table-column>
        <el-table-column prop="deviationPercent" :label="$t('jobSla.deviation')" width="120" align="center">
          <template #default="{ row }">{{ row.deviationPercent }}%</template>
        </el-table-column>
        <el-table-column :label="$t('jobSla.enabled')" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-has-perm="'SLA_MANAGE'"
              :model-value="row.enabled"
              @change="toggle(row as JobSla)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'SLA_MANAGE'" size="small" type="primary" plain :icon="Edit" @click="openEdit(row as JobSla)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'SLA_MANAGE'" size="small" type="danger" plain :icon="Delete" :loading="removeLoading === row.id" @click="confirmRemove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
    </div>

    <div class="table-wrapper mt16">
      <div class="section">{{ $t('jobSla.executions') }}</div>
      <el-alert type="info" :closable="false" class="mb8">
        <template #title>{{ $t('jobSla.mockNote') }}</template>
      </el-alert>
      <RxSkeleton type="table" :rows="5" :loading="execLoading">
        <el-table :data="executions" size="small" border>
        <el-table-column prop="JOB_NAME" :label="$t('jobSla.jobName')" min-width="140" />
        <el-table-column prop="SCHEDULE_NAME" :label="$t('jobSla.scheduleName')" min-width="160" />
        <el-table-column :label="$t('jobSla.expectedSec')" width="120" align="right">
          <template #default="{ row }">{{ row.EXPECTED_SEC }} s</template>
        </el-table-column>
        <el-table-column :label="$t('jobSla.actualSec')" width="120" align="right">
          <template #default="{ row }">{{ row.ACTUAL_SEC }} s</template>
        </el-table-column>
        <el-table-column :label="$t('jobSla.slaStatus')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.STATUS === 'OK' ? 'success' : 'danger'" size="small">
              {{ row.STATUS === 'OK' ? $t('jobSla.statusOk') : $t('jobSla.statusBreached') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!execLoading && !executions.length" :description="$t('common.noData')" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="var(--rx-form-label-width-wide)">
        <el-form-item :label="$t('jobSla.jobName')" prop="jobName">
          <el-input v-model="form.jobName" :placeholder="$t('jobSla.jobNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.scheduleName')" prop="scheduleName">
          <el-input v-model="form.scheduleName" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.expectedSec')" prop="expectedDurationSec">
          <el-input-number v-model="form.expectedDurationSec" :min="1" :max="86400" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.deviation')">
          <el-input-number v-model="form.deviationPercent" :min="0" :max="100" />
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
defineOptions({ name: 'JobSla' })
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  createSlaRule,
  deleteSlaRule,
  fetchSlaExecutions,
  fetchSlaRules,
  updateSlaRule,
  type JobSla,
  type SlaExecution,
} from '@/api/jobSla'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()

const slaRules = ref<JobSla[]>([])
const executions = ref<SlaExecution[]>([])
const loading = ref(false)
const execLoading = ref(false)

type SlaForm = { id?: number } & Omit<JobSla, 'id'>

const {
  dialogVisible,
  dialogTitle,
  loading: saving,
  formRef,
  form,
  rules: formRules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<SlaForm>({
  defaultForm: () => ({
    jobName: '',
    scheduleName: '',
    expectedDurationSec: 600,
    deviationPercent: 20,
    enabled: true,
  }),
  rules: {
    jobName: [{ required: true, message: () => t('jobSla.jobNameRequired'), trigger: 'blur' }],
    expectedDurationSec: [{ required: true, message: () => t('common.required'), trigger: 'change' }],
  },
  saveApi: async (isEdit, data) => {
    if (isEdit && data.id) await updateSlaRule(data.id, data)
    else await createSlaRule(data)
  },
  onSuccess: () => loadRules(),
  i18nPrefix: 'jobSla',
})

const loadRules = async () => {
  loading.value = true
  try {
    slaRules.value = await fetchSlaRules()
  } finally {
    loading.value = false
  }
}

const loadExecutions = async () => {
  execLoading.value = true
  try {
    executions.value = await fetchSlaExecutions()
  } finally {
    execLoading.value = false
  }
}

const loadAll = () => {
  loadRules()
  loadExecutions()
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: JobSla) => deleteSlaRule(row.id!),
  onSuccess: loadRules,
  confirmMessage: 'jobSla.deleteConfirm',
  confirmTitle: 'common.warning',
})

const toggle = async (row: JobSla) => {
  try {
    await updateSlaRule(row.id!, { ...row, enabled: !row.enabled })
    row.enabled = !row.enabled
    ElMessage.success(t('jobSla.toggled'))
  } catch {
    ElMessage.error(t('common.requestFailed'))
  }
}

onMounted(loadAll)
</script>

<style scoped>
/* 低-10：mt16/mb8 已收敛至 src/styles/common.css 全局工具类，删除 scoped 重复定义 */
</style>
