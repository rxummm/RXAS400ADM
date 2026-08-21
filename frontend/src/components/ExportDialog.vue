<template>
  <el-dialog
    v-model="dialogVisible"
    :title="$t('export.title')"
    width="480px"
    :close-on-click-modal="false"
  >
    <el-form label-position="top">
      <!-- 导出格式 -->
      <el-form-item :label="$t('export.format')">
        <el-radio-group v-model="form.format" class="w-full">
          <el-radio value="csv" class="export-option">
            <div class="export-option__content">
              <el-icon :size="20"><Document /></el-icon>
              <div>
                <div class="export-option__title">CSV</div>
                <div class="export-option__desc">{{ $t('export.csvDesc') }}</div>
              </div>
            </div>
          </el-radio>
          <el-radio value="print" class="export-option">
            <div class="export-option__content">
              <el-icon :size="20"><Printer /></el-icon>
              <div>
                <div class="export-option__title">{{ $t('export.print') }}</div>
                <div class="export-option__desc">{{ $t('export.printDesc') }}</div>
              </div>
            </div>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 导出范围 -->
      <el-form-item :label="$t('export.range')">
        <el-radio-group v-model="form.range">
          <el-radio value="all">{{ $t('export.all') }} ({{ total }})</el-radio>
          <el-radio value="selected">{{ $t('export.selected') }} ({{ selectedCount }})</el-radio>
          <el-radio value="page">{{ $t('export.currentPage') }} ({{ pageSize }})</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 选择列（可选） -->
      <el-form-item v-if="columns.length > 0" :label="$t('export.columns')">
        <el-checkbox
          v-model="form.allColumns"
          :indeterminate="form.selectedColumns.length > 0 && form.selectedColumns.length < columns.length"
        >
          {{ $t('export.selectAll') }}
        </el-checkbox>
        <el-divider class="export-divider" />
        <el-checkbox-group v-model="form.selectedColumns" class="export-columns">
          <el-checkbox v-for="col in columns" :key="col.key" :value="col.key">
            {{ col.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="exporting" @click="handleExport">
        {{ $t('export.export') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { Document, Printer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

/**
 * ExportDialog - 统一导出对话框
 *
 * 支持：
 * - CSV 导出（含 BOM 头，Excel 兼容）
 * - 打印预览
 * - 自定义导出列
 * - 导出范围选择（全部/选中/当前页）
 *
 * 用法：
 * ```vue
 * <ExportDialog
 *   ref="exportDialogRef"
 *   :data="tableData"
 *   :columns="columnOptions"
 *   :total="total"
 *   :page-size="pageSize"
 *   :selected-count="selectedRows.length"
 * />
 *
 * // 触发导出
 * exportDialogRef.value?.open()
 * ```
 */

export interface ExportColumn {
  key: string
  label: string
  formatter?: (row: Record<string, unknown>) => string
}

const props = withDefaults(defineProps<{
  /** 表格数据 */
  data: Record<string, unknown>[]
  /** 列配置 */
  columns?: ExportColumn[]
  /** 总条数 */
  total?: number
  /** 每页条数 */
  pageSize?: number
  /** 已选中行数 */
  selectedCount?: number
  /** 选中的行数据 */
  selectedData?: Record<string, unknown>[]
  /** 文件名前缀 */
  filename?: string
}>(), {
  columns: () => [],
  total: 0,
  pageSize: 10,
  selectedCount: 0,
  selectedData: () => [],
  filename: 'export',
})

const dialogVisible = ref(false)
const exporting = ref(false)

const form = reactive({
  format: 'csv' as 'csv' | 'print',
  range: 'all' as 'all' | 'selected' | 'page',
  allColumns: true,
  selectedColumns: [] as string[],
})

// 当全选时清空单独选择
watch(() => form.allColumns, (val) => {
  if (val) {
    form.selectedColumns = []
  }
})

// 当有单独选择时取消全选
watch(() => form.selectedColumns, (val) => {
  if (val.length > 0 && val.length < props.columns.length) {
    form.allColumns = false
  } else if (val.length === props.columns.length) {
    form.allColumns = true
  }
})

const open = () => {
  dialogVisible.value = true
}

const handleExport = async () => {
  exporting.value = true
  try {
    // 获取要导出的数据
    let exportData: Record<string, unknown>[] = []
    if (form.range === 'selected') {
      exportData = props.selectedData
    } else if (form.range === 'page') {
      exportData = props.data.slice(0, props.pageSize)
    } else {
      exportData = props.data
    }

    // 获取要导出的列
    const exportColumns = form.allColumns
      ? props.columns
      : props.columns.filter(col => form.selectedColumns.includes(col.key))

    if (form.format === 'csv') {
      exportToCSV(exportData, exportColumns)
    } else {
      printData(exportData, exportColumns)
    }

    dialogVisible.value = false
  } finally {
    exporting.value = false
  }
}

const exportToCSV = (data: Record<string, unknown>[], columns: ExportColumn[]) => {
  // CSV 内容
  const BOM = '\uFEFF' // UTF-8 BOM for Excel
  const headers = columns.map(col => col.label).join(',')
  const rows = data.map(row =>
    columns.map(col => {
      let value = col.formatter ? col.formatter(row) : row[col.key]
      if (value == null) value = ''
      value = String(value).replace(/"/g, '""')
      return `"${value}"`
    }).join(',')
  )

  const csv = BOM + headers + '\n' + rows.join('\n')

  // 下载
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${props.filename}_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)

  ElMessage.success(`已导出 ${data.length} 条数据`)
}

const printData = (data: Record<string, unknown>[], columns: ExportColumn[]) => {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('无法打开打印窗口，请检查浏览器设置')
    return
  }

  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>${props.filename}</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; }
        table { width: 100%; border-collapse: collapse; font-size: 12px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background: #f5f5f5; font-weight: 600; }
        tr:nth-child(even) { background: #fafafa; }
        .print-header { margin-bottom: 20px; }
        .print-header h1 { font-size: 18px; margin: 0 0 8px 0; }
        .print-header p { color: #666; font-size: 12px; margin: 0; }
        @media print { body { padding: 0; } }
      </style>
    </head>
    <body>
      <div class="print-header">
        <h1>${props.filename}</h1>
        <p>打印时间: ${new Date().toLocaleString()} | 共 ${data.length} 条</p>
      </div>
      <table>
        <thead>
          <tr>${columns.map(col => `<th>${col.label}</th>`).join('')}</tr>
        </thead>
        <tbody>
          ${data.map(row =>
            `<tr>${columns.map(col =>
              `<td>${col.formatter ? col.formatter(row) : (row[col.key] ?? '')}</td>`
            ).join('')}</tr>`
          ).join('')}
        </tbody>
      </table>
    </body>
    </html>
  `

  printWindow.document.write(html)
  printWindow.document.close()
  printWindow.print()
}

defineExpose({ open })
</script>

<style scoped>
.export-divider {
  margin: 8px 0;
}

.export-option {
  display: block;
  height: auto;
  margin-right: 0;
}

.export-option :deep(.el-radio__input) {
  margin-top: 4px;
}

.export-option__content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.export-option__title {
  font-weight: 500;
  margin-bottom: 4px;
}

.export-option__desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.export-columns {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
</style>