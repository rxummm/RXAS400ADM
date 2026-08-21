import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useTablePage, clearTablePageCache } from '@/composables/useTablePage'

// useTablePage 依赖的模块打桩：路由固定 /jobs、i18n 透传 key、Element Plus 消息 no-op
vi.mock('vue-router', () => ({ useRoute: () => ({ path: '/jobs' }) }))
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (k: string) => k }) }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() } }))
vi.mock('@/api/as400', () => ({ fetchSystems: vi.fn().mockResolvedValue([]) }))

describe('useTablePage 查询缓存', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearTablePageCache()
    // composable 在 setup 外调用会触发 Vue 生命周期警告（预期），压掉噪音
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  it('同参数第二次 fetch 命中缓存（不发请求，isFromCache=true）', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { fetchData, isFromCache, records } = useTablePage({ fetchApi, enableCache: true })

    await fetchData()
    expect(fetchApi).toHaveBeenCalledTimes(1)

    await fetchData()
    expect(fetchApi).toHaveBeenCalledTimes(1)
    expect(isFromCache.value).toBe(true)
    expect(records.value).toEqual([{ id: 1 }])
  })

  it('forceRefresh 强制回后端并清除缓存标记', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { fetchData, isFromCache } = useTablePage({ fetchApi, enableCache: true })

    await fetchData()
    await fetchData({}, true)
    expect(fetchApi).toHaveBeenCalledTimes(2)
    expect(isFromCache.value).toBe(false)
  })

  it('不同请求参数 → 不同缓存 key，均回后端', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [], total: 0 })
    const { fetchData } = useTablePage({ fetchApi, enableCache: true })

    await fetchData({ tab: 'a' })
    await fetchData({ tab: 'b' })
    expect(fetchApi).toHaveBeenCalledTimes(2)
  })

  it('clearTablePageCache 清空全部缓存（登出/切换账号场景）', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { fetchData } = useTablePage({ fetchApi, enableCache: true })

    await fetchData()
    clearTablePageCache()
    await fetchData()
    expect(fetchApi).toHaveBeenCalledTimes(2)
  })

  it('未启用缓存时每次 fetch 都回后端', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { fetchData } = useTablePage({ fetchApi, enableCache: false })

    await fetchData()
    await fetchData()
    expect(fetchApi).toHaveBeenCalledTimes(2)
  })
})

describe('useTablePage 前端分页与模糊搜索', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearTablePageCache()
    vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  it('frontendPage：total=全量长度，切片分页', async () => {
    const rows = Array.from({ length: 25 }, (_, i) => ({ id: i }))
    const fetchApi = vi.fn().mockResolvedValue({ records: rows, total: rows.length })
    const { fetchData, pagedData, total, current } = useTablePage({
      fetchApi,
      frontendPage: true,
      defaultSize: 20,
    })

    await fetchData()
    expect(total.value).toBe(25)
    expect(pagedData.value.length).toBe(20)

    current.value = 2
    expect(pagedData.value.length).toBe(5)
  })

  it('searchFields：keyword 实时过滤且不发请求', async () => {
    const rows = [{ name: 'Alice' }, { name: 'Bob' }]
    const fetchApi = vi.fn().mockResolvedValue(rows)
    const { fetchData, filteredData, keyword } = useTablePage({
      fetchApi,
      searchFields: ['name'],
      frontendPage: true,
      enableCache: false,
    })

    await fetchData()
    expect(fetchApi).toHaveBeenCalledTimes(1)

    keyword.value = 'ali'
    expect(filteredData.value).toEqual([{ name: 'Alice' }])
    keyword.value = '  '
    expect(filteredData.value).toHaveLength(2)
    expect(fetchApi).toHaveBeenCalledTimes(1)
  })

  it('matchRow 自定义匹配优先于 searchFields', async () => {
    const rows = [{ a: 1, b: 'x' }, { a: 2, b: 'y' }]
    const fetchApi = vi.fn().mockResolvedValue(rows)
    const { fetchData, filteredData, keyword } = useTablePage({
      fetchApi,
      frontendPage: true,
      matchRow: (row: { b: string }, kw: string) => row.b.includes(kw),
    })

    await fetchData()
    keyword.value = 'y'
    expect(filteredData.value).toEqual([{ a: 2, b: 'y' }])
  })
})
