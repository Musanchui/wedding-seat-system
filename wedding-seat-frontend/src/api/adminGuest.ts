import adminHttp from './adminHttp'
import type { ApiResponse } from './http'

export interface AdminGuestListItem {
  id: number
  displayName: string
  name: string
  phone: string
  category: string | null
  seatsDesc: string
  seatCount: number
  registerTime: string
}

export const getGuestList = (eventId: number): Promise<ApiResponse<AdminGuestListItem[]>> =>
  adminHttp.get('/admin/guest/list', { params: { eventId } })

/**
 * 导出接口不走统一的axios拦截器(那个默认把响应当JSON处理)，
 * 用原生fetch单独发请求，拿到二进制blob后触发浏览器下载
 */
export const exportGuestListExcel = async (eventId: number, adminToken: string) => {
  const res = await fetch(`/api/admin/guest/export?eventId=${eventId}`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  if (!res.ok) {
    throw new Error('导出失败')
  }
  const blob = await res.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '宾客名单.xlsx'
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

/** 邀请函二维码图片地址：直接当img的src用，浏览器会自动带上之前设置的通用请求头吗？不会！
 *  img标签发请求不会带Authorization头，所以这个接口不能直接当img src用，
 *  必须像下面这样单独fetch拿blob，再转成本地blob URL给img展示/下载 */
export const fetchInvitationQrCode = async (eventId: number, adminToken: string): Promise<string> => {
  const res = await fetch(`/api/admin/event/qrcode?eventId=${eventId}`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  if (!res.ok) {
    throw new Error('二维码生成失败')
  }
  const blob = await res.blob()
  return window.URL.createObjectURL(blob)
}
