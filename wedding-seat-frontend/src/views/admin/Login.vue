<template>
  <div class="admin-login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="brand-content">
          <h1 class="brand-title">✨ 喜缘 · 婚礼选座系统</h1>
          <p class="brand-subtitle">数字化婚礼现场调度与座位管理后台</p>
          <div class="brand-features">
            <div class="feature-item">📊 实时席位锁座监控</div>
            <div class="feature-item">🗺️ 可视化宴会厅大地图编辑</div>
            <div class="feature-item">🧾 来宾名单动态导入</div>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="form-header">
          <h2>管理端登录</h2>
          <p>请输入您的管理员账号密码</p>
        </div>

        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="loginRules" 
          label-position="top"
          size="large"
          @keyup.enter="handleLoginSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入管理员用户名" 
              prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码" 
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
            <span class="register-link" @click="goToRegister">没有账号？去注册</span>
          </div>

          <el-form-item style="margin-top: 24px;">
            <el-button 
              type="primary" 
              class="login-btn" 
              :loading="loading" 
              @click="handleLoginSubmit"
            >
              {{ loading ? '正在验证安全认证...' : '步入控制台' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="test-account-tip">
          💡 <b>测试种子账号：</b><br/>
          用户名：<code>spruce</code> / 密码：<code>123456</code>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { adminLogin } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue' // 👈 记得确保安装了 @element-plus/icons-vue
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const adminStore = useAdminStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(true)

// 表单响应式数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 严格的基础前端校验规则
const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请填写管理员账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ]
})

// 页面加载时自动回填记住的账号
onMounted(() => {
  const savedUser = localStorage.getItem('remembered_admin_username')
  if (savedUser) {
    loginForm.username = savedUser
  }
})

// 🚀 核心登录动作
const handleLoginSubmit = async () => {
  if (!loginFormRef.value) return
  
  // 1. Element 表单预校验
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      // 2. 发起登录 HTTP 请求
      const res = await adminLogin({
        username: loginForm.username,
        password: loginForm.password
      })

      // 3. 严格法则一：必须先在持久化 Store 里把 token 和明细焊死！
      adminStore.setLoginInfo(res.data.token, {
        adminId: res.data.adminId,
        username: res.data.username,
        nickname: res.data.nickname
      })

      // 处理“记住账号”功能
      if (rememberMe.value) {
        localStorage.setItem('remembered_admin_username', loginForm.username)
      } else {
        localStorage.removeItem('remembered_admin_username')
      }

      ElMessage.success({
        message: `🎉 登录成功！欢迎回来，新人的大管家 ${res.data.nickname}`,
        type: 'success',
        duration: 2000
      })

      // 4. 严格法则二：万事俱备，昂首阔步跳转去管理端大盘后台
      router.push('/admin/dashboard')

    } catch (err) {
      // 错误已由请求拦截器 adminHttp.ts 自动弹出 ElMessage，这里无需二次弹窗
      console.error('登录认证流程被截断:', err)
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/admin/register')
}
</script>

<style scoped>
.admin-login-container {
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

.login-box {
  width: 960px;
  height: 560px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
}

/* 左侧红金配色高级中国风喜庆氛围感 */
.login-left {
  width: 45%;
  background: linear-gradient(135deg, #8c1111 0%, #ff4d4f 100%);
  padding: 40px;
  display: flex;
  align-items: center;
  color: #ffffff;
  position: relative;
}
.login-left::before {
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

/* 右侧现代白净纯粹表单区 */
.login-right {
  width: 55%;
  padding: 48px 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.form-header {
  margin-bottom: 32px;
}
.form-header h2 {
  font-size: 24px;
  color: #1f1f1f;
  margin-bottom: 8px;
  font-weight: 600;
}
.form-header p {
  font-size: 14px;
  color: #8c8c8c;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -4px;
}
.register-link {
  font-size: 14px;
  color: #ff4d4f;
  cursor: pointer;
  transition: color 0.2s;
}
.register-link:hover {
  color: #cf1322;
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  background: #ff4d4f !important;
  border-color: #ff4d4f !important;
  font-weight: bold;
  letter-spacing: 2px;
  transition: all 0.3s;
}
.login-btn:hover {
  background: #ff7875 !important;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
}

.test-account-tip {
  margin-top: 32px;
  padding: 12px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 13px;
  color: #d46b08;
  line-height: 1.5;
}
.test-account-tip code {
  background: rgba(0,0,0,0.04);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-weight: bold;
  color: #531dab;
}
</style>