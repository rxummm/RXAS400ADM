import request from './request'

export interface ObjectRow {
  OBJECT_NAME: string
  OBJECT_TYPE: string
  OBJECT_LIBRARY: string
  OBJECT_SIZE: number
  OBJECT_CREATION_TIMESTAMP: string
}

/** 对象详情（QSYS2.OBJECT_STATISTICS 大写字字段） */
export interface ObjectDetail {
  OBJECT_NAME: string
  OBJECT_TYPE: string
  OBJECT_LIBRARY: string
  OBJECT_SIZE: number
  OBJECT_CREATION_TIMESTAMP: string
  OBJECT_CHANGE_TIMESTAMP?: string
  OBJECT_TEXT_DESCRIPTION?: string
  OBJECT_OWNER?: string
  ASP_NAME?: string
  [key: string]: unknown
}

/** 对象引用（引用方 / 被引用方，大写字字段） */
export interface ObjectReference {
  OBJECT_NAME?: string
  OBJECT_TYPE?: string
  OBJECT_LIBRARY?: string
  REF_OBJ_NAME?: string
  REF_OBJ_TYPE?: string
  REF_OBJ_LIBRARY?: string
  [key: string]: unknown
}

/** 对象权限（小写字字段） */
export interface ObjectAuthority {
  user: string
  authority: string
  authorityType: string
  [key: string]: unknown
}

export const searchObjects = (params: {
  library?: string
  type?: string
  keyword?: string
  current?: number
  size?: number
} = {}): Promise<{ total: number; records: ObjectRow[] }> => request.get('/objects', { params })

export const objectDetail = (library: string, name: string): Promise<ObjectDetail> =>
  request.get(`/objects/${library}/${name}/detail`)

export const objectReferences = (library: string, name: string, direction: 'IN' | 'OUT'): Promise<ObjectReference[]> =>
  request.get(`/objects/${library}/${name}/references`, { params: { direction } })

export const objectAuthorities = (library: string, name: string): Promise<ObjectAuthority[]> =>
  request.get(`/objects/${library}/${name}/authorities`)
