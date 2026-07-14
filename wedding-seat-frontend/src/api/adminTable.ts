import adminHttp from './adminHttp'
import type { ApiResponse } from './http'

export interface AdminSeatDetail {
  id: number
  seatNo: number
  status: number // 0=空闲 1=已占用
  guestName: string | null
  guestPhone: string | null
}

export interface AdminTableLayout {
  id: number
  tableNo: string
  seatCount: number
  remark: string | null
  posX: number | null
  posY: number | null
  rotation: number
  seats: AdminSeatDetail[]
}

export interface VenueElement {
  id: number
  type: 'stage' | 'screen' | 'entrance' | 'exit' | string
  label: string
  posX: number
  posY: number
  width: number
  height: number
  rotation: number
}

export interface AdminVenueLayout {
  canvasWidth: number
  canvasHeight: number
  elements: VenueElement[]
  tables: AdminTableLayout[]
}

export interface TableSaveParam {
  id?: number
  eventId: number
  tableNo: string
  seatCount: number
  remark?: string
  posX?: number
  posY?: number
  rotation?: number
}

/** 新增/编辑桌位共用一个接口：不传id是新增，传id是编辑 */
export const saveTable = (data: TableSaveParam): Promise<ApiResponse<null>> =>
  adminHttp.post('/admin/table/save', data)

/**
 * 删除桌位。force默认false：如果桌上有人入座会被拒绝(抛出业务异常，被axios拦截器reject)，
 * 前端捕获后二次确认，再传force=true重新调用一次强制删除
 */
export const deleteTable = (id: number, force = false): Promise<ApiResponse<null>> =>
  adminHttp.delete(`/admin/table/${id}`, { params: { force } })

export const listTables = (eventId: number): Promise<ApiResponse<AdminTableLayout[]>> =>
  adminHttp.get('/admin/table/list', { params: { eventId } })

/** 桌位大地图主数据源：画布尺寸+场地元素+每桌详细座位(带来宾姓名手机号) */
export const getAdminVenueLayout = (eventId: number): Promise<ApiResponse<AdminVenueLayout>> =>
  adminHttp.get('/admin/tables/layout', { params: { eventId } })

export interface VenueElementSaveParam {
  id?: number
  eventId: number
  type: string
  label?: string
  posX: number
  posY: number
  width?: number
  height?: number
  rotation?: number
}

export const saveVenueElement = (data: VenueElementSaveParam): Promise<ApiResponse<null>> =>
  adminHttp.post('/admin/venue-element/save', data)

export const deleteVenueElement = (id: number): Promise<ApiResponse<null>> =>
  adminHttp.delete(`/admin/venue-element/${id}`)
