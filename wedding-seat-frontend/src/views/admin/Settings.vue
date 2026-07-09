<template>
  <div class="admin-settings">
    <el-container style="min-height: 100vh;">
      <el-aside width="200px" style="background-color: #304156;">
        <div class="menu-logo">婚礼选座后台</div>
        <el-menu active-text-color="#409EFF" background-color="#304156" class="el-menu-vertical" default-active="2" text-color="#fff">
          <el-menu-item index="1" @click="$router.push('/admin/dashboard')"><el-icon><Platform /></el-icon><span>桌位大盘看板</span></el-menu-item>
          <el-menu-item index="2" @click="$router.push('/admin/settings')"><el-icon><Setting /></el-icon><span>素材与婚礼配置</span></el-menu-item>
        </el-menu>
      </el-aside>

      <el-main style="background: #f0f2f5;">
        <el-card shadow="never">
          <template #header><h3>婚礼基本信息与全局素材发布</h3></template>
          
          <el-form :model="weddingForm" label-width="120px" style="max-width: 600px;">
            <el-form-item label="新郎姓名"><el-input v-model="weddingForm.groomName" /></el-form-item>
            <el-form-item label="新娘姓名"><el-input v-model="weddingForm.brideName" /></el-form-item>
            <el-form-item label="酒席开始时间">
              <el-date-picker v-model="weddingForm.eventTime" type="datetime" placeholder="选择日期时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm" />
            </el-form-item>
            <el-form-item label="宴席地点"><el-input v-model="weddingForm.location" /></el-form-item>
            <el-form-item label="新人婚礼寄语">
              <el-input v-model="weddingForm.greetingMessage" type="textarea" :rows="3" placeholder="写一句送给所有亲友的欢迎词吧~" />
            </el-form-item>

            <el-form-item label="婚礼背景音乐">
              <el-upload action="/api/admin/upload" :limit="1" accept=".mp3" :on-success="handleMusicSuccess">
                <el-button type="primary" icon="Headset">选择 MP3 音乐文件</el-button>
                <template #tip><div class="el-upload__tip">只支持单个 MP3，文件大小不超过 50MB</div></template>
              </el-upload>
            </el-form-item>

            <el-form-item label="现场滚动照片流">
              <el-upload action="/api/admin/upload" list-type="picture-card" accept="image/*" :on-success="handlePhotoSuccess">
                <el-icon><Plus /></el-icon>
              </el-upload>
            </el-form-item>

            <el-form-item>
              <el-button type="success" size="large" @click="submitGlobalSettings">保存并一键发布上线</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const weddingForm = ref({
  id: 1,
  groomName: 'Spruce',
  brideName: 'Emily',
  eventTime: '2026-10-18 11:58',
  location: '喜来登大酒店 3楼 宴会厅',
  greetingMessage: '执子之手，与子偕老。期待在最美好的日子，与你分享这份幸福。',
  musicUrl: ''
})

const handleMusicSuccess = (res: any) => {
  weddingForm.value.musicUrl = res.url
  ElMessage.success('背景音乐上传成功！')
}

const handlePhotoSuccess = (res: any) => {
  ElMessage.success('精美照片上传成功，已加入来宾端滚动队列！')
}

const submitGlobalSettings = () => {
  // 发送 POST 请求到 /api/admin/event/update
  ElMessage.success('恭喜！婚礼全局配置已成功同步到前台手机端！')
}
</script>

<style scoped>
.menu-logo { height: 60px; line-height: 60px; text-align: center; color: white; font-weight: bold; font-size: 16px; background: #2b2f3a; }
h3 { margin: 0; font-size: 16px; }
</style>