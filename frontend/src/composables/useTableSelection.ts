/**
 * 表格多选状态管理 Composable
 *
 * 封装 el-table 的 Selection 列状态管理：
 * - selectedRows：当前选中的行数据
 * - selectedKeys：当前选中的行主键
 * - toggleRowSelection：切换行选中
 * - clearSelection：清空选择
 * - isSelected：判断行是否选中
 *
 * @example
 *   const { selectedRows, selectedKeys, handleSelectionChange, clearSelection } = useTableSelection({
 *     keyField: 'id',
 *   })
 *
 *   <el-table @selection-change="handleSelectionChange">
 *     <el-table-column type="selection" />
 *   </el-table>
 */
import { ref, computed } from 'vue'

export interface UseTableSelectionOptions<T = Record<string, unknown>> {
  /** 行主键字段名（默认 'id'） */
  keyField?: keyof T & string
}

export function useTableSelection<T extends Record<string, unknown> = Record<string, unknown>>(
  options: UseTableSelectionOptions<T> = {},
) {
  const { keyField = 'id' as keyof T & string } = options

  const selectedRows = ref<T[]>([]) as { value: T[] }

  const selectedKeys = computed(() =>
    selectedRows.value.map((row) => row[keyField]),
  )

  function handleSelectionChange(selection: T[]) {
    selectedRows.value = selection
  }

  function clearSelection() {
    selectedRows.value = []
  }

  function isSelected(row: T): boolean {
    return selectedKeys.value.includes(row[keyField])
  }

  return {
    selectedRows,
    selectedKeys,
    handleSelectionChange,
    clearSelection,
    isSelected,
  }
}
