import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/admin/dashboard' 
    },
    // 来宾端：动态路由，修正路径为标准的 @/views/...
    {
      path: '/guest/event/:slug/home',
      name: 'GuestHome',
      component: () => import('@/views/guest/Home.vue') 
    },
    {
      path: '/guest/event/:slug/seat',
      name: 'GuestSeatSelect',
      component: () => import('@/views/guest/SeatSelect.vue') 
    },
    // 管理端
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/Dashboard.vue') 
    },
    {
      path: '/admin/settings',
      name: 'AdminSettings',
      component: () => import('@/views/admin/Settings.vue') 
    }
  ]
})

export default router