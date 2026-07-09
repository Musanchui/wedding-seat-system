import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Vant from 'vant'
import App from './App.vue'
import router from './router'

import 'element-plus/dist/index.css'
import 'vant/lib/index.css'
import './assets/main.css' // 可以在这里写点全局重置样式

const app = createApp(App)

// 注册 Element Plus 所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(Vant)

app.mount('#app')