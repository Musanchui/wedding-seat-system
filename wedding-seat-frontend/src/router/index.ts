import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/admin/login' },

    // ================== 管理端 (PC) ==================
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
      // 数据库自增id，跟 handleEditEvent(event.id) 传的值一致
      path: '/admin/event/edit/:id',
      name: 'AdminEventEdit',
      component: () => import('@/views/admin/EventEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      // 桌位大地图管理接口用的是数据库id(eventId)，不是slug，这里统一用 :id，
      // Dashboard.vue 跳转时要传 event.id（不是event.slug），下面Dashboard重写时会保持一致
      path: '/admin/event/seats/:id',
      name: 'AdminSeatLayout',
      component: () => import('@/views/admin/SeatLayout.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/event/guests/:id',
      name: 'AdminGuestList',
      component: () => import('@/views/admin/GuestList.vue'),
      meta: { requiresAuth: true }
    },

    // ================== 来宾端 (H5) ==================
    {
      path: '/guest/event/:slug/home',
      name: 'GuestHome',
      component: () => import('@/views/guest/Home.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/guest/event/:slug/seat',
      name: 'GuestSeatSelect',
      component: () => import('@/views/guest/SeatSelect.vue'),
      meta: { requiresAuth: false }
    }
  ]
})

router.beforeEach((to) => {
  const adminStore = useAdminStore()
  const isAuthenticated = !!adminStore.token

  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'AdminLogin' }
  }
  if ((to.name === 'AdminLogin' || to.name === 'AdminRegister') && isAuthenticated) {
    return { name: 'AdminDashboard' }
  }
})

export default router
