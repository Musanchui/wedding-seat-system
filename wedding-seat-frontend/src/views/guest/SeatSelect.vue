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
      <div class="welcome-user">尊敬的 <b>{{ guestStore.guestName }}</b>，请选择您的桌位及座号：</div>
      
      <van-notice-bar v-if="guestStore.recommendedTable" color="#ed6a0c" background="#fffbe6" left-icon="info-o" wrapable>
        依据您的身份，系统为您推荐：【{{ guestStore.recommendedTable.tableNo }}号桌】 ({{ guestStore.recommendedTable.remark || '空闲最多' }})
      </van-notice-bar>

      <div class="seats-wrapper">
        <div class="table-center">
          <div class="table-text">
            {{ guestStore.recommendedTable?.tableNo }}号桌<br/>
            <span>{{ guestStore.recommendedTable?.remark }}</span>
          </div>
        </div>
        
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

      <div class="confirm-box">
        <van-button type="primary" block round color="#52c41a" :disabled="!chosenSeat" :loading="loading" @click="submitLockSeat">
          确认锁定该座位
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useGuestStore } from '@/stores/guest'
import { showToast, showDialog } from 'vant'
import { registerGuest, getTableSeats, lockSeat } from '@/api/guest'
import { useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const guestStore = useGuestStore()
const slug = route.params.slug as string

const showPicker = ref(false)
const loading = ref(false)
const chosenSeat = ref<any>(null) // 存下当前点击的座位对象
const seatList = ref<any[]>([])

const form = ref({ name: '', phone: '', category: '' })
const categories = ref([
  { text: '新郎同学', value: '新郎同学' },
  { text: '新娘闺蜜', value: '新娘闺蜜' },
  { text: '新郎亲戚', value: '新郎亲戚' },
  { text: '新娘同事', value: '新娘同事' }
])

const onConfirmCategory = ({ selectedOptions }: any) => {
  form.value.category = selectedOptions[0].text
  showPicker.value = false
}

// 2.3 提交来宾信息登记
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
      // 缓存用户信息
      guestStore.setGuestInfo(res.data.guestId, form.value.name, slug, res.data.recommendedTable)
      // 加载这一桌的全部座位状态
      await loadSeatsData(res.data.recommendedTable.id)
    } else {
      showToast(res.message) // 弹出异常文案（比如重复、手机格式有误等）
    }
  } catch (err) {
    showToast('登记失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 2.4 获取当前桌的实时座位数据
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
  chosenSeat.value = seat // 锁定选中，附带它的乐观锁 version
}

// 2.5 并发控制锁座逻辑
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
      // ➔ 核心改动：从全局 store 中安全提取当时在首页拿到的婚礼基本信息
      // （假设你已经在 store 存了，或者我们通过简单的格式化拼接提示）
      const tableNo = guestStore.recommendedTable?.tableNo || ''
      const seatNo = chosenSeat.value.seatNo
      
      showDialog({
        title: '🎉 恭喜您，选座成功！',
        message: `您已成功锁定【${tableNo}号桌 · ${seatNo}号座位】。\n\n我们在婚礼现场期待您的到来！`,
        theme: 'round-button',
        confirmButtonColor: '#ff4d4f'
      }).then(() => {
        // ➔ 核心改动：用户点击弹窗的“确定”按钮后，立刻优雅地退回到来宾端 H5 首页
        router.push(`/guest/event/${slug}/home`)
      })

    } else if (res.code === 409) {
      // 高并发对抗：座位被抢
      showDialog({
        title: '手速慢了一步',
        message: '哎呀！这个席位刚好被别的客友捷足先登了，请重新选择空闲席位。'
      }).then(() => {
        loadSeatsData(guestStore.recommendedTable.id)
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
.seat-select-page { min-height: 100vh; background: #f7f8fa; }
.form-container { padding: 20px 0; }
.form-title { text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 24px; color: #333; }
.map-container { padding: 12px 0 100px 0; }
.welcome-user { padding: 0 16px 12px 16px; font-size: 14px; color: #444; }
.seats-wrapper { background: white; margin: 12px; border-radius: 12px; padding: 24px 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.table-center { width: 120px; height: 120px; background: #ffeef0; border: 2px dashed #ff4d4f; border-radius: 50%; margin: 0 auto 32px auto; display: flex; align-items: center; justify-content: center; text-align: center; }
.table-text { font-size: 16px; font-weight: bold; color: #ff4d4f; line-height: 1.3; }
.table-text span { font-size: 11px; color: #999; font-weight: normal; display: block; margin-top: 2px; }
.seats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.seat-box { padding: 12px 0; border-radius: 8px; text-align: center; font-size: 14px; display: flex; flex-direction: column; gap: 4px; border: 1px solid #e8e8e8; transition: all 0.2s; }
.seat-box.available { background: #f6ffed; border-color: #b7eb8f; color: #52c41a; }
.seat-box.occupied { background: #fff1f0; border-color: #ffa39e; color: #f5222d; }
.seat-box.active { background: #e6f7ff; border-color: #91d5ff; color: #1890ff; box-shadow: 0 0 10px rgba(24,144,255,0.4); font-weight: bold; transform: scale(1.03); }
.status-txt { font-size: 11px; opacity: 0.8; }
.confirm-box { position: fixed; bottom: 0; left: 0; right: 0; padding: 16px 24px; background: white; box-shadow: 0 -2px 10px rgba(0,0,0,0.05); z-index: 99; }
</style>