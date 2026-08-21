import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { useSmartQueryTable } from '@/composables/useSmartQueryTable'
import { clearTablePageCache } from '@/composables/useTablePage'

vi.mock('vue-router', () => ({ useRoute: () => ({ path: '/test' }) }))
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k, locale: { value: 'zh-CN' } }),
}))
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() },
}))
vi.mock('@/api/as400', () => ({
  fetchSystems: vi.fn().mockResolvedValue([]),
}))

async function flushAll() {
  await vi.runAllTimersAsync()
  await nextTick()
}

describe('useSmartQueryTable', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearTablePageCache()
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('forceSearch 跳过缓存直接请求后端', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { forceSearch, records } = useSmartQueryTable({
      fetchApi,
      enableCache: true,
    })

    await flushAll()
    expect(fetchApi).toHaveBeenCalledTimes(1)

    await forceSearch()
    await flushAll()
    expect(fetchApi).toHaveBeenCalledTimes(2)
    expect(records.value).toEqual([{ id: 1 }])
  })

  it('resetSearch 清空 keyword 并强制回后端', async () => {
    const fetchApi = vi.fn().mockResolvedValue({ records: [{ id: 1 }], total: 1 })
    const { keyword, resetSearch, records } = useSmartQueryTable({
      fetchApi,
      enableCache: true,
    })

    await flushAll()
    keyword.value = 'test'
    await resetSearch()
    await flushAll()
    expect(keyword.value).toBe('')
    expect(records.value).toEqual([{ id: 1 }])
  })

  it('前端分页模式：防抖后触发过滤（不发请求）', async () => {
    const rows = [
      { name: 'Alice' },
      { name: 'Bob' },
      { name: 'Alicia' },
    ]
    const fetchApi = vi.fn().mockResolvedValue({ records: rows, total: rows.length })
    const { keyword, tableData } = useSmartQueryTable({
      fetchApi,
      frontendPage: true,
      searchFields: ['name'],
      debounceDelay: 500,
      enableCache: false,
    })

    await flushAll()
    expect(fetchApi).toHaveBeenCalledTimes(1)

    keyword.value = 'ali'
    vi.advanceTimersByTime(600)
    await flushAll()

    expect(fetchApi).toHaveBeenCalledTimes(1)
    expect(tableData.value.length).toBe(2)
  })

  it('前端过滤为空时 localFilterEmpty 为 true', async () => {
    const rows = [{ name: 'Alice' }, { name: 'Bob' }]
    const fetchApi = vi.fn().mockResolvedValue({ records: rows, total: rows.length })
    const { keyword, localFilterEmpty } = useSmartQueryTable({
      fetchApi,
      frontendPage: true,
      searchFields: ['name'],
      debounceDelay: 100,
      enableCache: false,
    })

    await flushAll()

    keyword.value = 'zzz'
    vi.advanceTimersByTime(200)
    await flushAll()

    expect(localFilterEmpty.value).toBe(true)
  })

  it('forceSearch 打断防抖定时器：forceSearch 调用后端而非前端过滤', async () => {
    const rows = [{ name: 'Alice' }, { name: 'Bob' }]
    const fetchApi = vi.fn().mockResolvedValue({ records: rows, total: rows.length })
    const { keyword, forceSearch } = useSmartQueryTable({
      fetchApi,
      frontendPage: true,
      searchFields: ['name'],
      debounceDelay: 500,
      enableCache: false,
    })

    await flushAll()
    const callsAfterInit = fetchApi.mock.calls.length

    // 输入关键字，启动防抖
    keyword.value = 'zzz'

    // 200ms 后（防抖未触发）立即 forceSearch
    vi.advanceTimersByTime(200)
    await forceSearch()
    await flushAll()

    // forceSearch 应该调用了 fetchApi（走后端），且次数增加
    expect(fetchApi.mock.calls.length).toBeGreaterThan(callsAfterInit)

    // 再等 600ms，防抖定时器已被 cancel，不应再触发前端过滤
    vi.advanceTimersByTime(600)
    await flushAll()

    // fetchApi 不应该再被额外调用（cancel 后没有前端过滤触发新请求）
    expect(fetchApi.mock.calls.length).toBe(callsAfterInit + 1)
  })
})
