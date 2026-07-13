import adminHttp from '@/utils/adminHttp'
import type { ApiResponse } from './admin'

// 单个桌位的数据定义
export interface TableItem {
  id?: number          // 后端生成的ID，前端新加的暂无
  tableName: string    // 桌名，如“男方主桌”、“大学同学”
  seatCount: number    // 这张桌子限坐几人
  x: number           // 画布上的 X 坐标
  y: number           // 画布上的 Y 坐标
}

// 整个画布的打包保存请求体
export interface SaveLayoutReq {
  layoutWidth: number
  layoutHeight: number
  tables: TableItem[]
}

/**
 * 3.1 获取某场婚礼的桌位布局
 */
export const getSeatLayout = (slug: string): Promise<ApiResponse<{
  layoutWidth: number
  layoutHeight: number
  tables: TableItem[]
}>> => {
  return adminHttp.get(`/admin/event/seats/${slug}`)
}

/**
 * 3.2 批量保存桌位布局（含坐标）
 */
export const saveSeatLayout = (slug: string, data: SaveLayoutReq): Promise<ApiResponse<null>> => {
  return adminHttp.post(`/admin/event/seats/${slug}/save`, data)
}