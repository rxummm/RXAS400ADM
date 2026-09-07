/**
 * 【AS400 业务增强】BPCS 客户订单时间轴 + 客户档案 + 库存可用量 + 发运看板 API
 */
import request from './request'
import blobClient from './blobClient'

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

// ===== BPCS 导出端点（Excel / PDF；CSV 由前端 ExportDropdown 生成） =====

export const BPCS_EXPORT = {
  orders: '/bpcs/export/orders',
  customers: '/bpcs/export/customers',
  inventory: '/bpcs/export/inventory',
  shipping: '/bpcs/export/shipping',
  invoices: '/bpcs/export/invoices',
  sales: '/bpcs/export/sales',
  purchases: '/bpcs/export/purchases',
  items: '/bpcs/export/items',
  supplyChain: (type: string) => `/bpcs/export/supply-chain/${type}`,
} as const

// ===== ⑩ 履行率与 Backorder =====

/** 行级履行率统计 */
export interface FulfillmentStats {
  totalLines: number
  filledLines: number
  lineFillRate: number
  totalOrdered: number
  totalShipped: number
  qtyFillRate: number
  backorderLines: number
  backorderQty: number
}

/** Backorder 行明细 */
export interface BackorderLine {
  cono: string
  orno: string
  orln: string
  item: string
  itemDesc: string
  qtyOrdered: number
  qtyShipped: number
  qtyAllocated: number
  qtyOpen: number
  customerNo: string
  reqDate: string
  headerStatus: string
}

/** Backorder 按物料聚合 */
export interface BackorderByItem {
  item: string
  itemDesc: string
  backorderCount: number
  totalBackorderQty: number
}

export const getFulfillmentStats = (cono = '001') =>
  request.get<FulfillmentStats>('/bpcs/orders/analytics/fulfillment', { params: { cono } })

export const getBackorderLines = (params: { cono?: string; itemFilter?: string; size?: number }) =>
  request.get<BackorderLine[]>('/bpcs/orders/analytics/backorder', { params })

export const getBackorderByItem = (cono = '001', limit = 20) =>
  request.get<BackorderByItem[]>('/bpcs/orders/analytics/backorder/by-item', { params: { cono, limit } })

// ===== ⑬ 交期绩效 OTD =====

/** OTD 统计 */
export interface OtdStats {
  totalDelivered: number
  onTime: number
  early: number
  late: number
  onTimeRate: number
  avgLateDays: number
}

/** OTD 按客户聚合 */
export interface OtdByCustomer {
  customerNo: string
  customerName: string
  totalOrders: number
  onTimeOrders: number
  otdRate: number
}

export const getOtdStats = (cono = '001') =>
  request.get<OtdStats>('/bpcs/orders/analytics/otd', { params: { cono } })

export const getOtdByCustomer = (cono = '001', limit = 20) =>
  request.get<OtdByCustomer[]>('/bpcs/orders/analytics/otd/by-customer', { params: { cono, limit } })

// ===== ㊿ 库存多级一致性 =====

/** 库存层级明细 */
export interface LevelDetail {
  identifier: string
  quantity: number
  extraInfo: string
}

/** 库存层级汇总 */
export interface LevelSummary {
  levelName: string
  totalQuantity: number
  detailCount: number
  matched: boolean
  details: LevelDetail[]
}

/** 一致性核对结果 */
export interface ConsistencyResult {
  item: string
  palletLevel: LevelSummary
  locationLevel: LevelSummary
  warehouseLevel: LevelSummary
  consistent: boolean
  discrepancyNote: string
}

export const checkInventoryConsistency = (cono: string, item: string) =>
  request.get<ConsistencyResult>('/bpcs/inventory/analytics/consistency', { params: { cono, item } })

// ===== ㉛ 呆滞物料 =====

/** 呆滞物料 */
export interface SlowMovingItem {
  item: string
  itemDesc: string
  warehouse: string
  qtyOnHand: number
  unit: string
  unitCost: number
  stockValue: number
  lastTxnDate: string
  idleDays: number
  idleLevel: string
}

export const getSlowMovingItems = (params: { cono?: string; cutoffDate: string; limit?: number }) =>
  request.get<SlowMovingItem[]>('/bpcs/inventory/analytics/slow-moving', { params })

// ===== ⑰ 订单详情增强 =====

/** 订单行增强详情 */
export interface OrderLineDetail {
  cono: string
  orno: string
  orln: string
  item: string
  itemDesc: string
  qtyOrdered: number
  qtyAllocated: number
  qtyShipped: number
  qtyInvoiced: number
  price: number
  shipAmount: number
  customerNo: string
  reqDate: string
  orderDate: string
  shipDate: string
  shipStatus: string
  loadNo: string
  invoiceNo: string
  invoiceDate: string
  invoiceAmount: number
  statusLabel: string
}

/** 订单时间线事件 */
export interface OrderTimelineEvent {
  eventType: string
  eventDate: string
  eventDesc: string
}

export const getOrderLineDetails = (cono: string, orno: string) =>
  request.get<OrderLineDetail[]>('/bpcs/orders/detail/lines', { params: { cono, orno } })

export const getOrderTimeline = (cono: string, orno: string) =>
  request.get<OrderTimelineEvent[]>('/bpcs/orders/detail/timeline', { params: { cono, orno } })

// ===== ⑳ 客户 360° 视图 =====

/** 最近订单 */
export interface CustomerRecentOrder {
  orderNo: string
  orderDate: string
  reqDate: string
  headerStatus: string
  lineCount: number
  orderTotal: number
}

/** 逾期发票 */
export interface CustomerOverdueInvoice {
  invoiceNo: string
  invoiceDate: string
  invoiceAmount: number
  orderNo: string
}

/** 客户 360° 概览 */
export interface CustomerOverview {
  cono: string
  cust: string
  customerName: string
  address: string
  city: string
  state: string
  zip: string
  phone: string
  contact: string
  creditLimit: number
  termsCode: string
  taxCode: string
  salesRep: string
  totalOrders: number
  openOrders: number
  totalRevenue: number
  recentOrders: CustomerRecentOrder[]
  overdueInvoices: CustomerOverdueInvoice[]
}

export const getCustomerOverview = (params: { cono?: string; cust: string; orderLimit?: number; invoiceLimit?: number }) =>
  request.get<CustomerOverview>('/bpcs/customers/overview', { params })

// ===== ㊲ ABC/XYZ 矩阵 =====

/** ABC/XYZ 矩阵项 */
export interface AbcXyzItem {
  item: string
  itemDesc: string
  totalQty: number
  stockValue: number
  abcClass: string
  avgDemand: number
  cv: number
  xyzClass: string
  matrixCell: string
}

export const getAbcXyzMatrix = (params: { cono?: string; fromDate?: string; limit?: number }) =>
  request.get<AbcXyzItem[]>('/bpcs/inventory/abc-xyz', { params })

// ===== ㉙ 循环盘点 =====

/** 盘点计划 */
export interface CycleCountPlan {
  id: number
  planNo: string
  item: string
  itemDesc: string
  warehouse: string
  plannedDate: string
  status: string
  abcClass: string
  operator: string
  createdBy: string
  createdTime: string
}

/** 盘点结果 */
export interface CycleCountResult {
  id: number
  planNo: string
  item: string
  warehouse: string
  systemQty: number
  countedQty: number
  difference: number
  differenceValue: number
  reason: string
  countedBy: string
  countTime: string
}

export const createCycleCountPlan = (data: { item: string; itemDesc?: string; warehouse: string; plannedDate: string; abcClass?: string }) =>
  request.post<CycleCountPlan>('/bpcs/cycle-count/plans', data)

export const listCycleCountPlans = (params: { status?: string; limit?: number }) =>
  request.get<CycleCountPlan[]>('/bpcs/cycle-count/plans', { params })

export const recordCycleCountResult = (data: { planId: number; countedQty: number; reason?: string }, systemQty: number) =>
  request.post<CycleCountResult>('/bpcs/cycle-count/results', data, { params: { systemQty } })

export const listCycleCountResults = (planId: number) =>
  request.get<CycleCountResult[]>('/bpcs/cycle-count/results', { params: { planId } })

// ===== Batch 2-5 剩余功能 =====

/** ⑨ 异常检测 */
export interface OrderAnomaly {
  cono: string
  orno: string
  cust: string
  custName: string
  orderDate: string
  reqDate: string
  status: string
  backorderLines: number
}
export const detectAnomalies = (cono = '001', limit = 50) =>
  request.get<OrderAnomaly[]>('/bpcs/anomaly/detect', { params: { cono, limit } })

/** ① BOM 展开 */
export interface BomLine {
  parent: string
  component: string
  description: string
  qty: number
  uom: string
  effective: string
  expired: string
  onHandQty: number
  allocQty: number
  availQty: number
}
export const findBomParents = (cono = '001', component: string) =>
  request.get<BomLine[]>('/bpcs/bom/parents', { params: { cono, component } })
export const expandBomChildren = (cono = '001', parent: string) =>
  request.get<BomLine[]>('/bpcs/bom/children', { params: { cono, parent } })

/** ㊳ 运单管理 */
export interface ShipmentVO {
  loadNo: string
  carrier: string
  destination: string
  shipDate: string
  lineCount: number
  weight: number
  orderNos: string
}
export const listShipments = (cono = '001', limit = 50) =>
  request.get<ShipmentVO[]>('/bpcs/shipment/list', { params: { cono, limit } })
export const exportShipmentPdf = (cono: string, waybillNo: string, params: Record<string, unknown>) =>
  blobClient.post('/bpcs/shipment/pdf', params, { params: { waybillNo, cono } })

/** ㊾ RCMX CSR 分配 */
export interface RcmxAssignment {
  cust: string
  custName: string
  csrId: string
  csrName: string
  active: string
  maintUser: string
  maintDate: string
}
export interface CsrOption { empId: string; name: string }
export interface CustOption { cust: string; custName: string }
export const listRcmx = (cono = '001', custLike?: string, csrLike?: string, limit = 50) =>
  request.get<RcmxAssignment[]>('/bpcs/rcmx/list', { params: { cono, custLike, csrLike, limit } })
export const searchCsrOptions = (cono = '001', keyword?: string) =>
  request.get<CsrOption[]>('/bpcs/rcmx/csrOptions', { params: { cono, keyword } })
export const searchCustOptions = (cono = '001', keyword?: string) =>
  request.get<CustOption[]>('/bpcs/rcmx/customers', { params: { cono, keyword } })
export const getRcmx = (cono: string, cust: string) =>
  request.get<RcmxAssignment>(`/bpcs/rcmx/get`, { params: { cono, cust } })
export const createRcmx = (cono: string, data: { cust: string; csrId: string; active: string; maintUser: string }) =>
  request.post<null>('/bpcs/rcmx/create', data, { params: { cono } })
export const updateRcmx = (cono: string, cust: string, data: { cust: string; csrId: string; active: string; maintUser: string }) =>
  request.put<null>('/bpcs/rcmx/update', data, { params: { cono, cust } })
export const deleteRcmx = (cono: string, cust: string) =>
  request.delete<null>('/bpcs/rcmx/delete', { params: { cono, cust } })
export const importRcmx = (cono: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<{ successCount: number; failureCount: number; errors: string[] }>('/bpcs/rcmx/import', form, { params: { cono }, headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportRcmx = (cono = '001') =>
  request.get<RcmxAssignment[]>('/bpcs/rcmx/export', { params: { cono } })

/** ㊽ WABP 配置 */
export interface WabpConfig {
  wh: string
  dayOfWeek: number
  time: string
  shipHold: string
  crHold: string
  prHold: string
  active: string
  maintUser: string
  maintDate: string
}
export const listWabp = (cono = '001', limit = 50) =>
  request.get<WabpConfig[]>('/bpcs/wabp/list', { params: { cono, limit } })
export const getWabp = (cono: string, wh: string, dayOfWeek: number) =>
  request.get<WabpConfig>(`/bpcs/wabp/get`, { params: { cono, wh, dayOfWeek } })
export const createWabp = (cono: string, data: {
  wh: string; dayOfWeek: number; time: string; shipHold: string; crHold: string; prHold: string; active: string; maintUser: string
}) => request.post<null>('/bpcs/wabp/create', data, { params: { cono } })
export const updateWabp = (cono: string, wh: string, dayOfWeek: number, data: {
  wh: string; dayOfWeek: number; time: string; shipHold: string; crHold: string; prHold: string; active: string; maintUser: string
}) => request.put<null>('/bpcs/wabp/update', data, { params: { cono, wh, dayOfWeek } })
export const deleteWabp = (cono: string, wh: string, dayOfWeek: number) =>
  request.delete<null>('/bpcs/wabp/delete', { params: { cono, wh, dayOfWeek } })
export const importWabp = (cono: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<{ successCount: number; failureCount: number; errors: string[] }>('/bpcs/wabp/import', form, { params: { cono }, headers: { 'Content-Type': 'multipart/form-data' } })
}
export const exportWabp = (cono = '001') =>
  request.get<WabpConfig[]>('/bpcs/wabp/export', { params: { cono } })

/** ㉗ 库位可视化 */
export interface LocationInventory {
  warehouse: string
  binLocation: string
  item: string
  description: string
  qtyOnHand: number
  status: string
}
export const listLocations = (cono = '001', limit = 100) =>
  request.get<LocationInventory[]>('/bpcs/location/list', { params: { cono, limit } })

/** ② 智能补货 */
export interface ReplenishmentVO {
  item: string
  description: string
  wh: string
  qtyOnHand: number
  safetyStock: number
  shortage: number
  avgDemand: number
}
export const listReplenishment = (cono = '001', limit = 50) =>
  request.get<ReplenishmentVO[]>('/bpcs/replenishment/list', { params: { cono, limit } })

/** ㉜ 预警规则 */
export interface AlertRuleVO {
  item: string
  description: string
  wh: string
  qtyOnHand: number
  safetyStock: number
  maxStock: number
  alertType: string
}
export const listAlertRules = (cono = '001', limit = 50) =>
  request.get<AlertRuleVO[]>('/bpcs/alert/rules', { params: { cono, limit } })

/** ㉚ 库存价值 */
export interface StockValueVO {
  item: string
  description: string
  wh: string
  qtyOnHand: number
  unitCost: number
  stockValue: number
}
export const listStockValue = (cono = '001', limit = 50) =>
  request.get<StockValueVO[]>('/bpcs/stockValue/report', { params: { cono, limit } })

/** ④ 供应商评分 */
export interface SupplierScoreVO {
  vendor: string
  vendorName: string
  totalPo: number
  onTime: number
  score: number
}
export const listSupplierScores = (cono = '001', limit = 50) =>
  request.get<SupplierScoreVO[]>('/bpcs/supplierScore/list', { params: { cono, limit } })

/** ⑤ PO 生命周期 */
export interface PoLifecycleVO {
  cono: string
  poNo: string
  vendor: string
  vendorName: string
  orderDate: string
  receivedDate: string
  status: string
  onHold: boolean
  lineCount: number
}
export const listPoLifecycle = (cono = '001', limit = 50) =>
  request.get<PoLifecycleVO[]>('/bpcs/po/lifecycle', { params: { cono, limit } })

/** ⑪ 信用 Hold */
export interface CreditHoldVO {
  cono: string
  orno: string
  cust: string
  custName: string
  orderDate: string
  reqDate: string
  hid: string
  hstat: string
  crHold: string
  shipHold: string
  prHold: string
}
export const listCreditHolds = (cono = '001', limit = 50) =>
  request.get<CreditHoldVO[]>('/bpcs/creditHold/list', { params: { cono, limit } })

/** ⑯ 订单看板 */
export interface KanbanVO {
  cono: string
  orno: string
  cust: string
  custName: string
  orderDate: string
  reqDate: string
  status: string
  pendingLines: number
  partialLines: number
  shippedLines: number
}
export const listKanbanOrders = (cono = '001', limit = 50) =>
  request.get<KanbanVO[]>('/bpcs/kanban/orders', { params: { cono, limit } })

// ==================== ㊵ 运费核算与成本分析 ====================
export interface FreightCostRuleVO {
  id: number
  ruleName: string
  carrier: string
  costType: string
  basePrice: number
  unitPrice: number
  minPrice: number
  maxPrice: number
  enabled: boolean
  description: string
}
export interface FreightCostRecordVO {
  id: number
  orderNo: string
  carrier: string
  weight: number
  volume: number
  pieceCount: number
  estimatedCost: number
  actualCost: number
  costDiff: number
  shipDate: string
}
export interface FreightCostTrendVO {
  month: string
  totalCost: number
  avgCost: number
  recordCount: number
  carrier: string
}
export const listFreightRules = () =>
  request.get<FreightCostRuleVO[]>('/bpcs/freight-cost/rules')
export const createFreightRule = (data: Partial<FreightCostRuleVO>) =>
  request.post<FreightCostRuleVO>('/bpcs/freight-cost/rules', data)
export const updateFreightRule = (id: number, data: Partial<FreightCostRuleVO>) =>
  request.put<FreightCostRuleVO>(`/bpcs/freight-cost/rules/${id}`, data)
export const deleteFreightRule = (id: number) =>
  request.delete(`/bpcs/freight-cost/rules/${id}`)
export const toggleFreightRule = (id: number, enabled: boolean) =>
  request.put<FreightCostRuleVO>(`/bpcs/freight-cost/rules/${id}/toggle`, null, { params: { enabled } })
export const calculateFreight = (carrier: string, costType: string, quantity: number) =>
  request.get<number>('/bpcs/freight-cost/calculate', { params: { carrier, costType, quantity } })
export const listFreightRecords = (params: Record<string, string | number>) =>
  request.get<{ records: FreightCostRecordVO[]; total: number }>('/bpcs/freight-cost/records', { params })
export const createFreightRecord = (data: Partial<FreightCostRecordVO>) =>
  request.post<FreightCostRecordVO>('/bpcs/freight-cost/records', data)
export const deleteFreightRecord = (id: number) =>
  request.delete(`/bpcs/freight-cost/records/${id}`)
export const getFreightTrend = (params: Record<string, string | number>) =>
  request.get<FreightCostTrendVO[]>('/bpcs/freight-cost/analysis/trend', { params })
export const getCarrierCostShare = () =>
  request.get<Record<string, number>>('/bpcs/freight-cost/analysis/carrier-share')

// ==================== ㉟ 库存模拟 ====================
export interface InventorySimulationVO {
  id: number
  simName: string
  itemNo: string
  warehouse: string
  currentStock: number
  demandChange: number
  leadTimeDays: number
  safetyStock: number
  reorderPoint: number
  resultStockoutDays: number
  resultReorderCount: number
  resultAvgStock: number
  resultServiceLevel: number
  status: string
}
export const listSimulations = (params: Record<string, string | number>) =>
  request.get<{ records: InventorySimulationVO[]; total: number }>('/bpcs/inventory-simulation', { params })
export const createSimulation = (data: Partial<InventorySimulationVO>) =>
  request.post<InventorySimulationVO>('/bpcs/inventory-simulation', data)
export const runSimulation = (id: number) =>
  request.post<InventorySimulationVO>(`/bpcs/inventory-simulation/${id}/run`)
export const deleteSimulation = (id: number) =>
  request.delete(`/bpcs/inventory-simulation/${id}`)

// ==================== ㉕ 订单协同 ====================
export interface OrderCollaborationVO {
  id: number
  orderNo: string
  customerCode: string
  customerName: string
  status: string
  priority: string
  assignedTo: string
  dueDate: string
  notes: string
  unreadNotifications: number
}
export interface CollaborationNotificationVO {
  id: number
  collaborationId: number
  sender: string
  recipient: string
  message: string
  channel: string
  isRead: boolean
  createdTime: string
}
export const listCollaborations = (params: Record<string, string | number>) =>
  request.get<{ records: OrderCollaborationVO[]; total: number }>('/bpcs/order-collaboration', { params })
export const createCollaboration = (data: Partial<OrderCollaborationVO>) =>
  request.post<OrderCollaborationVO>('/bpcs/order-collaboration', data)
export const updateCollabStatus = (id: number, status: string) =>
  request.put<OrderCollaborationVO>(`/bpcs/order-collaboration/${id}/status`, null, { params: { status } })
export const assignCollab = (id: number, assignedTo: string) =>
  request.put<OrderCollaborationVO>(`/bpcs/order-collaboration/${id}/assign`, null, { params: { assignedTo } })
export const deleteCollaboration = (id: number) =>
  request.delete(`/bpcs/order-collaboration/${id}`)
export const sendCollabNotification = (data: Partial<CollaborationNotificationVO>) =>
  request.post<CollaborationNotificationVO>('/bpcs/order-collaboration/notifications', data)
export const listCollabNotifications = (collabId: number) =>
  request.get<CollaborationNotificationVO[]>(`/bpcs/order-collaboration/${collabId}/notifications`)
export const listMyNotifications = (recipient: string) =>
  request.get<CollaborationNotificationVO[]>('/bpcs/order-collaboration/my-notifications', { params: { recipient } })
export const markNotificationRead = (notifId: number) =>
  request.put(`/bpcs/order-collaboration/notifications/${notifId}/read`)
export const getUnreadCount = (recipient: string) =>
  request.get<number>('/bpcs/order-collaboration/notifications/unread-count', { params: { recipient } })

// ===== ㊷ 多仓库联合补货 =====

export interface WarehouseStock {
  wh: string
  qtyOnHand: number | null
  qtyAllocated: number | null
  qtyAvailable: number | null
  qtyOnOrder: number | null
  lastTxnDate: string | null
  pct: number | null
}

export interface WarehouseReplenishItem {
  item: string
  itdsc: string | null
  totalQty: number | null
  safetyStock: number | null
  shortage: number | null
  avgDailyDemand: number | null
  suggestQty: number | null
  warehouses: WarehouseStock[]
}

export const searchWarehouseReplenish = (params: {
  cono?: string; item?: string; itdsc?: string; belowSafetyOnly?: boolean; current?: number; size?: number
}) =>
  request.get<PageResult<WarehouseReplenishItem>>('/bpcs/warehouse-replenish', { params })

// ==================== ⑥ 预测补货看板 ====================
export interface ForecastMonthlyDemand {
  ym: string
  actual: number
  forecast: number
  upperBound: number
  lowerBound: number
}
export interface ForecastStockLevel {
  ym: string
  onHand: number
  safetyStock: number
}
export interface ForecastReplenishSuggestion {
  item: string
  currentStock: number
  suggestedOrder: number
}
export interface ForecastMetrics {
  mape: number
  bias: number
  gmAbc: number
  gmXyz: number
}
export interface ForecastResult {
  monthlyDemand: ForecastMonthlyDemand[]
  stockLevels: ForecastStockLevel[]
  replenishSuggestions: ForecastReplenishSuggestion[]
  metrics: ForecastMetrics
}
export const getForecast = (params: { cono?: string; item?: string; months?: number }) =>
  request.get<ForecastResult>('/bpcs/forecast', { params })
export const getForecastItemOptions = (params: { cono?: string; limit?: number }) =>
  request.get<{ value: string; label: string }[]>('/bpcs/forecast/items', { params })

// ==================== ㉑ 订单模板 ====================
export interface OrderTemplate {
  id: number
  templateName: string
  cono: string | null
  cust: string | null
  shipTo: string | null
  remark: string | null
  lineJson: string | null
  useCount: number
  active: string | null
  createdBy: string | null
}
export const listOrderTemplates = (params?: { keyword?: string }) =>
  request.get<OrderTemplate[]>('/bpcs/orderTemplate', { params })
export const getOrderTemplate = (id: number) =>
  request.get<OrderTemplate>(`/bpcs/orderTemplate/${id}`)
export const createOrderTemplate = (data: Partial<OrderTemplate>) =>
  request.post<OrderTemplate>('/bpcs/orderTemplate', data)
export const updateOrderTemplate = (data: Partial<OrderTemplate>) =>
  request.put<OrderTemplate>('/bpcs/orderTemplate', data)
export const deleteOrderTemplate = (id: number) =>
  request.delete(`/bpcs/orderTemplate/${id}`)
export const useOrderTemplate = (id: number) =>
  request.post<OrderTemplate>(`/bpcs/orderTemplate/${id}/use`)

// ==================== ㉒ 订单复制 ====================
export const copyOrder = (params: { cono: string; sourceOrno: string }) =>
  request.post<string>('/bpcs/orderCopy', null, { params })

// ==================== ㉓ 订单变更管理 ====================
export interface OrderChange {
  id: number
  cono: string | null
  orno: string | null
  changeType: string | null
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  reason: string | null
  changedBy: string | null
  changedTime: string | null
}
export const listOrderChanges = (params: { cono: string; orno: string }) =>
  request.get<OrderChange[]>('/bpcs/orderChange', { params })

// ==================== ⑭ 退货与 RMA ====================
export interface Rma {
  id: number
  rmaNo: string | null
  orno: string | null
  item: string | null
  cono: string | null
  cust: string | null
  qty: number | null
  reason: string | null
  status: string | null
  createdBy: string | null
  createdTime: string | null
}
export const listRma = (params?: { status?: string }) =>
  request.get<Rma[]>('/bpcs/rma', { params })
export const getRma = (id: number) =>
  request.get<Rma>(`/bpcs/rma/${id}`)
export const createRma = (data: Partial<Rma>) =>
  request.post<Rma>('/bpcs/rma', data)
export const updateRmaStatus = (id: number, status: string) =>
  request.put<Rma>(`/bpcs/rma/${id}/status`, { status })

// ==================== Supply Chain 迁移补充 API ====================

/** 库存交易历史 */
export interface InventoryHistory {
  item: string
  warehouse: string
  type: string
  quantity: number
  referenceNo: string
  date: string
  time: string
  userId: string
}

export const getInventoryHistory = (params: Record<string, string | number>) =>
  request.get<InventoryHistory[]>('/bpcs/supply-chain/inventory/history', { params })

/** 供应链 KPI */
export interface SupplyChainKpi {
  totalOrders: number
  closedOrders: number
  completionRate: number
  totalItems: number
  inventoryValue: number
  onTimeDeliveryRate: number
}

export const getSupplyChainKpi = (params?: Record<string, string | number>) =>
  request.get<SupplyChainKpi>('/bpcs/supply-chain/kpi', { params })

/** 销售分析 Top N 条目 */
export interface SalesTopEntry {
  code: string
  name: string | null
  orderCount: number
  totalAmount: number
}

/** 销售分析 Top N 结果 */
export interface SalesTopN {
  topCustomers: SalesTopEntry[]
  topItems: SalesTopEntry[]
  totalRevenue: number
  totalOrders: number
}

export const getSalesTopN = (params: Record<string, string | number>) =>
  request.get<SalesTopN>('/bpcs/supply-chain/sales/analysis', { params })

/** 库存预警（低于安全库存） */
export interface InventoryAlert {
  item: string
  description: string | null
  warehouse: string
  uom: string
  onHand: number
  allocated: number
  onOrder: number
  available: number
  safetyStock: number
  deficit: number
}

export const getInventoryAlerts = (params: Record<string, string | number>) =>
  request.get<InventoryAlert[]>('/bpcs/supply-chain/inventory/alerts', { params })

// ========== A1 系统健康仪表板 ==========

export interface SystemHealthOverview {
  totalServers: number
  onlineServers: number
  offlineServers: number
  avgCpuUsage: number
  avgMemoryUsage: number
  avgDiskUsage: number
  activeJobs: number
  alertCount: number
  criticalAlertCount: number
  todayBackupSuccess: number
  todayBackupFailed: number
  complianceRate: number
}

export interface SecurityAuditSummary {
  serverId: number
  serverName: string
  totalLogins: number
  successfulLogins: number
  failedLogins: number
  uniqueUsers: number
  uniqueIps: number
  permissionChanges: number
  highRiskOperations: number
  lastLoginTime: string | null
  lastHighRiskTime: string | null
}

export interface UserPermissionMatrix {
  userName: string
  status: string
  roles: string[]
  permissions: string[]
  lastLoginTime: string | null
  passwordExpireDays: number
  changeCount: number
}

export const getSystemHealthOverview = () =>
  request.get<SystemHealthOverview>('/as400/system-health/overview')

export const getSecurityAuditSummary = () =>
  request.get<SecurityAuditSummary[]>('/as400/system-health/security-audit')

export const getUserPermissionMatrix = () =>
  request.get<UserPermissionMatrix[]>('/as400/system-health/permission-matrix')

// ========== A4 备份监控 ==========

export interface BackupStatus {
  id: number
  serverId: number
  serverName: string
  backupName: string
  backupType: string
  status: string
  startTime: string | null
  endTime: string | null
  durationSeconds: number | null
  objectsCount: number | null
  sizeBytes: number | null
  mediaName: string | null
  errorMessage: string | null
  createdTime: string
}

export const getBackupList = (params?: { serverId?: number }) =>
  request.get<BackupStatus[]>('/as400/backup/list', { params })

export const getBackupLatest = (params?: { serverId?: number }) =>
  request.get<BackupStatus>('/as400/backup/latest', { params })

// ========== A8 系统值合规检查 ==========

export interface SystemValueCompliance {
  id: number
  serverId: number
  serverName: string
  systemValue: string
  currentValue: string
  expectedValue: string
  complianceStatus: string
  severity: string
  description: string | null
  remediation: string | null
  lastChecked: string
  createdTime: string
  updatedTime: string
}

export const getComplianceList = (params?: { serverId?: number }) =>
  request.get<SystemValueCompliance[]>('/as400/compliance/list', { params })

export const getComplianceLatest = (params?: { serverId?: number }) =>
  request.get<SystemValueCompliance>('/as400/compliance/latest', { params })

// ==================== Supply Chain Control Tower 2.0 ====================

export interface OtifSummary {
  totalShipments: number
  otifCompliant: number
  otifRate: number
  avgLeadTimeDays: number
  fillRatePct: number
  lateShipments: number
  shortShipments: number
  totalDisruptions: number
}
export interface OtifByParty {
  partyCode: string
  partyName: string
  totalOrders: number
  onTimeInFull: number
  otifRate: number
  avgLeadTimeDays: number
  rating: string
}
export interface OtifTrend {
  ym: string
  otifRate: number
  totalShipments: number
  compliantCount: number
  lateCount: number
  shortCount: number
}
export interface OtifResult {
  summary: OtifSummary
  byCustomer: OtifByParty[]
  bySupplier: OtifByParty[]
  monthlyTrend: OtifTrend[]
}
export const getOtifTracking = (params?: { cono?: string; months?: number }) =>
  request.get<OtifResult>('/bpcs/supply-chain/otif', { params })

export interface DisruptionSummary {
  activeAlerts: number
  criticalCount: number
  warningCount: number
  infoCount: number
  resolvedToday: number
}
export interface DisruptionEvent {
  id: number
  eventType: string
  severity: string
  title: string
  description: string
  affectedItem: string
  affectedWarehouse: string
  detectedTime: string
  status: string
  impactOrders: string[]
}
export interface RiskItem {
  item: string
  itemDesc: string
  warehouse: string
  daysOfSupply: number
  leadTimeDays: number
  riskLevel: string
  recommendation: string
}
export interface DisruptionResult {
  summary: DisruptionSummary
  events: DisruptionEvent[]
  riskItems: RiskItem[]
}
export const getDisruptionAlerts = (params?: { cono?: string; limit?: number }) =>
  request.get<DisruptionResult>('/bpcs/supply-chain/disruption', { params })

export interface NodeInfo {
  nodeId: string
  nodeName: string
  nodeType: string
  totalItems: number
  totalOnHand: number
  totalValue: number
  capacityPct: number
  alertCount: number
}
export interface NodeFlow {
  fromNode: string
  toNode: string
  item: string
  quantity: number
  flowType: string
  plannedDate: string
}
export interface CrossNodeWhStock {
  warehouse: string
  onHand: number
  allocated: number
  available: number
  safetyStock: number
}
export interface ImbalanceItem {
  item: string
  itemDesc: string
  warehouseStocks: CrossNodeWhStock[]
  imbalanceIndex: number
  recommendation: string
}
export interface CrossNodeResult {
  nodes: NodeInfo[]
  flows: NodeFlow[]
  imbalances: ImbalanceItem[]
  heatmap: { items: string[]; warehouses: string[]; data: number[][] }
}
export const getCrossNodeInventory = (params?: { cono?: string }) =>
  request.get<CrossNodeResult>('/bpcs/supply-chain/cross-node', { params })

// ==================== CPFR 协同预测 ====================

export interface MonthlyComponent {
  ym: string
  actual: number
  trend: number
  seasonal: number
  residual: number
  deseasonalized: number
}
export interface SeasonalDecomposition {
  components: MonthlyComponent[]
  seasonalIndices: number[]
  dominantSeason: string
}
export interface ForecastAccuracyBacktest {
  ym: string
  actual: number
  predicted: number
  mapePct: number
  biasPct: number
  accuracyGrade: string
}
export interface CollaborativeForecast {
  ym: string
  salesForecast: number
  marketingForecast: number
  supplyForecast: number
  consensusForecast: number
  finalActual: number
  consensusDeviation: number
}
export interface CpfMetrics {
  forecastAccuracy: number
  bias: number
  seasonalStrength: number
  collaborativeAlignment: number
  overallScore: number
}
export interface CpfrResult {
  seasonal: SeasonalDecomposition
  accuracyBacktest: ForecastAccuracyBacktest[]
  collaborativeForecasts: CollaborativeForecast[]
  metrics: CpfMetrics
}
export const getCpfrAnalysis = (params?: { cono?: string; item?: string; months?: number }) =>
  request.get<CpfrResult>('/bpcs/forecast/cpfr', { params })

// ==================== TMS Lite 运输管理 ====================

export interface RoutePlan {
  routeId: string
  origin: string
  destination: string
  distanceKm: number
  estimatedHours: number
  stopCount: number
  carrier: string
  estimatedCost: number
  status: string
  orderNos: string[]
}
export interface CarrierComparison {
  carrierCode: string
  carrierName: string
  rating: number
  totalShipments: number
  onTimeRate: number
  avgCostPerKg: number
  avgTransitDays: number
  serviceLevel: string
  recommended: boolean
}
export interface CarrierCostShare {
  carrier: string
  totalCost: number
  sharePct: number
  shipmentCount: number
}
export interface MonthlyFreightTrend {
  ym: string
  totalCost: number
  shipmentCount: number
  avgCost: number
}
export interface FreightCostByRoute {
  route: string
  avgCost: number
  count: number
  pctOfTotal: number
}
export interface FreightAnalysis {
  totalFreightCost: number
  avgCostPerShipment: number
  avgCostPerKg: number
  costChangePct: number
  carrierShares: CarrierCostShare[]
  monthlyTrends: MonthlyFreightTrend[]
  costByRoutes: FreightCostByRoute[]
}
export interface TrackingEvent {
  timestamp: string
  location: string
  event: string
  detail: string
}
export interface DeliveryTracking {
  loadNo: string
  orderNo: string
  carrier: string
  origin: string
  destination: string
  shipDate: string
  estimatedArrival: string
  actualArrival: string | null
  status: string
  statusKey: string
  signedBy: string | null
  proofOfDelivery: string | null
  events: TrackingEvent[]
}
export interface TmsLiteResult {
  routePlans: RoutePlan[]
  carrierComparisons: CarrierComparison[]
  freightAnalysis: FreightAnalysis
  deliveryTrackings: DeliveryTracking[]
}
export const getTmsRoutePlans = (params?: { cono?: string; limit?: number }) =>
  request.get<RoutePlan[]>('/bpcs/tms/route-plans', { params })
export const getTmsCarrierComparison = (params?: { cono?: string }) =>
  request.get<CarrierComparison[]>('/bpcs/tms/carrier-comparison', { params })
export const getTmsFreightAnalysis = (params?: { cono?: string; months?: number }) =>
  request.get<FreightAnalysis>('/bpcs/tms/freight-analysis', { params })
export const getTmsDeliveryTracking = (params?: { cono?: string; status?: string; limit?: number }) =>
  request.get<DeliveryTracking[]>('/bpcs/tms/delivery-tracking', { params })
export const getTmsAll = (params?: { cono?: string; months?: number }) =>
  request.get<TmsLiteResult>('/bpcs/tms/all', { params })

// ==================== ATP Available to Promise ====================

export interface AtpSummary {
  totalItems: number
  atpSufficient: number
  atpShortage: number
  overallFillRate: number
  avgPromiseDays: number
}
export interface AtpTimePhased {
  period: string
  onHand: number
  plannedReceipt: number
  committedDemand: number
  atpQty: number
  cumAtpQty: number
  remark: string | null
}
export interface AtpLinePromise {
  cono: string
  orno: string
  lineNo: number
  item: string
  itemDesc: string
  requestedQty: number
  requestedDate: string
  availableNowQty: number
  earliestDate: string
  canFulfillNow: boolean
  leadTimeDays: number
  promiseStatus: string
}
export interface AtpDeviation {
  item: string
  itemDesc: string
  partyCode: string
  partyName: string
  atpAccuracy: number
  otifRate: number
  deviationPct: number
  totalPromises: number
  fulfilledOnTime: number
  rootCause: string
  recommendation: string
}
export interface AtpResult {
  summary: AtpSummary
  timePhased: AtpTimePhased[]
  linePromises: AtpLinePromise[]
  deviations: AtpDeviation[]
}
export const getAtpOverview = (params?: { cono?: string; weeks?: number }) =>
  request.get<AtpResult>('/bpcs/supply-chain/atp', { params })
export const getAtpDeviation = (params?: { cono?: string }) =>
  request.get<AtpDeviation[]>('/bpcs/supply-chain/atp/deviation', { params })
