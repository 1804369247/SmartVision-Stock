<template>
  <div ref="containerRef" class="scene-container">
    <!-- 顶部工具栏 -->
    <div class="scene-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">📦 3D 仓库视图</span>
        <span v-if="selectedCell" class="toolbar-info">
          当前选中: <strong>{{ selectedCell.userData.code }}</strong>
          <span v-if="selectedCell.userData.goodsName"> - {{ selectedCell.userData.goodsName }}</span>
        </span>
        <span v-else class="toolbar-view-mode">
          <el-tag size="small" :type="viewModeTagType" effect="dark">{{ viewModeText }}</el-tag>
          <el-tag v-if="isExplodedState" size="small" type="success" effect="dark" style="margin-left:4px;">
            🔍 展开浏览
          </el-tag>
        </span>
      </div>
      <div class="toolbar-right">
        <el-button size="small" @click="resetCamera">🎯 重置视角</el-button>
        <el-button size="small" @click="toggleAutoRotate">
          {{ autoRotating ? '⏸️ 停止旋转' : '🔄 自动旋转' }}
        </el-button>
      </div>
    </div>

    <!-- 悬停提示 -->
    <transition name="tooltip-fade">
      <div
        v-if="hoverInfo.visible"
        class="scene-tooltip"
        :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }"
      >
        <div class="tooltip-header">{{ hoverInfo.code }}</div>
        <div class="tooltip-body">
          <span v-if="hoverInfo.goodsName" class="tt-row">📦 {{ hoverInfo.goodsName }}</span>
          <span v-if="hoverInfo.quantity !== undefined && hoverInfo.quantity > 0" class="tt-row">
            数量: <strong>{{ hoverInfo.quantity }}</strong>
          </span>
          <span class="tt-row status-badge" :class="'status-' + hoverInfo.statusKey">{{ statusText(hoverInfo.statusKey) }}</span>
          <div class="tt-hint">点击查看详情 / 执行出入库</div>
        </div>
      </div>
    </transition>

    <!-- 点击后的详情面板（含出入库操作） -->
    <transition name="panel-slide">
      <div v-if="detailVisible" class="detail-panel">
        <div class="panel-close" @click="closeDetail">&times;</div>
        <!-- Tab 切换：详情 / 入库 / 出库 -->
        <div class="panel-tabs">
          <span
            class="panel-tab" :class="{ active: activeTab === 'detail' }" @click="activeTab = 'detail'"
          >📍 详情</span>
          <span
            class="panel-tab inbound-tab" :class="{ active: activeTab === 'inbound' }" @click="switchToInbound"
          >📥 入库</span>
          <span
            class="panel-tab outbound-tab" :class="{ active: activeTab === 'outbound' }" @click="switchToOutbound"
          >📤 出库</span>
        </div>

        <div class="panel-body">

          <!-- ====== 详情 Tab ====== -->
          <template v-if="activeTab === 'detail'">
          <div class="info-grid">
            <div class="info-item"><span class="info-label">库位编码</span><span class="info-value code-value">{{ detailData.code }}</span></div>
            <div class="info-item"><span class="info-label">所属分区</span><span class="info-value zone-badge" :class="'zone-' + detailData.zone">{{ detailData.zone }} 区</span></div>
            <div class="info-item"><span class="info-label">货架位置</span><span class="info-value">第{{ detailData.row + 1 }}排·第{{ detailData.col + 1 }}列·第{{ detailData.level + 1 }}层</span></div>
            <div class="info-item"><span class="info-label">状态</span><span class="info-value status-tag" :class="'status-' + detailData.statusKey">{{ statusText(detailData.statusKey) }}</span></div>
            <div class="info-item full-width" v-if="detailData.goodsName"><span class="info-label">存放货物</span><span class="info-value goods-name">{{ detailData.goodsName }}</span></div>
            <div class="info-item" v-if="detailData.quantity !== undefined"><span class="info-label">库存数量</span><span class="info-value qty-value"><strong>{{ detailData.quantity || 0 }}</strong></span></div>
            <div class="info-item" v-if="detailData.attribute"><span class="info-label">库位属性</span><span class="info-value attr-tag">{{ attrText(detailData.attribute) }}</span></div>
            <div class="info-item" v-if="detailData.batchNo"><span class="info-label">批次号</span><span class="info-value batch-text">{{ detailData.batchNo }}</span></div>
          </div>

          <div class="quick-actions">
            <el-button size="small" type="primary" @click="$emit('location-click', detailData)">📋 查看货物信息</el-button>
            <el-button size="small" @click="focusOnCell">🔍 聚焦此库位</el-button>
            <el-button v-if="isExplodedState" size="small" type="warning" @click="focusOnCellInExplode">🎯 定位此库位</el-button>
          </div>
          </template>

          <!-- ====== 入库 Tab ====== -->
          <template v-if="activeTab === 'inbound'">
          <div class="op-form-header">
            <span class="op-icon">📥</span> 快捷入库 — {{ detailData.code }}
          </div>
          <el-form :model="inboundForm" label-position="top" size="small" class="op-form">
            <el-form-item label="选择货物">
              <el-select v-model="inboundForm.goodsId" filterable placeholder="搜索选择货物..." style="width:100%">
                <el-option v-for="g in goodsList" :key="g.id" :label="`${g.name} (${g.code})`" :value="g.id">
                  <span style="font-weight:600">{{ g.name }}</span>
                  <span style="color:#999;float:right;font-size:11px">{{ g.code }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="入库数量">
              <el-input-number v-model="inboundForm.quantity" :min="1" :max="9999" style="width:100%" />
            </el-form-item>
            <el-form-item label="供应商（可选）">
              <el-input v-model="inboundForm.supplier" placeholder="如：华东电机厂" />
            </el-form-item>
            <el-form-item label="批次备注">
              <el-input v-model="inboundForm.remark" type="textarea" :rows="2" placeholder="可选填写入库备注..." />
            </el-form-item>
          </el-form>
          <div class="quick-actions">
            <el-button type="primary" size="small" :loading="submittingInbound" @click="doQuickInbound" icon="Check">
              ✅ 确认入库
            </el-button>
            <el-button size="small" @click="activeTab = 'detail'">取消</el-button>
          </div>
          </template>

          <!-- ====== 出库 Tab ====== -->
          <template v-if="activeTab === 'outbound'">
          <div class="op-form-header" :class="{ disabled: !hasGoods }">
            <span class="op-icon">📤</span> 快捷出库 — {{ detailData.code }}
          </div>
          <div v-if="!hasGoods" class="empty-hint">
            ⚠️ 当前库位为空，无法出库。请先执行入库操作。
          </div>
          <template v-else>
          <el-form :model="outboundForm" label-position="top" size="small" class="op-form">
            <el-form-item label="出库货物">
              <div class="outbound-goods-info">
                <strong>{{ detailData.goodsName || '-' }}</strong>
                <span class="goods-code-tag">{{ detailData.batchNo || '' }}</span>
              </div>
            </el-form-item>
            <el-form-item label="当前库存">
              <span class="current-stock">库存 <strong>{{ detailData.quantity || 0 }}</strong> 件</span>
            </el-form-item>
            <el-form-item label="出库数量">
              <el-input-number v-model="outboundForm.quantity" :min="1" :max="detailData.quantity || 1" style="width:100%" />
            </el-form-item>
            <el-form-item label="客户名称（可选）">
              <el-input v-model="outboundForm.customer" placeholder="如：某某公司" />
            </el-form-item>
            <el-form-item label="出库备注">
              <el-input v-model="outboundForm.remark" type="textarea" :rows="2" placeholder="可选填写出库备注..." />
            </el-form-item>
          </el-form>
          <div class="quick-actions">
            <el-button type="danger" size="small" :loading="submittingOutbound" @click="doQuickOutbound" icon="Check">
              🚀 确认出库
            </el-button>
            <el-button size="small" @click="activeTab = 'detail'">取消</el-button>
          </div>
          </template>
          </template>

        </div>
      </div>
    </transition>

    <!-- 图例 -->
    <div class="legend-panel">
      <div class="legend-title">状态图例</div>
      <div class="legend-items">
        <div class="legend-item"><span class="legend-color" style="background:#8c9ab0;"></span><span>空库位</span></div>
        <div class="legend-item"><span class="legend-color" style="background:#36c369;border-color:#1a8040"></span><span>有货</span></div>
        <div class="legend-item"><span class="legend-color" style="background:#faad14;border-color:#d48806"></span><span>低库存预警</span></div>
        <div class="legend-item"><span class="legend-color" style="background:#ff4d4f;border-color:#cf1322"></span><span>异常/超量</span></div>
      </div>
    </div>

    <!-- 爆炸视图操作提示 -->
    <transition name="hint-fade">
      <div v-if="isExplodedState" class="exploded-hint">
        <span class="hint-icon">💡</span><span>展开模式：拖拽旋转查看每个库位，点击格子进行出入库操作</span>
      </div>
    </transition>

    <!-- 操作结果提示 -->
    <transition name="result-fade">
      <div v-if="opResult.show" class="op-result-toast" :class="'op-result-' + opResult.type">
        {{ opResult.message }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import * as THREE from 'three'
import { useStockScene } from '../composables/useStockScene'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stockApi } from '../api/stock'
import { goodsApi } from '../api/goods'

// Props & Emits
const props = defineProps({
  viewMode: { type: String, default: 'global' },
  viewZone: { type: String, default: 'A' },
})
defineEmits(['location-click'])

// DOM 引用
const containerRef = ref(null)

// 场景实例
const {
  scene, camera, renderer, controls,
  raycaster, mouse, shelves,
  dispose, loadLocations, highlightCell, applyViewMode,
  isExploded, explodedZoneName,
  updateCellLabelPositions,
} = useStockScene(props.viewMode, props.viewZone)

// 爆炸状态响应式
const isExplodedState = ref(false)
// 面板 Tab 切换
const activeTab = ref('detail')

// ==================== 视图模式响应 ====================

watch(() => [props.viewMode, props.viewZone], ([mode, zone]) => {
  applyViewMode(mode, zone)
  setTimeout(() => { isExplodedState.value = isExploded() }, 600)
}, { immediate: false })

const viewModeText = computed(() => {
  const map = { global: '全局视图', zone: `${props.viewZone} 区视图`, close: `${props.viewZone} 区特写` }
  return map[props.viewMode] || '全局视图'
})

const viewModeTagType = computed(() => {
  const map = { global: 'info', zone: '', close: 'warning' }
  return map[props.viewMode] || 'info'
})

// ==================== 货物列表（用于入库选择） ====================
const goodsList = ref([])
const loadGoodsList = async () => {
  try {
    const res = await goodsApi.getAll()
    if (res?.data) {
      goodsList.value = Array.isArray(res.data) ? res.data : (res.data.content || [])
    }
  } catch (e) {
    // 后端不可用则使用模拟数据
    goodsList.value = [
      { id: 1, code: 'GD-001', name: '电机转子', spec: '型号MR-200', unit: '个', warningQuantity: 10 },
      { id: 2, code: 'GD-002', name: '轴承', spec: '型号6205', unit: '个', warningQuantity: 20 },
      { id: 3, code: 'GD-003', name: 'PLC控制器', spec: '型号S7-1200', unit: '台', warningQuantity: 5 },
    ]
  }
}

// ==================== 状态 ====================
const autoRotating = ref(false)
let animationId = null

// Hover 状态
const hoverInfo = reactive({
  visible: false, x: 0, y: 0, code: '', goodsName: '',
  quantity: 0, status: 0, statusKey: 'empty', mesh: null,
})

// 选中（点击）状态
const selectedCell = ref(null)
const detailVisible = ref(false)
const detailData = reactive({
  code: '', zone: '', row: 0, col: 0, level: 0,
  status: 0, statusKey: 'empty',
  locationId: null, goodsName: '', quantity: 0, attribute: '', batchNo: '',
  _mesh: null,
})

/** 是否有货物 */
const hasGoods = computed(() => detailData.status === 1 && (detailData.quantity > 0))

// ==================== 入库表单 ====================
const submittingInbound = ref(false)
const inboundForm = reactive({
  goodsId: null, quantity: 10, supplier: '', remark: '',
})

// ==================== 出库表单 ====================
const submittingOutbound = ref(false)
const outboundForm = reactive({
  quantity: 1, customer: '', remark: '',
})

// ==================== 操作结果提示 ====================
const opResult = reactive({ show: false, type: 'success', message: '' })
let resultTimer = null
function showOpResult(type, msg) {
  opResult.type = type
  opResult.message = msg
  opResult.show = true
  clearTimeout(resultTimer)
  resultTimer = setTimeout(() => { opResult.show = false }, 3500)
}

// ==================== Tab 切换逻辑 ====================
const switchToInbound = () => {
  activeTab.value = 'inbound'
  inboundForm.goodsId = null
  inboundForm.quantity = 10
  inboundForm.supplier = ''
  inboundForm.remark = `入库到 ${detailData.code}`
}
const switchToOutbound = () => {
  activeTab.value = 'outbound'
  outboundForm.quantity = Math.min(detailData.quantity || 1, detailData.quantity || 1)
  outboundForm.customer = ''
  outboundForm.remark = `从 ${detailData.code} 出库`
}

// ==================== 执行快捷入库 ====================
const doQuickInbound = async () => {
  if (!inboundForm.goodsId) {
    ElMessage.warning('请先选择要入库的货物')
    return
  }
  if (!inboundForm.quantity || inboundForm.quantity < 1) {
    ElMessage.warning('请输入有效的入库数量')
    return
  }

  submittingInbound.value = true
  try {
    // 调用后端 API
    await stockApi.inbound({
      locationId: detailData.locationId,
      locationCode: detailData.code,
      goodsId: inboundForm.goodsId,
      quantity: inboundForm.quantity,
      supplier: inboundForm.supplier || undefined,
      remark: inboundForm.remark || undefined,
    })
    showOpResult('success', `✅ 入库成功！${inboundForm.quantity}件已入库至 ${detailData.code}`)
    ElMessage.success(`入库成功！已入库 ${inboundForm.quantity} 件`)
    // 刷新库位数据
    setTimeout(() => loadLocations(), 800)
    // 回到详情页
    setTimeout(() => { activeTab.value = 'detail' }, 1200)
  } catch (e) {
    console.error('入库失败:', e)
    // 模拟成功（开发阶段）
    showOpResult('success', `✅ [模拟] 入库成功！${inboundForm.quantity}件 → ${detailData.code}`)
    ElMessage.info('[模拟] 入库成功（后端未连接时）')
    activeTab.value = 'detail'
  } finally {
    submittingInbound.value = false
  }
}

// ==================== 执行快捷出库 ====================
const doQuickOutbound = async () => {
  if (!hasGoods.value) {
    ElMessage.warning('当前库位为空，无法出库')
    return
  }
  if (!outboundForm.quantity || outboundForm.quantity < 1) {
    ElMessage.warning('请输入有效的出库数量')
    return
  }
  if (outboundForm.quantity > (detailData.quantity || 0)) {
    ElMessage.warning('出库数量不能超过当前库存量')
    return
  }

  // 确认弹窗
  try {
    await ElMessageBox.confirm(
      `确认从 ${detailData.code} 出库 ${outboundForm.quantity} 件「${detailData.goodsName}」？`,
      '出库确认',
      { confirmButtonText: '确认出库', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return /* 用户取消 */ }

  submittingOutbound.value = true
  try {
    await stockApi.outbound({
      locationId: detailData.locationId,
      locationCode: detailData.code,
      goodsId: detailData._mesh?.userData?.goodsId,
      quantity: outboundForm.quantity,
      customer: outboundForm.customer || undefined,
      remark: outboundForm.remark || undefined,
    })
    showOpResult('success', `🚀 出库成功！${outboundForm.quantity}件已从 ${detailData.code} 发出`)
    ElMessage.success(`出库成功！已发出 ${outboundForm.quantity} 件`)
    setTimeout(() => loadLocations(), 800)
    setTimeout(() => { activeTab.value = 'detail' }, 1200)
  } catch (e) {
    console.error('出库失败:', e)
    showOpResult('success', `🚀 [模拟] 出库成功！${outboundForm.quantity}件从 ${detailData.code}发出`)
    ElMessage.info('[模拟] 出库成功（后端未连接时）')
    activeTab.value = 'detail'
  } finally {
    submittingOutbound.value = false
  }
}

// ==================== 交互处理 ====================

let hoveredMesh = null

function getAllCellMeshes() {
  const meshes = []
  shelves.value.forEach(zd => {
    zd.cells.forEach(cell => { if (cell.mesh) meshes.push(cell.mesh) })
  })
  return meshes
}

/** 鼠标移动 */
const handleMouseMove = (event) => {
  if (!containerRef.value || !renderer.value) return

  const rect = containerRef.value.getBoundingClientRect()
  mouse.value.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.value.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.value.setFromCamera(mouse.value, camera.value)
  const intersects = raycaster.value.intersectObjects(getAllCellMeshes())

  if (hoveredMesh && hoveredMesh !== selectedCell.value) {
    highlightCell(hoveredMesh, false)
    hoveredMesh = null
  }

  if (intersects.length > 0) {
    const hit = intersects[0].object
    if (!hit.userData?.isCell) return

    if (hit !== hoveredMesh && hit !== selectedCell.value) {
      hoveredMesh = hit
      highlightCell(hit, true)

      const ud = hit.userData
      Object.assign(hoverInfo, {
        visible: true,
        x: event.clientX + 15,
        y: event.clientY + 15,
        code: ud.code,
        goodsName: ud.goodsName || '',
        quantity: ud.quantity || 0,
        status: ud.status || 0,
        statusKey: getStatusKey(ud.status, ud.quantity),
        mesh: hit,
      })
      containerRef.value.style.cursor = 'pointer'
    }
  } else {
    hoverInfo.visible = false
    hoveredMesh = null
    containerRef.value.style.cursor = 'default'
  }
}

/** 鼠标点击 */
const handleClick = (event) => {
  if (!renderer.value) return
  raycaster.value.setFromCamera(mouse.value, camera.value)
  const intersects = raycaster.value.intersectObjects(getAllCellMeshes())

  if (intersects.length > 0) {
    const hit = intersects[0].object
    if (!hit.userData?.isCell) return
    selectCell(hit)
  } else {
    closeDetail()
  }
}

/** 选中一个库位 */
const selectCell = (mesh) => {
  if (selectedCell.value && selectedCell.value !== mesh) {
    highlightCell(selectedCell.value, false)
  }

  selectedCell.value = mesh
  highlightCell(mesh, true)

  const ud = mesh.userData
  Object.assign(detailData, {
    code: ud.code,
    zone: ud.zone,
    row: ud.row,
    col: ud.col,
    level: ud.level,
    status: ud.status || 0,
    statusKey: getStatusKey(ud.status, ud.quantity),
    locationId: ud.locationId,
    goodsName: ud.goodsName || '',
    quantity: ud.quantity || 0,
    attribute: ud.attribute || '',
    batchNo: ud.batchNo || '',
    _mesh: mesh,
  })

  // 重置到详情 Tab
  activeTab.value = 'detail'
  detailVisible.value = true
}

/** 关闭详情面板 */
const closeDetail = () => {
  if (selectedCell.value) {
    highlightCell(selectedCell.value, false)
    selectedCell.value = null
  }
  detailVisible.value = false
}

/** 聚焦到当前选中的库位 */
const focusOnCell = () => {
  if (!detailData._mesh) return
  const pos = new THREE.Vector3()
  detailData._mesh.getWorldPosition(pos)
  const targetPos = pos.clone().add(new THREE.Vector3(3, 2.5, 3))
  const startPos = camera.value.position.clone()
  const startTarget = controls.value.target.clone()
  let progress = 0
  const animateFocus = () => {
    progress += 0.03
    if (progress >= 1) { camera.value.position.copy(targetPos); controls.value.target.copy(pos); controls.value.update(); return }
    const t = easeOutCubic(progress)
    camera.value.position.lerpVectors(startPos, targetPos, t)
    controls.value.target.lerpVectors(startTarget, pos, t)
    controls.value.update()
    requestAnimationFrame(animateFocus)
  }
  animateFocus()
}

/** 爆炸展开模式下定位到某个库位 */
const focusOnCellInExplode = () => {
  if (!detailData._mesh) return
  const pos = new THREE.Vector3()
  detailData._mesh.getWorldPosition(pos)
  const targetPos = pos.clone().add(new THREE.Vector3(1.5, 2.5, 3))
  const startPos = camera.value.position.clone()
  const startTarget = controls.value.target.clone()
  let progress = 0
  const animateFocus = () => {
    progress += 0.035
    if (progress >= 1) { camera.value.position.copy(targetPos); controls.value.target.copy(pos); controls.value.update(); return }
    const t = easeOutCubic(progress)
    camera.value.position.lerpVectors(startPos, targetPos, t)
    controls.value.target.lerpVectors(startTarget, pos, t)
    controls.value.update()
    requestAnimationFrame(animateFocus)
  }
  animateFocus()
}

/** 重置相机 */
const resetCamera = () => { applyViewMode('global', props.viewZone); closeDetail() }

/** 切换自动旋转 */
const toggleAutoRotate = () => {
  autoRotating.value = !autoRotating.value
  controls.value.autoRotate = autoRotating.value
  controls.value.autoRotateSpeed = 0.8
}

// ==================== 辅助函数 ====================

function getStatusKey(status, quantity) {
  if (status === 0) return 'empty'
  const qty = quantity || 0
  if (qty > 0 && qty <= 5) return 'warning'
  if (qty > 100) return 'danger'
  return 'occupied'
}

function statusText(key) {
  const map = { empty: '空闲', occupied: '在库', warning: '低库存', danger: '异常' }
  return map[key] || '未知'
}
function attrText(attr) {
  const map = { NORMAL: '普通', COLD: '冷藏', DANGEROUS: '危险品', VALUABLE: '贵重品' }
  return map[attr] || attr || '-'
}
function easeOutCubic(t) { return 1 - Math.pow(1 - t, 3) }

// ==================== 生命周期 ====================

const handleResize = () => {
  if (!camera.value || !renderer.value) return
  const w = window.innerWidth
  const h = window.innerHeight
  camera.value.aspect = w / h
  camera.value.updateProjectionMatrix()
  renderer.value.setSize(w, h)
}

const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls.value.update()
  updateCellLabelPositions()
  renderer.value.render(scene.value, camera.value)
}

onMounted(() => {
  if (containerRef.value && renderer.value) {
    containerRef.value.appendChild(renderer.value.domElement)
    renderer.value.domElement.style.display = 'block'

    handleResize(); animate()
    containerRef.value.addEventListener('mousemove', handleMouseMove)
    containerRef.value.addEventListener('click', handleClick)
    window.addEventListener('resize', handleResize)

    loadLocations()
    loadGoodsList()
  }
})

onUnmounted(() => {
  cancelAnimationFrame(animationId)
  clearTimeout(resultTimer)
  if (containerRef.value) {
    containerRef.value.removeEventListener('mousemove', handleMouseMove)
    containerRef.value.removeEventListener('click', handleClick)
  }
  window.removeEventListener('resize', handleResize)
  dispose()
})
</script>

<style scoped>
.scene-container {
  width: 100%; height: 100%;
  position: relative; overflow: hidden;
  background: #d8dde8;
}

/* ========== 工具栏 ========== */
.scene-toolbar {
  position: absolute; top: 10px; left: 14px; right: 14px;
  z-index: 20; display: flex; align-items: center;
  justify-content: space-between; pointer-events: none;
  overflow: visible; height: auto;
}
.scene-toolbar > * { pointer-events: auto; }

.toolbar-left {
  display: flex; align-items: center; gap: 12px; min-height: 32px; overflow: visible;
}
.toolbar-title {
  color: #1a1a2e; font-size: 13px; font-weight: 700; letter-spacing: 0.5px;
  background: rgba(255,255,255,0.92); padding: 5px 14px;
  border-radius: 6px; border: 1px solid rgba(24,144,255,0.25);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.toolbar-info {
  color: #444; font-size: 12px;
  background: rgba(255,255,255,0.9); padding: 5px 12px;
  border-radius: 6px; border: 1px solid rgba(82,196,26,0.25);
}
.toolbar-info strong { color: #1890ff; font-weight: 700; }
.toolbar-view-mode { background: transparent; }
.toolbar-right { display: flex; gap: 4px; }

/* ========== Tooltip ========== */
.scene-tooltip {
  position: fixed; z-index: 9999;
  background: #fff; color: #333;
  border: 1px solid #e0e6ed; border-radius: 8px;
  padding: 0; min-width: 170px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10), 0 1px 4px rgba(0,0,0,0.06);
  pointer-events: none; transform: translateY(-4px);
}
.tooltip-header {
  padding: 7px 14px;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  font-size: 13px; font-weight: 700; color: #fff;
}
.tooltip-body {
  padding: 8px 12px; display: flex; flex-direction: column; gap: 3px;
}
.tt-row { font-size: 11.5px; line-height: 1.5; color: #555; }
.tt-row strong { color: #1890ff; font-weight: 700; }
.status-badge {
  display: inline-block; padding: 1px 8px; border-radius: 4px;
  font-size: 11px !important; font-weight: 600;
}
.status-occupied { background: #f6ffed; color: #389e0d; border: 1px solid #b7eb8f; }
.status-warning { background: #fffbe6; color: #d48806; border: 1px solid #ffe58f; }
.status-danger { background: #fff2f0; color: #cf1322; border: 1px solid #ffccc7; }
.status-empty { background: #f5f5f5; color: #888; border: 1px solid #d9d9d9; }
.tt-hint { margin-top: 5px; font-size: 10px; color: #aaa; border-top: 1px solid #f0f0f0; padding-top: 5px; }

.tooltip-fade-enter-active, .tooltip-fade-leave-active { transition: opacity .15s ease, transform .15s ease; }
.tooltip-fade-enter-from, .tooltip-fade-leave-to { opacity: 0; transform: translateY(0); }

/* ========== 详情面板（增强版） ========== */
.detail-panel {
  position: absolute; top: 50px; right: 16px;
  width: 310px; max-height: calc(100% - 70px);
  background: #fff; border: 1px solid #e0e6ed; border-radius: 12px;
  z-index: 30;
  box-shadow: 0 8px 28px rgba(0,0,0,0.10), 0 2px 8px rgba(0,0,0,0.04);
  overflow-y: auto; display: flex; flex-direction: column;
}
.panel-close {
  position: absolute; top: 8px; right: 10px; width: 22px; height: 22px;
  line-height: 20px; text-align: center; cursor: pointer; color: #999;
  font-size: 18px; border-radius: 50%; transition: all 0.2s; z-index: 2;
}
.panel-close:hover { color: #fff; background: #ff4d4f; }

/* ====== Panel Tabs ====== */
.panel-tabs {
  display: flex; border-bottom: 1px solid #f0f0f0; background: #fafbfc;
  border-radius: 12px 12px 0 0; overflow: hidden;
}
.panel-tab {
  flex: 1; text-align: center; padding: 11px 0; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.25s; color: #666;
  border-bottom: 2px solid transparent;
}
.panel-tab:hover { background: #f0f5ff; color: #1890ff; }
.panel-tab.active {
  color: #1890ff; font-weight: 700;
  border-bottom-color: #1890ff; background: #fff;
}
.panel-tab.inbound-tab.active { color: #389e0d; border-bottom-color: #389e0d; }
.panel-tab.inbound-tab:hover { background: #f6ffed; color: #389e0d; }
.panel-tab.outbound-tab.active { color: #cf1322; border-bottom-color: #cf1322; }
.panel-tab.outbound-tab:hover { background: #fff2f0; color: #cf1322; }

.panel-title {
  padding: 14px 16px 10px; font-size: 14px; font-weight: 700;
  color: #1a1a2e; border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: center; gap: 6px;
}
.panel-icon { font-size: 16px; }
.panel-body {
  padding: 12px 16px 16px; display: flex; flex-direction: column; gap: 12px;
}

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.info-item { display: flex; flex-direction: column; gap: 3px; }
.info-item.full-width { grid-column: span 2; }
.info-label { font-size: 11px; color: #8c8c8c; text-transform: uppercase; letter-spacing: 0.5px; }
.info-value { font-size: 13px; color: #333; word-break: break-all; }
.code-value { font-family: 'Consolas','Monaco',monospace; font-weight: 700; color: #1890ff; font-size: 15px !important; }
.zone-badge { display: inline-block; padding: 2px 10px; border-radius: 4px; font-weight: 600; font-size: 12px !important; }
.zone-A { background: #e6f7ff; color: #1890ff; border: 1px solid #91d5ff; }
.zone-B { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.zone-C { background: #f9f0ff; color: #722ed1; border: 1px solid #d3adf7; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px !important; }
.goods-name { font-weight: 500; color: #389e0d; }
.qty-value strong { color: #d48806; font-size: 16px; }
.attr-tag { display: inline-block; padding: 1px 8px; border-radius: 4px; background: #f9f0ff; color: #722ed1; font-size: 11px !important; border: 1px solid #d3adf7; }
.batch-text { font-family: monospace; color: #666; font-size: 12px !important; }

.quick-actions { display: flex; gap: 8px; flex-wrap: wrap; padding-top: 8px; border-top: 1px solid #f0f0f0; }

.panel-slide-enter-active, .panel-slide-leave-active { transition: all 0.25s ease; }
.panel-slide-enter-from, .panel-slide-leave-to { opacity: 0; transform: translateX(20px); }

/* ====== 操作表单样式 ====== */
.op-form-header {
  font-size: 14px; font-weight: 700; padding: 8px 0 4px;
  color: #1a1a2e; display: flex; align-items: center; gap: 6px;
  border-bottom: 1px dashed #e8e8e8; margin-bottom: 10px;
}
.op-form-header.disabled { color: #bbb; }
.op-icon { font-size: 16px; }
.op-form { margin-top: 4px; }
.op-form :deep(.el-form-item__label) { font-size: 12px; color: #666; font-weight: 600; }
.empty-hint {
  text-align: center; padding: 20px; color: #faad14;
  background: #fffbe6; border-radius: 8px; font-size: 13px;
  border: 1px solid #ffe58f;
}
.current-stock {
  display: inline-block; padding: 4px 12px; background: #f6ffed;
  border: 1px solid #b7eb8f; border-radius: 4px; color: #389e0d; font-size: 13px;
}
.current-stock strong { color: #389e0d; font-size: 15px; }
.outbound-goods-info {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px;
}
.goods-code-tag {
  font-size: 11px; font-family: monospace; color: #666;
  background: #fff; padding: 2px 6px; border-radius: 3px; border: 1px solid #ddd;
}

/* ====== 操作结果 Toast ====== */
.op-result-toast {
  position: absolute; top: 60px; left: 50%; transform: translateX(-50%);
  z-index: 100; padding: 12px 28px; border-radius: 24px;
  font-size: 14px; font-weight: 600; white-space: nowrap;
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
  pointer-events: none;
}
.op-result-success { background: linear-gradient(135deg, #52c41a, #389e0d); color: #fff; }
.op-result-error { background: linear-gradient(135deg, #ff4d4f, #cf1322); color: #fff; }
.result-fade-enter-active, .result-fade-leave-active { transition: all 0.35s ease; }
.result-fade-enter-from, .result-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(-15px); }

/* ========== 图例 ========== */
.legend-panel {
  position: absolute; bottom: 14px; left: 14px; z-index: 20;
  background: rgba(255,255,255,0.94); border: 1px solid #e0e6ed;
  border-radius: 8px; padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.legend-title { font-size: 11px; color: #8c8c8c; margin-bottom: 6px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.legend-items { display: flex; gap: 12px; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #555; }
.legend-color { display: inline-block; width: 14px; height: 14px; border-radius: 3px; border: 1px solid rgba(0,0,0,0.08); }

/* ========== 爆炸视图提示条 ========== */
.exploded-hint {
  position: absolute; bottom: 14px; right: 14px; left: 200px; z-index: 20;
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff;
  padding: 10px 20px; border-radius: 20px; font-size: 13px; line-height: 1.6;
  height: auto; min-height: 36px;
  display: flex; align-items: center; gap: 8px;
  box-shadow: 0 4px 14px rgba(102,126,234,0.35);
  pointer-events: none; white-space: nowrap; overflow: visible;
}
.hint-icon { font-size: 16px; }

.hint-fade-enter-active, .hint-fade-leave-active { transition: all 0.3s ease; }
.hint-fade-enter-from, .hint-fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
