/**
 * 成本核算模块 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface CostCollection {
  id: number
  collectionNo: string
  cono: string
  costType: string
  costObjectType: string
  costObjectNo: string
  costObjectName: string
  period: string
  materialCost: number
  laborCost: number
  overheadCost: number
  totalCost: number
  unitCost: number
  qtyProduced: number
  status: string
  remark: string
  createdBy: string
  createdTime: string
}

export interface CostVariance {
  id: number
  varianceNo: string
  cono: string
  itemCode: string
  itemDesc: string
  costComponent: string
  standardCost: number
  actualCost: number
  varianceAmount: number
  variancePct: number
  varianceType: string
  period: string
  rootCause: string
  improvementAction: string
  status: string
  createdBy: string
  createdTime: string
}

export interface ProfitAnalysis {
  id: number
  analysisNo: string
  cono: string
  analysisType: string
  analysisKey: string
  analysisName: string
  period: string
  totalRevenue: number
  totalCost: number
  grossProfit: number
  profitMargin: number
  orderCount: number
  itemCount: number
  status: string
  remark: string
  createdBy: string
  createdTime: string
}

export const searchCostCollections = (params: Record<string, unknown>): Promise<PaginatedResult<CostCollection>> =>
  request.get('/cost/collections', { params })

export const getCostCollectionById = (id: number): Promise<CostCollection> =>
  request.get(`/cost/collections/${id}`)

export const listCostCollectionsByPeriod = (period: string): Promise<CostCollection[]> =>
  request.get(`/cost/collections/by-period/${period}`)

export const searchCostVariances = (params: Record<string, unknown>): Promise<PaginatedResult<CostVariance>> =>
  request.get('/cost/variances', { params })

export const getCostVarianceById = (id: number): Promise<CostVariance> =>
  request.get(`/cost/variances/${id}`)

export const searchProfitAnalyses = (params: Record<string, unknown>): Promise<PaginatedResult<ProfitAnalysis>> =>
  request.get('/cost/profit-analysis', { params })
