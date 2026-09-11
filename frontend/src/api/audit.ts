import request from './request'
import type { PaginatedResult } from './types'

/** 审计日志记录（rx_audit_log） */
export interface AuditLog {
  id: number
  module: string
  action: string
  userName: string
  target?: string
  ip?: string
  detail?: string
  createdTime: string
}

export const listAuditLogs = (params: {
  current?: number
  size?: number
  module?: string
  username?: string
  action?: string
  keyword?: string
} = {}): Promise<PaginatedResult<AuditLog>> =>
  request.get('/audit-logs', { params })
