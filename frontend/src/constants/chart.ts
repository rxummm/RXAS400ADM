/**
 * ECharts 图表共享颜色常量
 * 所有 BPCS 页面的图表颜色统一从此处引用，避免散落 30+ 处硬编码
 */
export const CHART_COLORS = {
  /** 主色（蓝）— 默认系列 */
  primary: '#409EFF',
  /** 成功/正面（绿）— 实际值、在手库存 */
  success: '#67C23A',
  /** 警告/中性（橙）— 季节性、计划收货 */
  warning: '#E6A23C',
  /** 危险/负面（红）— 预测、安全库存 */
  danger: '#F56C6C',
  /** 灰色 — 辅助/背景系列 */
  info: '#909399',
} as const

/** 按顺序获取图表系列颜色（循环使用） */
export function chartColor(index: number): string {
  const colors = Object.values(CHART_COLORS)
  return colors[index % colors.length]
}
