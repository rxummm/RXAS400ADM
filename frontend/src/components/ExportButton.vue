<template>
  <el-button :icon="Download" :loading="loading" @click="handleExport">
    <slot>{{ $t('common.export') }}</slot>
  </el-button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { triggerBlobDownload } from '@/api/blobClient'

/**
 * 通用 CSV 导出按钮（前端生成 UTF-8 BOM，Excel 中文不乱码）。
 * 用法：
 * <ExportButton :data="rows" :columns="[{key:'name',label:'名称'},{key:'status',label:'状态'}]"
 *               :title="'用户列表'" />
 */
export interface ExportColumn {
  key: string
  label: string
}

const props = defineProps<{
  /** 任意对象数组即可（P2-29：不要求调用方类型实现索引签名） */
  data: unknown[]
  columns: ExportColumn[]
  title?: string
}>()

const loading = ref(false)

function handleExport() {
  if (!props.data || props.data.length === 0) {
    return
  }
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
</script>