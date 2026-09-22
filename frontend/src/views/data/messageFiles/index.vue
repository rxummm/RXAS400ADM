<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('messageFiles.keyword')"
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
        <el-table :data="currentPageData" size="small" border>
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
        <el-table-column :label="$t('common.operation')" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-has-perm="'MSGF_EDIT'" size="small" type="primary" plain :icon="Edit" @click="openEditMsg(row as MessageFileRow)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button v-has-perm="'MSGF_DELETE'" size="small" type="danger" plain :icon="Delete" :loading="removeLoading === row.MESSAGE_ID" @click="confirmRemove(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="loadMessages" @size-change="loadMessages" />
      <el-empty v-if="!loading && !currentPageData.length" :description="$t('messageFiles.empty')" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="var(--rx-dialog-sm)" :close-on-click-modal="false">
      <el-form :model="form" label-width="var(--rx-form-label-width-wide)">
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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useFormDialog } from '@/composables/useFormDialog'
import QueryBar from '@/components/QueryBar.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import AppPagination from '@/components/AppPagination.vue'
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

const loading = ref(false)
const allMessages = ref<MessageFileRow[]>([])
const keyword = ref('')
const current = ref(1)
const size = ref(20)
const total = ref(0)

const currentPageData = computed(() => {
  let data = allMessages.value
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    data = data.filter(row =>
      row.MESSAGE_ID.toLowerCase().includes(kw) ||
      row.MESSAGE_TEXT.toLowerCase().includes(kw) ||
      (row.SECOND_LEVEL_TEXT || '').toLowerCase().includes(kw)
    )
  }
  total.value = data.length
  const start = (current.value - 1) * size.value
  return data.slice(start, start + size.value)
})

const loadMessages = async () => {
  if (!file.value || !library.value.trim()) return
  loading.value = true
  try {
    const result = await fetchMessages(library.value.trim(), file.value.trim())
    allMessages.value = result.records || []
    total.value = result.total
    current.value = 1
  } finally {
    loading.value = false
  }
}

const onFileChange = () => {
  if (file.value) loadMessages()
  else allMessages.value = []
}

const handleRefresh = () => {
  loadFiles()
  if (file.value) loadMessages()
}

const resetSearch = () => {
  keyword.value = ''
  loadFiles()
  if (file.value) loadMessages()
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
      void loadMessages()
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

const { removeLoading, confirmRemove } = useConfirmDelete({
  deleteApi: (row: MessageFileRow) => deleteMessage(library.value.trim(), file.value!.trim(), row.MESSAGE_ID),
  onSuccess: () => void loadMessages(),
  confirmMessage: 'messageFiles.deleteConfirm',
  confirmTitle: 'common.warning',
  idField: 'MESSAGE_ID',
})

onMounted(loadFiles)
</script>

<style scoped>
/* 通用样式收敛至 src/styles/common.css */
</style>
