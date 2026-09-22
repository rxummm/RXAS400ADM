/** 审批中心 API */
import request from './request'
import type { PaginatedResult } from './types'

export interface ApprovalNotification {
  id: number
  title: string
  content?: string
  targetType: string
  targetId: number
  approverId: number
  approverName: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  action?: 'APPROVED' | 'REJECTED' | 'RETURNED'
  comment?: string
  createdBy: string
  createdTime: string
}

export interface ApprovalActionDTO {
  notificationId: number
  action: 'APPROVED' | 'REJECTED' | 'RETURNED'
  comment?: string
}

/** 待审批列表 */
export const listPendingApprovals = (current: number, size: number): Promise<PaginatedResult<ApprovalNotification>> =>
  request.get('/approvals/pending', { params: { current, size } })

/** 全部审批历史 */
export const listAllApprovals = (
  current: number,
  size: number,
  status?: string,
  targetType?: string
): Promise<PaginatedResult<ApprovalNotification>> =>
  request.get('/approvals/all', { params: { current, size, status, targetType } })

/** 执行审批 */
export const approveNotification = (id: number, dto: ApprovalActionDTO): Promise<ApprovalNotification> =>
  request.post(`/approvals/${id}/approve`, dto)

/** 未读审批数量 */
export const countPendingApprovals = (): Promise<number> =>
  request.get('/approvals/pending/count')

/** 审批统计卡片数据 */
export const getApprovalStats = (): Promise<[number, number]> =>
  request.get('/approvals/stats')
