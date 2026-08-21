import request from './request'

/** 消息文件（*MSGF）列表项 */
export interface MessageFileSummary {
  MESSAGE_FILE_NAME: string
  MESSAGE_FILE_LIBRARY: string
  NUMBER_OF_MESSAGES: number
  [key: string]: unknown
}

/** 文件内消息描述行 */
export interface MessageFileRow {
  MESSAGE_ID: string
  MESSAGE_TEXT: string
  SECOND_LEVEL_TEXT?: string
  SEVERITY: string
  MESSAGE_FILE_NAME?: string
  [key: string]: unknown
}

/** 消息文件（*MSGF）列表，按库过滤 */
export const fetchMessageFiles = (library?: string): Promise<MessageFileSummary[]> =>
  request.get('/message-files/files', { params: { library } })

/** 文件内消息描述列表（MESSAGE_ID / 文本 / 二级文本 / 严重级别） */
export const fetchMessages = (library: string, file: string, keyword?: string): Promise<MessageFileRow[]> =>
  request.get('/message-files/messages', { params: { library, file, keyword } })

export interface MessageRequest {
  library?: string
  file: string
  id: string
  text: string
  secondLevel?: string
  severity: number
}

export const addMessage = (data: MessageRequest) => request.post('/message-files/messages', data)

export const updateMessage = (data: MessageRequest) => request.put('/message-files/messages', data)

export const deleteMessage = (library: string, file: string, id: string) =>
  request.delete('/message-files/messages', { params: { library, file, id } })
