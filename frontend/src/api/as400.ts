import request, { withNoDedupe } from './request'

export interface IbmiSystem {
  id: number
  name: string
  host: string
  port: number
  /** N2：连接账号（QSECOFR 等）只在 AS400_MANAGE 的 detail 接口返回，普通列表不包含 */
  username?: string
  password?: string
  passwordEncrypt?: string
  environment: string
  criticalLevel: string
  status: string
  description?: string
  region?: string
  haGroup?: string
  defaultLibraries?: string
  ccsid?: number
  sortOrder?: number
  enabled?: boolean
  defaultServer?: boolean
  sslEnabled?: boolean
  connectionStatus?: string
}

export interface CommandResult {
  output: string
  success: boolean
  elapsed: number
}

/** N2：全局服务器列表（仅登录可见，不含 username/passwordEncrypt 连接凭据）
 * noDedupe=true：与 Dashboard 等页面直连同一 URL 时避免互相 abort（P2-23 约定，
 * 见 monitor.ts fetchOverview）。store.fetchServers 须传 true，否则并发时被取消。 */
export const fetchSystems = (noDedupe = false): Promise<IbmiSystem[]> =>
  request.get('/as400/systems', noDedupe ? withNoDedupe() : undefined)

/** N2：完整服务器列表（含 username），仅供资产清单管理页（AS400_MANAGE） */
export const fetchSystemDetail = (): Promise<IbmiSystem[]> => request.get('/as400/systems/detail')

/** 登录页 AS400 模式下拉框：仅启用服务器（免登录公开接口） */
export const fetchEnabledServers = (): Promise<IbmiSystem[]> => request.get('/as400/servers/enabled')

export const createSystem = (data: Partial<IbmiSystem>): Promise<IbmiSystem> => request.post('/as400/systems', data)

export const updateSystem = (id: number, data: Partial<IbmiSystem>): Promise<IbmiSystem> => request.put(`/as400/systems/${id}`, data)

export const deleteSystem = (id: number): Promise<void> => request.delete(`/as400/systems/${id}`)

export const testSystem = (id: number): Promise<{ success: boolean; message: string }> => request.post(`/as400/systems/${id}/test`)

export const executeCommand = (id: number, command: string): Promise<CommandResult> =>
  request.post(`/as400/systems/${id}/command`, null, { params: { command } })