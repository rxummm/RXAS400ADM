/**
 * 智能防抖过滤 + 强制后端查询 — 通用数据表格 Composable
 *
 * 在 useTablePage 基础上增强以下能力：
 * 1. 输入防抖（500ms）：用户停止输入后自动触发前端过滤（frontendPage=true）或后端请求（false）
 * 2. 强制后端查询：点击查询/回车/刷新图标 → cancel() 防抖 → 立刻走后端
 * 3. 前端过滤空结果引导：localFilterEmpty / localFilterHint
 * 4. 重置按钮：清空 keyword + 强制后端查询
 *
 * 兼容策略：
 * - frontendPage=true（前端全量小数据）：防抖后触发前端内存过滤，不请求后端
 * - frontendPage=false（后端大数量分页）：防抖后触发后端请求（带 keyword），不在前端过滤
 *   注意：后端分页模式下 originData 只有当前页数据，前端过滤会严重误导用户
 *
 * @example（前端全量模式）
 * const { tableData, keyword, loading, forceSearch, localFilterEmpty } = useSmartQueryTable({
 *   fetchApi: () => listAll(),
 *   frontendPage: true,
 *   searchFields: ['name', 'code'],
 * })
 *
 * @example（后端分页模式）
 * const { tableData, keyword, loading, forceSearch } = useSmartQueryTable({
 *   fetchApi: (params) => searchUsers(params),
 *   frontendPage: false,
 * })
 */
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTablePage, type UseTablePageOptions } from './useTablePage'
import { useColumnSettings, type ColumnOption } from './useColumnSettings'
import { createCancellableDebounce } from '@/utils/cancellableDebounce'

export interface SmartQueryTableOptions<T = unknown> extends Omit<UseTablePageOptions<T>, 'enableCache'> {
  /** 防抖延迟时间 ms（默认 500，设为 0 则退化为无防抖的即时响应） */
  debounceDelay?: number
  /** 是否启用缓存（默认 true，但点击查询/刷新永远强制走后端） */
  enableCache?: boolean
  /** 是否显示刷新图标（默认 true，大数据量页面推荐显示，小数据量页面可隐藏） */
  showRefresh?: boolean
  /** 列设置配置（传入后启用列显隐 + localStorage 持久化） */
  columnSettings?: {
    /** localStorage 存储键名（建议格式：pageName-columns） */
    storageKey: string
    /** 列定义列表 */
    columns: ColumnOption[]
  }
}

export function useSmartQueryTable<T = unknown>(options: SmartQueryTableOptions<T>) {
  const {
    debounceDelay = 500,
    enableCache = true,
    showRefresh = true,
    frontendPage = false,
    searchFields = [],
    matchRow,
    columnSettings: columnSettingsOpt,
    ...restOptions
  } = options

  const { t } = useI18n()

  // 列设置（localStorage 持久化）
  const columnSettings = columnSettingsOpt
    ? useColumnSettings(columnSettingsOpt)
    : null

  // 底层 useTablePage：禁用其内部 keyword 驱动的 computed 过滤，
  // 由本 composable 接管防抖过滤逻辑，避免每按键一次立即触发过滤
  const base = useTablePage<T>({
    ...restOptions,
    frontendPage,
    searchFields: [],
    matchRow: undefined,
    enableCache,
    autoFetch: false,
  })

  const {
    records,
    loading,
    keyword,
    current,
    size,
    total,
    pageSizes,
    isFromCache,
    dataSourceTick,
    tableMaxHeight,
    fetchData,
    handlePageChange,
    handleSizeChange,
    updateItem,
    removeItem,
    addItem,
    resetSearch: baseResetSearch,
  } = base

  // ==================== 可取消防抖 ====================
  const { schedule, cancel } = createCancellableDebounce(debounceDelay)

  // ==================== 防抖关键字 ====================
  // 独立的防抖关键字：用户输入停止 debounceDelay ms 后才更新，
  // 此 ref 驱动前端过滤逻辑，确保过滤不会在每次按键时立即触发
  const debouncedKeyword = ref('')

  watch(
    () => keyword.value,
    (val) => {
      if (debounceDelay <= 0) {
        debouncedKeyword.value = val
        return
      }
      if (!val) {
        cancel()
        debouncedKeyword.value = ''
        return
      }
      schedule(() => {
        debouncedKeyword.value = val
      })
    },
  )

  // ==================== 前端过滤（由 debouncedKeyword 驱动） ====================
  const smartFiltered = computed<T[]>(() => {
    const kw = debouncedKeyword.value.trim().toLowerCase()
    if (!kw) return records.value

    if (matchRow) {
      return records.value.filter((item: T) => matchRow(item, kw))
    }
    if (searchFields.length === 0) return records.value

    return records.value.filter((item) =>
      searchFields.some((field) => {
        const v = (item as Record<string, unknown>)[field]
        return v != null && String(v).toLowerCase().includes(kw)
      }),
    )
  })

  // ==================== 表格展示数据 ====================
  const tableData = computed<T[]>(() => {
    if (!frontendPage) return smartFiltered.value
    const start = (current.value - 1) * size.value
    return smartFiltered.value.slice(start, start + size.value)
  })

  // 前端分页模式：total 跟随过滤结果变化
  watch(smartFiltered, (val) => {
    if (frontendPage) {
      total.value = val.length
    }
  })

  // ==================== 前端过滤空结果 ====================
  const localFilterEmpty = computed(() => {
    if (!debouncedKeyword.value.trim()) return false
    if (frontendPage) {
      return smartFiltered.value.length === 0 && records.value.length > 0
    }
    return false
  })

  const localFilterHint = computed(() => {
    return localFilterEmpty.value ? t('common.localFilterEmpty') : ''
  })

  // ==================== 强制后端查询 ====================
  // 查询按钮 / 回车 / 刷新图标 同源：取消防抖 → 强制走后端
  function forceSearch() {
    cancel()
    // 同步 debouncedKeyword，确保 UI 中的输入值不被过滤延迟覆盖
    debouncedKeyword.value = keyword.value
    current.value = 1
    void fetchData({}, true)
  }

  // ==================== 重置搜索 ====================
  // 清空 keyword + 防抖关键字 + 重置页码 → 强制后端查询
  function resetSearch() {
    cancel()
    baseResetSearch()
    debouncedKeyword.value = ''
    keyword.value = ''
  }

  // ==================== 首次加载 ====================
  if (restOptions.autoFetch !== false) {
    forceSearch()
  }

  return {
    // 数据
    originData: records,
    records,              // 兼容 useTablePage 的 records 别名
    tableData,
    filteredData: smartFiltered,  // 兼容 useTablePage 的 filteredData 别名
    pagedData: tableData,         // 兼容 useTablePage 的 pagedData 别名
    loading,
    isFromCache,
    dataSourceTick,
    localFilterEmpty,
    localFilterHint,

    // 分页
    current,
    size,
    total,
    pageSizes,
    tableMaxHeight,

    // 关键字
    keyword,

    // 操作方法
    forceSearch,
    handleSearch: forceSearch,   // 兼容 useTablePage 的 handleSearch 别名
    handleRefresh: forceSearch,  // 兼容 useTablePage 的 handleRefresh 别名
    resetSearch,
    handlePageChange,
    handleSizeChange,
    fetchData,
    showRefresh,

    // 列设置
    columnSettings,

    // 乐观更新
    updateItem,
    removeItem,
    addItem,
  }
}