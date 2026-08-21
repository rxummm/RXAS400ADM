/**
 * 统一格式化工具（参照旧项目 ui/src/utils/format.js 收敛）：
 * 各页面统一从这里引用，避免重复实现与口径不一致。
 */

/**
 * 格式化字节数为可读大小，如 2048 -> "2.0 KB"
 * @param bytes 字节数（可为 null/undefined/字符串数字）
 */
export function formatSize(bytes: number | string | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes === '') return '0 B'
  const n = Number(bytes)
  if (!Number.isFinite(n) || n === 0) return '0 B'
  const negative = n < 0
  let size = Math.abs(n)
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return (negative ? '-' : '') + size.toFixed(1) + ' ' + units[i]
}

/**
 * 格式化时间为可读文本，如 "2026-08-12T09:00:00" -> "2026-08-12 09:00:00"
 * @param val 日期字符串（ISO 或含 T 分隔）
 */
export function formatDate(val?: string | null): string {
  if (!val) return ''
  return String(val).replace('T', ' ').substring(0, 19)
}

/**
 * 格式化当前时间为 YYYYMMDDHHmmss，用于导出文件名时间戳
 */
export function formatTimestamp(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}
