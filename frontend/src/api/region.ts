import request from './request'

export interface Region {
  id: number
  code: string
  name: string
  level: number
  parentCode?: string
  hasChildren?: boolean
  pinyin?: string
  abbreviation?: string
  longitude?: number
  latitude?: number
  sort?: number
  status?: number
}

export const fetchRegionChildren = (parentCode?: string) =>
  request.get<Region[]>('/regions/children', { params: { parentCode } })


export const createRegion = (data: Partial<Region>) => request.post('/regions', data)

export const updateRegion = (id: number, data: Partial<Region>) =>
  request.put(`/regions/${id}`, data)

export const deleteRegion = (id: number) => request.delete(`/regions/${id}`)
