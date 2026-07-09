import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useGuestStore = defineStore('guest', () => {
  const guestId = ref<number | null>(null)
  const guestName = ref<string>('')
  const currentEventId = ref<number | null>(null)
  const recommendedTable = ref<any>(null)

  const setGuestInfo = (id: number, name: string, eventId: number, recommend: any) => {
    guestId.value = id
    guestName.value = name
    currentEventId.value = eventId
    recommendedTable.value = recommend
  }

  return { guestId, guestName, currentEventId, recommendedTable, setGuestInfo }
})