import request from './request'

/** 数据区域 */
export interface DataArea {
  DATA_AREA_LIBRARY: string
  DATA_AREA_NAME: string
  DATA_AREA_TYPE: string
  DATA_AREA_LENGTH: number
  DATA_AREA_VALUE: string
  DATA_AREA_DESCRIPTION: string
}

/** 查询数据区域列表 */
export const fetchDataAreas = (library?: string): Promise<DataArea[]> =>
  request.get('/data-areas', { params: { library } })

/** 获取数据区域详情 */
export const getDataArea = (library: string, name: string): Promise<DataArea> =>
  request.get('/data-areas/detail', { params: { library, name } })

/** 修改数据区域值 */
export const updateDataArea = (library: string, name: string, value: string): Promise<void> =>
  request.put(`/data-areas/${name}`, { value }, { params: { library } })

/** 创建数据_area */
export const createDataArea = (params: { library: string; name: string; length?: number; value?: string }): Promise<void> =>
  request.post('/data-areas', params)

/** 删除数据区域 */
export const deleteDataArea = (library: string, name: string): Promise<void> =>
  request.delete(`/data-areas/${name}`, { params: { library } })
