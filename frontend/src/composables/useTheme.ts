/**
 * 主题管理（参照旧项目 useTheme + useLayoutSettings）
 * - isDark：亮/暗切换（html.dark class + localStorage 持久化）
 * - themeColor：5 种主题色（html[data-theme] + localStorage 持久化）
 * - applyTheme 在模块加载时立即执行，避免刷新闪烁
 */
import { ref } from 'vue'
import { useStorage, STORAGE_KEYS } from './useStorage'

export const THEME_COLORS = [
  { name: 'default', label: 'theme.colorDefault', color: '#1677ff' },
  { name: 'green', label: 'theme.colorGreen', color: '#67c23a' },
  { name: 'purple', label: 'theme.colorPurple', color: '#9c27b0' },
  { name: 'orange', label: 'theme.colorOrange', color: '#e6a23c' },
  { name: 'cyan', label: 'theme.colorCyan', color: '#00bcd4' },
] as const

export type ThemeColorName = (typeof THEME_COLORS)[number]['name']

const themeStore = useStorage(STORAGE_KEYS.THEME)
const colorStore = useStorage(STORAGE_KEYS.THEME_COLOR)

const isDark = ref(themeStore.get() === 'dark')
const currentTheme = ref<ThemeColorName>((colorStore.get() as ThemeColorName) || 'default')

function applyTheme() {
  document.documentElement.classList.toggle('dark', isDark.value)
  const name = currentTheme.value === 'default' ? '' : currentTheme.value
  if (name) {
    document.documentElement.setAttribute('data-theme', name)
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

applyTheme() // 模块加载即应用，避免刷新后闪白/闪蓝

export function useTheme() {
  const toggleDark = () => {
    isDark.value = !isDark.value
    themeStore.set(isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  const setThemeColor = (name: ThemeColorName) => {
    currentTheme.value = name
    colorStore.set(name)
    applyTheme()
  }

  return {
    isDark,
    currentTheme,
    setThemeColor,
    toggleDark,
    themeColors: THEME_COLORS,
  }
}
