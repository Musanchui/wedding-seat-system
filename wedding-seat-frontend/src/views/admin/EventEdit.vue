<template>
  <div class="event-edit-page">
    <el-page-header title="返回控制台" @back="router.push('/admin/dashboard')">
      <template #content>
        <span class="font-600 mr-3"> 📝 编排婚礼详细档案 </span>
      </template>
    </el-page-header>

    <el-card class="edit-card" v-loading="loading">
      <el-form :model="editForm" label-position="left" label-width="120px" size="large">
        <h3 class="section-title">💍 新人基础明细</h3>
        <el-form-item label="新郎姓名">
          <el-input v-model="editForm.groomName" placeholder="请输入新郎姓名" />
        </el-form-item>
        <el-form-item label="新娘姓名">
          <el-input v-model="editForm.brideName" placeholder="请输入新娘姓名" />
        </el-form-item>
        <el-form-item label="访问短标识 slug">
          <el-input v-model="editForm.slug" placeholder="例如: zhang-li-0815" />
          <div class="alert-tip">⚠️ 警告：如果该链接已印在请柬上发出去，修改此项会导致来宾端旧链接彻底失效，请谨慎修改！</div>
        </el-form-item>

        <el-divider />
        <h3 class="section-title">📍 宴会时间与地点</h3>
        <el-form-item label="喜宴举行时间">
          <el-date-picker
            v-model="editForm.eventTime"
            type="datetime"
            placeholder="请选择喜宴开始时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="喜宴举办地点">
          <el-input v-model="editForm.location" placeholder="例如：汉开喜来登大酒店 5 楼百合厅" />
        </el-form-item>

        <el-divider />
        <h3 class="section-title">🎵 情感氛围与来宾欢迎页</h3>
        <el-form-item label="迎宾致辞文案">
          <el-input 
            v-model="editForm.greetingMessage" 
            type="textarea" 
            :rows="3" 
            placeholder="例如：执子之手，与子偕老。欢迎各位客友亲临见证我们的幸福时刻..." 
          />
        </el-form-item>
        <el-form-item label="背景音乐URL">
          <el-input v-model="editForm.musicUrl" placeholder="例如：/uploads/music/wedding-march.mp3" />
        </el-form-item>

        <el-divider />
        <h3 class="section-title">🚦 状态公开控制</h3>
        <el-form-item label="是否对外公开">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="0" border>🔒 筹备中（仅管理员可见，来宾端拦截）</el-radio>
            <el-radio :value="1" border>🎉 已对外发布（来宾可通过短链接对号入座）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item style="margin-top: 40px;">
          <el-button type="primary" size="large" color="#ff4d4f" :loading="submitLoading" @click="submitUpdate">
            保存修改并应用
          </el-button>
          <el-button size="large" @click="router.push('/admin/dashboard')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEventDetail, updateEvent } from '@/api/adminEvent'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const eventId = Number(route.params.id)

const loading = ref(false)
const submitLoading = ref(false)

const editForm = reactive({
  groomName: '',
  brideName: '',
  slug: '',
  eventTime: '',
  location: '',
  greetingMessage: '',
  musicUrl: '',
  status: 0
})

// 加载原有的婚礼单场详情
const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getEventDetail(eventId)
    const d = res.data
    editForm.groomName = d.groomName || ''
    editForm.brideName = d.brideName || ''
    editForm.slug = d.slug || ''
    editForm.eventTime = d.eventTime || ''
    editForm.location = d.location || ''
    editForm.greetingMessage = d.greetingMessage || ''
    editForm.musicUrl = d.musicUrl || ''
    editForm.status = d.status
  } catch (err) {
    // 跨权 403 或者是数据不存在已被拦截器处理
    router.push('/admin/dashboard')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

// 提交局部修改
const submitUpdate = async () => {
  submitLoading.value = true
  try {
    // 整理出非空要修改的局部对象
    const patchData = {
      groomName: editForm.groomName || undefined,
      brideName: editForm.brideName || undefined,
      slug: editForm.slug.trim() || undefined,
      eventTime: editForm.eventTime || undefined,
      location: editForm.location || undefined,
      greetingMessage: editForm.greetingMessage || undefined,
      musicUrl: editForm.musicUrl || undefined,
      status: editForm.status
    }

    await updateEvent(eventId, patchData)
    ElMessage.success('🎉 婚礼档案及状态已成功同步更新！')
    router.push('/admin/dashboard')
  } catch (err) {
    // 拦截器自动弹出报错
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.event-edit-page { padding: 24px; max-width: 800px; margin: 0 auto; }
.edit-card { margin-top: 24px; padding: 12px; border-radius: 12px; }
.section-title { color: #8c1111; font-size: 16px; margin-bottom: 20px; font-weight: bold; }
.alert-tip { font-size: 12px; color: #e6a23c; margin-top: 4px; line-height: 1.4; }
</style>