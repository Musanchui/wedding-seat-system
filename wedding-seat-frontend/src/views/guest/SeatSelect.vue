<template>
  <div class="seat-select-page">
    <van-nav-bar title="来宾登记与选座" left-arrow @click-left="$router.back()" />

    <div v-if="!guestStore.guestId" class="form-container">
      <div class="form-title">请留下您的赴宴信息</div>
      <van-form @submit="onSubmitRegister">
        <van-cell-group inset>
          <van-field v-model="form.name" name="name" label="姓名" placeholder="请输入您的真实姓名" :rules="[{ required: true, message: '请填写姓名' }]" />
          <van-field v-model="form.phone" name="phone" type="tel" label="手机号" placeholder="请输入手机号" :rules="[{ required: true, message: '请填写手机号' }]" />
          <van-field v-model="form.category" is-link readonly name="category" label="您的身份" placeholder="点击选择与新人的关系" @click="showPicker = true" />
        </van-cell-group>
        
        <div style="margin: 32px 16px;">
          <van-button round block type="primary" color="#ff4d4f" native-type="submit" :loading="loading">提交并选择座位</van-button>
        </div>
      </van-form>
      
      <van-popup v-model:show="showPicker" position="bottom">
        <van-picker :columns="categories" @confirm="onConfirmCategory" @cancel="showPicker = false" />
      </van-popup>
    </div>

    <div v-else class="map-container">
      <div class="welcome-user">尊敬的 <b>{{ guestStore.guestName }}</b>，请点击下方【宴会厅大地图】中的桌子切换查看：</div>

      <!--
        修复点1：不再用 v-if/v-else 整个替换 van-notice-bar（那样每次切换是"销毁重建"整个组件，
        两段文案长度差异很大，Vant内部重新计算跑马灯宽度时容易造成页面抖动）。
        现在改成同一个组件实例，只是颜色/图标/文案跟着当前状态变化，组件本身不会被销毁重建。
      -->
      <van-notice-bar 
        :color="isViewingRecommended ? '#ed6a0c' : '#1890ff'"
        :background="isViewingRecommended ? '#fffbe6' : '#e6f7ff'"
        :left-icon="isViewingRecommended ? 'star-o' : 'location-o'"
      >
        {{ noticeText }}
      </van-notice-bar>

      <div class="venue-canvas-wrapper" :style="{ '--canvas-w': layoutData.canvasWidth, '--canvas-h': layoutData.canvasHeight }">
        <div class="canvas-title">✨ 喜宴会大厅全景导航图 (点击桌位可切换) ✨</div>
        
        <div class="interactive-area">
          <svg 
            :viewBox="`0 0 ${layoutData.canvasWidth} ${layoutData.canvasHeight}`" 
            class="venue-svg"
            xmlns="http://www.w3.org/2000/svg"
            style="pointer-events: none;"
          >
            <g v-for="el in layoutData.elements" :key="'el-' + el.id" :transform="`translate(${el.posX}, ${el.posY}) rotate(${el.rotation})`">
              <rect v-if="el.type === 'stage'" :width="el.width" :height="el.height" rx="8" class="svg-stage" />
              <rect v-else-if="el.type === 'screen'" :width="el.width" :height="el.height" rx="4" class="svg-screen" />
              <rect v-else :width="el.width" :height="el.height" rx="6" class="svg-door" />
              <text :x="el.width / 2" :y="el.height / 2 + 5" text-anchor="middle" class="svg-el-text">{{ el.label }}</text>
            </g>

            <g 
              v-for="table in layoutData.tables" 
              :key="'table-geo-' + table.id" 
              :transform="`translate(${table.posX}, ${table.posY})`"
              class="svg-table-group"
              :class="{ 'is-active': currentTableId === table.id, 'is-recommend': recommendTableId === table.id }"
            >
              <circle cx="0" cy="0" r="45" class="svg-table-circle" />
              <circle cx="0" cy="0" r="35" class="svg-table-inner" />
              <text x="0" y="-5" text-anchor="middle" class="svg-table-text">{{ table.tableNo }}号桌</text>
              <text x="0" y="18" text-anchor="middle" class="svg-table-sub">余 {{ table.availableSeatsCount }} 席</text>
              <path v-if="recommendTableId === table.id" d="M 0,-58 L 3,-48 L 13,-48 L 5,-42 L 8,-32 L 0,-38 L -8,-32 L -5,-42 L -13,-48 L -3,-48 Z" fill="#ff9900" />
            </g>
          </svg>

          <div class="html-click-layer">
            <div 
              v-for="table in layoutData.tables" 
              :key="'click-' + table.id"
              class="html-table-hotspot"
              :class="{ 'active': currentTableId === table.id }"
              :style="{ 
                left: `calc((${table.posX} / ${layoutData.canvasWidth}) * 100%)`, 
                top: `calc((${table.posY} / ${layoutData.canvasHeight}) * 100%)` 
              }"
              @click.stop="switchTable(table)"
            ></div>
          </div>
        </div>
      </div>

      <div class="seats-wrapper">
        <div class="table-center-display">
          <div class="table-text">
            {{ currentTableNo }}号桌<br/>
            <span>{{ currentTableRemark || '主客齐聚' }}</span>
          </div>
        </div>
        
        <!--
          修复点2：座位网格外面套一层固定高度、内部滚动的容器。
          不同桌子座位数不同（比如10人桌 vs 8人桌），3列网格排出来的行数不一样，
          之前是"整个页面高度"跟着变，现在把这部分高度锁死，座位多的桌子自己内部滚动，
          不会影响页面其他部分的位置和整体高度。
        -->
        <div class="seats-grid-scroll">
          <div class="seats-grid">
            <div 
              v-for="seat in seatList" 
              :key="seat.id"
              :class="['seat-box', seat.status === 1 ? 'occupied' : 'available', chosenSeat?.id === seat.id ? 'active' : '']"
              @click="handleSeatClick(seat)"
            >
              {{ seat.seatNo }}号
              <span class="status-txt">{{ seat.status === 1 ? '已被占' : '空闲' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="confirm-box">
        <van-button type="primary" block round color="#52c41a" :disabled="!chosenSeat" :loading="loading" @click="submitLockSeat">
          确认锁定该座位
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGuestStore } from '@/stores/guest'
import { showToast, showDialog } from 'vant'
import { registerGuest, getTableSeats, lockSeat, getVenueLayout, type VenueLayoutData } from '@/api/guest'

const route = useRoute()
const router = useRouter()
const guestStore = useGuestStore()
const slug = route.params.slug as string

const showPicker = ref(false)
const loading = ref(false)
const chosenSeat = ref<any>(null)
const seatList = ref<any[]>([])

const recommendTableId = ref<number>(0)
const currentTableId = ref<number>(0)
const currentTableNo = ref<string>('')
const currentTableRemark = ref<string>('')

const layoutData = ref<VenueLayoutData>({
  canvasWidth: 1000,
  canvasHeight: 800,
  elements: [],
  tables: []
})

const form = ref({ name: '', phone: '', category: '' })
const categories = ref([
  { text: '新郎同学', value: '新郎同学' },
  { text: '新娘闺蜜', value: '新娘闺蜜' },
  { text: '新郎亲戚', value: '新郎亲戚' },
  { text: '新娘同事', value: '新娘同事' }
])

// 是否正在查看"系统推荐的那一桌"，提示条内容和颜色都靠这个变量派生，不再用v-if/v-else整个切组件
const isViewingRecommended = computed(() => recommendTableId.value === currentTableId.value)

const noticeText = computed(() => {
  if (isViewingRecommended.value) {
    return `🌟 系统已自动为您锁定推荐的【${currentTableNo.value}号桌】（${currentTableRemark.value}）。您可以直接在下方盲选，或点击地图里的其他桌子！`
  }
  return `📍 当前正在查看：【${currentTableNo.value}号桌】。点击大地图上的其他圆圈可自由切桌。`
})

const onConfirmCategory = ({ selectedOptions }: any) => {
  form.value.category = selectedOptions[0].text
  showPicker.value = false
}

// 来宾登记
const onSubmitRegister = async () => {
  loading.value = true
  try {
    const res = await registerGuest({
      eventSlug: slug,
      name: form.value.name,
      phone: form.value.phone,
      category: form.value.category
    })

    if (res.code === 200) {
      const recommend = res.data.recommendedTable
      guestStore.setGuestInfo(res.data.guestId, form.value.name, slug, recommend)
      
      recommendTableId.value = recommend.id
      currentTableId.value = recommend.id
      currentTableNo.value = recommend.tableNo
      currentTableRemark.value = recommend.remark

      await loadVenueLayoutData()
      await loadSeatsData(currentTableId.value)
    } else {
      showToast(res.message)
    }
  } catch (err) {
    showToast('登记失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 拉取布局
const loadVenueLayoutData = async () => {
  try {
    const res = await getVenueLayout(slug)
    if (res.code === 200) {
      layoutData.value = res.data
    }
  } catch (err) {
    console.error('全景画布加载异常')
  }
}

// 自由切桌函数（剥离任何 SVG 副作用）
const switchTable = async (table: any) => {
  if (currentTableId.value === table.id) return
  currentTableId.value = table.id
  currentTableNo.value = table.tableNo
  currentTableRemark.value = table.remark
  
  loading.value = true
  await loadSeatsData(table.id)
  loading.value = false
}

// 拉取座位
const loadSeatsData = async (tableId: number) => {
  chosenSeat.value = null
  try {
    const res = await getTableSeats(tableId)
    if (res.code === 200) {
      seatList.value = res.data
    }
  } catch (err) {
    showToast('拉取座位数据失败')
  }
}

const handleSeatClick = (seat: any) => {
  if (seat.status === 1) {
    showToast('该位置已有客友对号入座，请换一个绿色位置~')
    return
  }
  chosenSeat.value = seat
}

// 提交锁座
const submitLockSeat = async () => {
  if (!chosenSeat.value || !guestStore.guestId) return
  loading.value = true
  
  try {
    const res = await lockSeat({
      guestId: guestStore.guestId,
      seatId: chosenSeat.value.id,
      version: chosenSeat.value.version 
    })

    if (res.code === 200 && res.data === true) {
      showDialog({
        title: '🎉 恭喜您，选座成功！',
        message: `您已成功锁定【${currentTableNo.value}号桌 · ${chosenSeat.value.seatNo}号座位】。\n\n我们在婚礼现场期待您的到来！`,
        theme: 'round-button',
        confirmButtonColor: '#ff4d4f'
      }).then(() => {
        router.push(`/guest/event/${slug}/home`)
      })
    } else if (res.code === 409) {
      showDialog({
        title: '手速慢了一步',
        message: '哎呀！这个席位刚好被别的客友捷足先登了，请重新选择空闲席位。'
      }).then(() => {
        loadSeatsData(currentTableId.value)
      })
    } else {
      showToast(res.message || '选座失败')
    }
  } catch (err) {
    showToast('网络繁忙，选座超时')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ➔ 核心修正 1：强行让整个选座页面不管内容多少，都永远显示滚动条占位 */
/* 这样右侧就不会因为座位多寡导致滚动条忽隐忽现、从而引发页面宽度和画布等比例抖动 */
.seat-select-page { 
  min-height: 100vh; 
  background: #f7f8fa; 
  overflow-y: scroll; /* 👈 关键：强行开启垂直滚动条占位 */
}

.form-container { padding: 20px 0; }
.form-title { text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 24px; color: #333; }
.map-container { padding: 12px 0 120px 0; }
.welcome-user { padding: 0 16px 8px 16px; font-size: 14px; color: #444; }

/* 🗺️ 可视化大地图外层容器 */
.venue-canvas-wrapper { 
  background: #2a2a2e; 
  margin: 12px; 
  border-radius: 12px; 
  padding: 12px; 
  box-shadow: 0 4px 12px rgba(0,0,0,0.15); 
}
.canvas-title { text-align: center; color: #cca662; font-size: 12px; font-weight: bold; margin-bottom: 8px; letter-spacing: 1px; }

/* ➔ 核心修正 2：用现代 CSS 替代过时的 padding-bottom 撑高法 */
.interactive-area { 
  position: relative; 
  width: 100%; 
  /* 👈 换成绝对稳固的宽高比属性，宽高完全由原生的 1000/800 锁定，不再受外界任何 Flex 或外部 BFC 挤压 */
  aspect-ratio: var(--canvas-w) / var(--canvas-h); 
  background: #1e1e22; 
  border-radius: 8px; 
  border: 1px solid #3d3d42; 
  overflow: hidden; 
}

/* 底层：纯展示用 SVG */
.venue-svg { 
  position: absolute; 
  top: 0; 
  left: 0; 
  width: 100%; 
  height: 100%; 
}

.svg-stage { fill: #611f1f; stroke: #ff4d4f; stroke-width: 2; }
.svg-screen { fill: #2c3e50; stroke: #3498db; stroke-width: 1.5; }
.svg-door { fill: #2e4031; stroke: #27ae60; stroke-width: 1.5; }
.svg-el-text { fill: #e0e0e0; font-size: 14px; font-weight: bold; user-select: none; }

.svg-table-circle { fill: #fcf5ed; stroke: #cca662; stroke-width: 2; transition: all 0.2s; }
.svg-table-inner { fill: none; stroke: #cca662; stroke-width: 1; stroke-dasharray: 3 3; opacity: 0.6; }
.svg-table-text { fill: #5c4033; font-size: 14px; font-weight: bold; }
.svg-table-sub { fill: #8a7365; font-size: 11px; }

.svg-table-group.is-recommend .svg-table-circle { stroke: #ff9900; stroke-width: 3; }
.svg-table-group.is-active .svg-table-circle { fill: #ffeef0; stroke: #ff4d4f; stroke-width: 4; }
.svg-table-group.is-active .svg-table-text { fill: #ff4d4f; }

/* 顶层：自适应 HTML 点击热区 */
.html-click-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; }
.html-table-hotspot { position: absolute; width: 90px; height: 90px; border-radius: 50%; transform: translate(-50%, -50%); cursor: pointer; background: transparent; }
.html-table-hotspot:active { background: rgba(255, 77, 79, 0.15); }

/* 下方桌位细节网格 */
.seats-wrapper { background: white; margin: 12px; border-radius: 12px; padding: 24px 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.table-center-display { width: 110px; height: 110px; background: #fff5f5; border: 2px dashed #ff4d4f; border-radius: 50%; margin: 0 auto 24px auto; display: flex; align-items: center; justify-content: center; text-align: center; }
.table-text { font-size: 15px; font-weight: bold; color: #ff4d4f; line-height: 1.3; }
.table-text span { font-size: 11px; color: #999; font-weight: normal; display: block; margin-top: 2px; }

/*
  修复点2的CSS：给座位网格套一层固定高度+内部滚动的容器，
  不管当前桌子是10人桌还是8人桌，这块区域的高度永远不变，
  页面下方的确认按钮、整体页面高度不会因为切换桌子而跳动。
  280px大概能装下4行左右的座位（3列），如果你的桌子座位数普遍更多(比如12人桌)，
  可以适当调大这个数值，超出部分会在网格内部自己滚动，不影响外部页面布局。
*/
.seats-grid-scroll {
  max-height: 280px;
  overflow-y: auto;
}
.seats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding-right: 4px; }
.seat-box { padding: 12px 0; border-radius: 8px; text-align: center; font-size: 14px; display: flex; flex-direction: column; gap: 4px; border: 1px solid #e8e8e8; }
.seat-box.available { background: #f6ffed; border-color: #b7eb8f; color: #52c41a; }
.seat-box.occupied { background: #fff1f0; border-color: #ffa39e; color: #f5222d; }
.seat-box.active { background: #e6f7ff; border-color: #91d5ff; color: #1890ff; box-shadow: 0 0 8px rgba(24,144,255,0.4); font-weight: bold; }
.status-txt { font-size: 11px; opacity: 0.8; }
.confirm-box { position: fixed; bottom: 0; left: 0; right: 0; padding: 16px 24px; background: white; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 99; }
</style>