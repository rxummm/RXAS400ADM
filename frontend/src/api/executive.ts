/**
 * Executive Dashboard API
 */
import request from './request'

/** 月度数据点 */
export interface MonthData {
  ym: string
  revenue: number | null
  orderCount: number
}

/** Top N 条目 */
export interface TopEntry {
  code: string
  name: string | null
  count: number
  amount: number | null
}

/** Executive Dashboard 汇总数据 */
export interface ExecutiveSummary {
  // 系统概览
  totalServers: number
  onlineServers: number
  offlineServers: number
  // 监控指标
  avgCpu: number | null
  avgMemory: number | null
  avgDisk: number | null
  activeAlerts: number | null
  criticalAlerts: number | null
  // BPCS 业务概览
  totalOrders: number
  closedOrders: number
  completionRate: number | null
  totalItems: number
  inventoryValue: number | null
  onTimeDeliveryRate: number | null
  // 趋势与排名
  salesTrend: MonthData[]
  topItems: TopEntry[]
  topCustomers: TopEntry[]
}

export const getExecutiveSummary = (cono = '001'): Promise<ExecutiveSummary> =>
  request.get('/dashboard/executive/summary', { params: { cono } })
