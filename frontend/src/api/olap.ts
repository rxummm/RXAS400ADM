/**
 * OLAP 多维分析 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface OlapInventorySummary {
  cono: string
  warehouse: string
  itemCode: string
  onHandQty: number
  availableQty: number
  stockValue: number
  period: string
}

export interface OlapPurchaseSummary {
  cono: string
  vendorCode: string
  vendorGroup: string
  totalAmount: number
  totalQuantity: number
  orderCount: number
  avgLeadTimeDays: number
  period: string
}

export interface OlapSalesSummary {
  cono: string
  customerCode: string
  customerGroup: string
  totalRevenue: number
  totalQuantity: number
  orderCount: number
  avgOrderValue: number
  period: string
}

export const getSalesSummary = (params: Record<string, unknown>): Promise<PaginatedResult<OlapSalesSummary>> =>
  request.get('/olap/sales-summary', { params })

export const getInventorySummary = (params: Record<string, unknown>): Promise<PaginatedResult<OlapInventorySummary>> =>
  request.get('/olap/inventory-summary', { params })

export const getPurchaseSummary = (params: Record<string, unknown>): Promise<PaginatedResult<OlapPurchaseSummary>> =>
  request.get('/olap/purchase-summary', { params })