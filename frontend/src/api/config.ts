import request from './request'

export interface SysConfig {
  configKey: string
  configValue?: string
  description?: string
}

export const listConfigs = () => request.get<SysConfig[]>('/configs')

export const updateConfig = (configKey: string, data: { configValue?: string; description?: string }) =>
  request.put<SysConfig>(`/configs/${configKey}`, data)

export const deleteConfig = (configKey: string) => request.delete<void>(`/configs/${configKey}`)
