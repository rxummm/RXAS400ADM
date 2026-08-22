<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button v-has-perm="'SLA_MANAGE'" type="success" :icon="Plus" @click="openDialog()">
        {{ $t('jobSla.addRule') }}
      </el-button>
      <el-button :icon="Refresh" @click="loadAll">{{ $t('common.refresh') }}</el-button>
      <span class="hint ml8">{{ $t('jobSla.hint') }}</span>
    </div>

    <div class="table-wrapper">
      <div class="section">{{ $t('jobSla.rules') }}</div>
      <RxSkeleton type="table" :rows="5" :loading="loading">
        <el-table :data="rules" size="small" border>
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
              @change="toggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'SLA_MANAGE'" size="small" type="primary" plain :icon="Edit" @click="openDialogWrap(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'SLA_MANAGE'" size="small" type="danger" plain :icon="Delete" @click="handleDelete(row)">
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

    <el-dialog v-model="dialogVisible" :title="editing ? $t('jobSla.editTitle') : $t('jobSla.addRule')" width="460px">
      <el-form :model="form" label-width="120px">
        <el-form-item :label="$t('jobSla.jobName')" required>
          <el-input v-model="form.jobName" :placeholder="$t('jobSla.jobNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.scheduleName')">
          <el-input v-model="form.scheduleName" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.expectedSec')" required>
          <el-input-number v-model="form.expectedDurationSec" :min="1" :max="86400" />
        </el-form-item>
        <el-form-item :label="$t('jobSla.deviation')">
          <el-input-number v-model="form.deviationPercent" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

// keep-alive 缓存标识，需与路由 name 一致
//noinspection JSUnusedGlobalSymbols
defineOptions({ name: 'JobSla' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
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

const rules = ref<JobSla[]>([])
const executions = ref<SlaExecution[]>([])
const loading = ref(false)
const execLoading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref(0)
const form = reactive<JobSla>({
  jobName: '',
  scheduleName: '',
  expectedDurationSec: 600,
  deviationPercent: 20,
  enabled: true,
})

const loadRules = async () => {
  loading.value = true
  try {
    rules.value = await fetchSlaRules()
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

const openDialog = (row?: JobSla) => {
  editing.value = !!row
  form.jobName = row?.jobName || ''
  form.scheduleName = row?.scheduleName || ''
  form.expectedDurationSec = row?.expectedDurationSec || 600
  form.deviationPercent = row?.deviationPercent ?? 20
  form.enabled = row?.enabled ?? true
  dialogVisible.value = true
}

/** 编辑入口：记录当前编辑的规则 id */
const openDialogWrap = (row?: JobSla) => {
  editingId.value = row?.id || 0
  openDialog(row)
}

const handleSave = async () => {
  if (!form.jobName.trim()) {
    ElMessage.warning(t('jobSla.jobNameRequired'))
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateSlaRule(editingId.value, { ...form })
      ElMessage.success(t('common.updated'))
    } else {
      await createSlaRule({ ...form })
      ElMessage.success(t('common.created'))
    }
    dialogVisible.value = false
    loadRules()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row: JobSla) => {
  await ElMessageBox.confirm(t('jobSla.deleteConfirm', { name: row.jobName }), t('common.warning'), {
    type: 'warning',
  })
  await deleteSlaRule(row.id!)
  ElMessage.success(t('common.deleted'))
  loadRules()
}

const toggle = async (row: JobSla) => {
  await updateSlaRule(row.id!, { ...row, enabled: !row.enabled })
  row.enabled = !row.enabled
  ElMessage.success(t('jobSla.toggled'))
}

onMounted(loadAll)
</script>

<style scoped>
.mt16 {
  margin-top: 16px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>