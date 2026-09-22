/**
 * 采购订单管理（PO Management）API
 * 本地管理 + 多级审批流 + 状态跟踪
 */
import request from './request'
import type { PaginatedResult } from './types'

// ===== PO 状态常量 =====
export const PO_STATUS = {
  DRAFT: 'DRAFT',
  PENDING_APPROVAL: 'PENDING_APPROVAL',
  APPROVED: 'APPROVED',
  SHIPPED: 'SHIPPED',
  RECEIVED: 'RECEIVED',
  PAID: 'PAID',
  CANCELLED: 'CANCELLED',
} as const

export type PoStatus = (typeof PO_STATUS)[keyof typeof PO_STATUS]

// ===== 类型定义 =====

/** 采购订单行项 */
export interface PurchaseOrderItem {
  id?: number
  poId?: number
  lineNo?: number
  itemCode?: string
  itemDesc?: string
  uom?: string
  qtyOrdered?: number
  qtyReceived?: number
  qtyInvoiced?: number
  unitPrice?: number
  lineAmount?: number
  reqDate?: string
  receivedDate?: string
  notes?: string
}

/** 采购订单列表 VO */
export interface PurchaseOrderVO {
  id: number
  poNo: string
  cono: string
  vendorCode?: string
  vendorName?: string
  orderDate?: string
  reqDate?: string
  totalAmount?: number
  currency: string
  status: string
  statusKey: string
  approvalLevel?: number
  approvalStatus?: string
  approvedBy?: string
  approvedTime?: string
  notes?: string
  as400PoNo?: string
  createdBy: string
  createdTime: string
}

/** 审批记录 */
export interface PurchaseApprovalRecord {
  id: number
  poId: number
  level: number
  approver: string
  action: string
  comment?: string
  actionTime: string
}

/** 采购订单详情 VO */
export interface PurchaseOrderDetail extends PurchaseOrderVO {
  updatedBy?: string
  updatedTime?: string
  items: PurchaseOrderItem[]
  approvals: PurchaseApprovalRecord[]
}

/** 创建 DTO */
export interface PurchaseOrderCreateDTO {
  poNo: string
  cono?: string
  vendorCode?: string
  vendorName?: string
  orderDate?: string
  reqDate?: string
  currency?: string
  notes?: string
  items: {
    lineNo?: number
    itemCode?: string
    itemDesc?: string
    uom?: string
    qtyOrdered?: number
    unitPrice?: number
    reqDate?: string
    notes?: string
  }[]
}

/** 更新 DTO */
export interface PurchaseOrderUpdateDTO {
  poNo?: string
  vendorCode?: string
  vendorName?: string
  orderDate?: string
  reqDate?: string
  currency?: string
  notes?: string
  items?: {
    lineNo?: number
    itemCode?: string
    itemDesc?: string
    uom?: string
    qtyOrdered?: number
    unitPrice?: number
    reqDate?: string
    notes?: string
  }[]
}

/** 查询参数 */
export interface PurchaseOrderQuery {
  poNo?: string
  vendorCode?: string
  vendorName?: string
  status?: string
  orderDateFrom?: string
  orderDateTo?: string
  current?: number
  size?: number
}

/** 审批操作 DTO */
export interface PurchaseApprovalAction {
  poId: number
  action: 'APPROVED' | 'REJECTED' | 'RETURNED'
  comment?: string
}

// ===== API 方法 =====

/** 分页查询采购订单 */
export const pagePurchaseOrders = (params: PurchaseOrderQuery): Promise<PaginatedResult<PurchaseOrderVO>> =>
  request.post('/procurement/po/page', params)

/** 获取采购订单详情 */
export const getPurchaseOrderDetail = (id: number): Promise<PurchaseOrderDetail> =>
  request.get(`/procurement/po/${id}`)

/** 生成采购单号 */
export const generatePoNo = (): Promise<string> =>
  request.get('/procurement/po/generate-po-no')

/** 新建采购订单 */
export const createPurchaseOrder = (dto: PurchaseOrderCreateDTO): Promise<PurchaseOrderVO> =>
  request.post('/procurement/po', dto)

/** 更新采购订单 */
export const updatePurchaseOrder = (id: number, dto: PurchaseOrderUpdateDTO): Promise<PurchaseOrderVO> =>
  request.put(`/procurement/po/${id}`, dto)

/** 提交审批 */
export const submitForApproval = (id: number): Promise<void> =>
  request.post(`/procurement/po/${id}/submit`)

/** 审批操作 */
export const handleApproval = (dto: PurchaseApprovalAction): Promise<void> =>
  request.post('/procurement/po/approval', dto)

/** 收货 */
export const receiveOrder = (id: number): Promise<void> =>
  request.post(`/procurement/po/${id}/receive`)

/** 取消订单 */
export const cancelOrder = (id: number): Promise<void> =>
  request.post(`/procurement/po/${id}/cancel`)

/** 删除订单 */
export const deletePurchaseOrder = (id: number): Promise<void> =>
  request.delete(`/procurement/po/${id}`)