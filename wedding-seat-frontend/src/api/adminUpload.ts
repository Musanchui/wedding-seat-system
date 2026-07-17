import adminHttp from './adminHttp'
import type { ApiResponse } from './http'

export interface PhotoItem {
  id: number
  url: string
  sortOrder: number
}

export const uploadPhoto = (eventId: number, file: File): Promise<ApiResponse<PhotoItem>> => {
  const formData = new FormData()
  formData.append('file', file)
  return adminHttp.post('/admin/upload/photo', formData, {
    params: { eventId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const uploadMusic = (eventId: number, file: File): Promise<ApiResponse<string>> => {
  const formData = new FormData()
  formData.append('file', file)
  return adminHttp.post('/admin/upload/music', formData, {
    params: { eventId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const listPhotos = (eventId: number): Promise<ApiResponse<PhotoItem[]>> =>
  adminHttp.get('/admin/photo/list', { params: { eventId } })

export const deletePhoto = (id: number): Promise<ApiResponse<null>> =>
  adminHttp.delete(`/admin/photo/${id}`)

export const reorderPhotos = (
  eventId: number,
  items: { id: number; sortOrder: number }[]
): Promise<ApiResponse<null>> => adminHttp.put('/admin/photo/sort', { eventId, items })
