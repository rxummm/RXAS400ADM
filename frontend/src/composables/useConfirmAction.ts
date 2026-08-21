import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'

export interface ConfirmOptions {
  /** 确认框标题 */
  title?: string
  /** 确认框类型 */
  type?: 'warning' | 'info' | 'error' | 'success'
  /** 确认按钮文字 */
  confirmText?: string
  /** 取消按钮文字 */
  cancelText?: string
  /** 是否需要权限码（无权限时 disabled） */
  permission?: string
  /** 是否显示 loading */
  showLoading?: boolean
}

/**
 * 创建确认函数
 * @param defaults 默认配置
 */
export function createConfirmer(defaults?: ConfirmOptions) {
  const loading = ref(false)

  const confirm = async (
    message: string,
    options?: ConfirmOptions,
  ): Promise<boolean> => {
    const opts = { ...defaults, ...options }

    try {
      await ElMessageBox.confirm(
        message,
        opts.title || '确认操作',
        {
          type: opts.type || 'warning',
          confirmButtonText: opts.confirmText || '确定',
          cancelButtonText: opts.cancelText || '取消',
          showCancelButton: true,
        },
      )
      return true
    } catch {
      return false
    }
  }

  /**
   * 包装异步操作，自动处理 loading 状态和错误
   */
  const withConfirm = async <T>(
    message: string,
    action: () => Promise<T>,
    options?: ConfirmOptions & { successMessage?: string; errorMessage?: string },
  ): Promise<T | null> => {
    if (!(await confirm(message, options))) return null

    if (options?.showLoading !== false) {
      loading.value = true
    }
    try {
      const result = await action()
      return result
    } finally {
      loading.value = false
    }
  }

  return {
    confirm,
    withConfirm,
    loading,
  }
}

/**
 * 快捷使用（Vue 组件内使用）
 *
 * 用法：
 * ```ts
 * import { useConfirmAction } from '@/composables/useConfirmAction'
 * const confirm = useConfirmAction()
 *
 * async function handleDelete(row) {
 *   if (!await confirm(t('deleteConfirm', { name: row.name }))) return
 *   await deleteApi(row.id)
 *   ElMessage.success(t('deleted'))
 * }
 * ```
 */
export function useConfirmAction() {
  return createConfirmer()
}