/** 应收账款管理（AR Management）API */
import request from './request'
import type { PaginatedResult } from './types'

// ===== AR 状态常量 =====
export const AR_STATUS = {
  DRAFT: 'DRAFT',
  OPEN: 'OPEN',
  PARTIAL: 'PARTIAL',
  PAID: 'PAID',
  OVERDUE: 'OVERDUE',
  WRITE_OFF: 'WRITE_OFF',
} as const

export type ArStatus = (typeof AR_STATUS)[keyof typeof AR_STATUS]

// ===== 付款方式常量 =====
export const PAYMENT_METHOD = {
  BANK_TRANSFER: 'BANK_TRANSFER',
  CASH: 'CASH',
  CHECK: 'CHECK',
  CREDIT_CARD: 'CREDIT_CARD',
  OTHER: 'OTHER',
} as const

export type PaymentMethod = (typeof PAYMENT_METHOD)[keyof typeof PAYMENT_METHOD]

// ===== 类型定义 =====

/** 应收账款账单列表项 */
export interface ArInvoiceVO {
  id: number
  invoiceNo: string
  cono: string
  customerCode: string
  customerName?: string
  invoiceDate?: string
  dueDate?: string
  originPoNo?: string
  originSoNo?: string
  subtotal?: number
  taxRate?: number
  taxAmount?: number
  totalAmount: number
  paidAmount: number
  balance: number
  currency: string
  status: string
  statusKey: string
  agingBucket?: string
  notes?: string
  createdBy: string
  createdTime: string
  updatedBy?: string
  updatedTime?: string
}

/** 收款记录 */
export interface ArPaymentVO {
  id: number
  paymentNo: string
  cono: string
  invoiceId: number
  paymentDate?: string
  amount: number
  paymentMethod: string
  paymentMethodKey: string
  referenceNo?: string
  receivedBy?: string
  notes?: string
  createdBy: string
  createdTime: string
}

/** 账龄汇总 */
export interface ArAgingSummary {
  current?: number
  aging0_30?: number
  aging31_60?: number
  aging61_90?: number
  aging90plus?: number
  total?: number
}

/** 应收账款详情 */
export interface ArInvoiceDetail extends ArInvoiceVO {
  payments: ArPaymentVO[]
  agingSummary: ArAgingSummary
}

/** 创建账单 DTO */
export interface ArInvoiceCreateDTO {
  invoiceNo?: string
  customerCode: string
  customerName?: string
  invoiceDate: string
  dueDate: string
  originPoNo?: string
  originSoNo?: string
  totalAmount: number
  subtotal?: number
  taxRate?: number
  taxAmount?: number
  currency?: string
  notes?: string
}

/** 更新账单 DTO */
export interface ArInvoiceUpdateDTO {
  customerCode?: string
  customerName?: string
  invoiceDate?: string
  dueDate?: string
  originPoNo?: string
  originSoNo?: string
  totalAmount?: number
  subtotal?: number
  taxRate?: number
  taxAmount?: number
  currency?: string
  notes?: string
}

/** 查询参数 */
export interface ArInvoiceQuery {
  invoiceNo?: string
  customerCode?: string
  customerName?: string
  status?: string
  overdue?: boolean
  invoiceDateFrom?: string
  invoiceDateTo?: string
  dueDateFrom?: string
  dueDateTo?: string
  current?: number
  size?: number
}

/** 收款核销 DTO */
export interface ArPaymentCreateDTO {
  invoiceId: number
  paymentNo?: string
  paymentDate: string
  amount: number
  paymentMethod?: string
  referenceNo?: string
  receivedBy?: string
  notes?: string
}

// ===== API 方法 =====

/** 分页查询应收账款 */
export const pageArInvoices = (params: ArInvoiceQuery): Promise<PaginatedResult<ArInvoiceVO>> =>
  request.post('/ar/invoice/page', params)

/** 获取账单详情 */
export const getArInvoiceDetail = (id: number): Promise<ArInvoiceDetail> =>
  request.get(`/ar/invoice/${id}`)

/** 生成账单号 */
export const generateArInvoiceNo = (): Promise<string> =>
  request.get('/ar/invoice/generate-invoice-no')

/** 生成收款单号 */
export const generateArPaymentNo = (): Promise<string> =>
  request.get('/ar/invoice/generate-payment-no')

/** 新建账单 */
export const createArInvoice = (dto: ArInvoiceCreateDTO): Promise<ArInvoiceVO> =>
  request.post('/ar/invoice', dto)

/** 更新账单 */
export const updateArInvoice = (id: number, dto: ArInvoiceUpdateDTO): Promise<ArInvoiceVO> =>
  request.put(`/ar/invoice/${id}`, dto)

/** 删除账单 */
export const deleteArInvoice = (id: number): Promise<void> =>
  request.delete(`/ar/invoice/${id}`)

/** 收款核销 */
export const recordArPayment = (id: number, dto: ArPaymentCreateDTO): Promise<void> =>
  request.post(`/ar/invoice/${id}/receive`, dto)

/** 刷新逾期状态 */
export const refreshArOverdue = (): Promise<void> =>
  request.post('/ar/invoice/refresh-overdue')
