import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ================== 1. 公共与重定向 ==================
    {
      path: '/',
      redirect: '/admin/login'
    },

    // ================== 2. 管理端 (PC) 路由 ==================
    {
      path: '/admin/login',
      name: 'AdminLogin',
      component: () => import('@/views/admin/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/admin/register',
      name: 'AdminRegister',
      component: () => import('@/views/admin/Register.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/event/edit/:id',
      name: 'AdminEventEdit',
      component: () => import('@/views/admin/EventEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/event/seats/:slug',
      name: 'AdminSeatLayout',
      component: () => import('@/views/admin/SeatLayout.vue'), 
      meta: { requiresAuth: true }
    },

    // ================== 3. 来宾端 (H5) 路由 (把缺的补回来！) ==================
    {
      // 来宾选座登记页面
      path: '/guest/event/:slug/seat',
      name: 'GuestSeatSelect',
      component: () => import('@/views/guest/SeatSelect.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/guest/event/:slug/home',
      name: 'GuestHome',
      component: () => import('@/views/guest/Home.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

// ================== 4. 全局路由守卫（采用最新的 Return 语法，干掉警告） ==================
router.beforeEach((to, from) => {
  const adminStore = useAdminStore()
  const isAuthenticated = !!adminStore.token

  // 场景 A：去管理后台，但没登录 -> 拦截去登录页
  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'AdminLogin' }
  }

  // 场景 B：去登录/注册页，但已经登录过了 -> 弹飞去后台大盘
  if ((to.name === 'AdminLogin' || to.name === 'AdminRegister') && isAuthenticated) {
    return { name: 'AdminDashboard' }
  }

  // 场景 C：其他情况（包括来宾端的 H5 页面），直接放行 (不写 return 默认就是放行)
})

export default router

