<template>
  <div class="event-edit-page" v-loading="pageLoading">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="router.push('/admin/dashboard')">返回控制台</el-button>
      <h2>编辑婚礼信息</h2>
      <el-tag :type="form.status === 1 ? 'success' : 'info'">{{ form.status === 1 ? '已发布' : '筹备中' }}</el-tag>
    </div>

    <div class="edit-body">
      <el-form :model="form" label-position="top" label-width="120px">
        <div class="form-section">
          <div class="section-title">新人信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="新郎姓名">
                <el-input v-model="form.groomName" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新娘姓名">
                <el-input v-model="form.brideName" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">婚礼安排</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="婚礼时间">
                <el-date-picker
                  v-model="form.eventTime"
                  type="datetime"
                  placeholder="选择日期时间"
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="宴席地点">
                <el-input v-model="form.location" placeholder="例如：XX大酒店3楼宴会厅" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="欢迎寄语">
            <el-input v-model="form.greetingMessage" type="textarea" :rows="3" placeholder="欢迎来宾的一段话" />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="section-title">背景音乐</div>
          <div class="music-row">
            <audio v-if="form.musicUrl" :src="form.musicUrl" controls style="height: 36px" />
            <span v-else class="form-tip" style="margin: 0">还没有上传背景音乐</span>
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeMusicUpload"
              :http-request="handleMusicUpload"
              accept=".mp3,.wav,.m4a"
            >
              <el-button :loading="musicUploading">{{ form.musicUrl ? '重新上传' : '上传音乐' }}</el-button>
            </el-upload>
          </div>
          <div class="form-tip">支持 mp3/wav/m4a，最大50MB</div>
        </div>

        <div class="form-section">
          <div class="section-title">照片墙（{{ photoList.length }}张）</div>
          <div class="photo-grid">
            <div v-for="(photo, index) in photoList" :key="photo.id" class="photo-item">
              <img :src="photo.url" />
              <div class="photo-actions">
                <el-icon v-if="index > 0" @click="movePhoto(index, -1)"><ArrowLeft /></el-icon>
                <el-icon @click="handleDeletePhoto(photo.id)"><Delete /></el-icon>
                <el-icon v-if="index < photoList.length - 1" @click="movePhoto(index, 1)"><ArrowRight /></el-icon>
              </div>
            </div>
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforePhotoUpload"
              :http-request="handlePhotoUpload"
              accept=".jpg,.jpeg,.png,.webp"
              class="photo-upload-box"
            >
              <el-icon style="font-size: 24px; color: #999"><Plus /></el-icon>
            </el-upload>
          </div>
          <div class="form-tip">支持 jpg/jpeg/png/webp，最大50MB；用左右箭头调整轮播顺序</div>
        </div>

        <div class="form-section">
          <div class="section-title">访问设置</div>
          <el-form-item label="访问标识 slug">
            <el-input v-model="form.slug" />
            <div class="form-tip">这是来宾端链接的一部分，如果已经分发出去链接，修改会导致旧链接失效</div>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-switch
              v-model="isPublished"
              active-text="已发布（来宾端可访问）"
              inactive-text="筹备中（来宾端不可访问）"
            />
          </el-form-item>
        </div>
      </el-form>

      <div class="action-bar">
        <el-button type="primary" color="#ff4d4f" size="large" :loading="saving" @click="handleSave">
          保存修改
        </el-button>
        <el-button size="large" @click="router.push(`/admin/event/seats/${eventId}`)">
          去配置桌位大地图
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Delete, Plus } from '@element-plus/icons-vue'
import { getEventDetail, updateEvent } from '@/api/adminEvent'
import { uploadPhoto, uploadMusic, listPhotos, deletePhoto, reorderPhotos, type PhotoItem } from '@/api/adminUpload'

const route = useRoute()
const router = useRouter()
const eventId = Number(route.params.id)

const pageLoading = ref(false)
const saving = ref(false)
const musicUploading = ref(false)
const photoList = ref<PhotoItem[]>([])

const form = reactive({
  groomName: '',
  brideName: '',
  eventTime: '' as string | null,
  location: '',
  greetingMessage: '',
  musicUrl: '',
  slug: '',
  status: 0
})

const isPublished = computed({
  get: () => form.status === 1,
  set: (val: boolean) => { form.status = val ? 1 : 0 }
})

const loadDetail = async () => {
  pageLoading.value = true
  try {
    const res = await getEventDetail(eventId)
    form.groomName = res.data.groomName || ''
    form.brideName = res.data.brideName || ''
    form.eventTime = res.data.eventTime
    form.location = res.data.location || ''
    form.greetingMessage = res.data.greetingMessage || ''
    form.musicUrl = res.data.musicUrl || ''
    form.slug = res.data.slug
    form.status = res.data.status
  } catch (err: any) {
    ElMessage.error(err?.message || '加载失败')
  } finally {
    pageLoading.value = false
  }
}

const loadPhotos = async () => {
  try {
    const res = await listPhotos(eventId)
    photoList.value = res.data
  } catch (err) {
    // 静默失败即可，不影响页面其他部分
  }
}

const handleBeforeMusicUpload = (file: File) => {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件不能超过50MB')
    return false
  }
  return true
}

const handleMusicUpload = async (options: any) => {
  musicUploading.value = true
  try {
    const res = await uploadMusic(eventId, options.file)
    form.musicUrl = res.data
    ElMessage.success('背景音乐上传成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  } finally {
    musicUploading.value = false
  }
}

const handleBeforePhotoUpload = (file: File) => {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件不能超过50MB')
    return false
  }
  return true
}

const handlePhotoUpload = async (options: any) => {
  try {
    await uploadPhoto(eventId, options.file)
    ElMessage.success('照片上传成功')
    await loadPhotos()
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  }
}

const handleDeletePhoto = async (photoId: number) => {
  try {
    await deletePhoto(photoId)
    ElMessage.success('已删除')
    await loadPhotos()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

// 交换相邻两张照片的顺序号，交换后立即保存到后端
const movePhoto = async (index: number, direction: 1 | -1) => {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= photoList.value.length) return

  const list = photoList.value
  ;[list[index], list[targetIndex]] = [list[targetIndex], list[index]]

  try {
    await reorderPhotos(
      eventId,
      list.map((p, i) => ({ id: p.id, sortOrder: i }))
    )
  } catch (err: any) {
    ElMessage.error(err?.message || '排序保存失败')
    await loadPhotos()
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateEvent(eventId, {
      groomName: form.groomName,
      brideName: form.brideName,
      eventTime: form.eventTime || undefined,
      location: form.location,
      greetingMessage: form.greetingMessage,
      musicUrl: form.musicUrl,
      slug: form.slug,
      status: form.status
    })
    ElMessage.success('保存成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadDetail()
  loadPhotos()
})
</script>

<style scoped>
.event-edit-page { min-height: 100vh; background: #f5f7f9; }
.page-header { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: white; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 17px; flex: 1; }

.music-row { display: flex; align-items: center; gap: 16px; }

.photo-grid { display: grid; grid-template-columns: repeat(auto-fill, 110px); gap: 12px; }
.photo-item { position: relative; width: 110px; height: 110px; border-radius: 8px; overflow: hidden; }
.photo-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.photo-actions {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.55); display: flex; justify-content: space-around;
  padding: 6px 0; opacity: 0; transition: opacity 0.15s;
}
.photo-item:hover .photo-actions { opacity: 1; }
.photo-actions .el-icon { color: white; cursor: pointer; font-size: 16px; }
.photo-upload-box {
  width: 110px; height: 110px; border: 1px dashed #d9d9d9; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.photo-upload-box:hover { border-color: #ff4d4f; }

.edit-body { max-width: 720px; margin: 0 auto; padding: 32px 24px 80px; }
.form-section { background: white; border-radius: 12px; padding: 24px; margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 20px; border-left: 3px solid #ff4d4f; padding-left: 10px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }
.action-bar { display: flex; gap: 12px; }
</style>
