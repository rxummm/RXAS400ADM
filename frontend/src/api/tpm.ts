/**
 * TPM 设备管理 API
 */
import request from './request'
import type { PaginatedResult } from './types'

export interface Equipment {
  id: number
  equipmentNo: string
  cono: string
  equipmentName: string
  equipmentType: string
  manufacturer: string
  model: string
  serialNo: string
  location: string
  department: string
  purchaseDate: string
  installDate: string
  warrantyExpiry: string
  status: string
  responsiblePerson: string
  remark: string
  createdBy: string
  createdTime: string
}

export interface MaintenancePlan {
  id: number
  planNo: string
  cono: string
  equipmentId: number
  planName: string
  maintenanceType: string
  cycleDays: number
  nextDueDate: string
  lastMaintenanceDate: string
  responsiblePerson: string
  checklist: string
  status: string
  createdBy: string
  createdTime: string
}

export interface OeeRecord {
  id: number
  equipmentId: number
  calcDate: string
  availability: number
  performance: number
  quality: number
  oee: number
  plannedTime: number
  actualRuntime: number
  downtimeMinutes: number
  totalCount: number
  goodCount: number
  defectCount: number
  createdBy: string
  createdTime: string
}

export const searchEquipment = (params: Record<string, unknown>): Promise<PaginatedResult<Equipment>> =>
  request.get('/tpm/equipment', { params })

export const getEquipmentById = (id: number): Promise<Equipment> =>
  request.get(`/tpm/equipment/${id}`)

export const searchMaintenancePlans = (params: Record<string, unknown>): Promise<PaginatedResult<MaintenancePlan>> =>
  request.get('/tpm/maintenance', { params })

export const getMaintenancePlanById = (id: number): Promise<MaintenancePlan> =>
  request.get(`/tpm/maintenance/${id}`)

export const searchOee = (params: Record<string, unknown>): Promise<PaginatedResult<OeeRecord>> =>
  request.get('/tpm/oee', { params })

export const getLatestOee = (equipmentId: number): Promise<OeeRecord> =>
  request.get(`/tpm/oee/latest/${equipmentId}`)
