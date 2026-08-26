import { onMounted, onUnmounted } from 'vue'

export interface Shortcut {
  key: string
  description: string
  handler: (event: KeyboardEvent) => void
}

export function useShortcuts(shortcuts: Shortcut[]) {
  const registeredShortcuts = shortcuts.map((s) => ({ key: s.key, description: s.description }))

  function parseKeyCombination(key: string): { ctrl: boolean; shift: boolean; alt: boolean; meta: boolean; code: string } {
    const lower = key.toLowerCase()
    return {
      ctrl: lower.includes('ctrl+'),
      shift: lower.includes('shift+'),
      alt: lower.includes('alt+'),
      meta: lower.includes('meta+'),
      code: key.split('+').pop()!.toLowerCase(),
    }
  }

  function matchKey(event: KeyboardEvent, combo: ReturnType<typeof parseKeyCombination>): boolean {
    if (combo.ctrl !== event.ctrlKey) return false
    if (combo.shift !== event.shiftKey) return false
    if (combo.alt !== event.altKey) return false
    if (combo.meta !== event.metaKey) return false

    const eventKey = event.key.toLowerCase()
    if (combo.code === '?' || combo.code === '/') {
      return event.key === '?' || (event.key === '/' && event.shiftKey)
    }
    return eventKey === combo.code
  }

  function handleKeyDown(event: KeyboardEvent) {
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
      if (event.key !== '?') return
    }

    for (const shortcut of shortcuts) {
      const combo = parseKeyCombination(shortcut.key)
      if (matchKey(event, combo)) {
        event.preventDefault()
        shortcut.handler(event)
        return
      }
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })

  return { getRegisteredShortcuts: () => registeredShortcuts }
}
