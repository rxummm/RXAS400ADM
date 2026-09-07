import request from './request'

export interface I18nEntry {
  i18nKey: string
  lang: string
  text: string
  module?: string
  updatedAt?: string
  updatedBy?: string
}

/** Fetch translations as nested object for i18n runtime */
export const fetchTranslations = (lang: string, module?: string) =>
  request.get<Record<string, unknown>>('/i18n', { params: { lang, module } })

export const listI18nEntries = (params: {
  current?: number
  size?: number
  lang?: string
  keyword?: string
  module?: string
} = {}) => request.get<{ total: number; records: I18nEntry[] }>('/i18n/entries', { params })

export const saveI18nEntry = (data: I18nEntry) => request.post<I18nEntry>('/i18n/entry', data)

export const updateI18nEntry = (data: I18nEntry) => request.put<I18nEntry>('/i18n/entry', data)

export const deleteI18nEntry = (lang: string, key: string) =>
  request.delete<void>(`/i18n/entry/${lang}/${key}`)
