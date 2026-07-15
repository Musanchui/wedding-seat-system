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
          <el-button type="primary" :icon="Plus" @click="tableDialogVisible = true" style="width: 100%">
            新增桌位
          </el-button>
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

      <!-- 中间画布 -->
      <div class="canvas-wrapper" ref="canvasWrapperRef" :style="{ '--canvas-w': layout.canvasWidth, '--canvas-h': layout.canvasHeight }">
        <div class="canvas-inner" @click.self="clearSelection">
          <svg :viewBox="`0 0 ${layout.canvasWidth} ${layout.canvasHeight}`" class="venue-svg" style="pointer-events: none;">
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

          <!-- HTML热区层：拖拽 + 悬浮 + 点击选中 -->
          <div class="hotspot-layer">
            <div
              v-for="el in layout.elements"
              :key="'hotspot-el-' + el.id"
              class="hotspot hotspot-rect"
              :style="rectHotspotStyle(el)"
              @mousedown.stop="startDrag($event, 'element', el)"
              @click.stop="selectItem('element', el.id)"
            ></div>

            <el-popover
              v-for="table in layout.tables"
              :key="'hotspot-table-' + table.id"
              placement="top"
              width="220"
              trigger="hover"
              :show-after="150"
            >
              <template #reference>
                <div
                  class="hotspot hotspot-circle"
                  :style="circleHotspotStyle(table)"
                  @mousedown.stop="startDrag($event, 'table', table)"
                  @click.stop="selectItem('table', table.id)"
                ></div>
              </template>
              <div class="seat-tooltip">
                <div class="seat-tooltip-title">{{ table.tableNo }}号桌 · {{ table.remark || '无备注' }}</div>
                <div class="seat-tooltip-grid">
                  <div
                    v-for="seat in table.seats"
                    :key="seat.id"
                    :class="['seat-chip', seat.status === 1 ? 'occupied' : 'free']"
                    :title="seat.guestName ? `${seat.guestName} · ${seat.guestPhone}` : '空位'"
                  >
                    {{ seat.seatNo }}
                    <span v-if="seat.guestName" class="seat-chip-name">{{ seat.guestName }}</span>
                  </div>
                </div>
              </div>
            </el-popover>
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
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import {
  getAdminVenueLayout,
  saveTable,
  deleteTable,
  saveVenueElement,
  deleteVenueElement,
  type AdminVenueLayout,
  type AdminTableLayout,
  type VenueElement
} from '@/api/adminTable'

const route = useRoute()
const router = useRouter()
// 数据库自增id（不是slug），管理端接口都是按这个数字id查询的
const eventId = Number(route.params.id)

const pageLoading = ref(false)
const saving = ref(false)
const canvasWrapperRef = ref<HTMLElement | null>(null)

const layout = reactive<AdminVenueLayout>({ canvasWidth: 1000, canvasHeight: 800, elements: [], tables: [] })

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
    layout.canvasWidth = res.data.canvasWidth
    layout.canvasHeight = res.data.canvasHeight
    layout.elements = res.data.elements
    layout.tables = res.data.tables
  } catch (err: any) {
    ElMessage.error(err?.message || '加载场地大地图失败')
  } finally {
    pageLoading.value = false
  }
}

// ============================================
// 热区定位样式：用百分比定位，天然适配画布响应式缩放，不需要手动换算像素
// ============================================
const circleHotspotStyle = (table: AdminTableLayout) => ({
  left: `${((table.posX || 0) / layout.canvasWidth) * 100}%`,
  top: `${((table.posY || 0) / layout.canvasHeight) * 100}%`
})
const rectHotspotStyle = (el: VenueElement) => ({
  left: `${(el.posX / layout.canvasWidth) * 100}%`,
  top: `${(el.posY / layout.canvasHeight) * 100}%`,
  width: `${(el.width / layout.canvasWidth) * 100}%`,
  height: `${(el.height / layout.canvasHeight) * 100}%`
})

// ============================================
// 拖拽逻辑：记录起点，鼠标移动时按容器实际渲染尺寸换算回画布坐标系
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
  if (!dragging || !dragItem || !canvasWrapperRef.value) return
  const rect = canvasWrapperRef.value.getBoundingClientRect()
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

  // 松手后静默保存新坐标，不弹成功提示，避免拖拽这种高频操作被消息刷屏
  try {
    if (type === 'table') {
      const t = item as AdminTableLayout
      await saveTable({
        id: t.id,
        eventId,
        tableNo: t.tableNo,
        seatCount: t.seatCount,
        remark: t.remark || undefined,
        posX: t.posX ?? undefined,
        posY: t.posY ?? undefined,
        rotation: t.rotation
      })
    } else {
      const el = item as VenueElement
      await saveVenueElement({
        id: el.id,
        eventId,
        type: el.type,
        label: el.label,
        posX: el.posX,
        posY: el.posY,
        width: el.width,
        height: el.height,
        rotation: el.rotation
      })
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '坐标保存失败')
    loadLayout()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
})

// ============================================
// 新增桌位
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
    await saveTable({
      eventId,
      tableNo: newTableForm.tableNo.trim(),
      seatCount: newTableForm.seatCount,
      remark: newTableForm.remark || undefined,
      posX: 150,
      posY: 150,
      rotation: 0
    })
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

// ============================================
// 编辑/删除已选中的桌位
// ============================================
const saveSelectedTable = async () => {
  if (!selectedTable.value) return
  saving.value = true
  try {
    const t = selectedTable.value
    await saveTable({
      id: t.id,
      eventId,
      tableNo: t.tableNo,
      seatCount: t.seatCount,
      remark: t.remark || undefined,
      posX: t.posX ?? undefined,
      posY: t.posY ?? undefined,
      rotation: t.rotation
    })
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
    // 后端如果因为"有人入座"拒绝删除，会走到这里，弹二次确认
    try {
      await ElMessageBox.confirm(
        `${err?.message || '该桌还有来宾已经入座'}，是否强制删除？强制删除会清空这些来宾的座位信息，需要之后重新为他们安排座位。`,
        '强制删除确认',
        { confirmButtonText: '强制删除', cancelButtonText: '取消', type: 'warning' }
      )
      await deleteTable(tableId, true)
      ElMessage.warning('已强制删除，请记得为受影响的来宾重新安排座位')
      clearSelection()
      await loadLayout()
    } catch {
      // 用户取消，什么都不做
    }
  }
}

// ============================================
// 新增/编辑/删除场地元素
// ============================================
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
    await saveVenueElement({
      id: el.id,
      eventId,
      type: el.type,
      label: el.label,
      posX: el.posX,
      posY: el.posY,
      width: el.width,
      height: el.height,
      rotation: el.rotation
    })
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
})
</script>

<style scoped>
.seat-layout-page { display: flex; flex-direction: column; height: 100vh; background: #f5f7f9; }
.page-header { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: white; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 17px; color: #1f1f1f; }
.header-hint { margin-left: auto; font-size: 12px; color: #999; }

.workspace { display: flex; flex: 1; overflow: hidden; }

.toolbar { width: 200px; background: white; border-right: 1px solid #e8e8e8; padding: 20px; box-sizing: border-box; overflow-y: auto; }
.tool-section { margin-bottom: 24px; }
.tool-title { font-size: 13px; color: #999; margin-bottom: 10px; font-weight: 600; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #666; margin-bottom: 6px; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot.gold { background: #cca662; }
.dot.red { background: #ff4d4f; }
.dot.blue { background: #1890ff; }

.canvas-wrapper { flex: 1; display: flex; align-items: center; justify-content: center; padding: 24px; background: #eef0f2; overflow: auto; }
.canvas-inner {
  position: relative;
  width: 100%;
  max-width: 900px;
  /* 用padding-top百分比撑高，比aspect-ratio兼容性更好，不依赖CSS变量在flex布局里的计算 */
  height: 0;
  padding-top: calc(var(--canvas-h) / var(--canvas-w) * 100%);
  background: #1e1e22;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}
.canvas-inner > * { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.venue-svg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }

.svg-el { fill: #2c3e50; stroke: #3d4f61; stroke-width: 1.5; }
.svg-el-stage { fill: #611f1f; stroke: #ff4d4f; }
.svg-el-screen { fill: #2c3e50; stroke: #3498db; }
.svg-el-entrance, .svg-el-exit { fill: #2e4031; stroke: #27ae60; }
.svg-el-selected { stroke: #1890ff; stroke-width: 3; }
.svg-el-text { fill: #e8e8e8; font-size: 13px; font-weight: bold; }

.svg-table-circle { fill: #fcf5ed; stroke: #cca662; stroke-width: 2; transition: stroke 0.15s, fill 0.15s; }
.svg-table-full { stroke: #ff4d4f; }
.svg-table-selected { stroke: #1890ff; stroke-width: 4; fill: #e6f7ff; }
.svg-table-text { fill: #5c4033; font-size: 13px; font-weight: bold; }
.svg-table-sub { fill: #8a7365; font-size: 11px; }

.hotspot-layer { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.hotspot { position: absolute; cursor: move; }
.hotspot-circle { width: 90px; height: 90px; border-radius: 50%; transform: translate(-50%, -50%); }
.hotspot-circle:hover { background: rgba(24, 144, 255, 0.1); }
.hotspot-rect:hover { background: rgba(24, 144, 255, 0.08); outline: 1px dashed rgba(24, 144, 255, 0.4); }

.seat-tooltip-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; color: #333; }
.seat-tooltip-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.seat-chip { font-size: 11px; text-align: center; padding: 4px 2px; border-radius: 4px; line-height: 1.3; }
.seat-chip.free { background: #f6ffed; color: #52c41a; }
.seat-chip.occupied { background: #fff1f0; color: #f5222d; }
.seat-chip-name { display: block; font-size: 9px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.props-panel { width: 280px; background: white; border-left: 1px solid #e8e8e8; padding: 20px; box-sizing: border-box; overflow-y: auto; }
.panel-title { font-size: 15px; font-weight: 600; margin-bottom: 16px; color: #333; }
.panel-actions { margin-top: 8px; }
.form-tip { font-size: 12px; color: #999; margin-top: 4px; }
</style>
