import request from './request'

export interface CacheInfo {
  name: string
  size?: number | null
}

export const listCaches = () => request.get<CacheInfo[]>('/caches')

export const clearCache = (name: string) => request.delete<void>(`/caches/${name}`)

export const clearAllCaches = () => request.delete<void>('/caches')
