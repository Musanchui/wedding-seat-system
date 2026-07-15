<template>
  <div class="guest-list-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="router.push('/admin/dashboard')">返回控制台</el-button>
      <h2>宾客名单 & 邀请函二维码</h2>
    </div>

    <div class="content-body">
      <!-- 邀请函二维码 -->
      <div class="qrcode-card">
        <div class="card-title">邀请函二维码</div>
        <div class="qrcode-preview" v-loading="qrLoading">
          <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="邀请函二维码" />
          <el-empty v-else-if="!qrLoading" description="点击下方按钮生成" :image-size="60" />
        </div>
        <el-button type="primary" color="#ff4d4f" :loading="qrLoading" style="width: 100%" @click="handleGenerateQr">
          {{ qrCodeUrl ? '重新生成' : '生成二维码' }}
        </el-button>
        <el-button v-if="qrCodeUrl" style="width: 100%; margin-top: 8px; margin-left: 0" @click="handleDownloadQr">
          下载图片
        </el-button>
        <div class="card-tip">如果这场婚礼已经上传过照片，二维码会自动嵌入第一张照片，更美观</div>
      </div>

      <!-- 宾客名单 -->
      <div class="guest-table-card">
        <div class="card-title-row">
          <div class="card-title">宾客名单（共 {{ guestList.length }} 组，合计 {{ totalSeatCount }} 人）</div>
          <el-button type="primary" plain :loading="exporting" @click="handleExport">导出Excel</el-button>
        </div>

        <el-table :data="guestList" v-loading="tableLoading" style="width: 100%">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="displayName" label="姓名（含随行）" min-width="160" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="category" label="身份" width="120">
            <template #default="{ row }">{{ row.category || '—' }}</template>
          </el-table-column>
          <el-table-column prop="seatsDesc" label="座位" min-width="180">
            <template #default="{ row }">
              <span v-if="row.seatsDesc">{{ row.seatsDesc }}</span>
              <el-tag v-else type="info" size="small">尚未选座</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registerTime" label="登记时间" width="160">
            <template #default="{ row }">{{ formatTime(row.registerTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getGuestList, exportGuestListExcel, fetchInvitationQrCode, type AdminGuestListItem } from '@/api/adminGuest'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const eventId = Number(route.params.id)

const tableLoading = ref(false)
const exporting = ref(false)
const qrLoading = ref(false)
const qrCodeUrl = ref('')
const guestList = ref<AdminGuestListItem[]>([])

const totalSeatCount = computed(() => guestList.value.reduce((sum, g) => sum + g.seatCount, 0))

const loadGuestList = async () => {
  tableLoading.value = true
  try {
    const res = await getGuestList(eventId)
    guestList.value = res.data
  } catch (err: any) {
    ElMessage.error(err?.message || '加载失败')
  } finally {
    tableLoading.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    await exportGuestListExcel(eventId, adminStore.token)
    ElMessage.success('导出成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const handleGenerateQr = async () => {
  qrLoading.value = true
  try {
    if (qrCodeUrl.value) {
      window.URL.revokeObjectURL(qrCodeUrl.value)
    }
    qrCodeUrl.value = await fetchInvitationQrCode(eventId, adminStore.token)
  } catch (err: any) {
    ElMessage.error(err?.message || '生成失败')
  } finally {
    qrLoading.value = false
  }
}

const handleDownloadQr = () => {
  const a = document.createElement('a')
  a.href = qrCodeUrl.value
  a.download = '邀请函二维码.png'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

const formatTime = (timeStr: string) => timeStr.replace('T', ' ')

onMounted(() => {
  loadGuestList()
})
</script>

<style scoped>
.guest-list-page { min-height: 100vh; background: #f5f7f9; }
.page-header { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: white; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 17px; }

.content-body { display: flex; gap: 24px; padding: 24px; max-width: 1200px; margin: 0 auto; align-items: flex-start; }

.qrcode-card { width: 280px; background: white; border-radius: 12px; padding: 20px; flex-shrink: 0; }
.card-title { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 14px; }
.qrcode-preview { width: 100%; aspect-ratio: 3/4; background: #fafafa; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-bottom: 14px; overflow: hidden; }
.qrcode-preview img { width: 100%; height: 100%; object-fit: contain; }
.card-tip { font-size: 12px; color: #999; margin-top: 10px; line-height: 1.5; }

.guest-table-card { flex: 1; background: white; border-radius: 12px; padding: 20px; }
.card-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
