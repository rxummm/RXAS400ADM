/**
 * 通用表格分页 Composable（参照旧项目 composables/useTablePage.js，TypeScript 化并适配新项目约定）
 *
 * @deprecated 请使用 useSmartQueryTable 替代，该 composable 增加了防抖输入过滤、强制后端查询、
 * 前端过滤空结果引导等能力。所有页面已迁移至 useSmartQueryTable，useTablePage 仅作为底层能力
 * 被 useSmartQueryTable 内部依赖。
 *
 * 约定：
 * - 新项目 request.ts 响应拦截器已解包，fetchApi 直接返回 { records, total }（或全量数组）
 * - 分页请求参数：current（从 1 开始）/ size（与后端 MyBatis Plus Page 对齐）
 * - 可选能力：表格高度自适应（配合 .page-container--fit）、按 route+params+serverId 隔离的内存查询缓存（3 分钟）、
 *   前端模糊搜索（searchFields 驱动 keyword 实时过滤，不发请求）、前端分页（frontendPage）
 *
 * 前端缓存 + 模糊查询语义（参照旧项目）：
 * - 首次进入 / 缓存过期(>3min) 点击「查询」→ 从后端取全量
 * - 缓存有效期内点「查询」→ 只在前端数据里模糊匹配（keyword 驱动 filteredData）
 * - 点「刷新」→ 强制回后端（fetchData(extra, true) / handleRefresh）
 * - 页面可用 isFromCache 展示「来自缓存」tag
 *
 * @example（全量列表 + 前端过滤 + 缓存）
 * const { filteredData, pagedData, keyword, loading, total, isFromCache, handleSearch, handleRefresh } =
 *   useTablePage<Job>({
 *     fetchApi: () => fetchJobs(),
 *     frontendPage: true,
 *     enableCache: true,
 *     searchFields: ['jobName', 'jobUser', 'jobProgram'],
 *   })
 *
 * 模板：
 * <el-input v-model="keyword" @keyup.enter="handleSearch" />
 * <el-table :data="pagedData" v-loading="loading">…</el-table>
 * <el-tag v-if="isFromCache">来自缓存</el-tag>
 * <AppPagination :total="total" v-model:current="current" v-model:size="size"
 *                @change="handlePageChange" @size-change="handleSizeChange" />
 */
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, type Ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useAs400ServerStore } from '@/stores/as400Server'

export interface TablePageResponse<T = unknown> {
  records: T[]
  total: number
}

/**
 * useTablePage 透传给 fetchApi 的请求参数（P2-29）：
 * current/size/keyword 为固定字段（可选），其余为视图自定义扩展（Tab 切换、筛选等），
 * 具体值由调用方在使用处收窄类型（如 params.path as string）。
 */
export interface TablePageQueryParams {
  current?: number
  size?: number
  keyword?: string
  [key: string]: unknown
}

export interface UseTablePageOptions<T = unknown> {
  /** 请求函数，参数即分页+筛选，返回解包后的 { records, total }（全量列表可返回数组或 {records}） */
  fetchApi: (params: TablePageQueryParams) => Promise<TablePageResponse<T> | T[]>
  defaultSize?: number
  pageSizes?: number[]
  /** 表格高度自适应（ResizeObserver + 窗口 resize），默认开启 */
  enableResize?: boolean
  /** 表格行高，用于高度整除取整（默认 48，与紧凑表格一致） */
  rowHeight?: number
  /** 表格底部预留空间（默认 120） */
  padding?: number
  /** 查询缓存（按 route.path + params + serverId 隔离），默认关闭 */
  enableCache?: boolean
  /** 缓存有效时间 ms（默认 3 分钟） */
  cacheTime?: number
  autoFetch?: boolean
  /** 自定义请求参数组装（默认透传 current/size/keyword） */
  buildParams?: (base: { current: number; size: number; keyword?: string }) => TablePageQueryParams
  /** 后端返回全量列表、前端切片分页（true 时忽略 current/size 请求参数语义，total=records.length） */
  frontendPage?: boolean
  /** 前端模糊搜索字段列表（keyword 实时驱动 filteredData，不发请求） */
  searchFields?: string[]
  /** 自定义匹配函数（动态列/复杂匹配时替代 searchFields）：返回 true 表示命中 */
  matchRow?: (row: T, keyword: string) => boolean
  /** 表格容器 DOM 引用（避免 document.querySelector 全局查找导致误绑其他 .table-wrapper） */
  tableRef?: Ref<HTMLElement | null> | HTMLElement
}

const DEFAULT_PAGE_SIZE = 20
const DEFAULT_ROW_HEIGHT = 48
const DEFAULT_PADDING = 120
const DEFAULT_CACHE_TIME = 180_000
const MAX_CACHE_ENTRIES = 200

interface CacheEntry {
  data: unknown
  params: TablePageQueryParams
  timestamp: number
}

// 模块级内存缓存（SPA 会话内共享，按 route + params + serverId 隔离）
const cacheStore = new Map<string, CacheEntry>()

/**
 * P2-25：清空模块级表格缓存。登出/切换账号时由 userStore.logout() 调用，
 * 防止同 SPA 会话内第二个用户读到前一用户的列表数据（缓存 key 不含用户名）。
 */
export function clearTablePageCache() {
  cacheStore.clear()
}

/** 归一化后端响应 → { records, total } */
function normalize<T>(data: TablePageResponse<T> | T[]): TablePageResponse<T> {
  if (Array.isArray(data)) return { records: data, total: data.length }
  return { records: data.records || [], total: data.total || 0 }
}

export function useTablePage<T = unknown>(options: UseTablePageOptions<T>) {
  const {
    fetchApi,
    defaultSize = DEFAULT_PAGE_SIZE,
    pageSizes = [20, 50, 100],
    enableResize = true,
    rowHeight = DEFAULT_ROW_HEIGHT,
    padding = DEFAULT_PADDING,
    enableCache = false,
    cacheTime = DEFAULT_CACHE_TIME,
    autoFetch = true,
    buildParams = (base) => base,
    frontendPage = false,
    searchFields = [],
    matchRow,
    tableRef,
  } = options

  const route = useRoute()
  const { t } = useI18n()
  const as400Store = useAs400ServerStore()

  // shallowRef：行数据为普通对象、整体替换（fetch/update/remove/add 均重赋整个数组），
  // 避免 ref<T[]> 的 UnwrapRef 解包与泛型 T 冲突（P2-29 严格类型化后暴露）
  const records = shallowRef<T[]>([])
  const loading = ref(false)
  const keyword = ref('')
  const current = ref(1)
  const size = ref(defaultSize)
  const total = ref(0)
  const isFromCache = ref(false)
  /** 数据源变更信号：每次 fetch 成功（含缓存命中）自增，供 QueryBar 闪烁提示数据来源 */
  const dataSourceTick = ref(0)
  const tableMaxHeight = ref(400)

  // ==================== 前端模糊搜索 ====================
  const filteredData = computed<T[]>(() => {
    if (!keyword.value.trim()) return records.value
    const kw = keyword.value.trim().toLowerCase()
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

  // 表格实际渲染数据：frontendPage 时切片分页，否则原样（后端分页）
  const pagedData = computed<T[]>(() => {
    if (!frontendPage) return filteredData.value
    const start = (current.value - 1) * size.value
    return filteredData.value.slice(start, start + size.value)
  })

  // ==================== 表格高度自适应 ====================
  let resizeObserver: ResizeObserver | null = null

  function calcTableMaxHeight(el: Element | null) {
    if (!el) return
    const top = el.getBoundingClientRect().top
    const h = window.innerHeight - top - padding
    tableMaxHeight.value = Math.max(200, Math.floor(h / rowHeight) * rowHeight)
  }

  function resolveTableEl(): Element | null {
    if (tableRef) {
      const el = 'value' in tableRef ? tableRef.value : tableRef
      return el instanceof HTMLElement ? el : null
    }
    return document.querySelector('.table-wrapper')
  }

  const onWindowResize = () => calcTableMaxHeight(resolveTableEl())

  function bindResize() {
    if (!enableResize) return
    nextTick(() => {
      const el = resolveTableEl()
      if (el) {
        calcTableMaxHeight(el)
        resizeObserver = new ResizeObserver(() => calcTableMaxHeight(el))
        resizeObserver.observe(el)
      }
      window.addEventListener('resize', onWindowResize)
    })
  }

  function unbindResize() {
    resizeObserver?.disconnect()
    resizeObserver = null
    window.removeEventListener('resize', onWindowResize)
  }

  // ==================== 查询缓存 ====================
  const serverId = computed(() => as400Store.currentServerId || '')

  function buildCacheKey(params: TablePageQueryParams) {
    return `${route.path}:${JSON.stringify(params)}:${serverId.value}`
  }

  function evictStaleCache() {
    if (cacheStore.size <= MAX_CACHE_ENTRIES) return
    const now = Date.now()
    for (const [k, v] of cacheStore) {
      if (now - v.timestamp > cacheTime) cacheStore.delete(k)
    }
    if (cacheStore.size > MAX_CACHE_ENTRIES) {
      let oldestKey = ''
      let oldest = Infinity
      for (const [k, v] of cacheStore) {
        if (v.timestamp < oldest) {
          oldest = v.timestamp
          oldestKey = k
        }
      }
      if (oldestKey) cacheStore.delete(oldestKey)
    }
  }

  // ==================== 数据获取 ====================
  /**
   * @param extraParams 附加请求参数（参与缓存 key，如 Tab 切换参数）
   * @param forceRefresh true=强制回后端（跳过缓存）
   */
  async function fetchData(extraParams: TablePageQueryParams = {}, forceRefresh = false) {
    loading.value = true
    try {
      const params = buildParams({
        ...extraParams,
        current: current.value,
        size: size.value,
        keyword: keyword.value || undefined,
      })

      if (enableCache && !forceRefresh) {
        const key = buildCacheKey(params)
        const cached = cacheStore.get(key)
        if (cached && Date.now() - cached.timestamp < cacheTime) {
          const norm = normalize<T>(cached.data as TablePageResponse<T> | T[])
          records.value = norm.records
          total.value = frontendPage ? norm.records.length : norm.total
          isFromCache.value = true
          dataSourceTick.value += 1
          return
        }
      }

      const data = await fetchApi(params)
      const norm = normalize<T>(data)
      records.value = norm.records
      total.value = frontendPage ? norm.records.length : norm.total
      isFromCache.value = false

      if (enableCache) {
        evictStaleCache()
        cacheStore.set(buildCacheKey(params), { data, params, timestamp: Date.now() })
      }
      dataSourceTick.value += 1
    } catch (error: unknown) {
      if (error instanceof Error && (error.name === 'CanceledError' || error.message?.includes('canceled'))) return
      ElMessage.error(t('common.loadFailed'))
      records.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 查询：重置页码；缓存有效则不请求后端（前端模糊匹配由 keyword 实时完成） */
  function handleSearch() {
    current.value = 1
    void fetchData()
  }

  function resetSearch() {
    keyword.value = ''
    current.value = 1
    void fetchData()
  }

  /** 刷新：强制回后端 */
  function handleRefresh() {
    current.value = 1
    void fetchData({}, true)
  }

  function handlePageChange(val?: number) {
    // AppPagination 的 @change 不带参（v-model:current 已同步），仅前端分页时无需请求
    if (val != null) current.value = val
    if (!frontendPage) void fetchData()
  }

  function handleSizeChange(val: number) {
    size.value = val
    current.value = 1
    if (!frontendPage) void fetchData()
  }

  // ==================== 乐观更新 ====================
  function updateItem(item: T) {
    const id = (item as { id?: number | string }).id
    if (id == null) return
    records.value = records.value.map((r) => {
      if ((r as { id?: number | string }).id === id) return { ...r, ...item }
      return r
    })
  }

  function removeItem(id: number) {
    records.value = records.value.filter((r) => (r as { id?: number | string }).id !== id)
    total.value = Math.max(0, total.value - 1)
  }

  function addItem(item: T) {
    records.value = [item, ...records.value]
    total.value += 1
  }

  onMounted(() => {
    if (autoFetch) void fetchData()
    bindResize()
  })

  onUnmounted(unbindResize)

  return {
    records,
    filteredData,
    pagedData,
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
    handleSearch,
    resetSearch,
    handleRefresh,
    handlePageChange,
    handleSizeChange,
    updateItem,
    removeItem,
    addItem,
  }
}