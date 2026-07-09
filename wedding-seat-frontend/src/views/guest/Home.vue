<template>
  <div class="guest-home">
    <audio v-if="eventData.musicUrl" :src="eventData.musicUrl" autoplay loop></audio>

    <van-swipe class="banner-swipe" :autoplay="3500" indicator-color="#ff4d4f">
      <van-swipe-item v-for="photo in photoList" :key="photo.id">
        <div class="swipe-card">
          <img :src="photo.url" class="swipe-img" alt="结婚照" />
        </div>
      </van-swipe-item>
    </van-swipe>

    <div class="welcome-card">
      <h1 class="couple-names">{{ eventData.groomName }} ❤️ {{ eventData.brideName }}</h1>
      <p class="wedding-tag">我们要结婚啦</p>
      
      <div class="greetings">
        <van-icon name="comment-o" class="quote-icon" />
        <p class="text">{{ eventData.greetingMessage || '欢迎光临我们的婚礼见证温馨时刻！' }}</p>
      </div>

      <van-cell-group inset class="info-group">
        <van-cell title="婚礼时间" :value="eventData.eventTime" icon="clock-o" />
        <van-cell title="宴席地点" :value="eventData.location" icon="location-o" label="点击复制地址导航" @click="copyLocation" />
      </van-cell-group>
    </div>

    <div class="bottom-action">
      <van-button type="primary" block round color="linear-gradient(to right, #ff7875, #ff4d4f)" @click="goToRegister">
        开启喜宴 · 登记选座
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'

const router = useRouter()

// 模拟后端拉取的数据
const eventData = ref({
  id: 1,
  groomName: '新郎姓名',
  brideName: '新娘姓名',
  greetingMessage: '执子之手，与子偕老。期待在最美好的日子，与你分享这份幸福。',
  eventTime: '2026-10-18 11:58',
  location: '喜来登大酒店 3楼 宴会厅',
  musicUrl: ''
})

const photoList = ref([
  { id: 1, url: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-1.jpeg' },
  { id: 2, url: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-2.jpeg' }
])

const copyLocation = () => {
  navigator.clipboard.writeText(eventData.value.location)
  showToast('地址已复制，可前往地图导航')
}

const goToRegister = () => {
  router.push('/guest/seat')
}
</script>

<style scoped>
.guest-home {
  background-color: #fffbfa;
  min-height: 100vh;
  padding-bottom: 80px;
}
.banner-swipe {
  height: 60vh;
}
.swipe-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.welcome-card {
  margin: -30px 16px 0 16px;
  background: white;
  border-radius: 16px;
  padding: 24px 16px;
  box-shadow: 0 4px 16px rgba(255, 77, 79, 0.08);
  position: relative;
  z-index: 10;
}
.couple-names {
  text-align: center;
  color: #d32f2f;
  font-size: 24px;
  margin: 0;
}
.wedding-tag {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
.greetings {
  background: #fff5f5;
  padding: 16px;
  border-radius: 8px;
  margin: 16px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}
.info-group {
  margin: 0 !important;
}
.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px;
  background: white;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
}
</style>