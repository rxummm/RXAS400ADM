/**
 * 质量追溯模块 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface QualityInspection {
  id: number
  inspectionNo: string
  cono: string
  inspectionType: string
  sourceType: string
  sourceNo: string
  itemCode: string
  itemDesc: string
  batchNo: string
  qtyInspected: number
  qtyAccepted: number
  qtyRejected: number
  result: string
  inspector: string
  inspectionDate: string
  defectCode: string
  defectDesc: string
  ncrNo: string
  remark: string
  createdBy: string
  createdTime: string
}

export interface Ncr {
  id: number
  ncrNo: string
  cono: string
  inspectionId: number
  itemCode: string
  itemDesc: string
  batchNo: string
  qtyRejected: number
  defectType: string
  defectDescription: string
  disposition: string
  dispositionDate: string
  rootCause: string
  correctiveAction: string
  preventiveAction: string
  assignedTo: string
  dueDate: string
  status: string
  closeDate: string
  closeRemark: string
  createdBy: string
  createdTime: string
}

export interface SpcRecord {
  id: number
  itemCode: string
  qualityChar: string
  subgroupSize: number
  sampleDate: string
  subgroupNo: number
  value1: number
  value2: number
  value3: number
  value4: number
  value5: number
  mean: number
  range: number
  ucl: number
  cl: number
  lcl: number
  isOutOfControl: number
  remark: string
}

export interface TraceabilityChain {
  id: number
  itemCode: string
  batchNo: string
  traceType: string
  sourceType: string
  sourceNo: string
  targetType: string
  targetNo: string
  relationship: string
  createdTime: string
}

export const searchInspections = (params: Record<string, unknown>): Promise<PaginatedResult<QualityInspection>> =>
  request.get('/quality/inspections', { params })

export const getInspectionById = (id: number): Promise<QualityInspection> =>
  request.get(`/quality/inspections/${id}`)

export const listInspectionsByItem = (itemCode: string): Promise<QualityInspection[]> =>
  request.get(`/quality/inspections/by-item/${itemCode}`)

export const listInspectionsByBatch = (batchNo: string): Promise<QualityInspection[]> =>
  request.get(`/quality/inspections/by-batch/${batchNo}`)

export const searchNcr = (params: Record<string, unknown>): Promise<PaginatedResult<Ncr>> =>
  request.get('/quality/ncr', { params })

export const getNcrById = (id: number): Promise<Ncr> =>
  request.get(`/quality/ncr/${id}`)

export const updateNcrStatus = (id: number, status: string, closeRemark?: string): Promise<void> =>
  request.put(`/quality/ncr/${id}/status`, null, { params: { status, closeRemark } })

export const searchSpc = (params: Record<string, unknown>): Promise<PaginatedResult<SpcRecord>> =>
  request.get('/quality/spc', { params })

export const listSpcOutOfControl = (params: Record<string, unknown>): Promise<SpcRecord[]> =>
  request.get('/quality/spc/out-of-control', { params })

export const traceUpstream = (itemCode: string, batchNo: string): Promise<TraceabilityChain[]> =>
  request.get('/quality/traceability/upstream', { params: { itemCode, batchNo } })

export const traceDownstream = (itemCode: string, batchNo: string): Promise<TraceabilityChain[]> =>
  request.get('/quality/traceability/downstream', { params: { itemCode, batchNo } })

export const tracePaged = (params: Record<string, unknown>): Promise<PaginatedResult<TraceabilityChain>> =>
  request.get('/quality/traceability', { params })
