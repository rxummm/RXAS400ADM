import { createI18n } from 'vue-i18n'
import zhCN from './lang/zh-CN'
import enUS from './lang/en-US'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'

export type AppLocale = 'zh-CN' | 'en-US'

const localeStore = useStorage(STORAGE_KEYS.LOCALE)

function normalizeLocale(raw: string | null): AppLocale {
  if (raw === 'en' || raw === 'en-US') return 'en-US'
  return 'zh-CN'
}

const saved = normalizeLocale(localeStore.get())

const i18n = createI18n({
  legacy: false,
  locale: saved,
  fallbackLocale: 'zh-CN',
  globalInjection: true,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function setLocale(locale: string) {
  const normalized = normalizeLocale(locale)
  i18n.global.locale.value = normalized
  localeStore.set(normalized)
  document.documentElement.lang = normalized
}

export default i18n