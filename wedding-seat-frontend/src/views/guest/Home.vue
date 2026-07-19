<template>
  <div class="guest-home">
    <van-swipe class="banner-swipe" :autoplay="3500" indicator-color="#ff4d4f">
      <van-swipe-item v-for="photo in photoList" :key="photo.id">
        <div class="swipe-card">
          <img :src="photo.url" class="swipe-img" alt="婚礼照" />
        </div>
      </van-swipe-item>
    </van-swipe>

    <div class="welcome-card">
      <h1 class="couple-names" @click="toggleMusic">
        {{ eventData.groomName }} ❤️ {{ eventData.brideName }}
        <span class="music-note" :class="{ 'is-playing': isMusicPlaying }" v-if="hasMusic">
          <van-icon name="music-o" />
        </span>
      </h1>
      <p class="wedding-tag">我们要结婚啦</p>
      
      <div class="greetings">
        <van-icon name="comment-o" class="quote-icon" />
        <p class="text">{{ eventData.greetingMessage || '欢迎光临我们的婚礼，见证温馨时刻！' }}</p>
      </div>

      <van-cell-group inset class="info-group">
        <van-cell title="婚礼时间" :value="formatTime(eventData.eventTime)" icon="clock-o" />
        <van-cell title="宴席地点" :value="eventData.location" icon="location-o" label="点击复制地址" @click="copyLocation" />
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
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getWeddingEvent, getWeddingPhotos } from '@/api/guest'
import { playMusic, toggleMusic, isMusicPlaying, hasMusic } from '@/utils/musicPlayer'

const router = useRouter()
const route = useRoute()
const slug = route.params.slug as string

const eventData = ref({
  id: 0,
  groomName: '加载中...',
  brideName: '',
  eventTime: '',
  location: '',
  greetingMessage: '',
  musicUrl: ''
})

const photoList = ref<any[]>([])

// 处理后端的 LocalDateTime 字符串 (如 2026-08-15T18:00:00) 转换为友好展示
const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(async () => {
  try {
    // 2.1 获取婚礼信息
    const eventRes = await getWeddingEvent(slug)
    if (eventRes.code === 200) {
      eventData.value = eventRes.data
      if (eventData.value.musicUrl) {
        // 音乐播放器是全局单例，这里只是"告诉"它要播哪首、开始播；
        // 之后不管跳转到选座页还是别的来宾端页面，音乐都不会中断
        setTimeout(() => { playMusic(eventData.value.musicUrl) }, 500)
      }
    } else {
      showToast(eventRes.message)
    }

    // 2.2 获取照片墙
    const photoRes = await getWeddingPhotos(slug)
    if (photoRes.code === 200) {
      photoList.value = photoRes.data
    }
  } catch (error) {
    showToast('服务器异常，请检查网络')
  }
})

const copyLocation = () => {
  if (!eventData.value.location) return
  navigator.clipboard.writeText(eventData.value.location)
  showToast('地址已复制，可打开地图直接导航')
}

const goToRegister = () => {
  router.push(`/guest/event/${slug}/seat`)
}
</script>

<style scoped>
.guest-home { background-color: #fffbfa; min-height: 100vh; padding-bottom: 80px; }
.banner-swipe { height: 55vh; }
.swipe-img { width: 100%; height: 100%; object-fit: cover; }
.welcome-card { margin: -30px 16px 0 16px; background: white; border-radius: 16px; padding: 24px 16px; box-shadow: 0 4px 16px rgba(255, 77, 79, 0.08); position: relative; z-index: 10; }
.couple-names { text-align: center; color: #d32f2f; font-size: 24px; margin: 0; position: relative; }
.music-note { font-size: 16px; margin-left: 8px; color: #999; display: inline-block; }
.music-note.is-playing { animation: spin 3s linear infinite; color: #ff4d4f; }
.wedding-tag { text-align: center; color: #999; font-size: 14px; margin-top: 4px; }
.greetings { background: #fff5f5; padding: 14px; border-radius: 8px; margin: 16px 0; color: #666; font-size: 14px; line-height: 1.6; }
.info-group { margin: 0 !important; }
.bottom-action { position: fixed; bottom: 0; left: 0; right: 0; padding: 12px 24px; background: white; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 99; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>