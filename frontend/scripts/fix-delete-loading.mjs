/**
 * 批量修复删除操作：添加 loading 状态 + try/catch + useConfirmDelete 集成
 *
 * 扫描所有含 ElMessageBox.confirm 的 Vue 文件，
 * 将删除函数改为使用 useConfirmDelete composable。
 */
import { readFileSync, writeFileSync } from 'fs'
import { globSync } from 'glob'

const files = globSync('src/views/**/*.vue', { cwd: process.cwd() })

let fixed = 0
let skipped = 0

for (const file of files) {
  const fullPath = file
  let content = readFileSync(fullPath, 'utf-8')

  // Skip if already uses useConfirmDelete
  if (content.includes('useConfirmDelete')) {
    skipped++
    continue
  }

  // Skip if no MessageBox.confirm
  if (!content.includes('MessageBox.confirm')) {
    skipped++
    continue
  }

  // Pattern 1: Simple delete with no try/catch
  // const remove = async (row: Xxx) => {
  //   await ElMessageBox.confirm(...)
  //   await deleteXxx(row.id)
  //   ElMessage.success(...)
  //   await load()
  // }

  // Pattern 2: Delete with try/catch but no loading
  // const remove = async (row: Xxx) => {
  //   try {
  //     await ElMessageBox.confirm(...)
  //   } catch { return }
  //   try {
  //     await deleteXxx(row.id)
  //     ElMessage.success(...)
  //     await load()
  //   } catch { ... }
  // }

  // Strategy: Add useConfirmDelete import and convert the delete function
  // This requires manual per-file approach due to varying patterns

  console.log(`NEEDS_FIX: ${fullPath}`)
  fixed++
}

console.log(`\nTotal: ${files.length} files, ${fixed} need fixing, ${skipped} skipped`)
