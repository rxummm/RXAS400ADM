import i18n from './index'
import request from '@/api/request'

/**
 * 动态翻译（2.5.2）：后端 rx_i18n 表存 key → 文案（按语言），
 * 前端登录后/切换语言时合并进 vue-i18n，覆盖静态文案、支持运维维护。
 * 后端不可达时静默跳过（保持静态文案）。
 */
function toNested(flat: Record<string, string>): Record<string, unknown> {
  const root: Record<string, unknown> = {}
  for (const [key, text] of Object.entries(flat)) {
    const parts = key.split('.')
    let node = root
    for (let i = 0; i < parts.length - 1; i++) {
      const part = parts[i]
      if (typeof node[part] !== 'object' || node[part] === null) {
        node[part] = {}
      }
      node = node[part] as Record<string, unknown>
    }
    node[parts[parts.length - 1]] = text
  }
  return root
}

export async function loadDynamicI18n(lang: string): Promise<void> {
  try {
    const data = (await request.get('/i18n', { params: { lang } })) as Record<string, string>
    if (data && Object.keys(data).length > 0) {
      i18n.global.mergeLocaleMessage(lang, toNested(data))
    }
  } catch {
    /* 静默：后端不可用时使用静态文案 */
  }
}
