import { createApp } from 'vue'
import axios from 'axios'
import { createPinia } from 'pinia'
import { ElMessage } from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/theme.css'
import '@/styles/common.css'
import '@/styles/responsive.css'
import 'nprogress/nprogress.css'
import { FontAwesomeIcon } from '@/icons'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { useUserStore } from '@/stores/user'

// 按钮级权限指令：v-has-perm="'JOB_END'" 或 v-has-perm="['A','B']"（任一命中显示）
const hasPerm = (codes: string | string[]): boolean => {
  const userStore = useUserStore()
  const list = typeof codes === 'string' ? [codes] : codes || []
  if (list.length === 0) return true
  return list.some((code) => userStore.hasPermission(code))
}

const app = createApp(App)

// 重复请求取消策略（request.ts）会主动 abort 竞态请求，被取消方未 catch 时
// 会产生“Uncaught (in promise) CanceledError”/mounted hook 告警，这里全局静默。
const isCanceled = (e: unknown) => {
  const reason = (e as { reason?: unknown })?.reason ?? e
  const r = reason as { name?: string; code?: string }
  return r?.name === 'CanceledError' || r?.code === 'ERR_CANCELED' || axios.isCancel(reason)
}

window.addEventListener('unhandledrejection', (e) => {
  if (isCanceled(e)) e.preventDefault()
})

app.config.errorHandler = (err) => {
  if (isCanceled(err)) return
  console.error('[Vue errorHandler]', err)
  ElMessage.error({ message: String(err ?? 'Unknown error'), duration: 5000 })
}

// Element Plus 内部 clearable + filterable select 在 v-model 为 null/undefined 且选项列表为空时，
// 会触发 ElOption value prop 的校验警告（select.vue:407），属框架已知行为，静默处理。
app.config.warnHandler = (msg: string) => {
  if (msg.includes('Expected String | Number | Boolean | Object, got Undefined') && msg.includes('prop "value"')) {
    return
  }
  console.warn(msg)
}

app.component('FontAwesomeIcon', FontAwesomeIcon)

// 全局注册所有 Element Plus 图标（SubMenu 通过 <component :is="iconName" /> 动态渲染）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// P2-27：Element Plus 文案语言由 App.vue 的 el-config-provider 随 i18n 实时切换（不再在启动时冻结）
app.use(createPinia())
app.use(router)
app.use(i18n)
app.directive('has-perm', {
  mounted(el, binding) {
    if (!hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
})
app.mount('#app')