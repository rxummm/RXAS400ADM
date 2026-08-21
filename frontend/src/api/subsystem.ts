import request from './request'

/** 子系统清单（QSYS2.SUBSYSTEM_INFO 大写列名） */
export interface SubsystemRow {
  SUBSYSTEM_NAME: string
  SUBSYSTEM_DESCRIPTION?: string
  STATUS: string
  NUMBER_OF_ACTIVE_JOBS?: number
  MAXIMUM_ACTIVE_JOBS?: number
  SUBSYSTEM_LIBRARY?: string
  [key: string]: unknown
}

export const listSubsystems = (): Promise<SubsystemRow[]> => request.get('/subsystems')

export const startSubsystem = (name: string): Promise<{ message: string }> =>
  request.post(`/subsystems/${name}/start`)

export const endSubsystem = (name: string): Promise<{ message: string }> =>
  request.post(`/subsystems/${name}/end`)
