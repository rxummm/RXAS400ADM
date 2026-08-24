<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        :placeholder="$t('notice.title')"
        clearable
        class="w-220"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="status" :placeholder="$t('notice.status')" clearable class="w-130">
        <el-option :label="$t('notice.published')" :value="1" />
        <el-option :label="$t('notice.unpublished')" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon> {{ $t('common.search') }}
      </el-button>
      <el-button @click="resetSearch">{{ $t('common.reset') }}</el-button>
      <div class="flex-1" />
      <el-button type="primary" @click="() => openCreate()">
        <el-icon><Plus /></el-icon> {{ $t('notice.add') }}
      </el-button>
    </div>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border stripe class="w-full">
        <el-table-column prop="title" :label="$t('notice.title')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" :label="$t('notice.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? $t('notice.published') : $t('notice.unpublished') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" :label="$t('notice.publisher')" width="110" />
        <el-table-column prop="publishedTime" :label="$t('notice.time')" width="170">
          <template #default="{ row }">{{ formatTime(row.publishedTime || row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">
              {{ $t('common.edit') }}
            </el-button>
            <el-button link type="primary" size="small" @click="onPreview(row as Notice)">
              {{ $t('docs.preview') }}
            </el-button>
            <el-button link type="danger" size="small" @click="onDelete(row as Notice)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination :total="total" v-model:current="current" v-model:size="size"
        @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="70px">
        <el-form-item :label="$t('notice.title')" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item :label="$t('notice.content')" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item :label="$t('notice.status')">
          <el-switch
            :model-value="form.status === 1"
            :active-text="$t('notice.published')"
            :inactive-text="$t('notice.unpublished')"
            inline-prompt
            @change="(val: string | number | boolean) => (form.status = val ? 1 : 0)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="onSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="560px">
      <pre class="notice-preview">{{ previewContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNotices, createNotice, updateNotice, deleteNotice, type Notice } from '@/api/notice'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { useFormDialog } from '@/composables/useFormDialog'

defineOptions({ name: 'Notice' })

const { t } = useI18n()

const status = ref<number>()

const {
  pagedData,
  keyword,
  loading,
  total,
  current,
  size,
  handleSearch,
  resetSearch,
  handlePageChange,
  handleSizeChange,
  handleRefresh,
} = useSmartQueryTable<Notice>({
  fetchApi: (params) => listNotices({ ...params, status: status.value }),
  defaultSize: 10,
  buildParams: (base) => ({ ...base, status: status.value }),
})

interface NoticeForm { id?: number; title: string; content: string; status: number }

const {
  dialogVisible,
  dialogTitle,
  loading: submitLoading,
  formRef,
  form,
  rules: formRules,
  openCreate,
  openEdit,
  onSubmit,
} = useFormDialog<NoticeForm>({
  defaultForm: () => ({ id: undefined, title: '', content: '', status: 1 }),
  rules: {
    title: [{ required: true, message: () => t('notice.titleRequired'), trigger: 'blur' }],
    content: [{ required: true, message: () => t('notice.contentRequired'), trigger: 'blur' }],
  },
  createApi: (data) => createNotice(data),
  updateApi: (id, data) => updateNotice(Number(id), data),
  onSuccess: () => handleRefresh(),
  i18nPrefix: 'notice',
})

const previewVisible = ref(false)
const previewTitle = ref('')
const previewContent = ref('')

function formatTime(time?: string) {
  return time ? time.replace('T', ' ').slice(0, 19) : '-'
}

function onPreview(row: Notice) {
  previewTitle.value = row.title
  previewContent.value = row.content
  previewVisible.value = true
}

async function onDelete(row: Notice) {
  try {
    await ElMessageBox.confirm(t('notice.deleteConfirm'), t('common.tip'), { type: 'warning' })
    if (row.id) await deleteNotice(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    handleRefresh()
  } catch {
    /* cancelled */
  }
}
</script>

<style scoped>
.notice-preview {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
</style>