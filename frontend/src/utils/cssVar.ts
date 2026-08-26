/**
 * 读取 CSS 变量计算值（用于 ECharts 等 JS 配置场景）
 *
 * @example
 * import { cssVar } from '@/utils/cssVar'
 * const color = cssVar('--color-success') // '#67c23a'
 */
export function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
