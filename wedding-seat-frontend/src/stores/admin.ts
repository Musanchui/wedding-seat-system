import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore(
  'admin',
  () => {
    const token = ref<string>('')
    const adminId = ref<number | null>(null)
    const username = ref<string>('')
    const nickname = ref<string>('')

    const setLoginInfo = (info: { token: string; adminId: number; username: string; nickname: string | null }) => {
      token.value = info.token
      adminId.value = info.adminId
      username.value = info.username
      nickname.value = info.nickname || info.username
    }

    const logout = () => {
      token.value = ''
      adminId.value = null
      username.value = ''
      nickname.value = ''
    }

    return { token, adminId, username, nickname, setLoginInfo, logout }
  },
  {
    // 需要装 pinia-plugin-persistedstate 插件；main.ts里要注册这个插件，登录状态刷新页面才不会丢
    persist: true
  } as any
)
