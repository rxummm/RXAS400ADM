import { describe, it, expect, beforeEach } from 'vitest'
import {
  STORAGE_KEYS,
  getKeysByPrefix,
  removeKeysByPrefix,
  useNamespacedKey,
  useStorage,
} from '@/composables/useStorage'

describe('useStorage 解构调用（P3 回归：getJson 不再依赖 this）', () => {
  beforeEach(() => localStorage.clear())

  it('解构 setJson/getJson 后调用正确', () => {
    const { setJson, getJson } = useStorage('rxas400_test_json')
    setJson({ a: 1, nested: { b: 'x' } })
    expect(getJson<{ a: number }>()).toEqual({ a: 1, nested: { b: 'x' } })
  })

  it('getJson 对空值/非法 JSON 回退 fallback 或 null', () => {
    const { set, getJson } = useStorage('rxas400_test_json')
    expect(getJson('fb')).toBe('fb')
    expect(getJson<number>(0)).toBe(0)
    set('not-json{{')
    expect(getJson<number>(0)).toBe(0)
    expect(getJson()).toBeNull()
  })

  it('解构 get/set/remove 基本读写', () => {
    const { get, set, remove } = useStorage('rxas400_test_plain')
    set('v1')
    expect(get()).toBe('v1')
    remove()
    expect(get()).toBeNull()
  })

  it('token 混淆存取：落盘非明文、读取还原原文', () => {
    const { get, set } = useStorage(STORAGE_KEYS.TOKEN)
    set('secret-token')
    const raw = localStorage.getItem(STORAGE_KEYS.TOKEN)
    expect(raw).not.toBeNull()
    expect(raw!).not.toContain('secret-token')
    expect(get()).toBe('secret-token')
  })

  it('命名空间 key 工厂：清洗非法字符、保留合法字符', () => {
    expect(useNamespacedKey('rxas400_tags', '/a/b c')).toBe('rxas400_tags:/a/b_c')
    expect(useNamespacedKey('rxas400_tags', 'ok_1-2.3')).toBe('rxas400_tags:ok_1-2.3')
  })

  it('按前缀枚举/批量删除，不影响无关 key', () => {
    useStorage('rxas400_ns_a').set('1')
    useStorage('rxas400_ns_b').set('2')
    useStorage('other_k').set('y')
    expect(getKeysByPrefix('rxas400_ns_').length).toBe(2)
    removeKeysByPrefix('rxas400_ns_')
    expect(getKeysByPrefix('rxas400_ns_').length).toBe(0)
    expect(localStorage.getItem('other_k')).toBe('y')
  })
})
