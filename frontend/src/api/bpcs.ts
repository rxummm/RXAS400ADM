/**
 * 【AS400 业务增强】BPCS 客户订单时间轴 + 客户档案 + 库存可用量 + 发运看板 API
 */
import request from './request'

/** 分页结果 */
export interface PageResult<T> {
  total: number
  records: T[]
}

/** 时间轴节点 */
export interface TimelineNode {
  stageKey: string
  nameKey: string
  reached: boolean
  timestamp: string | null
  current: boolean
}

/** 冻结信息（预留，本期恒空数组） */
export interface OrderHold {
  type: string
  labelKey: string
}

/** 原始状态透传 */
export interface RawStatus {
  chsts: string
  hstat: string | null
  hid: string | null
}

/** 订单头 + 时间轴 */
export interface BpcsOrderHeader {
  cono: string
  orno: string
  customerNo: string | null
  shipTo: string | null
  orderDate: string | null
  reqDate: string | null
  totalAmount: number | null
  lineCount: number
  currentStageIndex: number
  timeline: TimelineNode[]
  holds: OrderHold[]
  raw: RawStatus
}

/** 订单行 */
export interface BpcsOrderLine {
  orln: string
  item: string | null
  itemDesc: string | null
  wh: string | null
  qtyOrdered: number | null
  qtyAllocated: number | null
  qtyShipped: number | null
  qtyInvoiced: number | null
  price: number | null
  discPct: number | null
  reqDate: string | null
  stageIndex: number
  stageKey: string
  rawClsts: string
}

export const getOrderHeader = (cono: string, orno: string) =>
  request.get<BpcsOrderHeader>('/bpcs/orders/header', { params: { cono, orno } })

export const getOrderLines = (cono: string, orno: string) =>
  request.get<BpcsOrderLine[]>('/bpcs/orders/lines', { params: { cono, orno } })

// ===== P2: 客户档案 =====

/** 收货点 */
export interface ShipTo {
  ship: string
  name: string | null
  address1: string | null
  city: string | null
  state: string | null
  zip: string | null
  phone: string | null
}

/** 客户主档 */
export interface BpcsCustomer {
  cono: string
  cust: string
  name: string | null
  address1: string | null
  address2: string | null
  city: string | null
  state: string | null
  zip: string | null
  phone: string | null
  contact: string | null
  creditLimit: number | null
  termsCode: string | null
  taxCode: string | null
  salesArea: string | null
  shipTos: ShipTo[]
}

export const searchCustomers = (params: { cono?: string; cust?: string; name?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsCustomer>>('/bpcs/customers', { params })

export const getCustomerDetail = (cono: string, cust: string) =>
  request.get<BpcsCustomer>(`/bpcs/customers/${cono}/${cust}`)

// ===== P2: 库存可用量 =====

/** 仓库明细 */
export interface WhDetail {
  wh: string
  onHand: number
  allocated: number
  onOrder: number
  available: number
  location: string | null
}

/** 库存可用量（物料级汇总 + 仓库明细） */
export interface BpcsInventory {
  item: string
  description: string | null
  uom: string | null
  totalOnHand: number
  totalAllocated: number
  totalOnOrder: number
  totalAvailable: number
  unitCost: number | null
  warehouses: WhDetail[]
}

export const searchInventory = (params: { cono?: string; item?: string; desc?: string; wh?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsInventory>>('/bpcs/inventory', { params })

// ===== P2: 发运看板 =====

/** 载荷卡片 */
export interface BpcsLoad {
  cono: string
  lhno: string
  status: number
  statusKey: string
  carrier: string | null
  destination: string | null
  shipDate: string | null
  orderNos: string[]
  lineCount: number
  weight: number
}

export const searchLoads = (params: { cono?: string; lhno?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsLoad>>('/bpcs/shipping', { params })

// ===== P2: 发票轨迹 =====

/** 发票明细行 */
export interface InvoiceLine {
  lineNo: string
  item: string | null
  itemDesc: string | null
  qty: number | null
  unitPrice: number | null
  lineAmount: number | null
}

/** 发票 */
export interface BpcsInvoice {
  invNo: string
  orno: string | null
  cust: string | null
  custName: string | null
  invDate: string | null
  status: string | null
  statusKey: string
  totalAmount: number | null
  taxAmount: number | null
  lineCount: number
  lines: InvoiceLine[]
}

export const searchInvoices = (params: { cono?: string; orno?: string; tab?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsInvoice>>('/bpcs/invoices', { params })

// ===== P2: 销售趋势 =====

/** 月度数据点 */
export interface MonthData {
  ym: string
  revenue: number | null
  orderCount: number
  lineCount: number
}

/** 销售趋势汇总 */
export interface BpcsSalesTrend {
  months: MonthData[]
  totalRevenue: number | null
  totalOrders: number
  totalLines: number
}

export const getSalesTrend = (params: { cono?: string; fromYm?: string; toYm?: string }) =>
  request.get<BpcsSalesTrend>('/bpcs/sales/trend', { params })

// ===== P2: 采购订单 =====

/** 采购订单行 */
export interface PurchaseLine {
  lineNo: string
  item: string | null
  itemDesc: string | null
  qtyOrdered: number | null
  qtyReceived: number | null
  unitPrice: number | null
  reqDate: string | null
}

/** 采购订单 */
export interface BpcsPurchaseOrder {
  cono: string
  pono: string
  vendor: string | null
  vendorName: string | null
  orderDate: string | null
  reqDate: string | null
  totalAmount: number | null
  lineCount: number
  status: number
  statusKey: string
  lines: PurchaseLine[]
}

export const searchPurchases = (params: { cono?: string; pono?: string; vendor?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsPurchaseOrder>>('/bpcs/purchases', { params })

// ===== P2: 物料主档（多 Tab） =====

/** 仓库库存 */
export interface WhStock {
  wh: string
  location: string | null
  onHand: number
  allocated: number
  onOrder: number
  available: number
}

/** 最近采购 */
export interface RecentPurchase {
  pono: string | null
  vendorName: string | null
  orderDate: string | null
  qty: number | null
  unitPrice: number | null
}

/** 最近销售 */
export interface RecentSale {
  orno: string | null
  custName: string | null
  orderDate: string | null
  qty: number | null
  unitPrice: number | null
}

/** 物料主档（含多维度 Tab 数据） */
export interface BpcsItem {
  item: string
  description: string | null
  uom: string | null
  category: string | null
  unitCost: number | null
  listPrice: number | null
  weight: number | null
  shelfLife: number | null
  warehouses: WhStock[]
  totalOnHand: number
  totalAllocated: number
  totalAvailable: number
  recentPurchases: RecentPurchase[]
  recentSales: RecentSale[]
}

export const searchItems = (params: { cono?: string; item?: string; desc?: string; current?: number; size?: number }) =>
  request.get<PageResult<BpcsItem>>('/bpcs/items', { params })

export const getItemDetail = (item: string) =>
  request.get<BpcsItem>(`/bpcs/items/${encodeURIComponent(item)}`)
