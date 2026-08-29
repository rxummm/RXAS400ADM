import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppPagination from '@/components/AppPagination.vue'

interface AppPaginationVm {
  layout: string
  size: number
  pageSizes: number[]
  onSizeChange: (size: number) => void
}

const vm = (wrapper: ReturnType<typeof mount<typeof AppPagination>>) => wrapper.vm as unknown as AppPaginationVm

describe('AppPagination', () => {
  const mountComponent = (props: Record<string, unknown> = {}) =>
    mount(AppPagination, {
      props: {
        total: 100,
        current: 1,
        size: 10,
        ...props,
      },
    })

  it('renders with class app-pagination', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.app-pagination').exists()).toBe(true)
  })

  it('showSizes=true 时 layout 含 sizes', () => {
    const wrapper = mountComponent({ showSizes: true })
    // 内部 computed layout 应包含 sizes
    expect(vm(wrapper).layout).toContain('sizes')
  })

  it('showSizes=false 时 layout 不含 sizes', () => {
    const wrapper = mountComponent({ showSizes: false })
    expect(vm(wrapper).layout).not.toContain('sizes')
  })

  it('onSizeChange 时自动回到第 1 页并触发 sizeChange', async () => {
    const wrapper = mountComponent({ current: 3, size: 10 })
    vm(wrapper).onSizeChange(20)
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('sizeChange')![0]).toEqual([20])
    expect(wrapper.emitted('update:current')![0]).toEqual([1])
    expect(vm(wrapper).size).toBe(20)
  })

  it('默认 pageSizes 为 [10,20,50]', () => {
    const wrapper = mountComponent()
    expect(vm(wrapper).pageSizes).toEqual([10, 20, 50])
  })

  it('自定义 pageSizes 透传', () => {
    const wrapper = mountComponent({ pageSizes: [20, 50, 100] })
    expect(vm(wrapper).pageSizes).toEqual([20, 50, 100])
  })
})
