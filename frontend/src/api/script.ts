import request from './request'

export interface CommandScript {
  id: number
  name: string
  command: string
  tags?: string
  favorite: boolean
  runCount?: number
  lastRunTime?: string
}

/** 执行结果（{ success, message }） */
export interface ScriptRunResult {
  success: boolean
  message: string
  [key: string]: unknown
}

export const listScripts = (params: { favorite?: boolean; tag?: string } = {}): Promise<CommandScript[]> =>
  request.get('/scripts', { params })

export const listScriptTags = (): Promise<string[]> => request.get('/scripts/tags')

export const createScript = (data: Partial<CommandScript>) => request.post('/scripts', data)

export const updateScript = (id: number, data: Partial<CommandScript>) =>
  request.put(`/scripts/${id}`, data)

export const deleteScript = (id: number) => request.delete(`/scripts/${id}`)

export const toggleScriptFavorite = (id: number, favorite: boolean) =>
  request.post(`/scripts/${id}/favorite`, null, { params: { favorite } })

export const executeScript = (id: number, serverId: number): Promise<ScriptRunResult> =>
  request.post(`/scripts/${id}/execute`, null, { params: { serverId } })
