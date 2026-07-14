<template>
  <div class="seat-layout-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="router.push('/admin/dashboard')">返回控制台</el-button>
      <h2>桌位大地图</h2>
      <div class="header-hint">拖动桌子/场地图标调整位置，松手自动保存</div>
    </div>
    <div class="workspace" v-loading="pageLoading">
      <!-- 左侧工具栏 -->
      <div class="toolbar">
        <div class="tool-section">
          <div class="tool-title">桌位</div>
          <el-button type="primary" :icon="Plus" @click="tableDialogVisible = true" style="width: 100%"> 新增桌位 </el-button>
        </div>
        <div class="tool-section">
          <div class="tool-title">场地元素</div>
          <el-button plain :icon="Plus" @click="quickAddElement('stage', '舞台', 300, 120)" style="width: 100%; margin-bottom: 8px">舞台</el-button>
          <el-button plain :icon="Plus" @click="quickAddElement('screen', '投影幕布', 200, 30)" style="width: 100%; margin-bottom: 8px">投影幕布</el-button>
          <el-button plain :icon="Plus" @click="quickAddElement('entrance', '入口', 120, 60)" style="width: 100%; margin-bottom: 8px">入口</el-button>
          <el-button plain :icon="Plus" @click="quickAddElement('exit', '出口', 120, 60)" style="width: 100%">出口</el-button>
        </div>
        <div class="tool-section legend">
          <div class="tool-title">图例</div>
          <div class="legend-item"><span class="dot gold"></span>正常桌位</div>
          <div class="legend-item"><span class="dot red"></span>已满座</div>
          <div class="legend-item"><span class="dot blue"></span>当前选中</div>
        </div>
      </div>

      <!-- 中间画布区域 -->
      <!-- 
        核心修复：
        1. .canvas-wrapper 使用 justify-content: center 让画布居中
        2. .canvas-wrapper 使用 min-width: 0 防止 flex 溢出
      -->
      <div class="canvas-wrapper" ref="canvasWrapperRef">
        <div class="canvas-container" :style="{ height: canvasHeightPx + 'px' }" ref="canvasContainerRef">
          <div class="canvas-inner" @click.self="clearSelection">
            <svg :viewBox="`0 0 ${layout.canvasWidth} ${layout.canvasHeight}`" class="venue-svg" preserveAspectRatio="none">
              <g v-for="el in layout.elements" :key="'el-' + el.id" :transform="`translate(${el.posX}, ${el.posY}) rotate(${el.rotation})`">
                <rect :width="el.width" :height="el.height" rx="8" :class="['svg-el', 'svg-el-' + el.type, { 'svg-el-selected': isSelected('element', el.id) }]" />
                <text :x="el.width / 2" :y="el.height / 2 + 5" text-anchor="middle" class="svg-el-text">{{ el.label }}</text>
              </g>
              <g v-for="table in layout.tables" :key="'table-' + table.id" :transform="`translate(${table.posX || 0}, ${table.posY || 0})`">
                <circle cx="0" cy="0" r="45" :class="['svg-table-circle', tableStatusClass(table), { 'svg-table-selected': isSelected('table', table.id) }]" />
                <text x="0" y="-5" text-anchor="middle" class="svg-table-text">{{ table.tableNo }}号桌</text>
                <text x="0" y="18" text-anchor="middle" class="svg-table-sub">{{ occupiedCount(table) }}/{{ table.seatCount }}</text>
              </g>
            </svg>
            
            <!-- HTML热区层 -->
            <div class="hotspot-layer">
              <div v-for="el in layout.elements" :key="'hotspot-el-' + el.id" 
                   class="hotspot hotspot-rect" 
                   :style="getRectStyle(el)" 
                   @mousedown.stop="startDrag($event, 'element', el)" 
                   @click.stop="selectItem('element', el.id)">
              </div>
              
              <el-popover v-for="table in layout.tables" :key="'hotspot-table-' + table.id" 
                          placement="top" width="220" trigger="hover" :show-after="150">
                <template #reference>
                  <div class="hotspot hotspot-circle" 
                       :style="getCircleStyle(table)" 
                       @mousedown.stop="startDrag($event, 'table', table)" 
                       @click.stop="selectItem('table', table.id)">
                  </div>
                </template>
                <div class="seat-tooltip">
                  <div class="seat-tooltip-title">{{ table.tableNo }}号桌 · {{ table.remark || '无备注' }}</div>
                  <div class="seat-tooltip-grid">
                    <div v-for="seat in table.seats" :key="seat.id" 
                         :class="['seat-chip', seat.status === 1 ? 'occupied' : 'free']" 
                         :title="seat.guestName ? `${seat.guestName} · ${seat.guestPhone}` : '空位'">
                      {{ seat.seatNo }} <span v-if="seat.guestName" class="seat-chip-name">{{ seat.guestName }}</span>
                    </div>
                  </div>
                </div>
              </el-popover>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="props-panel">
        <template v-if="selectedType === 'table' && selectedTable">
          <div class="panel-title">桌位属性</div>
          <el-form label-position="top" size="default">
            <el-form-item label="桌号">
              <el-input v-model="selectedTable.tableNo" />
            </el-form-item>
            <el-form-item label="座位数">
              <el-input-number v-model="selectedTable.seatCount" :min="1" :max="30" style="width: 100%" />
              <div class="form-tip">缩小座位数时，如果被砍掉的座位已有人入座，保存会失败</div>
            </el-form-item>
            <el-form-item label="备注（如：新郎大学同学）">
              <el-input v-model="selectedTable.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-form>
          <div class="panel-actions">
            <el-button type="primary" :loading="saving" style="width: 100%" @click="saveSelectedTable">保存修改</el-button>
            <el-button type="danger" plain style="width: 100%; margin-top: 8px; margin-left: 0" @click="handleDeleteTable">删除该桌</el-button>
          </div>
        </template>
        <template v-else-if="selectedType === 'element' && selectedElement">
          <div class="panel-title">场地元素属性</div>
          <el-form label-position="top" size="default">
            <el-form-item label="标签文字">
              <el-input v-model="selectedElement.label" />
            </el-form-item>
            <el-form-item label="宽度">
              <el-input-number v-model="selectedElement.width" :min="20" :max="600" style="width: 100%" />
            </el-form-item>
            <el-form-item label="高度">
              <el-input-number v-model="selectedElement.height" :min="20" :max="600" style="width: 100%" />
            </el-form-item>
          </el-form>
          <div class="panel-actions">
            <el-button type="primary" :loading="saving" style="width: 100%" @click="saveSelectedElement">保存修改</el-button>
            <el-button type="danger" plain style="width: 100%; margin-top: 8px; margin-left: 0" @click="handleDeleteElement">删除该元素</el-button>
          </div>
        </template>
        <el-empty v-else description="点击画布上的桌子或场地元素进行编辑" :image-size="70" />
      </div>
    </div>

    <!-- 新增桌位弹窗 -->
    <el-dialog v-model="tableDialogVisible" title="新增桌位" width="420px" destroy-on-close>
      <el-form :model="newTableForm" label-position="top">
        <el-form-item label="桌号" required>
          <el-input v-model="newTableForm.tableNo" placeholder="例如：1、A1、主桌" />
        </el-form-item>
        <el-form-item label="座位数" required>
          <el-input-number v-model="newTableForm.seatCount" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="newTableForm.remark" placeholder="例如：新郎大学同学" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tableDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitNewTable">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { getAdminVenueLayout, saveTable, deleteTable, saveVenueElement, deleteVenueElement, type AdminVenueLayout, type AdminTableLayout, type VenueElement } from '@/api/adminTable'

const route = useRoute()
const router = useRouter()
const eventId = Number(route.params.id)

const pageLoading = ref(false)
const saving = ref(false)
const canvasWrapperRef = ref<HTMLElement | null>(null)
const canvasContainerRef = ref<HTMLElement | null>(null)

// 动态高度，初始给一个较大的默认值，防止加载瞬间塌陷
const canvasHeightPx = ref(600)

const layout = reactive<AdminVenueLayout>({
  canvasWidth: 1000,
  canvasHeight: 800,
  elements: [],
  tables: []
})

const selectedType = ref<'table' | 'element' | null>(null)
const selectedId = ref<number | null>(null)
const selectedTable = computed(() => layout.tables.find((t) => t.id === selectedId.value) || null)
const selectedElement = computed(() => layout.elements.find((e) => e.id === selectedId.value) || null)

const isSelected = (type: 'table' | 'element', id: number) => selectedType.value === type && selectedId.value === id
const occupiedCount = (table: AdminTableLayout) => table.seats.filter((s) => s.status === 1).length

const tableStatusClass = (table: AdminTableLayout) => {
  if (isSelected('table', table.id)) return 'svg-table-selected'
  return occupiedCount(table) >= table.seatCount ? 'svg-table-full' : 'svg-table-normal'
}

const selectItem = (type: 'table' | 'element', id: number) => {
  selectedType.value = type
  selectedId.value = id
}

const clearSelection = () => {
  selectedType.value = null
  selectedId.value = null
}

const loadLayout = async () => {
  pageLoading.value = true
  try {
    const res = await getAdminVenueLayout(eventId)
    Object.assign(layout, res.data)
    // 数据加载后重新计算尺寸
    await nextTick()
    updateCanvasSize()
  } catch (err: any) {
    ElMessage.error(err?.message || '加载场地大地图失败')
  } finally {
    pageLoading.value = false
  }
}

// ============================================
// 核心修复：JS 强制计算画布尺寸
// ============================================
const updateCanvasSize = () => {
  if (!canvasWrapperRef.value) return
  
  // 获取容器的实际渲染宽度
  const wrapperWidth = canvasWrapperRef.value.clientWidth
  // 减去左右 padding (24px * 2 = 48px)
  const availableWidth = wrapperWidth - 48
  
  // 限制最大宽度为 1000px，如果容器不够宽则自适应
  const targetWidth = Math.min(availableWidth, 1000)
  
  // 防御性检查：如果宽度为 0，则使用默认宽度 1000
  const finalWidth = targetWidth > 0 ? targetWidth : 1000
  
  // 按照 1000:800 的比例计算高度
  const targetHeight = finalWidth * 0.8
  
  canvasHeightPx.value = targetHeight
}

// 监听窗口大小变化
const handleResize = () => {
  // 使用 requestAnimationFrame 确保在浏览器重绘后计算，避免获取到 0
  requestAnimationFrame(() => {
    updateCanvasSize()
  })
}

// ============================================
// 样式计算：基于百分比，确保热区与SVG图形完全重合
// ============================================
const getCircleStyle = (table: AdminTableLayout) => {
  const left = ((table.posX || 0) / layout.canvasWidth) * 100
  const top = ((table.posY || 0) / layout.canvasHeight) * 100
  // 直径是90px (r=45)，需要换算成百分比宽度
  const sizePercent = (90 / layout.canvasWidth) * 100
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${sizePercent}%`,
    height: `${sizePercent}%`, // 保持圆形
    transform: 'translate(-50%, -50%)'
  }
}

const getRectStyle = (el: VenueElement) => {
  const left = (el.posX / layout.canvasWidth) * 100
  const top = (el.posY / layout.canvasHeight) * 100
  const width = (el.width / layout.canvasWidth) * 100
  const height = (el.height / layout.canvasHeight) * 100
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${width}%`,
    height: `${height}%`
  }
}

// ============================================
// 拖拽逻辑
// ============================================
let dragging = false
let dragType: 'table' | 'element' | null = null
let dragItem: AdminTableLayout | VenueElement | null = null
let dragStartClientX = 0
let dragStartClientY = 0
let dragStartPosX = 0
let dragStartPosY = 0

const startDrag = (e: MouseEvent, type: 'table' | 'element', item: AdminTableLayout | VenueElement) => {
  selectItem(type, item.id)
  dragging = true
  dragType = type
  dragItem = item
  dragStartClientX = e.clientX
  dragStartClientY = e.clientY
  dragStartPosX = type === 'table' ? (item as AdminTableLayout).posX || 0 : (item as VenueElement).posX
  dragStartPosY = type === 'table' ? (item as AdminTableLayout).posY || 0 : (item as VenueElement).posY
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

const onDragMove = (e: MouseEvent) => {
  if (!dragging || !dragItem || !canvasContainerRef.value) return
  
  // 获取画布容器的实际渲染尺寸
  const rect = canvasContainerRef.value.getBoundingClientRect()
  
  // 计算缩放比例：画布逻辑尺寸 / 实际渲染尺寸
  const scaleX = layout.canvasWidth / rect.width
  const scaleY = layout.canvasHeight / rect.height
  
  const deltaX = (e.clientX - dragStartClientX) * scaleX
  const deltaY = (e.clientY - dragStartClientY) * scaleY
  
  const newX = Math.round(dragStartPosX + deltaX)
  const newY = Math.round(dragStartPosY + deltaY)
  
  if (dragType === 'table') {
    ;(dragItem as AdminTableLayout).posX = newX
    ;(dragItem as AdminTableLayout).posY = newY
  } else {
    ;(dragItem as VenueElement).posX = newX
    ;(dragItem as VenueElement).posY = newY
  }
}

const onDragEnd = async () => {
  if (!dragging || !dragItem) {
    dragging = false
    return
  }
  const type = dragType
  const item = dragItem
  dragging = false
  dragType = null
  dragItem = null
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  
  try {
    if (type === 'table') {
      const t = item as AdminTableLayout
      await saveTable({ id: t.id, eventId, tableNo: t.tableNo, seatCount: t.seatCount, remark: t.remark || undefined, posX: t.posX ?? undefined, posY: t.posY ?? undefined, rotation: t.rotation })
    } else {
      const el = item as VenueElement
      await saveVenueElement({ id: el.id, eventId, type: el.type, label: el.label, posX: el.posX, posY: el.posY, width: el.width, height: el.height, rotation: el.rotation })
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '坐标保存失败')
    loadLayout()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  window.removeEventListener('resize', handleResize)
})

// ============================================
// 业务逻辑
// ============================================
const tableDialogVisible = ref(false)
const newTableForm = reactive({ tableNo: '', seatCount: 10, remark: '' })

const submitNewTable = async () => {
  if (!newTableForm.tableNo.trim()) {
    ElMessage.warning('请填写桌号')
    return
  }
  saving.value = true
  try {
    await saveTable({ eventId, tableNo: newTableForm.tableNo.trim(), seatCount: newTableForm.seatCount, remark: newTableForm.remark || undefined, posX: 150, posY: 150, rotation: 0 })
    ElMessage.success('桌位创建成功')
    tableDialogVisible.value = false
    newTableForm.tableNo = ''
    newTableForm.seatCount = 10
    newTableForm.remark = ''
    await loadLayout()
  } catch (err: any) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

const saveSelectedTable = async () => {
  if (!selectedTable.value) return
  saving.value = true
  try {
    const t = selectedTable.value
    await saveTable({ id: t.id, eventId, tableNo: t.tableNo, seatCount: t.seatCount, remark: t.remark || undefined, posX: t.posX ?? undefined, posY: t.posY ?? undefined, rotation: t.rotation })
    ElMessage.success('桌位信息已保存')
    await loadLayout()
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDeleteTable = async () => {
  if (!selectedTable.value) return
  const tableId = selectedTable.value.id
  try {
    await deleteTable(tableId, false)
    ElMessage.success('桌位已删除')
    clearSelection()
    await loadLayout()
  } catch (err: any) {
    try {
      await ElMessageBox.confirm(
        `${err?.message || '该桌还有来宾已经入座'}，是否强制删除？`,
        '强制删除确认',
        { confirmButtonText: '强制删除', cancelButtonText: '取消', type: 'warning' }
      )
      await deleteTable(tableId, true)
      ElMessage.warning('已强制删除')
      clearSelection()
      await loadLayout()
    } catch {}
  }
}

const quickAddElement = async (type: string, label: string, width: number, height: number) => {
  try {
    await saveVenueElement({ eventId, type, label, posX: 200, posY: 200, width, height, rotation: 0 })
    ElMessage.success(`已添加：${label}`)
    await loadLayout()
  } catch (err: any) {
    ElMessage.error(err?.message || '添加失败')
  }
}

const saveSelectedElement = async () => {
  if (!selectedElement.value) return
  saving.value = true
  try {
    const el = selectedElement.value
    await saveVenueElement({ id: el.id, eventId, type: el.type, label: el.label, posX: el.posX, posY: el.posY, width: el.width, height: el.height, rotation: el.rotation })
    ElMessage.success('已保存')
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDeleteElement = async () => {
  if (!selectedElement.value) return
  try {
    await deleteVenueElement(selectedElement.value.id)
    ElMessage.success('已删除')
    clearSelection()
    await loadLayout()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

onMounted(() => {
  loadLayout()
  // 初始化尺寸计算
  nextTick(() => {
    // 延迟一点时间确保 DOM 完全渲染
    setTimeout(() => {
      updateCanvasSize()
    }, 100)
    window.addEventListener('resize', handleResize)
  })
})
</script>

<style scoped>
.seat-layout-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7f9;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0;
  font-size: 17px;
  color: #1f1f1f;
}

.header-hint {
  margin-left: auto;
  font-size: 12px;
  color: #999;
}

.workspace {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.toolbar {
  width: 200px;
  background: white;
  border-right: 1px solid #e8e8e8;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
  flex-shrink: 0;
}

.tool-section {
  margin-bottom: 24px;
}

.tool-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
  font-weight: 600;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.dot.gold { background: #cca662; }
.dot.red { background: #ff4d4f; }
.dot.blue { background: #1890ff; }

/* 
  核心修复：画布容器 
  1. flex: 1 占据剩余空间
  2. min-width: 0 防止 flex 子项溢出导致宽度计算错误
  3. display: flex + justify-content/align-items: center 让画布居中
*/
.canvas-wrapper {
  flex: 1;
  min-width: 0; /* 关键修复：允许 flex 子项收缩到内容宽度以下 */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #eef0f2;
  overflow: auto;
  position: relative;
}

/* 
  核心修复：画布本体 
  1. width: 100% 确保占满 .canvas-wrapper 的可用空间
  2. max-width: 1000px 限制最大宽度
  3. JS 动态绑定 height
*/
.canvas-container {
  width: 100%;
  max-width: 1000px; /* 限制最大宽度 */
  min-height: 400px; /* 兜底最小高度 */
  background: #1e1e22;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  position: relative;
  /* 调试用：如果还是看不到，取消下面这行的注释看看边界在哪里 */
  /* border: 2px solid red; */
}

.canvas-inner {
  width: 100%;
  height: 100%;
  position: relative;
}

.venue-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.svg-el {
  fill: #2c3e50;
  stroke: #3d4f61;
  stroke-width: 1.5;
}

.svg-el-stage { fill: #611f1f; stroke: #ff4d4f; }
.svg-el-screen { fill: #2c3e50; stroke: #3498db; }
.svg-el-entrance, .svg-el-exit { fill: #2e4031; stroke: #27ae60; }
.svg-el-selected { stroke: #1890ff; stroke-width: 3; }

.svg-el-text {
  fill: #e8e8e8;
  font-size: 13px;
  font-weight: bold;
}

.svg-table-circle {
  fill: #fcf5ed;
  stroke: #cca662;
  stroke-width: 2;
  transition: stroke 0.15s, fill 0.15s;
}

.svg-table-full { stroke: #ff4d4f; }
.svg-table-selected { stroke: #1890ff; stroke-width: 4; fill: #e6f7ff; }

.svg-table-text {
  fill: #5c4033;
  font-size: 13px;
  font-weight: bold;
}

.svg-table-sub {
  fill: #8a7365;
  font-size: 11px;
}

.hotspot-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* 让鼠标事件穿透到具体的 hotspot 元素 */
}

.hotspot {
  position: absolute;
  cursor: move;
  pointer-events: auto; /* 恢复热点区域的鼠标事件 */
}

.hotspot-circle:hover {
  background: rgba(24, 144, 255, 0.1);
  border-radius: 50%;
}

.hotspot-rect:hover {
  background: rgba(24, 144, 255, 0.08);
  outline: 1px dashed rgba(24, 144, 255, 0.4);
}

.seat-tooltip-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.seat-tooltip-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.seat-chip {
  font-size: 11px;
  text-align: center;
  padding: 4px 2px;
  border-radius: 4px;
  line-height: 1.3;
}

.seat-chip.free { background: #f6ffed; color: #52c41a; }
.seat-chip.occupied { background: #fff1f0; color: #f5222d; }

.seat-chip-name {
  display: block;
  font-size: 9px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.props-panel {
  width: 280px;
  background: white;
  border-left: 1px solid #e8e8e8;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
  flex-shrink: 0;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
}

.panel-actions { margin-top: 8px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }
</style>
