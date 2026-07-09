import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/guest/home'
    },
    // 来宾端
    {
      path: '/guest/home',
      name: 'GuestHome',
      component: () => import('@/views/guest/Home.vue')
    },
    {
      path: '/guest/seat',
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