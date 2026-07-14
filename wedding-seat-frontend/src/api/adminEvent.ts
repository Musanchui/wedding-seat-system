import adminHttp from './adminHttp'
import type { ApiResponse } from './http'

export interface EventDetail {
  id: number
  slug: string
  groomName: string | null
  brideName: string | null
  eventTime: string | null
  location: string | null
  greetingMessage: string | null
  musicUrl: string | null
  layoutWidth: number
  layoutHeight: number
  status: number // 0=筹备中 1=已发布
}

export interface EventListItem {
  id: number
  slug: string
  groomName: string | null
  brideName: string | null
  eventTime: string | null
  status: number
}

export const createEvent = (data: {
  groomName?: string
  brideName?: string
  slug?: string
}): Promise<ApiResponse<EventDetail>> => adminHttp.post('/admin/event', data)

/** 只传要修改的字段，其余不传，不会被清空 */
export const updateEvent = (
  id: number,
  data: Partial<Omit<EventDetail, 'id' | 'layoutWidth' | 'layoutHeight'>> & {
    layoutWidth?: number
    layoutHeight?: number
  }
): Promise<ApiResponse<null>> => adminHttp.put(`/admin/event/${id}`, data)

export const getEventDetail = (id: number): Promise<ApiResponse<EventDetail>> =>
  adminHttp.get(`/admin/event/${id}`)

export const getMyEventList = (): Promise<ApiResponse<EventListItem[]>> =>
  adminHttp.get('/admin/event/list')
