import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { getMenu } from '@/api/auth'

// userStore 依赖的 API / composable 模块全部打桩，仅测试纯 getter 逻辑
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  as400Login: vi.fn(),
  getMenu: vi.fn().mockResolvedValue({ menus: [], perms: [], tabs: [] }),
  revokeToken: vi.fn(),
}))
vi.mock('vue-router', () => ({ useRoute: () => ({ path: '/__test__' }) }))
vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (k: string) => k }) }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn(), warning: vi.fn() } }))
vi.mock('@/api/as400', () => ({ fetchSystems: vi.fn().mockResolvedValue([]) }))

describe('userStore.canSeeTab（Tab 授权模型）', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('未建模的 tab → 默认可见', () => {
    const store = useUserStore()
    store.tabs = []
    expect(store.canSeeTab('jobs', 'anyTab')).toBe(true)
  })

  it('已建模但 status=0（停用）→ 对所有人隐藏（含 ADMIN）', () => {
    const store = useUserStore()
    store.tabs = [{ page: 'jobs', title: 'queuesTab', menuName: 'jobs.queuesTab', status: 0, perms: '' }]
    store.permissions = ['ADMIN']
    expect(store.canSeeTab('jobs', 'queuesTab')).toBe(false)
  })

  it('已建模且 perms 为空 → 默认可见', () => {
    const store = useUserStore()
    store.tabs = [{ page: 'jobs', title: 'spoolTab', menuName: 'jobs.spoolTab', status: 1, perms: '' }]
    expect(store.canSeeTab('jobs', 'spoolTab')).toBe(true)
  })

  it('已建模且有 perms → 未授权不可见，授权后可见', () => {
    const store = useUserStore()
    store.tabs = [{ page: 'objects', title: 'refIn', menuName: 'objects.refIn', status: 1, perms: 'OBJECT_VIEW' }]
    expect(store.canSeeTab('objects', 'refIn')).toBe(false)
    store.permissions = ['OBJECT_VIEW']
    expect(store.canSeeTab('objects', 'refIn')).toBe(true)
  })

  it('tab 匹配按 page+title 精确匹配，不同页同名 tab 互不影响', () => {
    const store = useUserStore()
    store.tabs = [
      { page: 'objects', title: 'refIn', menuName: 'objects.refIn', status: 1, perms: 'OBJECT_VIEW' },
      { page: 'topology', title: 'refIn', menuName: 'topology.refIn', status: 1, perms: '' },
    ]
    // objects 页受限
    expect(store.canSeeTab('objects', 'refIn')).toBe(false)
    // topology 页同名 tab 不受限
    expect(store.canSeeTab('topology', 'refIn')).toBe(true)
  })
})

describe('userStore.hasPermission', () => {
  it('按权限码精确判断', () => {
    const store = useUserStore()
    store.permissions = ['JOB_VIEW', 'DEPLOY_EXECUTE']
    expect(store.hasPermission('JOB_VIEW')).toBe(true)
    expect(store.hasPermission('DEPLOY_EXECUTE')).toBe(true)
    expect(store.hasPermission('USER_MANAGE')).toBe(false)
  })
})

describe('userStore.fetchMenus 并发去重（P3）', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(getMenu).mockResolvedValue({ menus: [], perms: [], tabs: [] })
  })

  it('并发调用合并：/auth/menu 只请求一次', async () => {
    // 注意：pinia wrapAction 会给每次调用包一层 .then，Promise 实例必然不同；
    // 去重的可观测断言是「getMenu 只调用一次 + 两次 await 都拿到结果」。
    const store = useUserStore()
    const p1 = store.fetchMenus()
    const p2 = store.fetchMenus()
    await Promise.all([p1, p2])
    expect(getMenu).toHaveBeenCalledTimes(1)
  })

  it('并发期间数据只写一份，两处 await 都拿到同一份菜单', async () => {
    const store = useUserStore()
    vi.mocked(getMenu).mockResolvedValue({
      menus: [{ path: '/jobs', title: 'menu.jobs' }],
      perms: ['JOB_VIEW'],
      tabs: [],
    })
    const [r1, r2] = await Promise.all([store.fetchMenus(), store.fetchMenus()])
    expect(r1).toBe(r2)
    expect(store.menus).toHaveLength(1)
    expect(store.permissions).toContain('JOB_VIEW')
    expect(getMenu).toHaveBeenCalledTimes(1)
  })

  it('完成后再次调用会重新拉取（在途锁自动释放）', async () => {
    const store = useUserStore()
    await store.fetchMenus()
    await store.fetchMenus()
    expect(getMenu).toHaveBeenCalledTimes(2)
  })

  it('失败后锁释放，下次调用可重试', async () => {
    const store = useUserStore()
    vi.mocked(getMenu).mockRejectedValueOnce(new Error('boom'))
    await store.fetchMenus() // loadMenus 内部吞错
    expect(store.menus).toEqual([])
    await store.fetchMenus()
    expect(getMenu).toHaveBeenCalledTimes(2)
  })

  it('applyLogin 触发的在途请求与显式 fetchMenus 合并', async () => {
    const store = useUserStore()
    store.applyLogin({ token: 't1', username: 'u1', permissions: ['JOB_VIEW'] })
    await store.fetchMenus()
    expect(getMenu).toHaveBeenCalledTimes(1)
  })
})
