import axios from 'axios'

// 1. 定义后端统一返回的 Result 接口结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

http.interceptors.response.use(
  (response) => {
    // 这里强行返回经过拦截器剥离后的数据，并在下面用异步返回泛型对其进行约束
    return response.data as any
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 2. 改造导出函数，利用 Promise<ApiResponse<...>> 明确声明返回类型
// 2.1 获取婚礼基础信息
export const getWeddingEvent = (slug: string): Promise<ApiResponse> => {
  return http.get(`/guest/event/${slug}`)
}

// 2.2 获取滚动照片墙
export const getWeddingPhotos = (slug: string): Promise<ApiResponse<any[]>> => {
  return http.get(`/guest/event/${slug}/photos`)
}

// 2.3 来宾登记
export interface RegisterParam {
  eventSlug: string
  name: string
  phone: string
  category?: string
}
export const registerGuest = (data: RegisterParam): Promise<ApiResponse> => {
  return http.post('/guest/register', data)
}

// 2.4 获取某一桌的座位状态
export const getTableSeats = (tableId: number): Promise<ApiResponse<any[]>> => {
  return http.get(`/guest/table/${tableId}/seats`)
}

// 2.5 锁定座位
export interface LockSeatParam {
  guestId: number
  seatId: number
  version: number
}
export const lockSeat = (data: LockSeatParam): Promise<ApiResponse<boolean>> => {
  return http.post('/guest/seat/lock', data)
}
// 定义布局数据结构
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

export interface VenueTable {
  id: number
  tableNo: string
  remark: string
  seatCount: number
  availableSeatsCount: number
  posX: number
  posY: number
  rotation: number
}

export interface VenueLayoutData {
  canvasWidth: number
  canvasHeight: number
  elements: VenueElement[]
  tables: VenueTable[]
}

// 2.7 获取宴会厅全厅布局图
export const getVenueLayout = (slug: string): Promise<ApiResponse<VenueLayoutData>> => {
  return http.get(`/guest/event/${slug}/venue-layout`)
}