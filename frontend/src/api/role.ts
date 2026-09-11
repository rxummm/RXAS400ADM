import request from './request'
import type { PaginatedResult } from './types'

export interface SysRole {
  id?: number
  roleCode: string
  roleName: string
  description?: string
  sort?: number
  status?: number
  menuIds?: number[]
}

export const fetchRoles = () => request.get<SysRole[]>('/roles')

export const fetchRolePage = (params: { current?: number; size?: number; keyword?: string }): Promise<PaginatedResult<SysRole>> =>
  request.get('/roles/page', { params })

export const createRole = (data: SysRole) => request.post<SysRole>('/roles', data)

export const updateRole = (id: number, data: SysRole) => request.put<SysRole>(`/roles/${id}`, data)

export const deleteRole = (id: number) => request.delete<void>(`/roles/${id}`)

export const batchDeleteRoles = (ids: number[]) => request.post<number>('/roles/batch-delete', ids)
