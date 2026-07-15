<template>
  <div class="seat-select-page">
    <van-nav-bar title="来宾登记与选座" left-arrow @click-left="$router.back()" />

    <!-- 登记表单 -->
    <div v-if="!guestId" class="form-container">
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

    <!-- 登记成功后：先问要不要自己选座位 -->
    <div v-else-if="flowStage === 'choose'" class="choose-container">
      <div class="choose-card">
        <div class="choose-title">您好，{{ guestName }} 👋</div>
        <div class="choose-desc">请问您想自己挑选座位，还是让我们帮您安排？</div>

        <van-button round block type="primary" color="#ff4d4f" style="margin-bottom: 14px" @click="handleChooseSelf">
          我要自己选座位
        </van-button>
        <van-button round block plain type="primary" color="#ff4d4f" @click="flowStage = 'autoAssign'">
          帮我自动分配座位
        </van-button>
      </div>
    </div>

    <!-- 自动分配：问几个人 -->
    <div v-else-if="flowStage === 'autoAssign'" class="choose-container">
      <div class="choose-card">
        <div class="choose-title">帮您自动安排座位</div>
        <div class="choose-desc">请问一共几位（含您本人，最多3位）？系统会自动为您安排在同一桌相邻的座位。</div>

        <van-stepper v-model="autoAssignCount" min="1" max="3" integer input-width="60px" button-size="32px" style="margin: 20px 0" />

        <van-button round block type="primary" color="#ff4d4f" :loading="loading" @click="handleAutoAssign">
          确认，帮我安排
        </van-button>
        <van-button round block plain style="margin-top: 12px" @click="flowStage = 'choose'">返回上一步</van-button>
      </div>
    </div>

    <!-- 自动分配完成：直接展示结果，不需要进地图 -->
    <div v-else-if="flowStage === 'done'" class="choose-container">
      <div class="choose-card">
        <van-icon name="checked" color="#52c41a" size="48" style="display: block; margin: 0 auto 12px" />
        <div class="choose-title">安排成功！</div>
        <div class="choose-desc">已经为您安排好以下座位：</div>
        <div class="done-seats">
          <div v-for="seat in mySeats" :key="seat.seatId" class="done-seat-item">
            {{ seat.tableNo }}号桌 · {{ seat.seatNo }}号座位
          </div>
        </div>
        <van-button round block plain style="margin-top: 16px" @click="flowStage = 'map'; loadVenueLayoutData()">
          查看现场大地图
        </van-button>
      </div>
    </div>

    <!-- 选座主界面 -->
    <div v-else class="map-container">
      <div class="welcome-user">
        尊敬的 <b>{{ guestName }}</b>，最多可以为自己和同行的家人朋友选择 <b>3个</b> 座位。
      </div>

      <!-- 我的选座进度条 -->
      <div class="my-seats-bar">
        <div class="my-seats-title">我已选择（{{ mySeats.length }}/3）</div>
        <div class="my-seats-chips">
          <span v-if="mySeats.length === 0" class="chip-empty">还没有选座位，点击下方地图开始选择</span>
          <span v-for="seat in mySeats" :key="seat.seatId" class="seat-chip">
            {{ seat.tableNo }}号桌-{{ seat.seatNo }}号
            <van-icon name="cross" class="chip-close" @click="handleReleaseSeat(seat)" />
          </span>
        </div>
      </div>

      <van-notice-bar
        :color="isViewingRecommended ? '#ed6a0c' : '#1890ff'"
        :background="isViewingRecommended ? '#fffbe6' : '#e6f7ff'"
        :left-icon="isViewingRecommended ? 'star-o' : 'location-o'"
      >
        {{ noticeText }}
      </van-notice-bar>

      <div class="venue-canvas-wrapper" :style="{ '--canvas-w': layoutData.canvasWidth, '--canvas-h': layoutData.canvasHeight }">
        <div class="canvas-title">✨ 喜宴会大厅全景导航图（点击桌位可切换）✨</div>

        <div class="interactive-area">
          <svg :viewBox="`0 0 ${layoutData.canvasWidth} ${layoutData.canvasHeight}`" class="venue-svg" style="pointer-events: none;">
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
              :class="{ active: currentTableId === table.id }"
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
            {{ currentTableNo }}号桌<br />
            <span>{{ currentTableRemark || '主客齐聚' }}</span>
          </div>
        </div>

        <div class="seats-grid-scroll">
          <div class="seats-grid">
            <div
              v-for="seat in seatList"
              :key="seat.id"
              :class="['seat-box', seatDisplayClass(seat)]"
              @click="handleSeatClick(seat)"
            >
              {{ seat.seatNo }}号
              <span class="status-txt">{{ seatDisplayLabel(seat) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  registerGuest,
  getTableSeats,
  lockSeat,
  releaseSeat,
  getMySeats,
  getVenueLayout,
  autoAssignSeats,
  type VenueLayoutData,
  type Seat,
  type SeatSummary
} from '@/api/guest'
import { useGuestStore } from '@/stores/guest'

const route = useRoute()
const guestStore = useGuestStore()
const slug = route.params.slug as string

const showPicker = ref(false)
const loading = ref(false)
const seatList = ref<Seat[]>([])
const mySeats = ref<SeatSummary[]>([])

const guestId = ref<number | null>(null)
const guestName = ref('')

// 流程状态：choose=问要不要自己选，autoAssign=填人数，done=自动分配完成展示结果，map=选座地图
const flowStage = ref<'choose' | 'autoAssign' | 'done' | 'map'>('choose')
const autoAssignCount = ref(1)
let pendingRecommend: { id: number; tableNo: string; remark: string | null } | null = null

const recommendTableId = ref<number>(0)
const currentTableId = ref<number>(0)
const currentTableNo = ref<string>('')
const currentTableRemark = ref<string>('')

const layoutData = ref<VenueLayoutData>({ canvasWidth: 1000, canvasHeight: 800, elements: [], tables: [] })

const form = ref({ name: '', phone: '', category: '' })
const categories = ref([
  { text: '新郎同学', value: '新郎同学' },
  { text: '新娘闺蜜', value: '新娘闺蜜' },
  { text: '新郎亲戚', value: '新郎亲戚' },
  { text: '新娘同事', value: '新娘同事' }
])

const isViewingRecommended = computed(() => recommendTableId.value === currentTableId.value)
const noticeText = computed(() => {
  if (isViewingRecommended.value) {
    return `🌟 系统已自动为您锁定推荐的【${currentTableNo.value}号桌】（${currentTableRemark.value}）。您可以直接在下方选座，或点击地图里的其他桌子！`
  }
  return `📍 当前正在查看：【${currentTableNo.value}号桌】。点击大地图上的其他圆圈可自由切桌。`
})

const onConfirmCategory = ({ selectedOptions }: any) => {
  form.value.category = selectedOptions[0].text
  showPicker.value = false
}

// 座位在网格里的展示状态：free=空闲可选，occupied=别人占用，mine=我选的
const seatDisplayClass = (seat: Seat) => {
  if (mySeats.value.some((s) => s.seatId === seat.id)) return 'mine'
  return seat.status === 1 ? 'occupied' : 'available'
}
const seatDisplayLabel = (seat: Seat) => {
  if (mySeats.value.some((s) => s.seatId === seat.id)) return '我的座位'
  return seat.status === 1 ? '已被占' : '空闲'
}

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
      guestId.value = res.data.guestId
      guestName.value = form.value.name
      guestStore.setGuestInfo(slug, res.data.guestId, form.value.name)

      await loadVenueLayoutData()

      if (res.data.selectedSeats && res.data.selectedSeats.length > 0) {
        // 已经选过至少1个座位的老来宾：不用再问了，直接进地图展示已选状态
        mySeats.value = res.data.selectedSeats
        const firstSeat = res.data.selectedSeats[0]
        const table = layoutData.value.tables.find((t) => t.id === firstSeat.tableId)
        currentTableId.value = firstSeat.tableId
        currentTableNo.value = firstSeat.tableNo || ''
        currentTableRemark.value = table?.remark || ''
        recommendTableId.value = firstSeat.tableId
        await loadSeatsData(firstSeat.tableId)
        flowStage.value = 'map'
      } else if (res.data.recommendedTable) {
        // 新来宾/还没选座：先问"自己选"还是"帮我分配"，暂存推荐桌信息，等用户选了"自己选"再真正加载座位格子
        pendingRecommend = res.data.recommendedTable
        flowStage.value = 'choose'
      }
    } else {
      showToast(res.message)
    }
  } catch (err: any) {
    showToast(err?.message || '登记失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleChooseSelf = async () => {
  if (!pendingRecommend) return
  loading.value = true
  try {
    recommendTableId.value = pendingRecommend.id
    currentTableId.value = pendingRecommend.id
    currentTableNo.value = pendingRecommend.tableNo
    currentTableRemark.value = pendingRecommend.remark || ''
    await loadSeatsData(pendingRecommend.id)
    flowStage.value = 'map'
  } finally {
    loading.value = false
  }
}

const handleAutoAssign = async () => {
  if (!guestId.value) return
  loading.value = true
  try {
    const res = await autoAssignSeats({ guestId: guestId.value, seatCount: autoAssignCount.value })
    if (res.code === 200) {
      mySeats.value = res.data
      flowStage.value = 'done'
    } else {
      showToast(res.message || '分配失败')
    }
  } catch (err: any) {
    showToast(err?.message || '分配失败，可能是没有一桌能一次容纳这么多人，建议改为自己选座（可分开坐）')
  } finally {
    loading.value = false
  }
}

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

const switchTable = async (table: any) => {
  if (currentTableId.value === table.id) return
  currentTableId.value = table.id
  currentTableNo.value = table.tableNo
  currentTableRemark.value = table.remark
  loading.value = true
  await loadSeatsData(table.id)
  loading.value = false
}

const loadSeatsData = async (tableId: number) => {
  try {
    const res = await getTableSeats(tableId)
    if (res.code === 200) {
      seatList.value = res.data
    }
  } catch (err) {
    showToast('拉取座位数据失败')
  }
}

const refreshMySeats = async () => {
  if (!guestId.value) return
  try {
    const res = await getMySeats(guestId.value)
    if (res.code === 200) {
      mySeats.value = res.data
    }
  } catch (err) {
    // 静默失败即可，不影响主流程
  }
}

const handleSeatClick = async (seat: Seat) => {
  const isMine = mySeats.value.some((s) => s.seatId === seat.id)

  if (isMine) {
    // 点击自己已选的座位：走取消流程（有单独的❌图标，这里点格子本身也允许取消，双入口更方便操作）
    const target = mySeats.value.find((s) => s.seatId === seat.id)!
    handleReleaseSeat(target)
    return
  }

  if (seat.status === 1) {
    showToast('该位置已有客友对号入座，请换一个空位')
    return
  }

  if (mySeats.value.length >= 3) {
    showToast('每人最多可以选择3个座位，请先取消其中一个再选新的')
    return
  }

  try {
    const res = await lockSeat({ guestId: guestId.value!, seatId: seat.id, version: seat.version })
    if (res.code === 200 && res.data === true) {
      showToast('选座成功')
      await Promise.all([loadSeatsData(currentTableId.value), refreshMySeats()])
    } else if (res.code === 409) {
      showToast(res.message || '手速慢了一步，该座位刚被抢占')
      await loadSeatsData(currentTableId.value)
    } else {
      showToast(res.message || '选座失败')
    }
  } catch (err: any) {
    if (err?.code === 409) {
      showToast(err.message || '手速慢了一步，该座位刚被抢占')
      await loadSeatsData(currentTableId.value)
    } else {
      showToast(err?.message || '选座失败')
    }
  }
}

const handleReleaseSeat = async (seat: SeatSummary) => {
  try {
    await showConfirmDialog({
      title: '取消该座位？',
      message: `确定要取消【${seat.tableNo}号桌 - ${seat.seatNo}号】这个座位吗？`
    })
  } catch {
    return // 用户点了取消
  }

  try {
    await releaseSeat({ guestId: guestId.value!, seatId: seat.seatId })
    showToast('已取消该座位')
    await refreshMySeats()
    if (currentTableId.value === seat.tableId) {
      await loadSeatsData(currentTableId.value)
    }
  } catch (err: any) {
    showToast(err?.message || '取消失败')
  }
}

onMounted(() => {
  const saved = guestStore.getGuestInfo(slug)
  if (saved) {
    guestId.value = saved.guestId
    guestName.value = saved.guestName
    ;(async () => {
      await loadVenueLayoutData()
      await refreshMySeats()
      if (mySeats.value.length > 0) {
        const first = mySeats.value[0]
        currentTableId.value = first.tableId
        currentTableNo.value = first.tableNo || ''
        recommendTableId.value = first.tableId
        await loadSeatsData(first.tableId)
      }
      // 恢复会话直接进地图，不用再问一遍"自己选还是自动分配"
      flowStage.value = 'map'
    })()
  }
})
</script>

<style scoped>
.choose-container { min-height: calc(100vh - 46px); display: flex; align-items: center; justify-content: center; padding: 24px; }
.choose-card { width: 100%; max-width: 340px; background: white; border-radius: 16px; padding: 32px 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); text-align: center; }
.choose-title { font-size: 18px; font-weight: bold; color: #333; margin-bottom: 10px; }
.choose-desc { font-size: 13px; color: #888; margin-bottom: 24px; line-height: 1.6; }
.done-seats { background: #f6ffed; border-radius: 10px; padding: 12px; margin-top: 8px; }
.done-seat-item { font-size: 14px; color: #52c41a; padding: 6px 0; font-weight: 600; }

.seat-select-page { min-height: 100vh; background: #f7f8fa; overflow-y: scroll; }

.form-container { padding: 20px 0; }
.form-title { text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 24px; color: #333; }
.map-container { padding: 12px 0 40px 0; }
.welcome-user { padding: 0 16px 8px 16px; font-size: 14px; color: #444; }

.my-seats-bar { margin: 0 12px 12px 12px; background: white; border-radius: 10px; padding: 12px 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.my-seats-title { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 8px; }
.my-seats-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip-empty { font-size: 12px; color: #999; }
.seat-chip { display: inline-flex; align-items: center; gap: 4px; background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; border-radius: 16px; padding: 4px 10px; font-size: 12px; }
.chip-close { cursor: pointer; font-size: 12px; }

.venue-canvas-wrapper { background: #2a2a2e; margin: 12px; border-radius: 12px; padding: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.canvas-title { text-align: center; color: #cca662; font-size: 12px; font-weight: bold; margin-bottom: 8px; letter-spacing: 1px; }
.interactive-area { position: relative; width: 100%; aspect-ratio: var(--canvas-w) / var(--canvas-h); background: #1e1e22; border-radius: 8px; border: 1px solid #3d3d42; overflow: hidden; }
.venue-svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }

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

.html-click-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; }
.html-table-hotspot { position: absolute; width: 90px; height: 90px; border-radius: 50%; transform: translate(-50%, -50%); cursor: pointer; background: transparent; }
.html-table-hotspot:active { background: rgba(255, 77, 79, 0.15); }

.seats-wrapper { background: white; margin: 12px; border-radius: 12px; padding: 24px 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.table-center-display { width: 110px; height: 110px; background: #fff5f5; border: 2px dashed #ff4d4f; border-radius: 50%; margin: 0 auto 24px auto; display: flex; align-items: center; justify-content: center; text-align: center; }
.table-text { font-size: 15px; font-weight: bold; color: #ff4d4f; line-height: 1.3; }
.table-text span { font-size: 11px; color: #999; font-weight: normal; display: block; margin-top: 2px; }

.seats-grid-scroll { max-height: 320px; overflow-y: auto; }
.seats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding-right: 4px; }
.seat-box { padding: 12px 0; border-radius: 8px; text-align: center; font-size: 14px; display: flex; flex-direction: column; gap: 4px; border: 1px solid #e8e8e8; }
.seat-box.available { background: #f6ffed; border-color: #b7eb8f; color: #52c41a; }
.seat-box.occupied { background: #fff1f0; border-color: #ffa39e; color: #f5222d; }
.seat-box.mine { background: #e6f7ff; border-color: #91d5ff; color: #1890ff; box-shadow: 0 0 8px rgba(24,144,255,0.4); font-weight: bold; }
.status-txt { font-size: 11px; opacity: 0.8; }
</style>
