/**
 * MRP 物料需求计划 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface BomMaster {
  id: number
  bomNo: string
  cono: string
  parentItem: string
  parentDesc: string
  bomVersion: string
  effectiveDate: string
  expiryDate: string
  status: string
  createdBy: string
  createdTime: string
}

export interface BomLine {
  id: number
  bomId: number
  lineNo: number
  componentItem: string
  componentDesc: string
  quantity: number
  uom: string
  scrapRate: number
  efficiency: number
  createdTime: string
}

export interface MrpDemand {
  id: number
  demandNo: string
  cono: string
  demandType: string
  demandSource: string
  itemCode: string
  itemDesc: string
  grossRequirement: number
  scheduledReceipt: number
  allocated: number
  onHand: number
  safetyStock: number
  netRequirement: number
  requiredDate: string
  status: string
  createdBy: string
  createdTime: string
}

export interface MrpRecommendation {
  id: number
  recommendationNo: string
  cono: string
  demandId: number
  itemCode: string
  itemDesc: string
  recommendQty: number
  recommendType: string
  leadTimeDays: number
  suggestedDate: string
  orderNo: string
  status: string
  createdBy: string
  createdTime: string
}

export const searchBomMasters = (params: Record<string, unknown>): Promise<PaginatedResult<BomMaster>> =>
  request.get('/mrp/bom', { params })

export const getBomById = (id: number): Promise<BomMaster> =>
  request.get(`/mrp/bom/${id}`)

export const getBomLines = (id: number): Promise<BomLine[]> =>
  request.get(`/mrp/bom/${id}/lines`)

export const expandBom = (parentItem: string, maxLevels = 3): Promise<BomLine[]> =>
  request.get(`/mrp/bom/expand/${parentItem}`, { params: { maxLevels } })

export const searchMrpDemands = (params: Record<string, unknown>): Promise<PaginatedResult<MrpDemand>> =>
  request.get('/mrp/demands', { params })

export const updateMrpDemandStatus = (id: number, status: string): Promise<void> =>
  request.put(`/mrp/demands/${id}/status`, null, { params: { status } })

export const searchMrpRecommendations = (params: Record<string, unknown>): Promise<PaginatedResult<MrpRecommendation>> =>
  request.get('/mrp/recommendations', { params })

export const releaseRecommendation = (id: number): Promise<void> =>
  request.put(`/mrp/recommendations/${id}/release`)
