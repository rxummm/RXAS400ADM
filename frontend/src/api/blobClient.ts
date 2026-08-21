import axios from 'axios'
import { useStorage, STORAGE_KEYS } from '@/composables/useStorage'
import { API_BASE } from '@/api/request'

/**
 * 文件下载专用 axios 实例（P1-1 修复）：
 * 共享 request 实例的响应拦截器会把 Blob 当 ApiResponse 解包（res.code !== 0 恒真）
 * 导致每次下载都失败。本实例返回原始 Blob，不走统一 JSON 解包。
 */
const blobClient = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
  responseType: 'blob',
})

blobClient.interceptors.request.use((config) => {
  const token = useStorage(STORAGE_KEYS.TOKEN).get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // N6：文件下载同样需要携带当前 AS400 服务器标识（X-AS400-Server 头），
  // 否则 IFS 下载等会落到默认服务器，与页面所选服务器不一致
  const serverId = useStorage(STORAGE_KEYS.AS400_SERVER).get()
  if (serverId) {
    config.headers['X-AS400-Server'] = String(serverId)
  }
  return config
})

/** 触发浏览器下载一个 Blob 响应 */
export function triggerBlobDownload(data: unknown, filename: string) {
  const blob = data instanceof Blob ? data : new Blob([data as BlobPart])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

export default blobClient
