import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 来宾端状态：保存当前登记的guestId，避免用户刷新页面/中途退出后要重新登记。
 * 按eventSlug隔离存储，因为同一个手机可能扫过不同婚礼的码。
 */
export const useGuestStore = defineStore(
  'guest',
  () => {
    // 用一个对象记录 { [eventSlug]: { guestId, guestName } }，支持同一设备参加多场婚礼
    const registrations = ref<Record<string, { guestId: number; guestName: string }>>({})

    const setGuestInfo = (eventSlug: string, guestId: number, guestName: string) => {
      registrations.value[eventSlug] = { guestId, guestName }
    }

    const getGuestInfo = (eventSlug: string) => registrations.value[eventSlug] || null

    return { registrations, setGuestInfo, getGuestInfo }
  },
  { persist: true } as any
)
