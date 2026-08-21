/**
 * 异步表单提交 Composable
 *
 * 封装表单提交流程：loading 状态 + 错误处理 + 成功回调。
 * 配合 useFormDialog 使用效果更佳。
 *
 * @example
 *   const { submitting, submit } = useAsyncForm({
 *     onSuccess: () => {
 *       ElMessage.success('保存成功')
 *       dialogVisible.value = false
 *       loadData()
 *     },
 *   })
 *
 *   // 模板中：
 *   <el-button :loading="submitting" @click="submit(saveApi, formData)">保存</el-button>
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export interface UseAsyncFormOptions {
  /** 成功提示文案（为空则不显示成功提示） */
  successMessage?: string
  /** 成功回调（在提示之后执行） */
  onSuccess?: () => void
  /** 自定义错误处理（返回 true 则不显示默认错误提示） */
  onError?: (error: unknown) => boolean | void
}

export function useAsyncForm(options: UseAsyncFormOptions = {}) {
  const { successMessage, onSuccess, onError } = options
  const submitting = ref(false)

  /**
   * 执行异步提交
   * @param fn 实际提交函数（如 API 调用）
   * @param ...args 传递给 fn 的参数
   */
  async function submit<P extends unknown[]>(
    fn: (...args: P) => Promise<unknown>,
    ...args: P
  ): Promise<boolean> {
    if (submitting.value) return false
    submitting.value = true
    try {
      await fn(...args)
      if (successMessage) {
        ElMessage.success(successMessage)
      }
      onSuccess?.()
      return true
    } catch (error: unknown) {
      const handled = onError?.(error)
      if (!handled) {
        const msg = error instanceof Error ? error.message : String(error)
        ElMessage.error(msg || '操作失败')
      }
      return false
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting,
    submit,
  }
}
