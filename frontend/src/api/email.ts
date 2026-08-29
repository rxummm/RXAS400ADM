import request from './request'

export interface EmailConfig {
  configKey: string
  configValue?: string
  description?: string
  updatedTime?: string
}

export interface EmailGroup {
  id: number
  groupName: string
  description?: string
  memberCount?: number
  createdTime?: string
}

export interface EmailRecipient {
  id: number
  groupId: number
  email: string
  userId?: number
  enabled: number
  createdTime?: string
}

export interface EmailLog {
  id: number
  subject: string
  recipients: string
  channel: string
  status: string
  errorMessage?: string
  attachmentName?: string
  createdTime?: string
}

export interface PageResult<T> {
  total: number
  records: T[]
}

// --- Config ---
export const listEmailConfigs = () => request.get<EmailConfig[]>('/email/config')
export const updateEmailConfigs = (configs: Record<string, string>) =>
  request.put<void>('/email/config', { configs })
export const testSendEmail = (to: string) =>
  request.post<void>('/email/test-send', null, { params: { to } })

// --- Groups ---
export const listEmailGroups = (params?: { current?: number; size?: number; keyword?: string }) =>
  request.get<PageResult<EmailGroup>>('/email/groups', { params })
export const listAllEmailGroups = () => request.get<EmailGroup[]>('/email/groups/all')
export const createEmailGroup = (data: { groupName: string; description?: string }) =>
  request.post<void>('/email/groups', data)
export const updateEmailGroup = (id: number, data: { groupName: string; description?: string }) =>
  request.put<void>(`/email/groups/${id}`, data)
export const deleteEmailGroup = (id: number) => request.delete<void>(`/email/groups/${id}`)
export const listGroupMembers = (id: number) => request.get<EmailRecipient[]>(`/email/groups/${id}/members`)
export const addGroupMember = (id: number, data: { email: string; userId?: number }) =>
  request.post<void>(`/email/groups/${id}/members`, data)
export const removeGroupMember = (id: number, memberId: number) =>
  request.delete<void>(`/email/groups/${id}/members/${memberId}`)

// --- Logs ---
export const listEmailLogs = (params?: {
  current?: number; size?: number; channel?: string; status?: string; keyword?: string
}) => request.get<PageResult<EmailLog>>('/email/logs', { params })
export const getEmailLog = (id: number) => request.get<EmailLog>(`/email/logs/${id}`)

// --- Send ---
export const sendEmail = (data: {
  subject: string; text: string; recipients: string; priority?: string
}) => request.post<void>('/email/send', data)
