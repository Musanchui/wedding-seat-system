import axios from 'axios'
import router from '@/router'
import { useAdminStore } from '@/stores/admin'

/**
 * 管理端用的axios实例：自动在请求头带上token，并且统一处理401(未登录/token过期)。
 * 注意：这里不能在模块顶层直接调用 useAdminStore()，因为pinia此时可能还没初始化完成，
 * 要在拦截器函数内部调用（每次请求发出时才取一次），这是Vue生态里常见的坑。
 */
const adminHttp = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

adminHttp.interceptors.request.use((config) => {
  const adminStore = useAdminStore()
  if (adminStore.token) {
    config.headers.Authorization = `Bearer ${adminStore.token}`
  }
  return config
})

adminHttp.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const resData = error.response?.data

    if (resData?.code === 401) {
      // token失效/未登录：清空本地登录态，弹回登录页
      const adminStore = useAdminStore()
      adminStore.logout()
      router.push({ name: 'AdminLogin' })
      return Promise.reject(resData)
    }

    if (resData) {
      return Promise.reject(resData)
    }
    return Promise.reject({ code: -1, message: '网络异常，请检查网络连接', data: null })
  }
)

export default adminHttp
