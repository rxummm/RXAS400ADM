<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <el-button :icon="Download" :loading="loading">
      <slot>{{ $t('export.export') }}</slot>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="csv">
          <el-icon><Document /></el-icon>
          CSV
        </el-dropdown-item>
        <el-dropdown-item command="xlsx">
          <el-icon><DataLine /></el-icon>
          Excel
        </el-dropdown-item>
        <el-dropdown-item command="pdf">
          <el-icon><Printer /></el-icon>
          PDF
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Document, DataLine, Printer } from '@element-plus/icons-vue'
import blobClient, { triggerBlobDownload } from '@/api/blobClient'
import type { ExportColumn } from '@/components/ExportButton.vue'

/**
 * 通用导出下拉组件：CSV（前端生成）/ Excel / PDF（后端生成）。
 * 用法：
 * <ExportDropdown :data="rows" :columns="columns" title="订单列表"
 *                 export-url="/api/v1/bpcs/export/orders" :query-params="filters" />
 */
const props = defineProps<{
  /** 当前筛选数据（CSV 导出用） */
  data: unknown[]
  /** 列定义（CSV 导出用） */
  columns: ExportColumn[]
  /** 导出文件名前缀 */
  title?: string
  /** 后端导出端点（Excel / PDF），不传则仅支持 CSV */
  exportUrl?: string
  /** 附加查询参数（会拼到 URL query 上） */
  queryParams?: Record<string, unknown>
}>()

const loading = ref(false)

async function handleCommand(format: string) {
  if (format === 'csv') {
    exportCsv()
    return
  }
  await exportServer(format)
}

function exportCsv() {
  if (!props.data || props.data.length === 0) return
  loading.value = true
  try {
    const header = props.columns.map((c) => c.label)
    const lines = props.data.map((row) =>
      props.columns.map((c) => {
        const v = (row as Record<string, unknown>)?.[c.key]
        const s = v === null || v === undefined ? '' : String(v)
        return `"${s.replace(/"/g, '""')}"`
      }).join(','),
    )
    const csv = '\uFEFF' + [header.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const name = props.title || 'export'
    triggerBlobDownload(blob, `${name}-${new Date().toISOString().slice(0, 10)}.csv`)
  } finally {
    loading.value = false
  }
}

async function exportServer(format: string) {
  if (!props.exportUrl) {
    ElMessage.warning('当前模块暂不支持 Excel/PDF 导出')
    return
  }
  loading.value = true
  try {
    const params: Record<string, string> = { format, ...toQuery(props.queryParams) }
    const res = await blobClient.get(props.exportUrl, { params })
    const ext = format === 'pdf' ? 'pdf' : 'xlsx'
    const mime = format === 'pdf'
      ? 'application/pdf'
      : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    const blob = new Blob([res.data], { type: mime })
    const name = props.title || 'export'
    triggerBlobDownload(blob, `${name}-${new Date().toISOString().slice(0, 10)}.${ext}`)
  } finally {
    loading.value = false
  }
}

function toQuery(obj?: Record<string, unknown>): Record<string, string> {
  if (!obj) return {}
  const q: Record<string, string> = {}
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined && v !== null && v !== '') {
      q[k] = String(v)
    }
  }
  return q
}
</script>
