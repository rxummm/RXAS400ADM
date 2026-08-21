import request from './request'
import blobClient from './blobClient'

/** IFS 目录项 */
export interface IfsEntry {
  NAME: string
  PATH: string
  TYPE: 'DIR' | 'FILE'
  SIZE: number
  MODIFIED: string
  [key: string]: unknown
}

export const listIfsDir = (path: string): Promise<IfsEntry[]> =>
  request.get('/ifs', { params: { path } })

export const readIfsFile = (path: string) =>
  request.get<{ content?: string }>('/ifs/content', { params: { path } })

/** 文档管理「上传到 IFS」：把文档内容写入指定 IFS 路径（DOC_MANAGE 权限） */
export const writeIfsFile = (path: string, content: string) =>
  request.post<{ path: string }>('/ifs/write', { path, content })

/** IFS 页文件管理（IFS_MANAGE 权限）：上传本地文件 */
export const uploadIfsFile = (path: string, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<{ path: string; size: number }>('/ifs/upload', form, {
    params: { path },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** IFS 页文件管理：下载文件（P1-1：走独立 blob 实例，不经 ApiResponse 解包，返回真实 Blob） */
export const downloadIfsFile = async (path: string): Promise<Blob> => {
  const res = await blobClient.get<Blob>('/ifs/download', { params: { path } })
  return res.data
}

/** IFS 页文件管理：新建目录 */
export const mkdirIfs = (path: string) =>
  request.post<{ path: string }>('/ifs/mkdir', { path })

/** IFS 页文件管理：逻辑删除（移入回收站 .trash，返回回收站路径） */
export const deleteIfsFile = (path: string) =>
  request.delete<{ path: string; trashPath: string }>('/ifs/file', { params: { path } })

/** IFS 页文件管理：从回收站恢复（移回原路径） */
export const restoreIfsFile = (trashPath: string) =>
  request.post<{ path: string }>('/ifs/restore', null, { params: { trashPath } })
