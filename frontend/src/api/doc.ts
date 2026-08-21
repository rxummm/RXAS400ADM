import request from './request'
import blobClient from './blobClient'

/** 文档类型（与后端 DocService.DOC_TYPE_* 一致） */
export const DOC_TYPES = ['MARKDOWN', 'TEXT', 'HTML', 'PDF', 'IMAGE'] as const

export type DocType = (typeof DOC_TYPES)[number]

export interface DocItem {
  id: number
  title: string
  templateId?: number
  templateName?: string
  content: string
  docType?: DocType
  status: string
  createdBy?: string
  createdTime?: string
  updatedTime?: string
  version?: number
  approvedBy?: string
  rejectReason?: string
  /** 最近一次「上传到 IFS」的路径（发布/手动上传后回填） */
  ifsPath?: string
}

export interface TemplateItem {
  id: number
  name: string
  category: string
  content?: string
  docType?: DocType
  createdBy?: string
  createdTime?: string
  updatedTime?: string
}

export interface DocVersion {
  id: number
  version: number
  title: string
  content: string
  createdTime: string
  operator?: string
}

export const listDocs = (params: {
  current?: number
  size?: number
  keyword?: string
  status?: string
  deleted?: number
} = {}): Promise<{ total: number; records: DocItem[] }> => request.get('/docs', { params })

export const createDoc = (data: { title: string; templateId?: number; content: string; docType?: DocType }) =>
  request.post('/docs', data)

export const updateDoc = (id: number, data: { title: string; templateId?: number; content: string; docType?: DocType; ifsPath?: string }) =>
  request.put(`/docs/${id}`, data)

export const deleteDoc = (id: number) => request.delete(`/docs/${id}`)

export const restoreDoc = (id: number) => request.post(`/docs/${id}/restore`)

export const purgeDoc = (id: number) => request.delete(`/docs/${id}/purge`)

export const submitDoc = (id: number) => request.post(`/docs/${id}/submit`)

export const approveDoc = (id: number) => request.post(`/docs/${id}/approve`)

export const rejectDoc = (id: number, reason: string) =>
  request.post(`/docs/${id}/reject`, null, { params: { reason } })

export const docVersions = (id: number): Promise<DocVersion[]> => request.get(`/docs/${id}/versions`)

export const rollbackDoc = (id: number, version: number) =>
  request.post(`/docs/${id}/rollback/${version}`)

/**
 * 文档 IFS 发布文件（V49 文件型预览）：走 blob 实例携带鉴权头，返回原始 Blob。
 * PDF/图片 等二进制类型经它按 Content-Type 展示。
 */
export const docFile = async (id: number): Promise<Blob> => {
  const res = await blobClient.get<Blob>(`/docs/${id}/file`)
  return res.data
}

export const listTemplates = (category?: string): Promise<TemplateItem[]> =>
  request.get('/doc-templates', { params: { category } })

export const createTemplate = (data: { name: string; category: string; content: string; docType?: DocType }) =>
  request.post('/doc-templates', data)

export const updateTemplate = (id: number, data: { name: string; category: string; content: string; docType?: DocType }) =>
  request.put(`/doc-templates/${id}`, data)

export const deleteTemplate = (id: number) => request.delete(`/doc-templates/${id}`)