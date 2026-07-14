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
          <div class="section-title">素材</div>
          <el-form-item label="背景音乐 URL">
            <el-input v-model="form.musicUrl" placeholder="/uploads/music/xxx.mp3" />
            <div class="form-tip">素材上传功能还在开发中，目前需要先把文件传到服务器上，再把访问路径填在这里</div>
          </el-form-item>
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
import { ArrowLeft } from '@element-plus/icons-vue'
import { getEventDetail, updateEvent } from '@/api/adminEvent'

const route = useRoute()
const router = useRouter()
const eventId = Number(route.params.id)

const pageLoading = ref(false)
const saving = ref(false)

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
})
</script>

<style scoped>
.event-edit-page { min-height: 100vh; background: #f5f7f9; }
.page-header { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: white; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 17px; flex: 1; }

.edit-body { max-width: 720px; margin: 0 auto; padding: 32px 24px 80px; }
.form-section { background: white; border-radius: 12px; padding: 24px; margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 20px; border-left: 3px solid #ff4d4f; padding-left: 10px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }
.action-bar { display: flex; gap: 12px; }
</style>
