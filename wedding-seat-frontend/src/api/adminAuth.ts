import adminHttp from './adminHttp'
import type { ApiResponse } from './http'

export interface AdminLoginResult {
  adminId: number
  username: string
  nickname: string | null
  token: string
}

export const adminRegister = (data: {
  username: string
  password: string
  nickname?: string
  phone?: string
}): Promise<ApiResponse<null>> => adminHttp.post('/admin/register', data)

export const adminLogin = (data: {
  username: string
  password: string
}): Promise<ApiResponse<AdminLoginResult>> => adminHttp.post('/admin/login', data)
