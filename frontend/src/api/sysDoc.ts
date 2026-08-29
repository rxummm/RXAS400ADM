import request from './request'

/** 知识库文档状态 */
export type SysDocStatus = 'DRAFT' | 'PUBLISHED'

/** 知识库文档 */
export interface SysDocItem {
  id: number
  title: string
  content?: string
  category?: string
  tags?: string
  status: SysDocStatus
  createdBy?: string
  updatedBy?: string
  createdTime?: string
  updatedTime?: string
}

/** 分页查询知识库文档 */
export const listSysDocs = (params: {
  current?: number
  size?: number
  keyword?: string
  status?: string
  category?: string
} = {}): Promise<{ total: number; records: SysDocItem[] }> =>
  request.get('/sys-docs', { params })

/** 获取单个知识库文档 */
export const getSysDoc = (id: number): Promise<SysDocItem> =>
  request.get(`/sys-docs/${id}`)

/** 创建知识库文档 */
export const createSysDoc = (data: {
  title: string
  content?: string
  category?: string
  tags?: string
}) => request.post('/sys-docs', data)

/** 更新知识库文档 */
export const updateSysDoc = (
  id: number,
  data: {
    title: string
    content?: string
    category?: string
    tags?: string
    status?: SysDocStatus
  },
) => request.put(`/sys-docs/${id}`, data)

/** 删除知识库文档 */
export const deleteSysDoc = (id: number) => request.delete(`/sys-docs/${id}`)

/** 切换发布状态 */
export const toggleSysDocStatus = (id: number, status: SysDocStatus) =>
  request.put(`/sys-docs/${id}`, { status })
