<template>
  <div class="page-container page-container--fit">
    <QueryBar
      v-model:keyword="keyword"
      :placeholder="$t('common.keyword')"
      :from-cache="isFromCache"
      :flash-tick="dataSourceTick"
      :keyword-width="180"
      @force-search="handleRefresh"
      @reset="resetSearch"
    >
      <el-button :icon="Back" @click="goUp" :disabled="!canGoUp()">{{ $t('ifs.up') }}</el-button>
      <el-input v-model="path" :placeholder="$t('ifs.pathPlaceholder')" clearable class="flex-1" @keyup.enter="handleRefresh" />
      <template #right>
        <el-button :icon="Delete" @click="goTrash">{{ $t('ifs.trash') }}</el-button>
        <el-button v-has-perm="'IFS_MANAGE'" :icon="FolderAdd" @click="openMkdir">{{ $t('ifs.mkdir') }}</el-button>
        <el-button v-has-perm="'IFS_MANAGE'" type="primary" :icon="Upload" @click="openUpload">{{ $t('ifs.upload') }}</el-button>
      </template>
    </QueryBar>

    <div class="table-wrapper">
      <RxSkeleton type="table" :rows="8" :loading="loading">
        <el-table :data="pagedData" size="small" border>
        <el-table-column width="50">
          <template #default="{ row }: { row: IfsEntry }">
            <el-icon><Folder v-if="row.TYPE === 'DIR'" /><Document v-else /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="NAME" :label="$t('ifs.name')" min-width="200">
          <template #default="{ row }: { row: IfsEntry }">
            <el-link v-if="row.TYPE === 'DIR'" type="primary" @click="enter(row)">{{ row.NAME }}</el-link>
            <span v-else>{{ row.NAME }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="PATH" :label="$t('ifs.path')" min-width="200" show-overflow-tooltip />
        <el-table-column :label="$t('ifs.type')" width="90">
          <template #default="{ row }: { row: IfsEntry }">
            <el-tag :type="row.TYPE === 'DIR' ? 'warning' : 'info'" size="small">{{ row.TYPE }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="inTrash" :label="$t('ifs.state')" width="100" align="center">
          <template #default>
            <el-tag type="danger" size="small">{{ $t('ifs.deleted') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ifs.size')" width="120">
          <template #default="{ row }: { row: IfsEntry }">{{ row.TYPE === 'DIR' ? '-' : formatSize(row.SIZE) }}</template>
        </el-table-column>
        <el-table-column prop="MODIFIED" :label="$t('ifs.modified')" width="170" />
        <el-table-column :label="$t('common.operation')" width="240" fixed="right">
          <template #default="{ row }: { row: IfsEntry }">
            <el-button v-if="row.TYPE === 'FILE'" size="small" type="primary" plain @click="openFile(row)">
              {{ $t('ifs.view') }}
            </el-button>
            <el-button v-if="row.TYPE === 'FILE'" v-has-perm="'IFS_MANAGE'" size="small" plain :icon="Download" @click="downloadFile(row)">
              {{ $t('ifs.download') }}
            </el-button>
            <el-button v-if="inTrash" v-has-perm="'IFS_MANAGE'" size="small" type="success" plain :icon="RefreshLeft" @click="restoreRow(row)">
              {{ $t('ifs.restore') }}
            </el-button>
            <el-button v-else v-has-perm="'IFS_MANAGE'" size="small" type="danger" plain :icon="Delete" @click="deleteRow(row)">
              {{ $t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      </RxSkeleton>
      <el-empty v-if="!loading && filteredData.length === 0" :description="$t('ifs.empty')" />

      <AppPagination :total="total" v-model:current="current" v-model:size="size" @change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="fileVisible" :title="currentFile" width="640px">
      <pre class="file-content">{{ fileContent || $t('ifs.emptyContent') }}</pre>
    </el-dialog>

    <!-- 上传文件到当前目录 -->
    <el-dialog v-model="uploadVisible" :title="$t('ifs.upload')" width="480px">
      <el-form label-width="90px">
        <el-form-item :label="$t('ifs.uploadTarget')">{{ uploadTargetDir }}</el-form-item>
        <el-form-item :label="$t('ifs.selectFile')" required>
          <input type="file" @change="onFileChange" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!selectedFile" @click="confirmUpload">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新建目录 -->
    <el-dialog v-model="mkdirVisible" :title="$t('ifs.mkdir')" width="440px">
      <el-form label-width="90px">
        <el-form-item :label="$t('ifs.mkdirTarget')">{{ mkdirBaseDir }}</el-form-item>
        <el-form-item :label="$t('ifs.mkdirName')" required>
          <el-input v-model="mkdirName" class="w-full" :placeholder="$t('ifs.mkdirNamePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mkdirVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="mkdirLoading" :disabled="!mkdirName.trim()" @click="confirmMkdir">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Back, Delete, Document, Download, Folder, FolderAdd, RefreshLeft, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QueryBar from '@/components/QueryBar.vue'
import { useI18n } from 'vue-i18n'
import { deleteIfsFile, downloadIfsFile, listIfsDir, mkdirIfs, readIfsFile, restoreIfsFile, uploadIfsFile, type IfsEntry } from '@/api/ifs'
import { triggerBlobDownload } from '@/api/blobClient'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { formatSize } from '@/utils/format'
import AppPagination from '@/components/AppPagination.vue'
import RxSkeleton from '@/components/RxSkeleton.vue'

const { t } = useI18n()
const path = ref('/QOpenSys/rxas400')
const fileVisible = ref(false)
const currentFile = ref('')
const fileContent = ref('')

// 上传 / 新建目录
const uploadVisible = ref(false)
const uploading = ref(false)
const selectedFile = ref<File | null>(null)
const uploadTargetDir = ref('')
const mkdirVisible = ref(false)
const mkdirLoading = ref(false)
const mkdirBaseDir = ref('')
const mkdirName = ref('')

const {
  filteredData,
  pagedData,
  loading,
  keyword,
  current,
  size,
  total,
  isFromCache,
  dataSourceTick,
  resetSearch,
  handleRefresh,
  handlePageChange,
  handleSizeChange,
  fetchData,
} = useSmartQueryTable<IfsEntry>({
  fetchApi: (params) => listIfsDir(params.path as string),
  frontendPage: true,
  enableCache: true,
  searchFields: ['NAME', 'PATH', 'MODIFIED'],
  buildParams: () => ({ path: path.value }),
})

const canGoUp = () => path.value !== '/' && path.value !== ''

// 已删除文档历史目录（与后端 TRASH_ROOT 一致）
const TRASH_ROOT = '/QOpenSys/rxas400/temp/as400histdocs'

// 当前是否浏览已删除文档历史目录（显示「已删除」状态与「恢复」操作）
const inTrash = computed(() =>
  path.value.toLowerCase().startsWith(TRASH_ROOT.toLowerCase()) || path.value.includes('/temp/as400histdocs/'))

const goTrash = () => {
  path.value = TRASH_ROOT
  current.value = 1
  void fetchData({}, true)
}

const enter = (row: { PATH?: string }) => {
  if (row.PATH) path.value = row.PATH
  current.value = 1
  void fetchData({}, true)
}

const goUp = () => {
  if (!path.value || path.value === '/') return
  const idx = path.value.lastIndexOf('/')
  path.value = idx <= 0 ? '/' : path.value.substring(0, idx)
  current.value = 1
  void fetchData({}, true)
}

const openFile = async (row: { PATH?: string }) => {
  if (!row.PATH) return
  currentFile.value = row.PATH
  fileVisible.value = true
  fileContent.value = ''
  try {
    const result: { content?: string } = await readIfsFile(row.PATH)
    fileContent.value = result.content || ''
  } catch {
    fileContent.value = ''
    ElMessage.error(t('common.loadFailed'))
  }
}

// ---- IFS_MANAGE 文件管理操作 ----

const openUpload = () => {
  selectedFile.value = null
  uploadTargetDir.value = path.value || '/'
  uploadVisible.value = true
}

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  selectedFile.value = input.files?.[0] || null
}

const confirmUpload = async () => {
  if (!selectedFile.value) return
  const base = (uploadTargetDir.value || '/').replace(/\/+$/, '')
  const target = `${base}/${selectedFile.value.name}`
  uploading.value = true
  try {
    await uploadIfsFile(target, selectedFile.value)
    uploadVisible.value = false
    ElMessage.success(t('ifs.uploaded'))
    void fetchData({}, true)
  } catch {
    /* 拦截器已提示 */
  } finally {
    uploading.value = false
  }
}

const openMkdir = () => {
  mkdirName.value = ''
  mkdirBaseDir.value = path.value || '/'
  mkdirVisible.value = true
}

const confirmMkdir = async () => {
  const name = mkdirName.value.trim().replace(/[\\/:*?"<>|\s]+/g, '_')
  if (!name) return
  const base = (mkdirBaseDir.value || '/').replace(/\/+$/, '')
  const target = `${base}/${name}`
  mkdirLoading.value = true
  try {
    await mkdirIfs(target)
    mkdirVisible.value = false
    ElMessage.success(t('ifs.mkdirDone'))
    void fetchData({}, true)
  } catch {
    /* 拦截器已提示 */
  } finally {
    mkdirLoading.value = false
  }
}

const downloadFile = async (row: { PATH?: string; NAME?: string }) => {
  if (!row.PATH) return
  try {
    const blob = await downloadIfsFile(row.PATH)
    triggerBlobDownload(blob, row.NAME || 'download')
  } catch {
    /* 拦截器已提示 */
  }
}

const deleteRow = async (row: { PATH?: string; TYPE?: string; NAME?: string }) => {
  if (!row.PATH) return
  await ElMessageBox.confirm(
    t('ifs.deleteConfirm', { name: row.NAME || row.PATH }),
    t('common.confirm'),
    { type: 'warning' },
  )
  try {
    await deleteIfsFile(row.PATH)
    ElMessage.success(t('ifs.trashed'))
    void fetchData({}, true)
  } catch {
    /* 拦截器已提示 */
  }
}

const restoreRow = async (row: { PATH?: string; NAME?: string }) => {
  if (!row.PATH) return
  try {
    const res = await restoreIfsFile(row.PATH)
    ElMessage.success(`${t('ifs.restored')}: ${res.path}`)
    void fetchData({}, true)
  } catch {
    /* 拦截器已提示 */
  }
}

// 首次进入自动加载（autoFetch 已关闭，手动触发以带上 path 参数缓存隔离）
onMounted(() => {
  void fetchData()
})
</script>

<style scoped>
.file-content {
  margin: 0;
  padding: 12px;
  background: var(--bg-hover);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 480px;
  overflow: auto;
}
</style>