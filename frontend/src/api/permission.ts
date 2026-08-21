import request from './request'

export interface PermissionRequest {
  id?: number
  username?: string
  permissionCode?: string
  /** JSON 数组字符串：申请的菜单/按钮 ID */
  menuIds?: string
  /** JSON 数组字符串：申请的菜单名称（审批展示用） */
  menuNames?: string
  reason?: string
  status?: string
  approver?: string
  approveComment?: string
  createdTime?: string
  updatedTime?: string
}

export interface RequestableMenu {
  id: number
  menuName: string
  menuType: number
  title?: string
  perms?: string
  icon?: string
  children?: RequestableMenu[]
}

/** 可申请菜单树（登录即可） */
export const getRequestableMenus = () => request.get<RequestableMenu[]>('/menus/requestable')

/** 菜单树模式申请（推荐，参照旧项目） */
export const createPermissionRequest = (data: {
  menuIds?: number[]
  menuNames?: string[]
  permissionCode?: string
  reason?: string
}) => request.post<PermissionRequest>('/permission-requests', data)

export const listMyPermissionRequests = (params: { current?: number; size?: number } = {}) =>
  request.get<{ total: number; records: PermissionRequest[] }>('/permission-requests/mine', { params })

export const listPermissionRequests = (params: {
  current?: number
  size?: number
  status?: string
  keyword?: string
} = {}) => request.get<{ total: number; records: PermissionRequest[] }>('/permission-requests', { params })

export const getPermissionRequestPendingCount = () =>
  request.get<{ count: number }>('/permission-requests/pending-count')

export const approvePermissionRequest = (id: number, comment?: string) =>
  request.post<PermissionRequest>(`/permission-requests/${id}/approve`, { comment })

export const rejectPermissionRequest = (id: number, comment?: string) =>
  request.post<PermissionRequest>(`/permission-requests/${id}/reject`, { comment })

/** ---------- 权限码管理（rx_permission CRUD + 菜单匹配下拉建议） ---------- */

export interface PermissionCode {
  id?: number
  permissionCode?: string
  permissionName?: string
  module?: string
  description?: string
  menuUsage?: number
  roleUsage?: number
}

/** 分页查询权限码 */
export const listPermissionCodes = (params: {
  current?: number
  size?: number
  keyword?: string
  module?: string
} = {}) => request.get<{ total: number; records: PermissionCode[] }>('/permissions', { params })

/** 全部权限码（下拉字典） */
export const listAllPermissionCodes = () => request.get<PermissionCode[]>('/permissions/all')

/** 按菜单业务域过滤的建议码（菜单管理页 perms 下拉） */
export const suggestPermissionCodes = (params: { menuTitle?: string; keyword?: string } = {}) =>
  request.get<PermissionCode[]>('/permissions/suggest', { params })

export const createPermissionCode = (data: Partial<PermissionCode>) => request.post<PermissionCode>('/permissions', data)

export const updatePermissionCode = (id: number, data: Partial<PermissionCode>) =>
  request.put<PermissionCode>(`/permissions/${id}`, data)

export const deletePermissionCode = (id: number) => request.delete<void>(`/permissions/${id}`)

/** 解析后端 JSON 数组字段 */
export const parseJsonArray = (raw?: string): string[] => {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw) as unknown
    return Array.isArray(arr) ? (arr as string[]) : [raw]
  } catch {
    return [raw]
  }
}
