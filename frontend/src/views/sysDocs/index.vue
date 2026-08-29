<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('sysDoc.keyword')"
      :from-cache="isFromCache"
      :flash-tick="tick"
      :keyword-width="240"
      @force-search="load"
      @reset="resetSearch"
    >
      <template #right>
        <el-select v-model="filterStatus" clearable size="small" class="w-120" @change="load">
          <el-option :label="$t('sysDoc.stDraft')" value="DRAFT" />
          <el-option :label="$t('sysDoc.stPublished')" value="PUBLISHED" />
        </el-select>
        <el-select v-model="filterCategory" clearable size="small" class="w-120" :placeholder="$t('sysDoc.category')" @change="load">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button v-has-perm="'SYS_DOC_MANAGE'" type="primary" size="small" @click="openCreate">{{ $t('sysDoc.create') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border>
          <el-table-column prop="title" :label="$t('docs.docTitle')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" :label="$t('sysDoc.category')" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.category" size="small">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tags" :label="$t('sysDoc.tags')" width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="row.tags">
                <el-tag v-for="tag in row.tags.split(',')" :key="tag" size="small" type="info" class="mr4">{{ tag.trim() }}</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('sysDoc.status')" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
                {{ row.status === 'PUBLISHED' ? $t('sysDoc.stPublished') : $t('sysDoc.stDraft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdBy" :label="$t('sysDoc.author')" width="100" />
          <el-table-column prop="updatedTime" :label="$t('sysDoc.updated')" width="170" />
          <el-table-column :label="$t('common.operation')" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-has-perm="'SYS_DOC_MANAGE'" size="small" type="primary" plain @click="openEdit(row as SysDocItem)">{{ $t('sysDoc.edit') }}</el-button>
              <el-button v-has-perm="'SYS_DOC_MANAGE'" size="small" type="danger" plain @click="handleDelete(row as SysDocItem)">{{ $t('sysDoc.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </RxSkeleton>
      <AppPagination v-model:current="current" v-model:size="size" :total="total" @change="load" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('sysDoc.edit') : $t('sysDoc.create')" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item :label="$t('docs.docTitle')" prop="title">
          <el-input v-model="form.title" :placeholder="$t('sysDoc.titlePlaceholder')" maxlength="200" />
        </el-form-item>
        <el-form-item :label="$t('sysDoc.category')">
          <el-input v-model="form.category" :placeholder="$t('sysDoc.categoryPlaceholder')" maxlength="50" />
        </el-form-item>
        <el-form-item :label="$t('sysDoc.tags')">
          <el-input v-model="form.tags" :placeholder="$t('sysDoc.tagsPlaceholder')" maxlength="500" />
        </el-form-item>
        <el-form-item :label="$t('sysDoc.status')" v-if="isEdit">
          <el-switch v-model="form.status" active-value="PUBLISHED" inactive-value="DRAFT" :active-text="$t('sysDoc.stPublished')" :inactive-text="$t('sysDoc.stDraft')" />
        </el-form-item>
        <el-form-item :label="$t('sysDoc.content')" prop="content">
          <MarkdownEditor v-model="form.content" :height="400" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import QueryBar from '@/components/QueryBar.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import AppPagination from '@/components/AppPagination.vue'
import MarkdownEditor from '@/views/docs/MarkdownEditor.vue'
import {
  listSysDocs,
  createSysDoc,
  updateSysDoc,
  deleteSysDoc,
  type SysDocItem,
} from '@/api/sysDoc'

const { t } = useI18n()

const keyword = ref('')
const filterStatus = ref('')
const filterCategory = ref('')
const categories = ref<string[]>([])

const {
  records: rows, total, current, size, loading, dataSourceTick: tick, isFromCache,
  handleRefresh: rawLoad, resetSearch,
} = useSmartQueryTable<SysDocItem>({
  fetchApi: (params) =>
    listSysDocs({
      current: params.current,
      size: params.size,
      keyword: params.keyword || undefined,
      status: filterStatus.value || undefined,
      category: filterCategory.value || undefined,
    }),
})

const load = () => rawLoad()

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  content: '',
  category: '',
  tags: '',
  status: 'DRAFT' as 'DRAFT' | 'PUBLISHED',
})

const rules: FormRules = {
  title: [{ required: true, message: () => t('sysDoc.titleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: () => t('sysDoc.contentRequired'), trigger: 'blur' }],
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.title = ''
  form.content = ''
  form.category = ''
  form.tags = ''
  form.status = 'DRAFT'
  dialogVisible.value = true
}

function openEdit(row: SysDocItem) {
  isEdit.value = true
  editId.value = row.id
  form.title = row.title
  form.content = row.content || ''
  form.category = row.category || ''
  form.tags = row.tags || ''
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      title: form.title,
      content: form.content,
      category: form.category || undefined,
      tags: form.tags || undefined,
      status: form.status,
    }
    if (isEdit.value && editId.value) {
      await updateSysDoc(editId.value, data)
      ElMessage.success(t('common.updated'))
    } else {
      await createSysDoc(data)
      ElMessage.success(t('common.created'))
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysDocItem) {
  await ElMessageBox.confirm(t('sysDoc.deleteConfirm', { title: row.title }), t('common.warning'), {
    type: 'warning',
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
  })
  await deleteSysDoc(row.id)
  ElMessage.success(t('common.deleted'))
  load()
}
</script>
