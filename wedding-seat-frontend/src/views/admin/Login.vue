<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1>婚礼选座系统</h1>
        <p>管理端登录</p>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="handleLogin" />
        </el-form-item>
      </el-form>

      <el-button type="primary" color="#ff4d4f" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
        登录
      </el-button>

      <div class="auth-footer">
        还没有账号？<router-link to="/admin/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/adminAuth'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await adminLogin({ username: form.username.trim(), password: form.password })
    adminStore.setLoginInfo(res.data)
    ElMessage.success('登录成功')
    router.push({ name: 'AdminDashboard' })
  } catch (err: any) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f0 100%);
}
.auth-card {
  width: 380px;
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(255, 77, 79, 0.1);
}
.auth-header { text-align: center; margin-bottom: 32px; }
.auth-header h1 { font-size: 22px; color: #d32f2f; margin: 0 0 8px 0; }
.auth-header p { font-size: 14px; color: #999; margin: 0; }
.auth-footer { text-align: center; margin-top: 20px; font-size: 14px; color: #666; }
.auth-footer a { color: #ff4d4f; text-decoration: none; font-weight: 600; }
</style>
