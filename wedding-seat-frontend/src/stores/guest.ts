import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGuestStore = defineStore('guest', () => {
  const guestId = ref<number | null>(null)
  const guestName = ref<string>('')
  const currentSlug = ref<string>('')
  const recommendedTable = ref<any>(null)

  const setGuestInfo = (id: number, name: string, slug: string, recommend: any) => {
    guestId.value = id
    guestName.value = name
    currentSlug.value = slug
    recommendedTable.value = recommend
  }

  const clearGuestInfo = () => {
    guestId.value = null
    guestName.value = ''
    recommendedTable.value = null
  }

  return { guestId, guestName, currentSlug, recommendedTable, setGuestInfo, clearGuestInfo }
})