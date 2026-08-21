/**
 * 全局快捷键（参照旧项目 composables/useKeyboardShortcuts.js，TypeScript 化）
 * - 统一注册/注销机制，输入框内自动忽略（Esc 除外）
 * - 默认快捷键由 layout 注册：Ctrl+B 折叠 / Ctrl+D 主题 / Ctrl+R 刷新 / ? 帮助
 */
import { onMounted, onUnmounted, ref } from 'vue'

export interface ShortcutDef {
  key: string
  description: string
  handler: (e: KeyboardEvent) => void
}

const shortcuts = new Map<string, ShortcutDef>()
const enabled = ref(true)
let globalListenerAttached = false

function normalizeKey(key: string): string {
  return key
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace('ctrl', 'control')
    .replace('left', 'arrowleft')
    .replace('right', 'arrowright')
    .replace('up', 'arrowup')
    .replace('down', 'arrowdown')
}

function parseKeyEvent(e: KeyboardEvent): string {
  const keys: string[] = []
  if (e.ctrlKey || e.metaKey) keys.push('control')
  if (e.altKey) keys.push('alt')
  if (e.shiftKey) keys.push('shift')
  if (e.key === ' ') keys.push('space')
  else if (e.key.startsWith('Arrow')) keys.push(e.key.toLowerCase())
  else if (e.key.length === 1) keys.push(e.key.toLowerCase())
  else keys.push(e.key.toLowerCase())
  return keys.join('+')
}

function formatKeyDisplay(key: string): string {
  return key
    .replace('control', 'Ctrl')
    .replace('alt', 'Alt')
    .replace('shift', 'Shift')
    .replace('arrowleft', '←')
    .replace('arrowright', '→')
    .replace('space', 'Space')
    .split('+')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' + ')
}

function handleKeyDown(e: KeyboardEvent) {
  if (!enabled.value) return
  // 输入框内忽略（Esc 除外，用于关闭弹窗）
  const target = e.target as HTMLElement
  const isInput =
    target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
  if (isInput && e.key !== 'Escape') return

  const combo = parseKeyEvent(e)
  const def = shortcuts.get(combo)
  if (def) {
    e.preventDefault()
    e.stopPropagation()
    def.handler(e)
  }
}

export function useShortcuts(configs: ShortcutDef[] = []) {
  onMounted(() => {
    configs.forEach((c) => shortcuts.set(normalizeKey(c.key), c))
    if (!globalListenerAttached) {
      document.addEventListener('keydown', handleKeyDown, true)
      globalListenerAttached = true
    }
  })

  onUnmounted(() => {
    configs.forEach((c) => shortcuts.delete(normalizeKey(c.key)))
  })

  /** 帮助面板展示列表（按 key 排序） */
  function getRegisteredShortcuts(): { key: string; description: string }[] {
    return [...shortcuts.entries()]
      .map(([k, v]) => ({ key: formatKeyDisplay(k), description: v.description }))
      .sort((a, b) => a.key.localeCompare(b.key))
  }

  return { getRegisteredShortcuts, enabled }
}
