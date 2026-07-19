import { ref } from 'vue'

/**
 * 全局背景音乐播放器：整个来宾端(H5)共用同一个 Audio 实例。
 * 这个模块只会被JS加载一次，里面的 audioInstance 在整个应用生命周期里只创建一次，
 * 不属于任何Vue组件的模板/DOM树，所以切换路由(Home -> SeatSelect)组件被销毁重建时，
 * 这个音频实例完全不受影响，播放状态自然就能跨页面保持。
 */
let audioInstance: HTMLAudioElement | null = null
let currentSrc = ''

export const isMusicPlaying = ref(false)
export const hasMusic = ref(false)

const ensureAudio = () => {
  if (!audioInstance) {
    audioInstance = new Audio()
    audioInstance.loop = true
    audioInstance.addEventListener('play', () => { isMusicPlaying.value = true })
    audioInstance.addEventListener('pause', () => { isMusicPlaying.value = false })
  }
  return audioInstance
}

/**
 * 设置音乐URL并尝试播放。如果传入的url跟当前正在播的是同一个，不会重新加载/重新播放
 * (避免同一首歌从Home页跳到选座页时，因为组件重新mount又调用一次setSrc导致重新从头播)
 */
export const playMusic = (url: string) => {
  if (!url) return
  hasMusic.value = true
  const audio = ensureAudio()

  if (currentSrc === url && !audio.paused) {
    return // 已经在播同一首，不重复处理
  }
  if (currentSrc !== url) {
    currentSrc = url
    audio.src = url
  }
  audio.play().catch(() => {
    // 浏览器自动播放策略拦截，静默失败即可，等用户点一下音符图标手动触发
  })
}

export const toggleMusic = () => {
  const audio = ensureAudio()
  if (audio.paused) {
    audio.play().catch(() => {})
  } else {
    audio.pause()
  }
}