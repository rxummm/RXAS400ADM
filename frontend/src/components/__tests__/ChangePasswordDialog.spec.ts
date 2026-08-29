import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import { createI18n } from 'vue-i18n'
import enUS from '@/i18n/lang/en-US'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: { en: enUS },
})

vi.mock('@/api/auth', () => ({
  getProfile: vi.fn().mockResolvedValue({
    username: 'testuser',
    email: 'test@example.com',
    permissions: ['JOB_VIEW'],
  }),
  changePassword: vi.fn().mockResolvedValue(undefined),
}))

interface ChangePasswordDialogVm {
  visible: boolean
  username: string
  email: string
  permissionCount: number
  form: { oldPassword: string; newPassword: string; confirmPassword: string }
  open: () => Promise<void>
}

const vm = (wrapper: ReturnType<typeof mount<typeof ChangePasswordDialog>>) =>
  wrapper.vm as unknown as ChangePasswordDialogVm

describe('ChangePasswordDialog', () => {
  const mountComponent = () =>
    mount(ChangePasswordDialog, {
      global: {
        plugins: [i18n],
        stubs: {
          ElDialog: {
            template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
            props: ['modelValue', 'title'],
          },
          ElForm: { template: '<form><slot /></form>', props: ['model', 'rules'] },
          ElFormItem: { template: '<div><slot /></div>', props: ['label', 'prop'] },
          ElInput: false,
          ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          ElDescriptions: { template: '<div><slot /></div>' },
          ElDescriptionsItem: { template: '<div><slot /></div>', props: ['label'] },
        },
      },
    })

  it('弹窗默认不可见（visible=false）', () => {
    const wrapper = mountComponent()
    expect(vm(wrapper).visible).toBe(false)
  })

  it('调用 open() 后弹窗可见并加载用户信息', async () => {
    const wrapper = mountComponent()
    await vm(wrapper).open()
    await wrapper.vm.$nextTick()
    expect(vm(wrapper).visible).toBe(true)
    expect(vm(wrapper).username).toBe('testuser')
    expect(vm(wrapper).email).toBe('test@example.com')
    expect(vm(wrapper).permissionCount).toBe(1)
  })

  it('open() 重置表单字段', async () => {
    const wrapper = mountComponent()
    vm(wrapper).form.oldPassword = 'old'
    vm(wrapper).form.newPassword = 'new'
    await vm(wrapper).open()
    expect(vm(wrapper).form.oldPassword).toBe('')
    expect(vm(wrapper).form.newPassword).toBe('')
    expect(vm(wrapper).form.confirmPassword).toBe('')
  })

  it('getProfile 失败时静默处理（不抛异常）', async () => {
    const { getProfile } = await import('@/api/auth')
    vi.mocked(getProfile).mockRejectedValueOnce(new Error('network'))
    const wrapper = mountComponent()
    await vm(wrapper).open()
    expect(vm(wrapper).username).toBe('')
  })

  it('点击取消关闭弹窗', async () => {
    const wrapper = mountComponent()
    await vm(wrapper).open()
    await wrapper.vm.$nextTick()
    vm(wrapper).visible = false
    await wrapper.vm.$nextTick()
    expect(vm(wrapper).visible).toBe(false)
  })
})
