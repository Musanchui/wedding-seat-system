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
          <van-button round block type="primary" color="#ff4d4f" native-type="submit">提交并选择座位</van-button>
        </div>
      </van-form>
      
      <van-popup v-model:show="showPicker" position="bottom">
        <van-picker :columns="categories" @confirm="onConfirmCategory" @cancel="showPicker = false" />
      </van-popup>
    </div>

    <div v-else class="map-container">
      <div class="welcome-user">尊敬的 <b>{{ guestStore.guestName }}</b>，请选择您的桌位及座号：</div>
      
      <van-notice-bar v-if="guestStore.recommendedTable" color="#ed6a0c" background="#fffbe6" left-icon="info-o">
        根据您的身份，推荐坐：【{{ guestStore.recommendedTable.tableNo }}】 ({{ guestStore.recommendedTable.remark }})
      </van-notice-bar>

      <div class="table-selector">
        <van-dropdown-menu>
          <van-dropdown-item v-model="selectedTableId" :options="tableOptions" @change="loadSeatsData" />
        </van-dropdown-menu>
      </div>

      <div class="seats-wrapper">
        <div class="table-center">
          <div class="table-text">{{ currentTableInfo.tableNo }}<br/><span>{{ currentTableInfo.remark }}</span></div>
        </div>
        
        <div class="seats-grid">
          <div 
            v-for="seat in seatList" 
            :key="seat.id"
            :class="['seat-box', seat.status === 1 ? 'occupied' : 'available', chosenSeatId === seat.id ? 'active' : '']"
            @click="handleSeatClick(seat)"
          >
            {{ seat.seatNo }}号
            <span class="status-txt">{{ seat.status === 1 ? '有人' : '空闲' }}</span>
          </div>
        </div>
      </div>

      <div class="confirm-box">
        <van-button type="primary" block round color="#52c41a" :disabled="!chosenSeatId" @click="submitLockSeat">
          确认锁定该座位
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGuestStore } from '@/stores/guest'
import { showToast, showDialog } from 'vant'

const guestStore = useGuestStore()
const showPicker = ref(false)
const selectedTableId = ref(101)
const chosenSeatId = ref<number | null>(null)

const form = ref({ name: '', phone: '', category: '' })
const categories = ref([
  { text: '新郎同学', value: '新郎同学' },
  { text: '新娘同事', value: '新娘同事' },
  { text: '新郎亲戚', value: '新郎亲戚' },
  { text: '新娘闺蜜', value: '新娘闺蜜' }
])

// 模拟桌位下拉列表
const tableOptions = ref([
  { text: '主桌-1号桌', value: 101 },
  { text: '新郎大学同学-2号桌', value: 102 },
  { text: '新娘高中同学-3号桌', value: 103 }
])

const currentTableInfo = ref({ tableNo: '2号桌', remark: '新郎大学同学' })
const seatList = ref<any[]>([])

const onConfirmCategory = ({ selectedOptions }: any) => {
  form.value.category = selectedOptions[0].text
  showPicker.value = false
}

// 模拟提交登记
const onSubmitRegister = async () => {
  // 正常走 Axios：const res = await axios.post('/api/guest/register', form.value)
  // 这里模拟后端返回
  const mockRecommend = { id: 102, tableNo: '2号桌', remark: '新郎大学同学' }
  guestStore.setGuestInfo(999, form.value.name, 1, mockRecommend)
  selectedTableId.value = mockRecommend.id
  loadSeatsData()
}

// 模拟加载对应桌子的座位状态（带version乐观锁机制）
const loadSeatsData = () => {
  chosenSeatId.value = null
  // 模拟从后端 /api/guest/table/{id}/seats 拿取10个座位
  seatList.value = Array.from({ length: 10 }, (_, i) => ({
    id: selectedTableId.value * 10 + i,
    seatNo: i + 1,
    status: Math.random() > 0.6 ? 1 : 0, // 随机搞几个已占用的位置
    version: 1
  }))
}

const handleSeatClick = (seat: any) => {
  if (seat.status === 1) {
    showToast('此座已有人预定，换个空闲的座位吧~')
    return
  }
  chosenSeatId.value = seat.id
}

// 模拟最终抢座锁座提交
const submitLockSeat = async () => {
  // 正常打包参数：{ guestId: guestStore.guestId, seatId: chosenSeatId.value, version: xxx }
  showToast({ type: 'loading', message: '正在锁座中...', forbidClick: true })
  
  setTimeout(() => {
    // 模拟高并发冲突场景（30%概率抢座失败）
    if (Math.random() > 0.7) {
      showDialog({
        title: '选座失败',
        message: '哎呀！这个座位刚好被别的客友抢先一步锁定了，请重新选择空位。'
      }).then(() => {
        loadSeatsData() // 自动刷新列表
      })
    } else {
      showDialog({
        title: '恭喜您',
        message: '您的选座登记已成功完成，届时请对号入座，祝您用餐愉快！'
      })
    }
  }, 1000)
}
</script>

<style scoped>
.seat-select-page { min-height: 100vh; background: #f7f8fa; }
.form-container { padding: 20px 0; }
.form-title { text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 24px; color: #333; }
.map-container { padding: 12px 0 80px 0; }
.welcome-user { padding: 0 16px 12px 16px; font-size: 14px; color: #444; }
.table-selector { margin-bottom: 12px; }
.seats-wrapper { background: white; margin: 12px; border-radius: 12px; padding: 20px 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.table-center { width: 110px; height: 110px; background: #ffeef0; border: 2px dashed #ff4d4f; border-radius: 50%; margin: 0 auto 24px auto; display: flex; align-items: center; justify-content: center; text-align: center; }
.table-text { font-size: 16px; font-weight: bold; color: #ff4d4f; line-height: 1.3; }
.table-text span { font-size: 11px; color: #999; font-weight: normal; }
.seats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.seat-box { padding: 12px 0; border-radius: 8px; text-align: center; font-size: 14px; display: flex; flex-direction: column; gap: 4px; border: 1px solid #e8e8e8; }
.seat-box.available { background: #f6ffed; border-color: #b7eb8f; color: #52c41a; }
.seat-box.occupied { background: #fff1f0; border-color: #ffa39e; color: #f5222d; }
.seat-box.active { background: #e6f7ff; border-color: #91d5ff; color: #1890ff; box-shadow: 0 0 8px rgba(24,144,255,0.4); font-weight: bold; }
.status-txt { font-size: 11px; opacity: 0.8; }
.confirm-box { position: fixed; bottom: 0; left: 0; right: 0; padding: 16px; background: white; }
</style>