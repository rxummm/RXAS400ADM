import request from './request'

export interface CompileRequest {
  library: string
  sourceFile: string
  member: string
  command?: string
}

export const compileMember = (data: CompileRequest) =>
  request.post<{ status: string; message?: string }>('/compile', data)
