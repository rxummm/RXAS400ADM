import request from './request'

// ==================== Types ====================

export interface OrderListItem {
  cono: string; orno: string; custNo: string; custName: string | null;
  shipTo: string | null; orderDate: string | null; reqDate: string | null;
  statusLabel: string; currentStageIndex: number; lineCount: number; rawChsts: string
}

export interface InventoryAlert {
  item: string; description: string | null; warehouse: string; uom: string;
  onHand: number; allocated: number; onOrder: number; available: number;
  safetyStock: number; deficit: number
}

export interface TopEntry {
  code: string; name: string | null; orderCount: number; totalAmount: number
}

export interface SalesAnalysis {
  topCustomers: TopEntry[]; topItems: TopEntry[];
  totalRevenue: number; totalOrders: number
}

export interface InventoryHistory {
  item: string; warehouse: string; type: string; quantity: number;
  referenceNo: string; date: string; time: string; userId: string
}

export interface PurchaseReceiving {
  pono: string; vendorName: string; orderDate: string; item: string;
  itemDesc: string; qtyOrdered: number; qtyReceived: number; qtyOpen: number;
  unitPrice: number; reqDate: string
}

export interface ShippingLoad {
  cono: string; lhno: string; status: number; statusKey: string;
  carrier: string | null; destination: string | null; shipDate: string | null;
  orderNos: string[]; lineCount: number; weight: number
}

export interface AbcItem {
  item: string; description: string | null; warehouse: string;
  quantity: number; unitCost: number; stockValue: number; abcClass: string
}

export interface SupplierPerf {
  vendorName: string; poCount: number; onTimeCount: number;
  onTimeRate: number; avgPrice: number
}

export interface SupplyChainKpi {
  totalOrders: number; closedOrders: number; completionRate: number;
  totalItems: number; inventoryValue: number; onTimeDeliveryRate: number
}

export interface TrackingLine {
  item: string; itemDesc: string; qtyOrdered: number; qtyAllocated: number;
  qtyShipped: number; qtyInvoiced: number; shipDate: string | null; shipStatus: string | null
}

export interface OrderTracking {
  cono: string; orno: string; custNo: string; custName: string | null;
  statusLabel: string; lines: TrackingLine[]
}

// ==================== Phase 1 ====================

export const searchOrders = (params: Record<string, string | number>): Promise<OrderListItem[]> =>
  request.get('/bpcs/supply-chain/orders', { params })

export const fetchInventoryAlerts = (params: Record<string, string | number>): Promise<InventoryAlert[]> =>
  request.get('/bpcs/supply-chain/inventory/alerts', { params })

export const fetchSalesAnalysis = (params: Record<string, string | number>): Promise<SalesAnalysis> =>
  request.get('/bpcs/supply-chain/sales/analysis', { params })

// ==================== Phase 2 ====================

export const fetchInventoryHistory = (params: Record<string, string | number>): Promise<InventoryHistory[]> =>
  request.get('/bpcs/supply-chain/inventory/history', { params })

export const fetchPurchaseReceiving = (params: Record<string, string | number>): Promise<PurchaseReceiving[]> =>
  request.get('/bpcs/supply-chain/purchase/receiving', { params })

export const fetchShippingList = (params: Record<string, string | number>): Promise<ShippingLoad[]> =>
  request.get('/bpcs/supply-chain/shipping/list', { params })

// ==================== Phase 3 ====================

export const fetchAbcAnalysis = (params: Record<string, string | number>): Promise<AbcItem[]> =>
  request.get('/bpcs/supply-chain/inventory/abc', { params })

export const fetchSupplierPerformance = (params: Record<string, string | number>): Promise<SupplierPerf[]> =>
  request.get('/bpcs/supply-chain/supplier/performance', { params })

// ==================== Phase 4 ====================

export const fetchSupplyChainKpi = (params?: Record<string, string | number>): Promise<SupplyChainKpi> =>
  request.get('/bpcs/supply-chain/kpi', { params })

export const fetchOrderTracking = (cono: string, orno: string): Promise<OrderTracking> =>
  request.get('/bpcs/supply-chain/order/tracking', { params: { cono, orno } })
