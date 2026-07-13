import adminHttp from '@/utils/adminHttp'
import type { ApiResponse } from './admin'

// 婚礼详情接口返回结构定义
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
  status: number // 0: 筹备中, 1: 已发布
}

// 婚礼列表项结构定义（比详情更轻量）
export interface EventListItem {
  id: number
  slug: string
  groomName: string | null
  brideName: string | null
  eventTime: string | null
  status: number
}

/**
 * 2.1 创建婚礼
 */
export const createEvent = (data: { groomName?: string; brideName?: string; slug?: string }): Promise<ApiResponse<EventDetail>> => {
  return adminHttp.post('/admin/event', data)
}

/**
 * 2.2 编辑婚礼信息
 */
export const updateEvent = (id: number, data: Partial<Omit<EventDetail, 'id' | 'layoutWidth' | 'layoutHeight'>>): Promise<ApiResponse<null>> => {
  return adminHttp.put(`/admin/event/${id}`, data)
}

/**
 * 2.3 查询单场婚礼详情
 */
export const getEventDetail = (id: number): Promise<ApiResponse<EventDetail>> => {
  return adminHttp.get(`/admin/event/${id}`)
}

/**
 * 2.4 查询我的婚礼列表
 */
export const getMyEventList = (): Promise<ApiResponse<EventListItem[]>> => {
  return adminHttp.get('/admin/event/list')
}