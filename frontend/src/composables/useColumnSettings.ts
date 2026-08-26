import { ref, watch } from 'vue'

export interface ColumnOption {
  key: string
  label: string
  width?: number
  defaultVisible?: boolean
}

export interface UseColumnSettingsOptions {
  columns: ColumnOption[]
  storageKey: string
}

const STORAGE_PREFIX = 'rx-columns-'

export function useColumnSettings(options: UseColumnSettingsOptions) {
  const { columns, storageKey } = options
  const fullKey = `${STORAGE_PREFIX}${storageKey}`

  function loadSaved(): string[] | null {
    try {
      const raw = localStorage.getItem(fullKey)
      if (!raw) return null
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : null
    } catch {
      return null
    }
  }

  function save(keys: string[]) {
    try {
      localStorage.setItem(fullKey, JSON.stringify(keys))
    } catch {
      /* storage full / private mode */
    }
  }

  const allKeys = columns.map((c) => c.key)
  const saved = loadSaved()
  const visibleColumns = ref<string[]>(saved ?? allKeys)

  watch(visibleColumns, (val) => save(val), { deep: true })

  const columnSettings = {
    columns,
  }

  return { visibleColumns, columnSettings }
}
