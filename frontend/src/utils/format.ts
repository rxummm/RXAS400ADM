/**
 * 统一格式化工具（参照旧项目 ui/src/utils/format.js 收敛）：
 * 各页面统一从这里引用，避免重复实现与口径不一致。
 */

/**
 * 格式化金额数字，null 显示 "—"
 * @param v 数字（可为 null）
 * @param fractionDigits 小数位数（默认 2，如价格/金额；传 0 用于销售总额等整数场景）
 */
export function formatMoney(v: number | null | undefined, fractionDigits = 2): string {
  if (v == null) return '—'
  return v.toLocaleString(undefined, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  })
}

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
 * @param format 格式类型：'datetime'（默认）| 'date' | 'time' | 'relative'
 */
export function formatDate(val?: string | null, format: 'datetime' | 'date' | 'time' | 'relative' = 'datetime'): string {
  if (!val) return ''
  const str = String(val)
  const date = new Date(str)
  if (isNaN(date.getTime())) return str

  const pad = (n: number) => String(n).padStart(2, '0')
  const y = date.getFullYear()
  const m = pad(date.getMonth() + 1)
  const d = pad(date.getDate())
  const h = pad(date.getHours())
  const min = pad(date.getMinutes())
  const s = pad(date.getSeconds())

  switch (format) {
    case 'date':
      return `${y}-${m}-${d}`
    case 'time':
      return `${h}:${min}:${s}`
    case 'relative': {
      const now = Date.now()
      const diff = now - date.getTime()
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 30) return `${days}天前`
      return `${y}-${m}-${d}`
    }
    default:
      return `${y}-${m}-${d} ${h}:${min}:${s}`
  }
}

/**
 * 格式化当前时间为 YYYYMMDDHHmmss，用于导出文件名时间戳
 */
export function formatTimestamp(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}
