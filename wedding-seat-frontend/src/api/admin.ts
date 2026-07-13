import adminHttp from '@/utils/adminHttp'

// 基础响应结构定义
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 登录成功后的 Data 结构
export interface LoginData {
  adminId: number
  username: string
  nickname: string
  token: string
}

/**
 * 2.1 管理员注册
 */
export const adminRegister = (data: Record<string, any>): Promise<ApiResponse<null>> => {
  return adminHttp.post('/admin/register', data)
}

/**
 * 2.2 管理员登录
 */
export const adminLogin = (data: Record<string, any>): Promise<ApiResponse<LoginData>> => {
  return adminHttp.post('/admin/login', data)
}