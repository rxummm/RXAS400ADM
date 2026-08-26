/**
 * 删除确认 + loading + 错误处理 Composable
 *
 * 统一项目中 35+ 个视图的删除模式：
 *   ElMessageBox.confirm → loading → API call → success message → refresh
 *
 * @example
 * const { removeLoading, confirmRemove } = useConfirmDelete<IbmiSystem>({
 *   deleteApi: (row) => deleteSystem(row.id),
 *   onSuccess: () => load(),
 * })
 *
 * 模板：
 * <el-button :loading="removeLoading === row.id" @click="confirmRemove(row)">
 *   {{ $t('common.delete') }}
 * </el-button>
 */
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface UseConfirmDeleteOptions<Row> {
  /** 删除 API：接收行数据，返回 Promise */
  deleteApi: (row: Row) => Promise<unknown>
  /** 删除成功后回调（接收被删的行，通常用于刷新列表或清理关联状态） */
  onSuccess?: (row: Row) => void
  /** 确认弹窗消息 i18n key，支持动态参数如 'assets.deleteConfirm' */
  confirmMessage?: string
  /** 确认弹窗标题 i18n key，默认 'common.confirm' */
  confirmTitle?: string
  /** 成功提示 i18n key，默认 'common.deleteSuccess' */
  successMessage?: string
  /** 用于标识 loading 的行 ID 字段名，默认 'id' */
  idField?: string
}

export function useConfirmDelete<Row extends object>(
  options: UseConfirmDeleteOptions<Row>,
): {
  removeLoading: ReturnType<typeof ref<string | number | null>>
  confirmRemove: (row: object) => Promise<void>
} {
  const { t } = useI18n()
  const removeLoading = ref<string | number | null>(null)

  // 入参收宽为 object：EP 表格插槽的 row 是 DefaultRow（Record<string, any>），
  // 受函数参数逆变限制无法直接传给 (row: Row)，在内部统一断言收窄，调用方零改动
  const confirmRemove = async (rawRow: object) => {
    const row = rawRow as Row
    // 确认弹窗
    const confirmMsg = options.confirmMessage
      ? t(options.confirmMessage, row as Record<string, unknown>)
      : t('common.confirmDelete')
    const confirmTitle = options.confirmTitle ? t(options.confirmTitle) : t('common.confirm')

    try {
      await ElMessageBox.confirm(confirmMsg, confirmTitle, { type: 'warning' })
    } catch {
      return // 用户取消
    }

    // 行 ID 仅支持 string/number（用于按钮 loading 匹配）
    const key = (options.idField ?? 'id') as keyof Row
    const value = row[key]
    if (typeof value === 'string' || typeof value === 'number') {
      removeLoading.value = value
    }

    try {
      await options.deleteApi(row)
      ElMessage.success(options.successMessage ? t(options.successMessage) : t('common.deleteSuccess'))
      options.onSuccess?.(row)
    } finally {
      removeLoading.value = null
    }
  }

  return { removeLoading, confirmRemove }
}
