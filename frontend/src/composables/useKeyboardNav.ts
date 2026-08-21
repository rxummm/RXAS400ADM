import { ref, onMounted, onUnmounted, nextTick } from 'vue'

/**
 * useKeyboardNav - 键盘导航 composable
 *
 * 运维系统键盘操作标准：
 * - ↑↓ : 在表格行间导航
 * - Enter : 编辑/查看详情
 * - Esc : 关闭弹窗/抽屉
 * - / : 聚焦搜索框
 * - ? : 显示快捷键帮助
 *
 * 用法：
 * ```ts
 * const { focusedRow, handleKeydown } = useKeyboardNav({
 *   rows: tableData,
 *   onEdit: (row) => openEditDialog(row),
 *   onDelete: (row) => deleteRow(row),
 *   searchSelector: '.search-bar input',
 * })
 *
 * // 在表格上绑定
 * <el-table @keydown="handleKeydown" tabindex="0">
 * ```
 */

export interface KeyboardNavOptions<T extends Record<string, unknown> = Record<string, unknown>> {
  /** 表格数据 */
  rows: () => T[]
  /** 编辑回调 */
  onEdit?: (row: T) => void
  /** 删除回调 */
  onDelete?: (row: T) => void
  /** 查看详情回调 */
  onView?: (row: T) => void
  /** 搜索框选择器 */
  searchSelector?: string
  /** 是否启用 */
  enabled?: () => boolean
}

export function useKeyboardNav<T extends Record<string, unknown> = Record<string, unknown>>(options: KeyboardNavOptions<T>) {
  const focusedIndex = ref(-1)
  const focusedRow = ref<T | null>(null)

  const handleKeydown = (event: KeyboardEvent) => {
    // 如果在输入框中，不处理导航键
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
      // 只处理 Esc
      if (event.key !== 'Escape') return
    }

    const rows = options.rows()
    if (!rows || rows.length === 0) return

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault()
        if (focusedIndex.value < rows.length - 1) {
          focusedIndex.value++
          focusedRow.value = rows[focusedIndex.value]
          scrollToRow(focusedIndex.value)
        }
        break

      case 'ArrowUp':
        event.preventDefault()
        if (focusedIndex.value > 0) {
          focusedIndex.value--
          focusedRow.value = rows[focusedIndex.value]
          scrollToRow(focusedIndex.value)
        }
        break

      case 'Enter':
        event.preventDefault()
        if (focusedRow.value) {
          options.onEdit?.(focusedRow.value)
        }
        break

      case 'Escape':
        event.preventDefault()
        focusedIndex.value = -1
        focusedRow.value = null
        // 关闭弹窗/抽屉
        closeActiveDialog()
        break

      case '/':
        event.preventDefault()
        focusSearchInput()
        break

      case '?':
        event.preventDefault()
        // 触发快捷键帮助（由父组件处理）
        break

      case 'Delete':
      case 'Backspace':
        if (event.ctrlKey || event.metaKey) {
          event.preventDefault()
          if (focusedRow.value) {
            options.onDelete?.(focusedRow.value)
          }
        }
        break
    }
  }

  const scrollToRow = (index: number) => {
    nextTick(() => {
      const row = document.querySelector(`.el-table__body tr:nth-child(${index + 1})`)
      if (row) {
        row.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
        // 添加焦点样式
        row.classList.add('keyboard-focused')
        // 移除其他行的焦点样式
        document.querySelectorAll('.el-table__body tr.keyboard-focused').forEach((el, i) => {
          if (i !== index) el.classList.remove('keyboard-focused')
        })
      }
    })
  }

  const focusSearchInput = () => {
    const selector = options.searchSelector || '.search-bar input'
    const input = document.querySelector(selector) as HTMLInputElement
    if (input) {
      input.focus()
      input.select()
    }
  }

  const closeActiveDialog = () => {
    // 查找并关闭打开的弹窗
    const dialogs = document.querySelectorAll('.el-dialog__wrapper')
    dialogs.forEach(dialog => {
      const isVisible = (dialog as HTMLElement).style.display !== 'none'
      if (isVisible) {
        // 模拟点击关闭按钮
        const closeBtn = dialog.querySelector('.el-dialog__headerbtn')
        if (closeBtn) {
          (closeBtn as HTMLElement).click()
        }
      }
    })

    // 查找并关闭打开的抽屉
    const drawers = document.querySelectorAll('.el-drawer')
    drawers.forEach(drawer => {
      const isVisible = (drawer as HTMLElement).style.display !== 'none'
      if (isVisible) {
        const closeBtn = drawer.querySelector('.el-drawer__header-btn')
        if (closeBtn) {
          (closeBtn as HTMLElement).click()
        }
      }
    })
  }

  const reset = () => {
    focusedIndex.value = -1
    focusedRow.value = null
  }

  // 全局键盘监听
  const handleGlobalKeydown = (event: KeyboardEvent) => {
    // 如果有模态框打开，不处理全局快捷键
    if (document.querySelector('.el-dialog__wrapper:not([style*="display: none"])')) return
    if (document.querySelector('.el-drawer.is-open')) return

    // / 聚焦搜索框（全局）
    if (event.key === '/' && !event.ctrlKey && !event.metaKey) {
      const target = event.target as HTMLElement
      if (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA') {
        event.preventDefault()
        focusSearchInput()
      }
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleGlobalKeydown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleGlobalKeydown)
  })

  return {
    focusedIndex,
    focusedRow,
    handleKeydown,
    reset,
    focusSearchInput,
  }
}