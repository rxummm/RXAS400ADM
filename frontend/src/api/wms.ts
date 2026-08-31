import request from './request'

export interface Warehouse {
  cono: string
  whse: string
  whname: string
  location: string
}

export interface Bin {
  cono: string
  whse: string
  binno: string
  bintype: string
  status: string
  capacity: number
}

export interface BinInventory {
  whse: string
  binno: string
  bintype: string
  status: string
  capacity: number
  item: string
  itdsc: string
  qty: number
  lotno: string
}

export interface Movement {
  cono: string
  whse: string
  item: string
  frombin: string
  tobin: string
  qty: number
  ittyp: string
  trndate: string
  trntime: string
  refno: string
  userid: string
}

export interface BatchTracking {
  whse: string
  binno: string
  item: string
  itdsc: string
  qty: number
  lotno: string
  trndate: string
  ittyp: string
  trnqty: number
  refno: string
}

export interface WarehouseSummary {
  whse: string
  whname: string
  totalBins: number
  occupied: number
  free: number
  totalCapacity: number
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export const searchWarehouses = (params: Record<string, unknown>): Promise<PageResult<Warehouse>> =>
  request.get('/bpcs/wms/warehouses', { params })

export const searchBins = (params: Record<string, unknown>): Promise<PageResult<Bin>> =>
  request.get('/bpcs/wms/bins', { params })

export const searchBinInventory = (params: Record<string, unknown>): Promise<PageResult<BinInventory>> =>
  request.get('/bpcs/wms/bin-inventory', { params })

export const searchMovements = (params: Record<string, unknown>): Promise<PageResult<Movement>> =>
  request.get('/bpcs/wms/movements', { params })

export const searchBatches = (params: Record<string, unknown>): Promise<PageResult<BatchTracking>> =>
  request.get('/bpcs/wms/batches', { params })

export const warehouseSummary = (cono = '001'): Promise<WarehouseSummary[]> =>
  request.get('/bpcs/wms/summary', { params: { cono } })
