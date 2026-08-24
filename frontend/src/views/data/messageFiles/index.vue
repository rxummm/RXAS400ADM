<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('messageFiles.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-input
        v-model="library"
        :placeholder="$t('messageFiles.library')"
        clearable
        class="w-160"
        @change="loadFiles"
        @keyup.enter="loadFiles"
      />
      <el-select
        v-model="file"
        :placeholder="$t('messageFiles.selectFile')"
        filterable
        allow-create
        clearable
        class="w-240"
        :loading="fileLoading"
        @change="onFileChange"
      >
        <el-option
          v-for="f in fileOptions"
          :key="f.MESSAGE_FILE_NAME"
          :label="$t('messageFiles.optionLabel', { name: f.MESSAGE_FILE_NAME, lib: f.MESSAGE_FILE_LIBRARY, count: f.NUMBER_OF_MESSAGES })"
          :value="f.MESSAGE_FILE_NAME"
        />
      </el-select>
      <template #right>
        <el-button v-has-perm="'MSGF_ADD'" type="success" :icon="Plus" @click="openCreate()">
          {{ $t('messageFiles.add') }}
        </el-button>
        <span class="hint">{{ $t('messageFiles.hint') }}</span>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="filteredRows" size="small" border>
        <el-table-column prop="MESSAGE_ID" :label="$t('messageFiles.id')" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.MESSAGE_ID }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="MESSAGE_TEXT" :label="$t('messageFiles.text')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="SECOND_LEVEL_TEXT" :label="$t('messageFiles.secondLevel')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.SECOND_LEVEL_TEXT || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('messageFiles.severity')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="severityTag(Number(row.SEVERITY))" size="small">{{ row.SEVERITY }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'MSGF_EDIT'" size="small" type="primary" plain :icon="Edit" @click="openEditMsg(row as MessageFileRow)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'MSGF_DELETE'" size="small" type="danger" plain :icon="Delete" @click="handleDelete(row as MessageFileRow)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && !filteredRows.length" :description="$t('messageFiles.empty')" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="$t('messageFiles.library')">
          <el-input v-model="form.library" :disabled="isEdit" :placeholder="$t('messageFiles.library')" />
        </el-form-item>
        <el-form-item :label="$t('messageFiles.file')">
          <el-input v-model="form.file" :disabled="isEdit" :placeholder="$t('messageFiles.file')" />
        </el-form-item>
        <el-form-item :label="$t('messageFiles.id')">
          <el-input v-model="form.id" :disabled="isEdit" maxlength="7" :placeholder="$t('messageFiles.idPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('messageFiles.text')" required>
          <el-input v-model="form.text" type="textarea" :rows="2" maxlength="132" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('messageFiles.secondLevel')">
          <el-input v-model="form.secondLevel" type="textarea" :rows="2" maxlength="2000" />
        </el-form-item>
        <el-form-item :label="$t('messageFiles.severity')">
          <el-input-number v-model="form.severity" :min="0" :max="99" />
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
defineOptions({ name: 'MessageFiles' })
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'
import QueryBar from '@/components/QueryBar.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import {
  addMessage,
  deleteMessage,
  fetchMessageFiles,
  fetchMessages,
  updateMessage,
  type MessageFileRow,
  type MessageFileSummary,
  type MessageRequest,
} from '@/api/messageFiles'

const { t } = useI18n()

const library = ref('APP')
const file = ref<string | null>(null)
const fileOptions = ref<MessageFileSummary[]>([])
const fileLoading = ref(false)

const loadFiles = async () => {
  if (!library.value.trim()) {
    fileOptions.value = []
    return
  }
  fileLoading.value = true
  try {
    fileOptions.value = await fetchMessageFiles(library.value.trim())
  } catch {
    fileOptions.value = []
  } finally {
    fileLoading.value = false
  }
}

// 3 分钟查询缓存 + 已加载消息前端实时模糊匹配（关键词不再发后端，缓存 key 仅按库+文件隔离）
const {
  filteredData: filteredRows,
  records,
  loading,
  keyword,
  isFromCache,
  dataSourceTick,
  resetSearch: baseResetSearch,
  fetchData,
} = useSmartQueryTable<MessageFileRow>({
  fetchApi: () => fetchMessages(library.value.trim(), file.value!.trim()),
  enableCache: true,
  autoFetch: false,
  buildParams: () => ({ library: library.value.trim(), file: file.value! }),
  searchFields: ['MESSAGE_ID', 'MESSAGE_TEXT', 'SECOND_LEVEL_TEXT'],
})

const load = () => {
  if (!file.value) {
    ElMessage.warning(t('messageFiles.selectFirst'))
    return
  }
  void fetchData()
}

const onFileChange = () => {
  if (file.value) load()
  else records.value = []
}

const handleRefresh = () => {
  loadFiles()
  void fetchData({}, true)
}

const resetSearch = () => {
  baseResetSearch()
  loadFiles()
  void fetchData({}, true)
}

const severityTag = (sev: number) => {
  if (sev >= 70) return 'danger'
  if (sev >= 40) return 'warning'
  return 'success'
}

// 弹窗状态统一由 useFormDialog 管理
const { dialogVisible, dialogTitle, isEdit, loading: saving, form, openCreate, openEdit, onSubmit } =
  useFormDialog<MessageRequest>({
    defaultForm: () => ({
      library: library.value.trim() || 'APP',
      file: file.value!,
      id: '',
      text: '',
      secondLevel: '',
      severity: 0,
    }),
    validate: false,
    saveApi: async (edit, data) => {
      if (edit) await updateMessage(data)
      else await addMessage(data)
    },
    onSuccess: () => {
      void fetchData({}, true)
    },
    i18nPrefix: 'messageFiles',
  })

const openEditMsg = (row: MessageFileRow) => {
  openEdit({
    library: library.value.trim() || 'APP',
    file: row.MESSAGE_FILE_NAME || file.value!,
    id: row.MESSAGE_ID || '',
    text: row.MESSAGE_TEXT || '',
    secondLevel: row.SECOND_LEVEL_TEXT || '',
    severity: Number(row.SEVERITY) || 0,
  })
}

const handleSave = () => {
  if (!form.value.file || !form.value.text.trim()) {
    ElMessage.warning(t('messageFiles.required'))
    return
  }
  if (!isEdit.value && !form.value.id.trim()) {
    ElMessage.warning(t('messageFiles.idRequired'))
    return
  }
  void onSubmit()
}

const handleDelete = async (row: MessageFileRow) => {
  await ElMessageBox.confirm(t('messageFiles.deleteConfirm', { id: row.MESSAGE_ID }), t('common.warning'), {
    type: 'warning',
  })
  await deleteMessage(library.value.trim(), file.value!.trim(), row.MESSAGE_ID)
  ElMessage.success(t('common.deleted'))
  void fetchData({}, true)
}

onMounted(loadFiles)
</script>

<style scoped>
/* 通用样式收敛至 src/styles/common.css */
</style>