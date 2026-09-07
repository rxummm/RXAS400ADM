import { ref, watchEffect, type Ref } from 'vue'
import { listDictItems, type DictItem } from '@/api/dict'

/**
 * 字典数据 composable：按 typeCode 加载字典项，自动缓存，响应式更新。
 *
 * 用法：
 * ```ts
 * const { items, loading, getLabel, getTagType } = useDict('ENVIRONMENT')
 *
 * // 模板中
 * <el-option v-for="d in items" :key="d.itemKey" :label="d.itemValue" :value="d.itemKey" />
 * ```
 */
export function useDict(typeCode: Ref<string> | string) {
  const items = ref<DictItem[]>([])
  const loading = ref(false)

  // 全局缓存：typeCode → DictItem[]
  const cache = new Map<string, DictItem[]>()

  async function load(code: string) {
    if (cache.has(code)) {
      items.value = cache.get(code)!
      return
    }
    loading.value = true
    try {
      const data = await listDictItems(code)
      cache.set(code, data)
      items.value = data
    } catch {
      items.value = []
    } finally {
      loading.value = false
    }
  }

  // 响应式监听 typeCode 变化
  if (typeof typeCode === 'object' && 'value' in typeCode) {
    watchEffect(() => load(typeCode.value))
  } else {
    load(typeCode)
  }

  /** 根据 itemKey 获取 itemValue（显示文本） */
  function getLabel(key: string): string {
    return items.value.find(i => i.itemKey === key)?.itemValue ?? key
  }

  /** 根据 itemKey 获取 el-tag type（前端约定映射） */
  function getTagType(key: string): 'success' | 'warning' | 'danger' | 'info' | undefined {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
      // ENVIRONMENT
      PROD: 'danger', TEST: 'warning', DEV: 'success', DR: 'info',
      // CRITICAL_LEVEL
      CRITICAL: 'danger', NORMAL: 'info',
      // ALERT_LEVEL
      WARNING: 'warning',
      // EXEC_STATUS
      SUCCESS: 'success', FAILED: 'danger',
      // DOC_STATUS
      DRAFT: 'info', PENDING: 'warning', PUBLISHED: 'success', REJECTED: 'danger',
      // ALERT_CHANNEL
      NONE: 'info', EMAIL: 'warning', WEBHOOK: 'success',
      // USER_STATUS
      '*ENABLED': 'success', '*DISABLED': 'danger',
    }
    return map[key]
  }

  return { items, loading, getLabel, getTagType }
}
