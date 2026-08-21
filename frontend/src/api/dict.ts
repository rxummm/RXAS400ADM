import request from './request'

export interface DictType {
  id?: number
  code: string
  name: string
  remark?: string
  sort?: number
  status?: number
  createdTime?: string
}

export interface DictItem {
  id?: number
  typeCode: string
  itemKey: string
  itemValue: string
  sort?: number
  status?: number
  createdTime?: string
}

export const listDictTypes = () => request.get<DictType[]>('/dicts/types')
export const createDictType = (data: DictType) => request.post<DictType>('/dicts/types', data)
export const updateDictType = (id: number, data: DictType) => request.put<DictType>(`/dicts/types/${id}`, data)
export const deleteDictType = (id: number) => request.delete<void>(`/dicts/types/${id}`)

export const listDictItems = (typeCode: string) => request.get<DictItem[]>('/dicts/items', { params: { typeCode } })
export const createDictItem = (data: DictItem) => request.post<DictItem>('/dicts/items', data)
export const updateDictItem = (id: number, data: DictItem) => request.put<DictItem>(`/dicts/items/${id}`, data)
export const deleteDictItem = (id: number) => request.delete<void>(`/dicts/items/${id}`)
