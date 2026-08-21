/**
 * 列显隐设置 Composable（localStorage 持久化）
 *
 * 与 TableColumnSettings.vue 组件配合使用，提供：
 * 1. 列显隐状态管理（v-model 绑定）
 * 2. localStorage 持久化（按 storageKey 隔离，不同页面互不干扰）
 * 3. 首次使用默认全部可见
 *
 * @example
 * const { visibleColumns, columnSettings } = useColumnSettings({
 *   storageKey: 'system-roles',
 *   columns: [
 *     { key: 'id', label: 'ID' },
 *     { key: 'name', label: 'Name' },
 *   ],
 * })
 *
 * 模板：
 * <TableColumnSettings :columns="columnSettings.columns" v-model:visible="visibleColumns" />
 * <el-table :data="rows">
 *   <el-table-column v-if="visibleColumns.includes('id')" prop="id" label="ID" />
 *   <el-table-column v-if="visibleColumns.includes('name')" prop="name" label="Name" />
 * </el-table>
 */
import { ref, watch, type Ref } from 'vue'

export interface ColumnOption {
  key: string
  label: string
}

export interface UseColumnSettingsOptions {
  /** localStorage 存储键名（建议格式：pageName-columns，如 system-roles） */
  storageKey: string
  /** 列定义列表 */
  columns: ColumnOption[]
}

export interface UseColumnSettingsReturn {
  /** 当前可见的列 key 列表（v-model 绑定到 TableColumnSettings） */
  visibleColumns: Ref<string[]>
  /** 透传给 TableColumnSettings 组件的 props */
  columnSettings: {
    columns: ColumnOption[]
  }
}

const STORAGE_PREFIX = 'rx-columns-'

function readStorage(key: string, fallback: string[]): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch {
    // localStorage 读取失败，使用默认值
  }
  return fallback
}

function writeStorage(key: string, value: string[]): void {
  try {
    localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value))
  } catch {
    // localStorage 写入失败（如配额满），静默忽略
  }
}

export function useColumnSettings(options: UseColumnSettingsOptions): UseColumnSettingsReturn {
  const { storageKey, columns } = options

  const defaultVisible = columns.map((c) => c.key)
  const visibleColumns = ref<string[]>(readStorage(storageKey, defaultVisible))

  watch(
    visibleColumns,
    (val) => {
      writeStorage(storageKey, val)
    },
    { deep: true },
  )

  return {
    visibleColumns,
    columnSettings: {
      columns,
    },
  }
}