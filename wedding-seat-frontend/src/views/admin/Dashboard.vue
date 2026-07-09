<template>
  <div class="admin-layout-container">
    <el-container style="min-height: 100vh;">
      <el-aside width="200px" style="background-color: #304156;">
        <div class="menu-logo">婚礼选座后台</div>
        <el-menu active-text-color="#409EFF" background-color="#304156" class="el-menu-vertical" default-active="1" text-color="#fff">
          <el-menu-item index="1" @click="$router.push('/admin/dashboard')">
            <el-icon><Platform /></el-icon><span>桌位大盘看板</span>
          </el-menu-item>
          <el-menu-item index="2" @click="$router.push('/admin/settings')">
            <el-icon><Setting /></el-icon><span>素材与婚礼配置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-main class="main-body">
        <div class="page-header">
          <h2>宴会厅桌位动态实时看板</h2>
          <el-button type="primary" icon="Plus" @click="handleOpenAddTable">添加新桌位</el-button>
        </div>

        <el-alert title="数据实时汇总：总桌数 3 桌 | 总席位 30 席 | 已入座 12 席 | 空余 18 席" type="success" :closable="false" style="margin-bottom: 20px;" />

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="table in tableLayoutList" :key="table.id" style="margin-bottom: 20px;">
            <el-card class="table-card" shadow="hover">
              <template #header>
                <div class="card-title-bar">
                  <span class="t-name">{{ table.tableNo }} <el-tag size="small" type="info">{{ table.remark }}</el-tag></span>
                  <el-button size="small" type="primary" link icon="Edit" @click="handleEditTable(table)">调整</el-button>
                </div>
              </template>
              
              <div class="badge-grid">
                <el-tooltip
                  v-for="seat in table.seats"
                  :key="seat.seatNo"
                  class="box-item"
                  effect="dark"
                  :content="seat.status === 1 ? `来宾: ${seat.guestName} (${seat.guestPhone})` : '空闲无人'"
                  placement="top"
                >
                  <div :class="['seat-dot', seat.status === 1 ? 'is-busy' : 'is-empty']">
                    {{ seat.seatNo }}
                  </div>
                </el-tooltip>
              </div>
              
              <div class="card-footer-info">
                <span>容纳数: {{ table.seatCount }}人</span>
                <span class="fill-rate">已锁定: {{ table.seats.filter(s => s.status === 1).length }} 人</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>

    <el-dialog v-model="dialogVisible" :title="formTitle" width="400px">
      <el-form :model="tableForm" label-width="90px">
        <el-form-item label="桌号/名称">
          <el-input v-model="tableForm.tableNo" placeholder="如：主桌 或 05号桌" />
        </el-form-item>
        <el-form-item label="计划座位数">
          <el-input-number v-model="tableForm.seatCount" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="亲友备注">
          <el-input v-model="tableForm.remark" placeholder="如：新郎初中同学" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTableConfig">保存提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const dialogVisible = ref(false)
const formTitle = ref('新增桌位')
const tableForm = ref({ id: null, tableNo: '', seatCount: 10, remark: '' })

// 模拟从后端 /api/admin/tables/layout 拉取的完整树状结构
const tableLayoutList = ref([
  {
    id: 1,
    tableNo: '主桌',
    seatCount: 10,
    remark: '新郎新娘至亲',
    seats: [
      { seatNo: 1, status: 1, guestName: '张三爸爸', guestPhone: '138****0001' },
      { seatNo: 2, status: 1, guestName: '李四妈妈', guestPhone: '138****0002' },
      { seatNo: 3, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 4, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 5, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 6, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 7, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 8, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 9, status: 0, guestName: '', guestPhone: '' },
      { seatNo: 10, status: 0, guestName: '', guestPhone: '' }
    ]
  },
  {
    id: 2,
    tableNo: '02号桌',
    seatCount: 10,
    remark: '新郎大学同学',
    seats: Array.from({ length: 10 }, (_, i) => ({
      seatNo: i + 1,
      status: i < 6 ? 1 : 0, // 模拟前6个人坐满了
      guestName: '客友' + (i + 1),
      guestPhone: '139****8888'
    }))
  },
  {
    id: 3,
    tableNo: '03号桌',
    seatCount: 10,
    remark: '新娘公司同事',
    seats: Array.from({ length: 10 }, (_, i) => ({
      seatNo: i + 1,
      status: i < 4 ? 1 : 0,
      guestName: '同事' + (i + 1),
      guestPhone: '135****6666'
    }))
  }
])

const handleOpenAddTable = () => {
  formTitle.value = '新增桌位配置'
  tableForm.value = { id: null, tableNo: '', seatCount: 10, remark: '' }
  dialogVisible.value = true
}

const handleEditTable = (row: any) => {
  formTitle.value = '修改桌位配置'
  tableForm.value = { ...row }
  dialogVisible.value = true
}

const saveTableConfig = () => {
  // 发送 POST 请求到 /api/admin/table/save
  ElMessage({ type: 'success', message: '桌位数据更新成功！' })
  dialogVisible.value = false
}
</script>

<style scoped>
.menu-logo { height: 60px; line-height: 60px; text-align: center; color: white; font-weight: bold; font-size: 16px; background: #2b2f3a; }
.main-body { background: #f0f2f5; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; color: #333; }
.card-title-bar { display: flex; justify-content: space-between; align-items: center; }
.t-name { font-weight: bold; font-size: 15px; }
.badge-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; margin-bottom: 16px; }
.seat-dot { height: 32px; line-height: 32px; text-align: center; border-radius: 4px; font-size: 12px; font-weight: bold; color: white; cursor: pointer; }
.seat-dot.is-empty { background-color: #67c23a; }
.seat-dot.is-busy { background-color: #f56c6c; }
.card-footer-info { display: flex; justify-content: space-between; font-size: 12px; color: #999; border-top: 1px solid #f0f0f0; padding-top: 10px; }
.fill-rate { color: #f56c6c; font-weight: bold; }
</style>