<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input v-model="keyword" :placeholder="$t('common.keyword')" clearable class="w-200" @keyup.enter="forceSearch" />
      <el-button type="primary" @click="forceSearch">{{ $t('common.search') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="openCreate">{{ $t('opTemplate.create') }}</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="tableData" size="small" border v-loading="loading">
        <el-table-column prop="name" :label="$t('opTemplate.name')" min-width="150" />
        <el-table-column prop="description" :label="$t('opTemplate.description')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('opTemplate.stepCount')" width="100" align="center">
          <template #default="{ row }">
            {{ getStepCount(row.steps) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('opTemplate.createdAt')" width="170" />
        <el-table-column :label="$t('common.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row as OpTemplate)">{{ $t('common.edit') }}</el-button>
            <el-button v-has-perm="'SCRIPT_MANAGE'" size="small" type="primary" plain @click="openExecute(row as OpTemplate)">{{ $t('opTemplate.execute') }}</el-button>
            <el-button v-has-perm="'SCRIPT_MANAGE'" size="small" type="danger" plain :loading="removeLoading === row.id" @click="confirmRemove(row)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination
        :total="total"
        v-model:current="current"
        v-model:size="size"
        @change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('opTemplate.edit') : $t('opTemplate.create')" width="var(--rx-dialog-md)" :close-on-click-modal="false">
      <el-form :model="form" label-width="var(--rx-form-label-width)">
        <el-form-item :label="$t('opTemplate.name')" required>
          <el-input v-model="form.name" :placeholder="$t('opTemplate.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('opTemplate.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="$t('opTemplate.descriptionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('opTemplate.steps')" required>
          <div class="steps-container">
            <div v-for="(step, index) in parsedSteps" :key="index" class="step-item">
              <el-input v-model="step.command" :placeholder="$t('opTemplate.commandPlaceholder')" class="flex-1" />
              <el-button type="danger" :icon="Delete" circle size="small" @click="removeStep(index)" />
            </div>
            <el-button type="primary" plain size="small" @click="addStep">{{ $t('opTemplate.addStep') }}</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 执行弹窗 -->
    <el-dialog v-model="executeVisible" :title="$t('opTemplate.execute')" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
      <el-form label-width="var(--rx-form-label-width)">
        <el-form-item :label="$t('scripts.selectServer')">
          <el-select v-model="executeServerId" :placeholder="$t('scripts.selectServer')" class="w-full">
            <el-option v-for="s in servers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="executing" @click="handleExecute">{{ $t('common.execute') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import AppPagination from '@/components/AppPagination.vue'
import {
  listOpTemplates,
  createOpTemplate,
  updateOpTemplate,
  deleteOpTemplate,
  executeOpTemplate,
  type OpTemplate,
  type OpTemplateStep,
} from '@/api/opTemplate'
import { fetchSystems, type IbmiSystem } from '@/api/as400'

const { t } = useI18n()

const saving = ref(false)
const executing = ref(false)
const servers = ref<IbmiSystem[]>([])

const {
  tableData,
  keyword,
  loading,
  current,
  size,
  total,
  forceSearch,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<OpTemplate>({
  fetchApi: (params) => listOpTemplates({
    current: params.current ?? 1,
    size: params.size ?? 20,
    keyword: params.keyword,
  }),
  autoFetch: false,
  defaultSize: 20,
})

// 弹窗状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', description: '', steps: '[]' })
const parsedSteps = ref<OpTemplateStep[]>([])

// 执行弹窗
const executeVisible = ref(false)
const executeTemplateId = ref<number | null>(null)
const executeServerId = ref<number | undefined>()

function getStepCount(steps: string): number {
  try {
    const arr = JSON.parse(steps)
    return Array.isArray(arr) ? arr.length : 0
  } catch {
    return 0
  }
}

async function loadServers() {
  try {
    servers.value = await fetchSystems()
  } catch {
    /* ignored */
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  form.value = { name: '', description: '', steps: '[]' }
  parsedSteps.value = []
  dialogVisible.value = true
}

function openEdit(row: OpTemplate) {
  isEdit.value = true
  editingId.value = row.id
  form.value = { name: row.name, description: row.description || '', steps: row.steps }
  try {
    parsedSteps.value = JSON.parse(row.steps) || []
  } catch {
    parsedSteps.value = []
  }
  dialogVisible.value = true
}

function addStep() {
  parsedSteps.value.push({ type: 'command', command: '' })
}

function removeStep(index: number) {
  parsedSteps.value.splice(index, 1)
}

async function handleSave() {
  if (!form.value.name) {
    ElMessage.warning(t('opTemplate.nameRequired'))
    return
  }
  if (parsedSteps.value.length === 0) {
    ElMessage.warning(t('opTemplate.stepsRequired'))
    return
  }
  form.value.steps = JSON.stringify(parsedSteps.value)
  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await updateOpTemplate(editingId.value, form.value)
    } else {
      await createOpTemplate(form.value)
    }
    ElMessage.success(t('common.save') + ' OK')
    dialogVisible.value = false
    forceSearch()
  } catch {
    ElMessage.error(t('common.requestFailed'))
  } finally {
    saving.value = false
  }
}

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: OpTemplate) => deleteOpTemplate(row.id),
  onSuccess: forceSearch,
  confirmMessage: 'opTemplate.deleteConfirm',
})

function openExecute(row: OpTemplate) {
  executeTemplateId.value = row.id
  executeServerId.value = servers.value[0]?.id
  executeVisible.value = true
}

async function handleExecute() {
  if (!executeTemplateId.value || !executeServerId.value) {
    ElMessage.warning(t('scripts.selectServerFirst'))
    return
  }
  executing.value = true
  try {
    await executeOpTemplate(executeTemplateId.value, executeServerId.value)
    ElMessage.success(t('opTemplate.executeSuccess'))
    executeVisible.value = false
  } catch {
    ElMessage.error(t('opTemplate.executeFailed'))
  } finally {
    executing.value = false
  }
}

onMounted(() => {
  forceSearch()
  loadServers()
})
</script>

<style scoped>
.steps-container {
  width: 100%;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
