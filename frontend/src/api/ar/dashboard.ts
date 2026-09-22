/**
 * AR Dashboard API - 应收账款仪表板
 * 注：后端暂无独立 dashboard 端点，统一复用 ar/invoice 列表接口聚合数据
 */
import request from '../request'
import type { ArInvoiceVO } from '../ar'

export interface ArDashboardSummary {
  openInvoiceTotal: number
  openInvoiceCount: number
  overdueTotal: number
  overdueCount: number
  collectionRate: number
  totalInvoiceCount: number
  agingSummary: {
    total: number
    rows: Array<{ bucket: string; total: number; color: string }>
  }
  topCustomers: Array<{ customerCode: string; customerName: string; balance: number; balancePct: number; agingStatus: string }>
}

export const fetchDashboardSummary = (): Promise<ArDashboardSummary> => {
  return request.post('/ar/invoice/page', { current: 1, size: 1000 })
    .then((data: unknown) => {
      const invoices = (data as { records: ArInvoiceVO[]; total: number }).records
      const openInvoices = invoices.filter(i => i.status === 'OPEN' || i.status === 'PARTIAL')
      const overdueInvoices = openInvoices.filter(i => {
        if (!i.dueDate) return false
        const due = new Date(i.dueDate)
        return due < new Date() && (i.balance ?? 0) > 0
      })

      const openTotal = openInvoices.reduce((s, i) => s + (i.balance ?? 0), 0)
      const overdueTotal = overdueInvoices.reduce((s, i) => s + (i.balance ?? 0), 0)
      const totalAmount = invoices.reduce((s, i) => s + (i.totalAmount ?? 0), 0)
      const paidAmount = invoices.reduce((s, i) => s + (i.paidAmount ?? 0), 0)
      const collectionRate = totalAmount > 0 ? paidAmount / totalAmount : 0

      const agingRows: Array<{ bucket: string; total: number; color: string }> = [
        { bucket: 'current', total: 0, color: '#67c23a' },
        { bucket: '0-30', total: 0, color: '#e6a23c' },
        { bucket: '31-60', total: 0, color: '#f56c6c' },
        { bucket: '61-90', total: 0, color: '#f56c6c' },
        { bucket: '90+', total: 0, color: '#f56c6c' },
      ]
      for (const inv of invoices) {
        if (!inv.dueDate || (inv.balance ?? 0) <= 0) continue
        const days = Math.floor((Date.now() - new Date(inv.dueDate).getTime()) / 86400000)
        if (days <= 0) agingRows[0].total += inv.balance ?? 0
        else if (days <= 30) agingRows[1].total += inv.balance ?? 0
        else if (days <= 60) agingRows[2].total += inv.balance ?? 0
        else if (days <= 90) agingRows[3].total += inv.balance ?? 0
        else agingRows[4].total += inv.balance ?? 0
      }

      const customerMap = new Map<string, { code: string; name: string; balance: number }>()
      for (const inv of invoices) {
        const key = inv.customerCode
        const existing = customerMap.get(key)
        if (existing) {
          existing.balance += inv.balance ?? 0
          if (inv.customerName) existing.name = inv.customerName
        } else {
          customerMap.set(key, { code: key, name: inv.customerName || '', balance: inv.balance ?? 0 })
        }
      }
      const topCustomers = Array.from(customerMap.values())
        .sort((a, b) => b.balance - a.balance)
        .slice(0, 10)
        .map(c => ({
          customerCode: c.code,
          customerName: c.name,
          balance: c.balance,
          balancePct: openTotal > 0 ? c.balance / openTotal : 0,
          agingStatus: c.balance > 0 ? 'OVERDUE' : 'OPEN',
        }))

      return {
        openInvoiceTotal: openTotal,
        openInvoiceCount: openInvoices.length,
        overdueTotal,
        overdueCount: overdueInvoices.length,
        collectionRate,
        totalInvoiceCount: invoices.length,
        agingSummary: {
          total: agingRows.reduce((s, r) => s + r.total, 0),
          rows: agingRows,
        },
        topCustomers,
      }
    })
}
