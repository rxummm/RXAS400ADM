<template>
  <div class="page-container page-container--fit">
    <div class="search-bar">
      <el-button @click="showDefList = true">{{ $t('reports.builder.list') }}</el-button>
      <el-button type="primary" @click="showSaveDialog = true" :disabled="!selectedKeys.length">
        {{ $t('reports.builder.save') }}
      </el-button>
    </div>

    <div class="builder-layout">
      <!-- 左：数据源 + 字段选择 -->
      <div class="builder-sidebar">
        <div class="section">{{ $t('reports.builder.dataSource') }}</div>
        <el-select v-model="currentSource" class="w-full" @change="onSourceChange"
          :placeholder="$t('reports.builder.selectSource')">
          <el-option v-for="ds in dataSources" :key="ds.key" :label="ds.label" :value="ds.key" />
        </el-select>

        <template v-if="currentSourceMeta">
          <div class="section" style="margin-top:16px">{{ $t('reports.builder.availableFields') }}</div>
          <el-checkbox-group v-model="selectedKeys" class="field-list">
            <el-checkbox v-for="f in currentSourceMeta.fields" :key="f.key" :value="f.key"
              :label="f.label" class="field-item" />
          </el-checkbox-group>
        </template>
      </div>

      <!-- 右：筛选 + 排序 + 预览 -->
      <div class="builder-main">
        <!-- 筛选 -->
        <div class="section">{{ $t('reports.builder.filters') }}</div>
        <div v-for="(filter, i) in filters" :key="i" class="filter-row">
          <el-select v-model="filter.field" class="filter-field" :placeholder="$t('reports.builder.field')">
            <el-option v-for="f in currentSourceMeta?.fields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
          <el-select v-model="filter.op" class="filter-op">
            <el-option label="=" value="eq" />
            <el-option label="!=" value="ne" />
            <el-option :label="$t('reports.builder.contains')" value="contains" />
            <el-option :label="$t('reports.builder.startsWith')" value="startsWith" />
            <el-option label=">" value="gt" />
            <el-option label=">=" value="gte" />
            <el-option label="<" value="lt" />
            <el-option label="<=" value="lte" />
          </el-select>
          <el-input v-model="filter.value" class="filter-value" :placeholder="$t('reports.builder.value')" />
          <el-button :icon="Delete" circle size="small" @click="filters.splice(i, 1)" />
        </div>
        <el-button size="small" @click="filters.push({ field: '', op: 'contains', value: '' })">
          + {{ $t('reports.builder.addFilter') }}
        </el-button>

        <!-- 排序 -->
        <div class="section" style="margin-top:16px">{{ $t('reports.builder.sorts') }}</div>
        <div v-for="(sort, i) in sorts" :key="i" class="filter-row">
          <el-select v-model="sort.field" class="filter-field" :placeholder="$t('reports.builder.field')">
            <el-option v-for="f in currentSourceMeta?.fields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
          <el-radio-group v-model="sort.asc" size="small">
            <el-radio-button :value="true">{{ $t('reports.builder.asc') }}</el-radio-button>
            <el-radio-button :value="false">{{ $t('reports.builder.desc') }}</el-radio-button>
          </el-radio-group>
          <el-button :icon="Delete" circle size="small" @click="sorts.splice(i, 1)" />
        </div>
        <el-button size="small" @click="sorts.push({ field: '', asc: true })">
          + {{ $t('reports.builder.addSort') }}
        </el-button>

        <!-- 预览 -->
        <div class="section" style="margin-top:16px">{{ $t('reports.builder.preview') }}</div>
        <div class="preview-actions">
          <el-button type="primary" :loading="executing" @click="executePreview">
            {{ $t('reports.builder.execute') }}
          </el-button>
          <el-button v-if="previewData" @click="exportXlsx">
            {{ $t('reports.exportXlsx') }}
          </el-button>
          <el-button v-if="previewData" @click="exportPdf">
            {{ $t('reports.exportPdf') }}
          </el-button>
          <span v-if="previewData" class="hint">{{ previewData.total }} {{ $t('reports.builder.rows') }}</span>
        </div>

        <el-table v-if="previewData" :data="previewData.rows" size="small" border
          style="margin-top:8px" max-height="400" v-loading="executing">
          <el-table-column v-for="(col, i) in previewData.columns" :key="i"
            :prop="previewData.keys[i]" :label="col" min-width="120" />
        </el-table>
      </div>
    </div>

    <!-- 报表定义列表弹窗 -->
    <el-dialog v-model="showDefList" :title="$t('reports.builder.listTitle')" width="600px">
        <el-table :data="definitions" size="small" border @row-click="onDefRowClick">
          <el-table-column prop="name" :label="$t('reports.name')" />
          <el-table-column prop="dataSource" :label="$t('reports.builder.dataSource')" />
          <el-table-column prop="createdBy" :label="$t('reports.author')" width="100" />
          <el-table-column :label="$t('common.operation')" width="100">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click.stop="deleteDef(row)">
                {{ $t('common.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
    </el-dialog>

    <!-- 保存弹窗 -->
    <el-dialog v-model="showSaveDialog" :title="$t('reports.builder.saveTitle')" width="400px">
      <el-form label-position="top">
        <el-form-item :label="$t('reports.name')" required>
          <el-input v-model="saveForm.name" :placeholder="$t('reports.builder.nameHint')" />
        </el-form-item>
        <el-form-item :label="$t('reports.builder.title')">
          <el-input v-model="saveForm.title" :placeholder="$t('reports.builder.titleHint')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveDefinition" :loading="saving">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  listDataSources,
  listReportDefinitions,
  getReportDefinition,
  createReportDefinition,
  updateReportDefinition,
  deleteReportDefinition,
  executeReport,
  exportReport,
  type DataSourceMeta,
  type ReportExecuteResult,
  type ReportDefinition
} from '@/api/reportBuilder'

const { t } = useI18n()

interface FilterRow { field: string; op: string; value: string }
interface SortRow { field: string; asc: boolean }

const dataSources = ref<DataSourceMeta[]>([])
const currentSource = ref('')
const selectedKeys = ref<string[]>([])
const filters = ref<FilterRow[]>([])
const sorts = ref<SortRow[]>([])
const definitions = ref<ReportDefinition[]>([])
const previewData = ref<ReportExecuteResult | null>(null)
const executing = ref(false)
const saving = ref(false)
const showDefList = ref(false)
const showSaveDialog = ref(false)
const editId = ref<number | null>(null)
const saveForm = ref({ name: '', title: '' })

const currentSourceMeta = computed(() =>
  dataSources.value.find(ds => ds.key === currentSource.value) || null
)

onMounted(async () => {
  try {
    dataSources.value = await listDataSources()
  } catch { /* ignore */ }
})

function onSourceChange() {
  selectedKeys.value = []
  filters.value = []
  sorts.value = []
  previewData.value = null
}

async function executePreview() {
  if (!currentSource.value || !selectedKeys.value.length) {
    ElMessage.warning(t('reports.builder.selectFieldsFirst'))
    return
  }
  executing.value = true
  try {
    // 先保存临时定义再执行
    const tempDef = buildDefinition()
    let id = editId.value
    if (!id) {
      const created = await createReportDefinition({ ...tempDef, name: `__temp_${Date.now()}` })
      id = created.id!
      editId.value = id
    } else {
      await updateReportDefinition(id, tempDef)
    }
    previewData.value = await executeReport(id)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(t('reports.builder.executeFailed') + ': ' + msg)
  } finally {
    executing.value = false
  }
}

function buildDefinition() {
  return {
    name: saveForm.value.name || `temp_${Date.now()}`,
    dataSource: currentSource.value,
    title: saveForm.value.title || '',
    columnsJson: JSON.stringify(
      selectedKeys.value.map(k => {
        const f = currentSourceMeta.value?.fields.find(ff => ff.key === k)
        return { key: k, label: f?.label || k, type: f?.type || 'dimension' }
      })
    ),
    filtersJson: JSON.stringify(filters.value.filter(f => f.field)),
    sortsJson: JSON.stringify(sorts.value.filter(s => s.field))
  }
}

async function saveDefinition() {
  if (!saveForm.value.name) {
    ElMessage.warning(t('reports.builder.nameRequired'))
    return
  }
  saving.value = true
  try {
    const def = buildDefinition()
    def.name = saveForm.value.name
    if (editId.value) {
      await updateReportDefinition(editId.value, def)
    } else {
      const created = await createReportDefinition(def)
      editId.value = created.id!
    }
    ElMessage.success(t('reports.saved'))
    showSaveDialog.value = false
    showDefList.value = false
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}

async function loadDefinitionList() {
  try {
    definitions.value = await listReportDefinitions()
  } catch { /* ignore */ }
}

async function loadDefinition(row: { id: number }) {
  try {
    const def = await getReportDefinition(row.id)
    currentSource.value = def.dataSource
    editId.value = def.id!
    saveForm.value.name = def.name
    saveForm.value.title = def.title || ''

    const cols: { key: string }[] = JSON.parse(def.columnsJson || '[]')
    selectedKeys.value = cols.map(c => c.key)

    const fil: FilterRow[] = JSON.parse(def.filtersJson || '[]')
    filters.value = fil

    const sor: SortRow[] = JSON.parse(def.sortsJson || '[]')
    sorts.value = sor

    showDefList.value = false
    previewData.value = null
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e)
    ElMessage.error(msg)
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onDefRowClick(row: any) {
  loadDefinition(row)
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function deleteDef(row: any) {
  try {
    await ElMessageBox.confirm(t('reports.deleteConfirm', { name: row.name }))
    await deleteReportDefinition(row.id)
    definitions.value = definitions.value.filter(d => d.id !== row.id)
  } catch { /* cancel */ }
}

async function exportXlsx() {
  if (!editId.value) return
  await exportReport(editId.value, 'xlsx', saveForm.value.title || saveForm.value.name || 'report')
}

async function exportPdf() {
  if (!editId.value) return
  await exportReport(editId.value, 'pdf', saveForm.value.title || saveForm.value.name || 'report')
}

// 打开定义列表时加载
watch(showDefList, (v) => { if (v) loadDefinitionList() })
</script>

<style scoped>
.builder-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.builder-sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.builder-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 300px;
  overflow-y: auto;
}
.field-item {
  margin-right: 0;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.filter-field {
  width: 180px;
}
.filter-op {
  width: 100px;
}
.filter-value {
  flex: 1;
}
.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
