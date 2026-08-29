import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ExportButton from '../ExportButton.vue'
import { createI18n } from 'vue-i18n'
import enUS from '@/i18n/lang/en-US'

const i18n = createI18n({ legacy: false, locale: 'en', messages: { en: enUS } })

interface ExportButtonVm {
  handleExport: () => void
}

const vm = (wrapper: ReturnType<typeof mount<typeof ExportButton>>) => wrapper.vm as unknown as ExportButtonVm

describe('ExportButton', () => {
  const columns = [
    { key: 'name', label: '姓名' },
    { key: 'age', label: '年龄' },
  ]

  beforeEach(() => { vi.restoreAllMocks() })

  it('does not export when data is empty', () => {
    const spy = vi.spyOn(URL, 'createObjectURL')
    const wrapper = mount(ExportButton, {
      props: { data: [], columns },
      global: { plugins: [i18n], stubs: { 'el-button': { template: '<button><slot /></button>', props: ['icon', 'loading'] } } },
    })
    vm(wrapper).handleExport()
    expect(spy).not.toHaveBeenCalled()
  })
})
