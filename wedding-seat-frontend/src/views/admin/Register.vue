<template>
  <div class="admin-register-container">
    <div class="register-box">
      <!-- 左侧：保持一致的婚礼品牌展示区 -->
      <div class="register-left">
        <div class="brand-content">
          <h1 class="brand-title">✨ 喜缘 · 加入我们</h1>
          <p class="brand-subtitle">加入大管家团队，为新人打造完美的数字选座体验</p>
          <div class="brand-features">
            <div class="feature-item">🔐 工业级安全权限加密</div>
            <div class="feature-item">⚙️ 极简配置，一键生成会场</div>
            <div class="feature-item">📱 H5 来宾端无缝数据联动</div>
          </div>
        </div>
      </div>

      <!-- 右侧：注册表单区 -->
      <div class="register-right">
        <div class="form-header">
          <h2>创建管理员账号</h2>
          <p>请填写以下信息完成注册</p>
        </div>

        <el-form 
          ref="registerFormRef" 
          :model="registerForm" 
          :rules="registerRules" 
          label-position="top"
          size="large"
          @keyup.enter="handleRegisterSubmit"
        >
          <el-form-item label="用户名 (必填)" prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="3-20位字符，作为登录账号" 
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码 (必填)" prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="6-30位字符，请确保密码安全" 
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item label="昵称 (选填)" prop="nickname">
            <el-input 
              v-model="registerForm.nickname" 
              placeholder="不填默认与用户名一致" 
              prefix-icon="Postcard"
              clearable
            />
          </el-form-item>

          <el-form-item label="手机号 (选填)" prop="phone">
            <el-input 
              v-model="registerForm.phone" 
              placeholder="请输入11位手机号" 
              prefix-icon="Cellphone"
              clearable
            />
          </el-form-item>

          <div class="form-options">
            <span class="login-link" @click="goToLogin">已有账号？返回登录</span>
          </div>

          <el-form-item style="margin-top: 20px;">
            <el-button 
              type="primary" 
              class="register-btn" 
              :loading="loading" 
              @click="handleRegisterSubmit"
            >
              {{ loading ? '正在提交注册申请...' : '提交注册' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { adminRegister } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard, Cellphone } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

// 响应式表单数据
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: ''
})

// 严格对应接口文档的规则校验
const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须在 3 到 20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度必须在 6 到 30 个字符之间', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称过长，请控制在 20 个字符内', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号格式', trigger: 'blur' }
  ]
})

// 🚀 核心注册动作
const handleRegisterSubmit = async () => {
  if (!registerFormRef.value) return
  
  // 1. 前端表单校验
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    
    // 2. 依照文档的“选填处理逻辑”：如果不传 nickname，默认用 username 代替
    const submitData = {
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname.trim() || registerForm.username,
      // 如果手机号是空的，直接不传或者传 undefined
      phone: registerForm.phone.trim() || undefined
    }

    try {
      // 3. 发起注册 API 请求
      const res = await adminRegister(submitData)

      if (res.code === 200) {
        ElMessage.success({
          message: '🎉 注册成功！即将跳转至登录页面',
          duration: 1500
        })
        
        // 自动将刚刚注册成功的用户名缓存一下，登录页能自动回填，非常贴心
        localStorage.setItem('remembered_admin_username', submitData.username)
        
        // 延迟 1.5 秒跳转，给足用户看成功提示的时间
        setTimeout(() => {
          router.push('/admin/login')
        }, 1500)
      }
    } catch (err) {
      // 这里的 400 错误（如“该用户名已被注册”）已被 Axios 拦截器 adminHttp.ts 统一弹窗处理
      console.error('注册提交被阻断:', err)
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-register-container {
  width: 100vw;
  height: 100vh;
  background: #f0f2f5;
  background-image: radial-gradient(#ff4d4f 0.5px, transparent 0.5px), radial-gradient(#ff4d4f 0.5px, #f0f2f5 0.5px);
  background-size: 20px 20px;
  background-position: 0 0, 10px 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-box {
  width: 960px;
  height: 600px; /* 注册表单项较多，略微调高整体容器 */
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
}

/* 左侧红金配色高级氛围感 */
.register-left {
  width: 45%;
  background: linear-gradient(135deg, #8c1111 0%, #ff4d4f 100%);
  padding: 40px;
  display: flex;
  align-items: center;
  color: #ffffff;
  position: relative;
}
.register-left::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: radial-gradient(circle at 80% 20%, rgba(255,215,0,0.15) 0%, transparent 50%);
  pointer-events: none;
}

.brand-title {
  font-size: 28px;
  font-weight: bold;
  color: #ffd700;
  margin-bottom: 12px;
  letter-spacing: 1px;
}
.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 48px;
}
.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.feature-item {
  font-size: 15px;
  background: rgba(255, 255, 255, 0.1);
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* 右侧表单区 */
.register-right {
  width: 55%;
  padding: 36px 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 24px;
}
.form-header h2 {
  font-size: 24px;
  color: #1f1f1f;
  margin-bottom: 6px;
  font-weight: 600;
}
.form-header p {
  font-size: 14px;
  color: #8c8c8c;
}

.form-options {
  text-align: right;
  margin-top: -4px;
}
.login-link {
  font-size: 14px;
  color: #ff4d4f;
  cursor: pointer;
  transition: color 0.2s;
}
.login-link:hover {
  color: #cf1322;
  text-decoration: underline;
}

.register-btn {
  width: 100%;
  background: #ff4d4f !important;
  border-color: #ff4d4f !important;
  font-weight: bold;
  letter-spacing: 2px;
  transition: all 0.3s;
}
.register-btn:hover {
  background: #ff7875 !important;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}
</style>