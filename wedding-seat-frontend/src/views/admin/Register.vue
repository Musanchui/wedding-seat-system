<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1>婚礼选座系统</h1>
        <p>创建管理员账号</p>
      </div>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名（3-20位）">
          <el-input v-model="form.username" placeholder="用于登录" size="large" />
        </el-form-item>
        <el-form-item label="密码（6-30位）">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-form-item label="昵称（选填）">
          <el-input v-model="form.nickname" placeholder="不填则默认使用用户名" size="large" />
        </el-form-item>
        <el-form-item label="手机号（选填）">
          <el-input v-model="form.phone" placeholder="用于找回密码" size="large" />
        </el-form-item>
      </el-form>

      <el-button type="primary" color="#ff4d4f" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
        注册
      </el-button>

      <div class="auth-footer">
        已经有账号？<router-link to="/admin/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminRegister } from '@/api/adminAuth'

const router = useRouter()
const loading = ref(false)

const form = reactive({ username: '', password: '', nickname: '', phone: '' })

const handleRegister = async () => {
  if (form.username.trim().length < 3) {
    ElMessage.warning('用户名至少3位')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  loading.value = true
  try {
    await adminRegister({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'AdminLogin' })
  } catch (err: any) {
    ElMessage.error(err?.message || '注册失败')
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
