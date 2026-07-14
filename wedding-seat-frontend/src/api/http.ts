import axios from 'axios'

/** 后端统一返回体结构，所有接口都是这个外壳 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/**
 * 来宾端用的axios实例：不带Authorization头，因为guest接口都不需要登录
 */
export const guestHttp = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

guestHttp.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 网络层面的错误(超时、断网、5xx导致代理报错等)，统一包装成跟后端Result一样的结构，
    // 这样页面里 catch 到的东西格式始终一致，不用分两套判断逻辑
    if (error.response?.data) {
      return Promise.reject(error.response.data)
    }
    return Promise.reject({ code: -1, message: '网络异常，请检查网络连接', data: null })
  }
)
