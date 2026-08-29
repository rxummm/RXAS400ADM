import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import RxSkeleton from '@/components/RxSkeleton.vue'

describe('RxSkeleton', () => {
  const mountComponent = (props: Record<string, unknown> = {}) =>
    mount(RxSkeleton, {
      props: { loading: true, ...props },
      slots: { default: '<div class="content">Data</div>' },
    })

  it('loading=true 时渲染骨架屏', () => {
    const wrapper = mountComponent({ loading: true })
    expect(wrapper.find('.rx-skeleton-wrap').exists()).toBe(true)
    expect(wrapper.find('.content').exists()).toBe(false)
  })

  it('loading=false 时渲染 slot 内容', () => {
    const wrapper = mountComponent({ loading: false })
    expect(wrapper.find('.rx-skeleton-wrap').exists()).toBe(false)
    expect(wrapper.find('.content').exists()).toBe(true)
  })

  it('默认 type=table 渲染表格骨架行', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.rx-skeleton-table').exists()).toBe(true)
    expect(wrapper.findAll('.rx-skeleton-row')).toHaveLength(5)
  })

  it('type=card 渲染卡片骨架', () => {
    const wrapper = mountComponent({ type: 'card' })
    expect(wrapper.find('.rx-skeleton-card').exists()).toBe(true)
    expect(wrapper.find('.rx-skeleton-row--title').exists()).toBe(true)
  })

  it('type=list 渲染列表骨架（头像+内容）', () => {
    const wrapper = mountComponent({ type: 'list', rows: 3 })
    expect(wrapper.find('.rx-skeleton-list').exists()).toBe(true)
    expect(wrapper.findAll('.rx-skeleton-list-item')).toHaveLength(3)
    expect(wrapper.findAll('.rx-skeleton-avatar')).toHaveLength(3)
  })

  it('rows prop 控制骨架行数', () => {
    const wrapper = mountComponent({ type: 'table', rows: 8 })
    expect(wrapper.findAll('.rx-skeleton-row')).toHaveLength(8)
  })

  it('偶数行有 short 修饰类', () => {
    const wrapper = mountComponent({ type: 'table', rows: 4 })
    const rows = wrapper.findAll('.rx-skeleton-row')
    expect(rows[0].classes()).not.toContain('rx-skeleton-row--short')
    expect(rows[1].classes()).toContain('rx-skeleton-row--short')
    expect(rows[2].classes()).not.toContain('rx-skeleton-row--short')
    expect(rows[3].classes()).toContain('rx-skeleton-row--short')
  })
})
