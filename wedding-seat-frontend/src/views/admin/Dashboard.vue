<template>
  <div class="admin-dashboard">
    <div class="dashboard-header">
      <div class="header-welcome">
        <h2>欢迎回来，{{ adminStore.nickname || adminStore.username }}</h2>
        <p>管理你创建的婚礼，配置桌位、发布来宾端页面。</p>
      </div>
      <div class="header-actions">
        <el-button text @click="handleLogout">退出登录</el-button>
        <el-button type="primary" size="large" color="#ff4d4f" @click="openCreateDialog">
          创建新婚礼
        </el-button>
      </div>
    </div>

    <el-skeleton :loading="pageLoading" animated :count="3">
      <template #template>
        <div style="padding: 20px; display: flex; gap: 20px;">
          <el-skeleton-item variant="rect" style="width: 300px; height: 180px" v-for="i in 3" :key="i" />
        </div>
      </template>

      <template #default>
        <div v-if="eventList.length === 0" class="empty-state">
          <el-empty description="暂无您创建的婚礼，点击右上角创建一个吧" />
        </div>

        <div v-else class="event-grid">
          <el-card v-for="event in eventList" :key="event.id" class="event-card" shadow="hover">
            <div class="card-status-tag">
              <el-tag :type="event.status === 1 ? 'success' : 'info'" effect="dark">
                {{ event.status === 1 ? '已发布' : '筹备中' }}
              </el-tag>
            </div>

            <div class="card-body">
              <h3 class="wedding-names">{{ event.groomName || '新郎' }} 🤍 {{ event.brideName || '新娘' }}</h3>
              <p class="wedding-slug">
                <el-icon><Link /></el-icon> 访问标识：<code>{{ event.slug }}</code>
              </p>
              <p class="wedding-time">
                <el-icon><Calendar /></el-icon> 婚礼时间：{{ event.eventTime ? formatTime(event.eventTime) : '尚未设置' }}
              </p>
            </div>

            <div class="card-actions">
              <el-button size="small" type="primary" plain @click="handleEditEvent(event.id)">编辑详情</el-button>
              <el-button size="small" type="success" plain @click="handleManageSeats(event.id)">桌位大地图</el-button>
              <el-button size="small" plain @click="handleGuestList(event.id)">宾客名单</el-button>
              <el-button
                size="small"
                :type="event.status === 1 ? 'warning' : 'danger'"
                :loading="statusLoading === event.id"
                @click="toggleEventStatus(event)"
              >
                {{ event.status === 1 ? '下线' : '开放访问' }}
              </el-button>
            </div>
          </el-card>
        </div>
      </template>
    </el-skeleton>

    <el-dialog v-model="createVisible" title="创建新婚礼" width="480px" destroy-on-close>
      <el-form :model="createForm" label-position="top">
        <el-form-item label="新郎姓名（选填）">
          <el-input v-model="createForm.groomName" placeholder="例如：张三" />
        </el-form-item>
        <el-form-item label="新娘姓名（选填）">
          <el-input v-model="createForm.brideName" placeholder="例如：李四" />
        </el-form-item>
        <el-form-item label="访问标识 slug（选填）">
          <el-input v-model="createForm.slug" placeholder="例：zhang-li-0815，不填系统将随机生成" />
          <div class="form-tip">只能包含小写字母、数字和短横线，且全站唯一</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" color="#ff4d4f" :loading="submitLoading" @click="submitCreateEvent">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { getMyEventList, createEvent, updateEvent, type EventListItem } from '@/api/adminEvent'
import { ElMessage } from 'element-plus'
import { Calendar, Link } from '@element-plus/icons-vue'

const router = useRouter()
const adminStore = useAdminStore()

const pageLoading = ref(false)
const submitLoading = ref(false)
const createVisible = ref(false)
const statusLoading = ref<number | null>(null)
const eventList = ref<EventListItem[]>([])

const createForm = reactive({ groomName: '', brideName: '', slug: '' })

const loadEventList = async () => {
  pageLoading.value = true
  try {
    const res = await getMyEventList()
    eventList.value = res.data
  } catch (err) {
    // adminHttp拦截器已经统一处理了错误提示（401会自动跳转登录页），这里不需要重复提示
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  loadEventList()
})

const openCreateDialog = () => {
  createForm.groomName = ''
  createForm.brideName = ''
  createForm.slug = ''
  createVisible.value = true
}

const submitCreateEvent = async () => {
  submitLoading.value = true
  try {
    const res = await createEvent({
      groomName: createForm.groomName || undefined,
      brideName: createForm.brideName || undefined,
      slug: createForm.slug.trim() || undefined
    })
    ElMessage.success('婚礼创建成功')
    createVisible.value = false
    router.push(`/admin/event/edit/${res.data.id}`)
  } catch (err: any) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const toggleEventStatus = async (event: EventListItem) => {
  statusLoading.value = event.id
  const targetStatus = event.status === 1 ? 0 : 1
  try {
    await updateEvent(event.id, { status: targetStatus })
    event.status = targetStatus
    ElMessage.success(targetStatus === 1 ? '婚礼已发布，来宾端可以访问了' : '婚礼已下线，转为筹备状态')
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    statusLoading.value = null
  }
}

const handleEditEvent = (id: number) => {
  router.push(`/admin/event/edit/${id}`)
}

// 注意：这里传的是 event.id（数字），不是 event.slug —— 桌位大地图相关的管理端接口
// 都是按数字eventId查询的，跟来宾端用slug访问是两套体系，不要混用
const handleManageSeats = (id: number) => {
  router.push(`/admin/event/seats/${id}`)
}

const handleGuestList = (id: number) => {
  router.push(`/admin/event/guests/${id}`)
}

const handleLogout = () => {
  adminStore.logout()
  router.push({ name: 'AdminLogin' })
}

const formatTime = (timeStr: string) => timeStr.replace('T', ' ')
</script>

<style scoped>
.admin-dashboard { padding: 24px; max-width: 1200px; margin: 0 auto; }
.dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; border-bottom: 1px solid #e8e8e8; padding-bottom: 20px; }
.header-welcome h2 { margin: 0 0 8px 0; color: #1f1f1f; }
.header-welcome p { margin: 0; color: #8c8c8c; font-size: 14px; }
.header-actions { display: flex; align-items: center; gap: 12px; }

.event-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 24px; }
.event-card { border-radius: 12px; position: relative; overflow: visible; }
.card-status-tag { position: absolute; top: -10px; right: 16px; z-index: 10; }

.wedding-names { font-size: 18px; color: #1a1a1a; margin: 8px 0 16px 0; }
.card-body p { font-size: 13px; color: #666; display: flex; align-items: center; gap: 6px; margin: 8px 0; }
.card-body code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-family: monospace; color: #ff4d4f; font-weight: bold; }

.card-actions { border-top: 1px solid #f0f0f0; margin-top: 20px; padding-top: 16px; display: flex; justify-content: space-between; gap: 8px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }
.empty-state { padding: 60px 0; }
</style>
