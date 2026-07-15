import { guestHttp, type ApiResponse } from './http'

// ============================================
// 类型定义
// ============================================

export interface EventInfo {
  id: number
  groomName: string | null
  brideName: string | null
  eventTime: string | null
  location: string | null
  greetingMessage: string | null
  musicUrl: string | null
}

export interface Photo {
  id: number
  url: string
  sortOrder: number
}

export interface TableSummary {
  id: number
  tableNo: string
  remark: string | null
  seatCount: number
  availableSeatsCount: number
}

export interface Seat {
  id: number
  seatNo: number
  status: number // 0=空闲 1=已占用
  version: number
}

export interface RecommendTable {
  id: number
  tableNo: string
  remark: string | null
  availableSeatsCount: number
}

export interface SeatSummary {
  seatId: number
  tableId: number
  tableNo: string | null
  seatNo: number
}

export interface GuestRegisterResult {
  guestId: number
  /** 新来宾/还没选座时有值，selectedSeats为null */
  recommendedTable: RecommendTable | null
  /** 已经选过至少1个座位时有值(1-3个)，recommendedTable为null */
  selectedSeats: SeatSummary[] | null
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

export interface VenueTable {
  id: number
  tableNo: string
  remark: string | null
  seatCount: number
  availableSeatsCount: number
  posX: number | null
  posY: number | null
  rotation: number
}

export interface VenueLayoutData {
  canvasWidth: number
  canvasHeight: number
  elements: VenueElement[]
  tables: VenueTable[]
}

// ============================================
// 接口
// ============================================

export const getWeddingEvent = (slug: string): Promise<ApiResponse<EventInfo>> =>
  guestHttp.get(`/guest/event/${slug}`)

export const getWeddingPhotos = (slug: string): Promise<ApiResponse<Photo[]>> =>
  guestHttp.get(`/guest/event/${slug}/photos`)

export const getEventTables = (slug: string): Promise<ApiResponse<TableSummary[]>> =>
  guestHttp.get(`/guest/event/${slug}/tables`)

export const getVenueLayout = (slug: string): Promise<ApiResponse<VenueLayoutData>> =>
  guestHttp.get(`/guest/event/${slug}/venue-layout`)

export interface RegisterParam {
  eventSlug: string
  name: string
  phone: string
  category?: string
}
export const registerGuest = (data: RegisterParam): Promise<ApiResponse<GuestRegisterResult>> =>
  guestHttp.post('/guest/register', data)

export const getTableSeats = (tableId: number): Promise<ApiResponse<Seat[]>> =>
  guestHttp.get(`/guest/table/${tableId}/seats`)

export interface LockSeatParam {
  guestId: number
  seatId: number
  version: number
}
/** 选座：一个来宾最多可以选3个，超过会返回 code=400 的业务错误；乐观锁冲突返回 code=409 */
export const lockSeat = (data: LockSeatParam): Promise<ApiResponse<boolean>> =>
  guestHttp.post('/guest/seat/lock', data)

export interface ReleaseSeatParam {
  guestId: number
  seatId: number
}
/** 取消(释放)已选的某个座位 */
export const releaseSeat = (data: ReleaseSeatParam): Promise<ApiResponse<boolean>> =>
  guestHttp.post('/guest/seat/release', data)

/** 查询某个来宾当前已选定的所有座位(0-3个) */
export const getMySeats = (guestId: number): Promise<ApiResponse<SeatSummary[]>> =>
  guestHttp.get('/guest/my-seats', { params: { guestId } })

export interface AutoAssignParam {
  guestId: number
  seatCount: number
}
/** 不想自己选座的来宾用这个：告诉系统要几个座位(含自己，最多3个)，系统自动分配 */
export const autoAssignSeats = (data: AutoAssignParam): Promise<ApiResponse<SeatSummary[]>> =>
  guestHttp.post('/guest/seat/auto-assign', data)
