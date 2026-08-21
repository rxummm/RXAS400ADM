import request from './request'
import type { SysMenu } from './menu'
import type { SysRole } from './role'

/** 用户状态（后端 rx_user.status，仅两个取值） */
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface UserVO {
  id: number
  username: string
  email?: string
  status: UserStatus
  loginSource?: string
  as400ServerId?: number
  roles?: SysRole[]
  createdTime?: string
}

export interface PaginatedResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

/** 登录失败/锁定记录（后端 LoginAttemptVO，字段以 VO 为准） */
export interface LoginAttemptRecord {
  username: string
  serverId?: number
  failedCount?: number
  lockedUntil?: string
  lastFailTime?: string
  lastIp?: string
  updatedTime?: string
}

/** 按 IP 聚合统计（后端 aggregateByIp，大写列名原样保留） */
export interface IpStat {
  ip: string
  attempts: number
  accounts: number
  locked: number
  last_time?: string
}

export const fetchUsers = (params: { current?: number; size?: number; keyword?: string } = {}): Promise<PaginatedResult<UserVO>> =>
  request.get('/users', { params })

export const createUser = (data: { username: string; password?: string; email?: string; roleIds?: number[] }): Promise<UserVO> =>
  request.post('/users', data)

export const updateUser = (id: number, data: { password?: string; email?: string; status?: UserStatus; roleIds?: number[] }): Promise<UserVO> =>
  request.put(`/users/${id}`, data)

export const deleteUser = (id: number): Promise<void> => request.delete(`/users/${id}`)

export const fetchRoles = (): Promise<SysRole[]> => request.get('/roles')

export const loginAttempts = (serverId?: number): Promise<LoginAttemptRecord[]> =>
  request.get('/auth/login-attempts', { params: { serverId } })

export const loginAttemptIps = (): Promise<IpStat[]> => request.get('/auth/login-attempts/ips')

export const unlockUser = (username: string, serverId?: number): Promise<void> =>
  request.delete(`/auth/login-attempts/${username}`, { params: { serverId } })

// ==================== 用户-菜单直接授权（参照旧项目 SysPermissionManage） ====================

export const getUserMenuIds = (userId: number): Promise<number[]> => request.get<number[]>(`/users/${userId}/menus`)

export const getUserDirectMenuIds = (userId: number): Promise<number[]> =>
  request.get<number[]>(`/users/${userId}/menus/direct`)

export const getUserManageableTree = (userId: number): Promise<SysMenu[]> =>
  request.get<SysMenu[]>(`/users/${userId}/menus/manageable-tree`)

export const addUserMenus = (userId: number, menuIds: number[]): Promise<void> =>
  request.post(`/users/${userId}/menus/add`, { menuIds })

export const removeUserMenus = (userId: number, menuIds: number[]): Promise<void> =>
  request.post(`/users/${userId}/menus/remove`, { menuIds })