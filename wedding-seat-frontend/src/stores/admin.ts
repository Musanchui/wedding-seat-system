import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const adminInfo = ref<{
    adminId: number | null
    username: string
    nickname: string
  }>({
    adminId: null,
    username: '',
    nickname: ''
  })

  // 登录成功，保存数据
  const setLoginInfo = (loginToken: string, info: { adminId: number; username: string; nickname: string }) => {
    token.value = loginToken
    adminInfo.value = info
    localStorage.setItem('admin_token', loginToken)
  }

  // 登出或 401 被踢，清空数据
  const clearLoginInfo = () => {
    token.value = ''
    adminInfo.value = { adminId: null, username: '', nickname: '' }
    localStorage.removeItem('admin_token')
  }

  return {
    token,
    adminInfo,
    setLoginInfo,
    clearLoginInfo
  }
})