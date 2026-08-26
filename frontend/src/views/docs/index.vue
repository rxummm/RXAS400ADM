<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('docs.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-select v-model="filters.status" :placeholder="$t('docs.status')" clearable class="w-140" @change="handleRefresh">
        <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="value" />
      </el-select>
      <template #right>
        <div class="flex-1" />
        <el-button @click="templatesVisible = true">{{ $t('docs.templateManage') }}</el-button>
        <el-button type="primary" :icon="Plus" @click="() => openCreate()">{{ $t('docs.create') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <el-tabs v-model="viewMode" class="docs-tabs" @tab-change="onViewChange">
        <el-tab-pane :label="$t('docs.normalView')" name="normal" />
        <el-tab-pane :label="$t('docs.deletedView')" name="deleted" />
      </el-tabs>
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="rows" size="small" border>
        <el-table-column prop="title" :label="$t('docs.docTitle')" min-width="200" show-overflow-tooltip />
        <template v-if="viewMode === 'normal'">
          <el-table-column prop="templateName" :label="$t('docs.template')" width="110">
            <template #default="{ row }">{{ row.templateName || '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('docs.docType')" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ $t(docTypeKey(row.docType)) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="version" :label="$t('docs.version')" width="80" align="center" />
          <el-table-column :label="$t('docs.status')" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
        </template>
        <template v-else>
          <el-table-column :label="$t('docs.state')" width="90" align="center">
            <template #default>
              <el-tag type="danger" size="small">{{ $t('docs.deleted') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deletedTime" :label="$t('docs.deletedAt')" width="170" />
        </template>
        <el-table-column prop="createdBy" :label="$t('docs.author')" width="110" />
        <el-table-column v-if="viewMode === 'normal'" prop="updatedTime" :label="$t('docs.updated')" width="170" />
        <el-table-column :label="$t('common.operation')" width="330" fixed="right">
          <template #default="{ row }">
            <template v-if="viewMode === 'normal'">
              <el-button size="small" link type="primary" @click="openDetail(row as DocItem)">{{ $t('docs.view') }}</el-button>
              <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" size="small" link type="primary" @click="openEdit(row as DocItem)">{{ $t('common.edit') }}</el-button>
              <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" size="small" link type="warning" @click="doSubmit(row as DocItem)">{{ $t('docs.submit') }}</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" link type="success" @click="doApprove(row as DocItem)">{{ $t('docs.approve') }}</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" link type="danger" @click="doReject(row as DocItem)">{{ $t('docs.reject') }}</el-button>
              <el-button size="small" link @click="openVersions(row as DocItem)">{{ $t('docs.history') }}</el-button>
              <el-button v-if="row.status !== 'PENDING'" size="small" link type="danger" :loading="removeLoading_doDelete === row.id" @click="doDelete(row as DocItem)">{{ $t('common.delete') }}</el-button>
            </template>
            <template v-else>
              <el-button size="small" link type="primary" @click="openDetail(row as DocItem)">{{ $t('docs.view') }}</el-button>
              <el-button size="small" link type="success" :loading="removeLoading_doRestore === row.id" @click="doRestore(row as DocItem)">{{ $t('docs.restore') }}</el-button>
              <el-button size="small" link type="danger" :loading="removeLoading_doPurge === row.id" @click="doPurge(row as DocItem)">{{ $t('docs.purge') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <AppPagination
        :total="total"
        v-model:current="current"
        v-model:size="size"
        @change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新建/编辑 -->
    <el-dialog v-model="editVisible" :title="editing ? $t('docs.edit') : $t('docs.create')" width="var(--rx-dialog-lg)" :close-on-click-modal="false">
      <el-form label-width="var(--rx-form-label-width)">
        <el-form-item :label="$t('docs.docTitle')" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item :label="$t('docs.template')">
          <el-select v-model="form.templateId" clearable class="w-full" @change="applyTemplate">
            <el-option v-for="tp in templates" :key="tp.id" :label="`${tp.category || '-'} / ${tp.name}`" :value="tp.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('docs.docType')">
          <el-select v-model="form.docType" class="w-full" @change="onTypeChange">
            <el-option v-for="tp in allTypes" :key="tp" :label="$t(docTypeKey(tp))" :value="tp" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isTextType" :label="$t('docs.content')">
          <MarkdownEditor v-if="form.docType === 'MARKDOWN'" v-model="form.content" />
          <el-input v-else v-model="form.content" type="textarea" :rows="12" />
        </el-form-item>
        <el-form-item v-else>
          <div class="hint">{{ $t('docs.fileDocHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ $t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" :title="detailRow?.title" size="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item :label="$t('docs.docType')">{{ $t(docTypeKey(detailRow?.docType)) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.version')">v{{ detailRow?.version }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.status')">{{ statusMap[detailRow?.status || ''] || detailRow?.status }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.author')">{{ detailRow?.createdBy }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.updated')">{{ detailRow?.updatedTime }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.approver')">{{ detailRow?.approvedBy || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('docs.rejectReason')">{{ detailRow?.rejectReason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow?.ifsPath" :label="$t('docs.ifsPath')">{{ detailRow.ifsPath }}</el-descriptions-item>
      </el-descriptions>
      <div class="toolbar">
        <el-button v-has-perm="'DOC_MANAGE'" type="primary" :icon="Upload" @click="ifsUploadVisible = true">
          {{ $t('docs.uploadIfs') }}
        </el-button>
      </div>
      <h4 class="section">{{ $t('docs.content') }}</h4>
      <DocRenderer :content="detailRow?.content || ''" :doc-type="detailRow?.docType" :file-url="fileUrl" />
    </el-drawer>

    <!-- 驳回原因 -->
    <el-dialog v-model="rejectVisible" :title="$t('docs.reject')" width="var(--rx-dialog-xs)" :close-on-click-modal="false">
      <el-input v-model="rejectReason" type="textarea" :rows="3" :placeholder="$t('docs.rejectReason')" />
      <template #footer>
        <el-button @click="rejectVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" @click="confirmReject">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 子组件弹窗：上传 IFS / 版本历史 / 模板管理 -->
    <IfsUploadDialog v-model="ifsUploadVisible" :doc="detailRow" @saved="onIfsSaved" />
    <VersionHistoryDrawer v-model="versionsVisible" :doc-id="versionsDocId" :doc-type="versionsDocType" @changed="reload" />
    <TemplateManageDialog v-model="templatesVisible" @changed="loadTemplates" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import {
  approveDoc, createDoc, deleteDoc, docFile, listDocs, listTemplates, purgeDoc, rejectDoc, restoreDoc,
  submitDoc, updateDoc,
  type DocItem,
  type DocType,
  type TemplateItem,
} from '@/api/doc'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'
import QueryBar from '@/components/QueryBar.vue'
import IfsUploadDialog from './IfsUploadDialog.vue'
import VersionHistoryDrawer from './VersionHistoryDrawer.vue'
import TemplateManageDialog from './TemplateManageDialog.vue'
import DocRenderer from './DocRenderer.vue'
import { defineAsyncComponent } from 'vue'
const MarkdownEditor = defineAsyncComponent(() => import('./MarkdownEditor.vue'))

defineOptions({ name: 'Docs' })

const { t } = useI18n()
// 低-19：computed 惰性求值，切语言后 keep-alive 页文案随响应式更新（原 setup 期一次性快照）
const statusMap = computed<Record<string, string>>(() => ({
  DRAFT: t('docs.stDraft'),
  PENDING: t('docs.stPending'),
  PUBLISHED: t('docs.stPublished'),
  REJECTED: t('docs.stRejected'),
}))
const statusType = (s: string): 'info' | 'warning' | 'success' | 'danger' =>
  ({ DRAFT: 'info', PENDING: 'warning', PUBLISHED: 'success', REJECTED: 'danger' } as Record<string, 'info' | 'warning' | 'success' | 'danger'>)[s] || 'info'

/** 文档可选类型 + i18n key（docTypeMarkdown / docTypeText / ...） */
const allTypes: DocType[] = ['MARKDOWN', 'TEXT', 'HTML', 'PDF', 'IMAGE']
const textTypes = ['MARKDOWN', 'TEXT', 'HTML']
const docTypeKey = (docType?: DocType) =>
  `docs.docType${(docType || 'MARKDOWN').charAt(0) + (docType || 'MARKDOWN').slice(1).toLowerCase()}`

const filters = reactive({ status: null as string | null })
const viewMode = ref<'normal' | 'deleted'>('normal')

// 服务端分页 + 3 分钟查询缓存；关键词驱动已加载记录的实时前端模糊匹配（status 仍走后端过滤）
const {
  pagedData: rows,
  loading,
  keyword,
  current,
  size,
  total,
  isFromCache,
  dataSourceTick,
  fetchData,
  resetSearch,
  handleRefresh,
  handlePageChange,
  handleSizeChange,
} = useSmartQueryTable<DocItem>({
  fetchApi: (params) =>
    listDocs({
      current: params.current,
      size: params.size,
      keyword: params.keyword || undefined,
      status: filters.status || undefined,
      deleted: viewMode.value === 'deleted' ? 1 : 0,
    }),
  frontendPage: false,
  enableCache: true,
  searchFields: ['title', 'templateName', 'createdBy', 'status'],
})

const onViewChange = () => {
  keyword.value = ''
  filters.status = null
  void fetchData({}, true)
}

const editVisible = ref(false)
const editing = ref<DocItem | null>(null)
const form = reactive({
  title: '',
  templateId: undefined as number | undefined,
  content: '',
  docType: 'MARKDOWN' as DocType,
})
const templates = ref<TemplateItem[]>([])

const detailVisible = ref(false)
const detailRow = ref<DocItem | null>(null)
const fileUrl = ref('')
const rejectVisible = ref(false)
const rejectTarget = ref<DocItem | null>(null)
const rejectReason = ref('')

// 子组件弹窗状态
const ifsUploadVisible = ref(false)
const versionsVisible = ref(false)
const versionsDocId = ref<number | null>(null)
const versionsDocType = ref<DocType | undefined>(undefined)
const templatesVisible = ref(false)

const isTextType = computed(() => textTypes.includes(form.docType))

const reload = () => void fetchData({}, true)

const loadTemplates = async () => {
  try {
    templates.value = await listTemplates()
  } catch {
    templates.value = []
  }
}

const openCreate = () => {
  editing.value = null
  form.title = ''
  form.templateId = undefined
  form.content = ''
  form.docType = 'MARKDOWN'
  editVisible.value = true
  loadTemplates()
}

const openEdit = (row: DocItem) => {
  editing.value = row
  form.title = row.title
  form.templateId = row.templateId
  form.docType = row.docType || 'MARKDOWN'
  form.content = textTypes.includes(form.docType) ? row.content : ''
  editVisible.value = true
  loadTemplates()
}

const applyTemplate = (id?: number) => {
  if (!id) return
  const tp = templates.value.find((x) => x.id === id)
  if (tp) {
    if (tp.docType) form.docType = tp.docType
    if (form.docType !== 'MARKDOWN' && !textTypes.includes(form.docType)) {
      form.content = ''
      return
    }
    if (!form.content) {
      form.content = (tp.content || '').replace('${title}', form.title || tp.name)
    }
  }
}

/** 切到文件型（PDF/图片）时清空正文；切回正文型时恢复编辑器 */
const onTypeChange = () => {
  if (!textTypes.includes(form.docType)) {
    form.content = ''
  }
}

const save = async () => {
  if (!form.title.trim()) {
    ElMessage.warning(t('docs.titleRequired'))
    return
  }
  try {
    const payload = { title: form.title, templateId: form.templateId, content: form.content, docType: form.docType }
    if (editing.value) {
      await updateDoc(editing.value.id, payload)
    } else {
      await createDoc(payload)
    }
    editVisible.value = false
    ElMessage.success(t('common.save'))
    reload()
  } catch {
    /* 拦截器已提示 */
  }
}

const openDetail = (row: DocItem) => {
  detailRow.value = row
  detailVisible.value = true
}

/** 文件型文档（PDF/图片）详情打开时按需拉取 IFS 文件生成预览 URL */
watch(detailVisible, async (open) => {
  if (fileUrl.value) {
    URL.revokeObjectURL(fileUrl.value)
    fileUrl.value = ''
  }
  if (!open || !detailRow.value) return
  const docType = detailRow.value.docType
  if ((docType === 'PDF' || docType === 'IMAGE') && detailRow.value.ifsPath) {
    try {
      const blob = await docFile(detailRow.value.id)
      fileUrl.value = URL.createObjectURL(blob)
    } catch {
      /* 文件不可达时 DocRenderer 显示空态 */
    }
  }
})

const onIfsSaved = (path: string) => {
  // 子组件已持久化 ifsPath（V32）；同步到行对象让详情抽屉即时显示
  if (detailRow.value) detailRow.value.ifsPath = path
}

const doSubmit = async (row: DocItem) => {
  await submitDoc(row.id)
  ElMessage.success(t('docs.submitted'))
  reload()
}

const doApprove = async (row: DocItem) => {
  await approveDoc(row.id)
  ElMessage.success(t('docs.approved'))
  reload()
}

const doReject = (row: DocItem) => {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectTarget.value) return
  await rejectDoc(rejectTarget.value.id, rejectReason.value || '-')
  rejectVisible.value = false
  ElMessage.success(t('docs.rejected'))
  reload()
}

const removeLoading_doDelete = ref<number | null>(null)
const doDelete = async (row: DocItem) => {
  try {
    const ifsNote = row.ifsPath ? `\n${t('docs.deleteIfsNote')}: ${row.ifsPath}` : ''
    await ElMessageBox.confirm(t('docs.deleteConfirm') + ifsNote, t('common.confirm'), { type: 'warning' })
  } catch {
    return
  }
  removeLoading_doDelete.value = row.id
  try {
    await deleteDoc(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    reload()
  } finally {
    removeLoading_doDelete.value = null
  }
}

const removeLoading_doRestore = ref<number | null>(null)
const doRestore = async (row: DocItem) => {
  try {
    await ElMessageBox.confirm(t('docs.restoreConfirm'), t('common.confirm'), { type: 'info' })
  } catch {
    return
  }
  removeLoading_doRestore.value = row.id
  try {
    await restoreDoc(row.id)
    ElMessage.success(t('docs.restored'))
    reload()
  } finally {
    removeLoading_doRestore.value = null
  }
}

const removeLoading_doPurge = ref<number | null>(null)
const doPurge = async (row: DocItem) => {
  try {
    await ElMessageBox.confirm(t('docs.purgeConfirm'), t('common.confirm'), { type: 'warning' })
  } catch {
    return
  }
  removeLoading_doPurge.value = row.id
  try {
    await purgeDoc(row.id)
    ElMessage.success(t('docs.purged'))
    reload()
  } finally {
    removeLoading_doPurge.value = null
  }
}

const openVersions = (row: DocItem) => {
  versionsDocId.value = row.id
  versionsDocType.value = row.docType
  versionsVisible.value = true
}


</script>

<style scoped>
/* toolbar/pager/section 已收敛至 src/styles/common.css */

/* 视图切换型 tabs（表格在 tabs 之外，非 job/Users 的包裹式）：
   全局 .page-container--fit .table-wrapper > .el-tabs { flex:1 } 只适用于表格在 tab-pane 内的页面；
   本页 el-table 是 el-tabs 的兄弟节点，须让 tabs 只占头部高度，避免空内容区被 flex 撑开造成大间距 */
.table-wrapper > .el-tabs.docs-tabs,
.table-wrapper > .el-tabs.docs-tabs :deep(.el-tabs__content),
.table-wrapper > .el-tabs.docs-tabs :deep(.el-tab-pane) {
  flex: 0 0 auto;
  min-height: 0;
}
</style>