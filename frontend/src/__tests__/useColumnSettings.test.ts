import { describe, it, expect, beforeEach } from 'vitest'
import { nextTick } from 'vue'
import { useColumnSettings } from '@/composables/useColumnSettings'

describe('useColumnSettings', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('默认全部列可见', () => {
    const columns = [
      { key: 'name', label: 'Name', width: 100 },
      { key: 'age', label: 'Age', width: 80 },
    ]
    const { visibleColumns } = useColumnSettings({
      columns,
      storageKey: 'test-cols',
    })

    expect(visibleColumns.value).toEqual(['name', 'age'])
  })

  it('隐藏列后持久化到 localStorage', async () => {
    const columns = [
      { key: 'name', label: 'Name', width: 100 },
      { key: 'age', label: 'Age', width: 80 },
    ]
    const { visibleColumns } = useColumnSettings({
      columns,
      storageKey: 'test-cols-toggle',
    })

    // 直接移除 age
    visibleColumns.value = visibleColumns.value.filter((k) => k !== 'age')
    await nextTick()

    expect(visibleColumns.value).toEqual(['name'])

    // 等待 watcher flush
    await nextTick()
    const stored = JSON.parse(
      localStorage.getItem('rx-columns-test-cols-toggle') || '[]',
    )
    expect(stored).toEqual(['name'])
  })

  it('从 localStorage 恢复已保存的列设置', () => {
    localStorage.setItem(
      'rx-columns-test-cols-restore',
      JSON.stringify(['name']),
    )

    const columns = [
      { key: 'name', label: 'Name', width: 100 },
      { key: 'age', label: 'Age', width: 80 },
    ]
    const { visibleColumns } = useColumnSettings({
      columns,
      storageKey: 'test-cols-restore',
    })

    expect(visibleColumns.value).toEqual(['name'])
  })

  it('重置为全部列可见', async () => {
    localStorage.setItem(
      'rx-columns-test-cols-reset',
      JSON.stringify(['name']),
    )

    const columns = [
      { key: 'name', label: 'Name', width: 100 },
      { key: 'age', label: 'Age', width: 80 },
    ]
    const { visibleColumns, columnSettings } = useColumnSettings({
      columns,
      storageKey: 'test-cols-reset',
    })

    expect(visibleColumns.value).toEqual(['name'])

    // 重置：恢复为全部列可见
    visibleColumns.value = columnSettings.columns.map((c) => c.key)
    await nextTick()

    expect(visibleColumns.value).toEqual(['name', 'age'])
  })

  it('columnSettings.columns 透传列配置', () => {
    const columns = [
      { key: 'name', label: 'Name', width: 100 },
      { key: 'age', label: 'Age', width: 80 },
    ]
    const { columnSettings } = useColumnSettings({
      columns,
      storageKey: 'test-cols-props',
    })

    expect(columnSettings.columns).toBe(columns)
    expect(columnSettings.columns.length).toBe(2)
  })
})
