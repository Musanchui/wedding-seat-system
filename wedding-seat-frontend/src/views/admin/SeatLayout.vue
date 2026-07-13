<template>
  <div class="seat-layout-container">
    <!-- 顶部状态导航栏 -->
    <div class="layout-header">
      <el-page-header title="返回控制台" @back="router.push('/admin/dashboard')">
        <template #content>
          <span class="header-title">🎨 宴会厅桌位上帝视角排布 —— 婚礼标识: <code>{{ slug }}</code></span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button type="success" :loading="saveLoading" @click="handleSaveLayout">
          💾 保存排布地图
        </el-button>
      </div>
    </div>

    <!-- 主工作区 -->
    <div class="workspace">
      <!-- 左侧工具栏 -->
      <div class="toolbar">
        <h4>🛠️ 组件库</h4>
        <el-button type="primary" plain class="tool-btn" @click="addTable('circle')">
          ⭕ 添加标准圆桌
        </el-button>
        <el-button type="primary" plain class="tool-btn" @click="addTable('rect')">
          方形主桌
        </el-button>
        <div class="canvas-tips">
          <h5>💡 操作指南：</h5>
          <ul>
            <li>点击画布上的桌子可以进行选中。</li>
            <li>按住鼠标左键可任意**拖拽**更换桌位。</li>
            <li>别忘了点击右上角保存数据。</li>
          </ul>
        </div>
      </div>

      <!-- 中间 Canvas 画布区 -->
      <div class="canvas-area" ref="canvasContainer">
        <canvas 
          ref="seatCanvas"
          @mousedown="onCanvasMouseDown"
          @mousemove="onCanvasMouseMove"
          @mouseup="onCanvasMouseUp"
        ></canvas>
      </div>

      <!-- 右侧属性编辑面板 -->
      <div class="properties-panel">
        <h4>📋 选中元素属性</h4>
        <div v-if="selectedTable" class="props-form">
          <el-form label-position="top">
            <el-form-item label="桌位名称">
              <el-input v-model="selectedTable.tableName" @input="draw" />
            </el-form-item>
            <el-form-item label="容纳人数 (当前桌)">
              <el-input-number v-model="selectedTable.seatCount" :min="1" :max="20" @change="draw" />
            </el-form-item>
            <el-form-item label="坐标 X">
              <el-input-number v-model="selectedTable.x" disabled />
            </el-form-item>
            <el-form-item label="坐标 Y">
              <el-input-number v-model="selectedTable.y" disabled />
            </el-form-item>
            <el-button type="danger" style="width: 100%; margin-top: 20px;" @click="deleteSelectedTable">
              🗑️ 拆除该桌位
            </el-button>
          </el-form>
        </div>
        <div v-else class="no-selected">
          <el-empty description="在画布上点选一张桌子来编辑属性" :image-size="80" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSeatLayout, saveSeatLayout, type TableItem } from '@/api/adminSeat'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const slug = ref(route.params.slug as string)

// 画布尺寸相关
const canvasWidth = ref(1000)
const canvasHeight = ref(800)

// 画布引用与交互状态
const seatCanvas = ref<HTMLCanvasElement | null>(null)
const canvasContainer = ref<HTMLDivElement | null>(null)
const tables = ref<TableItem[]>([])
const selectedTable = ref<TableItem | null | undefined>(null)
const saveLoading = ref(false)

// 拖拽变量跟踪
let isDragging = false
let dragOffsetX = 0
let dragOffsetY = 0
const TABLE_RADIUS = 40 // 桌子的绘制半径

// 1. 初始化从后端加载数据
const loadLayoutData = async () => {
  try {
    const res = await getSeatLayout(slug.value)
    canvasWidth.value = res.data.layoutWidth || 1000
    canvasHeight.value = res.data.layoutHeight || 800
    tables.value = res.data.tables || []
    initCanvas()
  } catch (err) {
    ElMessage.error('加载桌位地图失败')
  }
}

// 2. 初始化 Canvas 画布基础配置
const initCanvas = () => {
  const canvas = seatCanvas.value
  if (!canvas) return
  canvas.width = canvasWidth.value
  canvas.height = canvasHeight.value
  draw()
}

// 3. 核心绘制引擎 (Canvas Render)
const draw = () => {
  const canvas = seatCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 绘制浪漫科技感的背景网格线
  ctx.strokeStyle = '#f0f0f0'
  ctx.lineWidth = 1
  for (let i = 50; i < canvas.width; i += 50) {
    ctx.beginPath(); ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height); ctx.stroke()
  }
  for (let j = 50; j < canvas.height; j += 50) {
    ctx.beginPath(); ctx.moveTo(0, j); ctx.lineTo(canvas.width, j); ctx.stroke()
  }

  // 循环渲染每一张桌子
  tables.value.forEach((table) => {
    const isSelected = selectedTable.value === table

    // 绘制外圈座位小圆点（氛围感）
    ctx.fillStyle = isSelected ? '#ff4d4f' : '#8c8c8c'
    for (let k = 0; k < table.seatCount; k++) {
      const angle = (Math.PI * 2 / table.seatCount) * k
      const chairX = table.x + Math.cos(angle) * (TABLE_RADIUS + 12)
      const chairY = table.y + Math.sin(angle) * (TABLE_RADIUS + 12)
      ctx.beginPath()
      ctx.arc(chairX, chairY, 5, 0, Math.PI * 2)
      ctx.fill()
    }

    // 绘制主体桌身
    ctx.beginPath()
    ctx.arc(table.x, table.y, TABLE_RADIUS, 0, Math.PI * 2)
    ctx.fillStyle = isSelected ? '#fff0f6' : '#ffffff'
    ctx.fill()
    ctx.strokeStyle = isSelected ? '#ff4d4f' : '#d9d9d9'
    ctx.lineWidth = isSelected ? 4 : 2
    ctx.stroke()

    // 绘制桌子中心的名字标签
    ctx.fillStyle = '#1f1f1f'
    ctx.font = 'bold 13px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    // 若名字过长进行截断
    let name = table.tableName || '未命名'
    if (name.length > 5) name = name.substring(0, 4) + '..'
    ctx.fillText(name, table.x, table.y - 6)

    // 绘制人数标签
    ctx.fillStyle = '#8c8c8c'
    ctx.font = '11px sans-serif'
    ctx.fillText(`${table.seatCount}人`, table.x, table.y + 12)
  })
}

// 4. 新增一张桌子入场
const addTable = (type: 'circle' | 'rect') => {
  const newTable: TableItem = {
    tableName: `新桌位-${tables.value.length + 1}`,
    seatCount: 10,
    x: 150,
    y: 150
  }
  tables.value.push(newTable)
  selectedTable.value = newTable // 默认选中刚加的
  draw()
  ElMessage.success('成功放入一张新圆桌，请将其拖拽到合适位置')
}

// 5. 鼠标点下：碰撞检测，看看点中了哪张桌子
const onCanvasMouseDown = (e: MouseEvent) => {
  const canvas = seatCanvas.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  // 计算出在 Canvas 内部的真实坐标
  const clientX = e.clientX - rect.left
  const clientY = e.clientY - rect.top

  // 逆序查找，优先点选最上层的桌子
  let found: TableItem | null = null
  for (let i = tables.value.length - 1; i >= 0; i--) {
    const t = tables.value[i]
    if (!t) continue
    // 计算点到圆心的毕达哥拉斯距离
    const dist = Math.sqrt((clientX - t.x) ** 2 + (clientY - t.y) ** 2)
    if (dist <= TABLE_RADIUS) {
      found = t
      break
    }
  }

  if (found) {
    selectedTable.value = found
    isDragging = true
    dragOffsetX = clientX - found.x
    dragOffsetY = clientY - found.y
  } else {
    selectedTable.value = null // 点在空白处，清空选中
  }
  draw()
}

// 6. 鼠标移动：如果是拖拽状态，实时同步刷新坐标
const onCanvasMouseMove = (e: MouseEvent) => {
  if (!isDragging || !selectedTable.value) return
  const canvas = seatCanvas.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const clientX = e.clientX - rect.left
  const clientY = e.clientY - rect.top

  // 边界限定限制，不移出画布
  let targetX = clientX - dragOffsetX
  let targetY = clientY - dragOffsetY
  if (targetX < TABLE_RADIUS) targetX = TABLE_RADIUS
  if (targetX > canvas.width - TABLE_RADIUS) targetX = canvas.width - TABLE_RADIUS
  if (targetY < TABLE_RADIUS) targetY = TABLE_RADIUS
  if (targetY > canvas.height - TABLE_RADIUS) targetY = canvas.height - TABLE_RADIUS

  selectedTable.value.x = Math.round(targetX)
  selectedTable.value.y = Math.round(targetY)
  draw()
}

// 7. 释放鼠标
const onCanvasMouseUp = () => {
  isDragging = false
}

// 8. 删除选中的桌子
const deleteSelectedTable = () => {
  if (!selectedTable.value) return
  tables.value = tables.value.filter(t => t !== selectedTable.value)
  selectedTable.value = null
  draw()
  ElMessage.warning('桌位已从当前画布移除')
}

// 9. 保存大布局发给后端
const handleSaveLayout = async () => {
  saveLoading.value = true
  try {
    await saveSeatLayout(slug.value, {
      layoutWidth: canvasWidth.value,
      layoutHeight: canvasHeight.value,
      tables: tables.value
    })
    ElMessage.success('🎉 恭喜！全场桌位排布及物理坐标已完美保存！')
  } catch (err) {
    // 自动被拦截器解析
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  loadLayoutData()
})
</script>

<style scoped>
.seat-layout-container { display: flex; flex-direction: column; height: 100vh; background-color: #f7f9fa; box-sizing: border-box; }
.layout-header { display: flex; justify-content: space-between; align-items: center; background: #fff; padding: 12px 24px; border-bottom: 1px solid #e8e8e8; }
.header-title code { background: #fff0f6; color: #ff4d4f; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-weight: bold; }

.workspace { display: flex; flex: 1; overflow: hidden; }

/* 左侧工具栏 */
.toolbar { width: 220px; background: #fff; border-right: 1px solid #e8e8e8; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
.toolbar h4 { margin: 0 0 10px 0; color: #333; border-left: 4px solid #ff4d4f; padding-left: 8px; }
.tool-btn { width: 100%; margin-left: 0 !important; margin-bottom: 8px; font-weight: bold; }
.canvas-tips { margin-top: auto; background: #fff7e6; border: 1px solid #ffd591; padding: 12px; border-radius: 8px; }
.canvas-tips h5 { margin: 0 0 6px 0; color: #d46b08; }
.canvas-tips ul { margin: 0; padding-left: 16px; font-size: 12px; color: #666; line-height: 1.6; }

/* 中间主画布 */
.canvas-area { flex: 1; padding: 30px; display: flex; justify-content: center; align-items: center; overflow: auto; background: #eef1f2; }
canvas { background: #ffffff; box-shadow: 0 4px 20px rgba(0,0,0,0.08); border-radius: 8px; cursor: crosshair; }

/* 右侧属性栏 */
.properties-panel { width: 280px; background: #fff; border-left: 1px solid #e8e8e8; padding: 20px; box-sizing: border-box; }
.properties-panel h4 { margin: 0 0 20px 0; color: #333; border-left: 4px solid #1890ff; padding-left: 8px; }
.no-selected { padding-top: 60px; }
</style>