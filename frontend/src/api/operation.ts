import request from './request'
import type { PaginatedResult } from './types'

export interface CreateOperationReq {
  operationType: string
  targetType?: string
  targetName?: string
  requestData?: string
  idempotencyKey?: string
}

export interface OperationVO {
  id: number
  operationType: string
  status: string
  currentStep?: string
  steps?: string[]
  targetType?: string
  targetName?: string
  requestData?: string
  resultData?: string
  errorCode?: string
  errorMessage?: string
  retryCount: number
  maxRetry: number
  idempotencyKey?: string
  riskLevel?: string
  requestedBy?: string
  requestedAt?: string
  startedAt?: string
  completedAt?: string
}

export function createOperation(data: CreateOperationReq): Promise<OperationVO> {
  return request.post('/operations', data)
}

export function getOperation(id: number): Promise<OperationVO> {
  return request.get(`/operations/${id}`)
}

export function listOperations(current: number, size: number): Promise<PaginatedResult<OperationVO>> {
  return request.get('/operations', { params: { current, size } })
}

export function retryOperation(id: number): Promise<OperationVO> {
  return request.post(`/operations/${id}/retry`)
}

export function cancelOperation(id: number): Promise<void> {
  return request.post(`/operations/${id}/cancel`)
}