import axios from 'axios'
import { useAdminStore } from '@/stores/admin'
import router from '@/router' // 👈 确保你路由的引入路径正确
import { ElMessage } from 'element-plus' // 👈 假设管理端 PC 用的是 Element Plus 提示

const adminHttp = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api', // 根据你的环境配置调整
  timeout: 10000
})

// ======= 请求拦截器：自动追加 Authorization 请求头 =======
adminHttp.interceptors.request.use(
  (config) => {
    const adminStore = useAdminStore()
    
    // 如果是 /admin/** 的请求，且本地存在 token，就自动塞入 Bearer
    if (config.url?.startsWith('/admin') && adminStore.token) {
      config.headers['Authorization'] = `Bearer ${adminStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ======= 响应拦截器：统一处理 200 成功与 401 认证失效 =======
adminHttp.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 如果后端成功返回 200，直接把数据包丢给业务层
    if (res.code === 200) {
      return res
    }

    // ➔ 核心改动：收到 401 说明未登录或 Token 过期，直接在这里截断拦截
    if (res.code === 401) {
      const adminStore = useAdminStore()
      adminStore.clearLoginInfo() // 1. 清空本地 Token 和持久化缓存
      
      ElMessage.error(res.message || '登录已过期，请重新登录')
      
      // 2. 闪电踢回登录页
      router.push('/admin/login')
      return Promise.reject(new Error(res.message || 'Unauthorized'))
    }

    // 其他业务错误（如 400 用户名密码错误、用户名重复等），弹出报错信息
    ElMessage.error(res.message || '系统繁忙，请稍后再试')
    return Promise.reject(res)
  },
  (error) => {
    // 处理 HTTP 状态码层面的错误（网络断开、网关 500 等）
    ElMessage.error('网络请求异常，请检查网络连接')
    return Promise.reject(error)
  }
)

export default adminHttp