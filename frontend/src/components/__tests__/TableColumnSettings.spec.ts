import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TableColumnSettings from '../TableColumnSettings.vue'
import { createI18n } from 'vue-i18n'
import enUS from '@/i18n/lang/en-US'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: { en: enUS },
})

interface TableColumnSettingsVm {
  toggleColumn: (key: string) => void
}

const vm = (wrapper: ReturnType<typeof mount<typeof TableColumnSettings>>) =>
  wrapper.vm as unknown as TableColumnSettingsVm

describe('TableColumnSettings', () => {
  const columns = [
    { key: 'name', label: '姓名' },
    { key: 'age', label: '年龄' },
    { key: 'email', label: '邮箱' },
  ]
  const stubs = {
    'el-dropdown': { template: '<div><slot /></div>' },
    'el-dropdown-menu': { template: '<div><slot /></div>' },
    'el-dropdown-item': { template: '<div><slot /></div>', props: ['command'] },
    'el-button': { template: '<button><slot /></button>' },
    'el-icon': { template: '<i />' },
  }

  it('toggleColumn removes visible key when present', () => {
    const visible = ['name', 'age', 'email']
    const wrapper = mount(TableColumnSettings, {
      props: { columns, visible },
      global: { plugins: [i18n], stubs },
    })
    vm(wrapper).toggleColumn('age')
    expect(visible).toEqual(['name', 'email'])
  })

  it('toggleColumn adds visible key when absent', () => {
    const visible = ['name']
    const wrapper = mount(TableColumnSettings, {
      props: { columns, visible },
      global: { plugins: [i18n], stubs },
    })
    vm(wrapper).toggleColumn('email')
    expect(visible).toEqual(['name', 'email'])
  })
})
