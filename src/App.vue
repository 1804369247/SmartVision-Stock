<template>
  <div class="dashboard">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="logo-section">
        <span class="logo-icon">📦</span>
        <span class="logo-text">SmartVision Stock</span>
      </div>
      
      <div class="view-presets">
        <el-button 
          :type="currentView === 'global' ? 'primary' : 'default'" 
          size="small" 
          @click="setView('global')"
        >🌍 全局视图</el-button>
        <el-dropdown trigger="click" @command="(cmd) => setView('zone', cmd)">
          <el-button 
            :type="currentView === 'zone' ? 'primary' : 'default'" 
            size="small"
          >
            📌 分区视图 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="A">A区</el-dropdown-item>
              <el-dropdown-item command="B">B区</el-dropdown-item>
              <el-dropdown-item command="C">C区</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="(cmd) => setView('close', cmd)">
          <el-button 
            :type="currentView === 'close' ? 'primary' : 'default'" 
            size="small"
          >
            🔍 货架特写 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="A">A区</el-dropdown-item>
              <el-dropdown-item command="B">B区</el-dropdown-item>
              <el-dropdown-item command="C">C区</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="main-menu">
        <el-dropdown trigger="click" @command="(cmd) => switchModule('basic', cmd)">
          <el-button 
            :class="['menu-btn', { active: currentModule === 'basic' }]"
            size="small"
          >
            📋 基础档案 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="goods">📦 货物档案</el-dropdown-item>
              <el-dropdown-item command="locations">🏭 库位管理</el-dropdown-item>
              <el-dropdown-item command="suppliers">🏬 供应商管理</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-dropdown trigger="click" @command="(cmd) => switchModule('order', cmd)">
          <el-button 
            :class="['menu-btn', { active: currentModule === 'order' }]"
            size="small"
          >
            📦 出入库管理 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="inbound">📥 采购入库</el-dropdown-item>
              <el-dropdown-item command="outbound">📤 销售出库</el-dropdown-item>
              <el-dropdown-item command="inboundList">📋 入库单查询</el-dropdown-item>
              <el-dropdown-item command="outboundList">📋 出库单查询</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-dropdown trigger="click" @command="(cmd) => switchModule('inventory', cmd)">
          <el-button 
            :class="['menu-btn', { active: currentModule === 'inventory' }]"
            size="small"
          >
            📊 库存管理 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="query">🔍 库存查询</el-dropdown-item>
              <el-dropdown-item command="count">📝 库存盘点</el-dropdown-item>
              <el-dropdown-item command="warning">⚠️ 库存预警</el-dropdown-item>
              <el-dropdown-item command="transfer">🔄 库存调拨</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-dropdown trigger="click" @command="(cmd) => switchModule('report', cmd)">
          <el-button 
            :class="['menu-btn', { active: currentModule === 'report' }]"
            size="small"
          >
            📈 报表中心 <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="inventory">📦 库存台账</el-dropdown-item>
              <el-dropdown-item command="flow">📋 出入库流水</el-dropdown-item>
              <el-dropdown-item command="utilization">📊 库位利用率</el-dropdown-item>
              <el-dropdown-item command="alert">⚠️ 预警报表</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-button 
          :class="['menu-btn', { active: currentModule === 'system' }]"
          size="small" 
          @click="switchModule('system', '')"
        >
          ⚙️ 系统管理
        </el-button>
      </div>

      <div class="top-actions">
        <el-button size="small" @click="guideModalRef.show()" class="help-btn">🎓 新手引导</el-button>
        <el-button size="small" @click="helpCenterRef.show()" class="help-btn">❓ 帮助中心</el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧3D场景 -->
      <div class="left-panel">
        <div ref="containerRef" class="scene-container" @dblclick="handleDoubleClick">
          <!-- 加载动画 -->
          <div v-if="sceneLoading" class="loading-overlay">
            <div class="loader"></div>
            <span>场景加载中...</span>
          </div>

          <!-- 悬浮信息面板 -->
          <div v-if="hoverInfo.visible" class="hover-panel" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
            <div class="hover-header">
              <span class="hover-code">{{ hoverInfo.code }}</span>
              <span v-if="hoverInfo.frozen" class="hover-frozen">🔒 已冻结</span>
            </div>
            <div class="hover-status" :class="'status-' + hoverInfo.statusClass">{{ hoverInfo.statusText }}</div>
            <div class="hover-attr" v-if="hoverInfo.attribute !== 'NORMAL'">{{ hoverInfo.attributeText }}</div>
            <div v-if="hoverInfo.goodsName" class="hover-row">📦 {{ hoverInfo.goodsName }}</div>
            <div v-if="hoverInfo.goodsCategory" class="hover-row">🏷️ {{ hoverInfo.goodsCategory }}</div>
            <div v-if="hoverInfo.batchNo" class="hover-row">🔖 {{ hoverInfo.batchNo }}</div>
            <div v-if="hoverInfo.quantity" class="hover-row">📊 {{ hoverInfo.quantity }} 件</div>
            <div v-if="hoverInfo.supplier" class="hover-row">🏭 {{ hoverInfo.supplier }}</div>
            <div v-if="hoverInfo.expiryDate" class="hover-row" :class="{ 'expiry-warning': hoverInfo.isExpiringSoon }">📅 到期: {{ formatDate(hoverInfo.expiryDate) }}</div>
            <div v-if="hoverInfo.storageRule" class="hover-row">📋 存放: {{ hoverInfo.storageRule }}</div>
            <div v-if="hoverInfo.frozenReason" class="hover-row frozen-reason">❄️ {{ hoverInfo.frozenReason }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <!-- 业务模块时显示返回3D按钮 -->
        <div v-if="currentModule" class="back-to-3d">
          <el-button type="primary" size="small" @click="goBackTo3D">
            <el-icon class="el-icon--left"><VideoPlay /></el-icon>
            返回 3D 视图
          </el-button>
        </div>
        
        <el-tabs v-model="activeTab" type="card" class="panel-tabs">
          <el-tab-pane label="数据看板" name="dashboard">
            <DataDashboard 
              ref="dashboardRef" 
              :locations="locationsList" 
              :goods="goodsList"
              @refresh="onRefreshData"
              @export="handleExport"
              @locate-warnings="locateWarnings"
              @locate-location="onLocateCell"
            />
          </el-tab-pane>
          <el-tab-pane label="历史记录" name="records">
            <HistoryPanel ref="historyPanelRef" @locate-cell="onLocateCell" />
          </el-tab-pane>
          <el-tab-pane label="系统设置" name="settings">
            <SettingsPanel
              @settings-change="onSettingsChange"
              @refresh-data="onRefreshData"
              @reset-camera="resetCamera"
            />
          </el-tab-pane>
          <el-tab-pane label="业务管理" name="module">
            <BasicManagement 
              v-if="currentModule === 'basic'" 
              :sub-page="moduleSubPage"
              @locate="onLocateCell"
            />
            <OrderManagement 
              v-else-if="currentModule === 'order'" 
              :sub-page="moduleSubPage"
            />
            <InventoryManagement 
              v-else-if="currentModule === 'inventory'" 
              :sub-page="moduleSubPage"
              :locations="locationsList"
              @locate="onLocateCell"
            />
            <ReportCenter 
              v-else-if="currentModule === 'report'" 
              :locations="locationsList"
            />
            <div v-else-if="currentModule === 'system'" class="report-placeholder">
              <div class="placeholder-icon">⚙️</div>
              <div class="placeholder-title">系统管理</div>
              <div class="placeholder-desc">用户权限、操作日志、系统配置</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 库位操作对话框 -->
    <el-dialog v-model="operationDialogVisible" title="库位操作" width="420px" @close="resetOperationForm">
      <el-form :model="operationForm" label-width="80px" size="small">
        <el-form-item label="库位编号">
          <el-tag type="primary">{{ selectedCell?.code }}</el-tag>
        </el-form-item>
        
        <el-form-item label="操作类型">
          <el-radio-group v-model="operationForm.type">
            <el-radio value="inbound" :disabled="selectedCell?.status !== 0">入库</el-radio>
            <el-radio value="outbound" :disabled="selectedCell?.status === 0">出库</el-radio>
            <el-radio value="move" :disabled="selectedCell?.status === 0">移库</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="operationForm.type === 'inbound'">
          <el-form-item label="货物" prop="goodsId">
            <el-select v-model="operationForm.goodsId" placeholder="请选择货物" style="width:100%">
              <el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="批次号" prop="batchNo">
            <el-input v-model="operationForm.batchNo" placeholder="请输入批次号" />
          </el-form-item>
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="operationForm.quantity" :min="1" :max="9999" style="width:100%" />
          </el-form-item>
        </template>

        <template v-else-if="operationForm.type === 'outbound'">
          <el-form-item label="货物名称">
            <span>{{ selectedCell?.goodsName }}</span>
          </el-form-item>
          <el-form-item label="批次号">
            <span>{{ selectedCell?.batchNo }}</span>
          </el-form-item>
          <el-form-item label="当前库存">
            <span>{{ selectedCell?.quantity }} 件</span>
            <el-tag v-if="selectedCell?.quantity <= systemSettings.warningThreshold" type="warning" size="small" style="margin-left:8px">低于预警</el-tag>
          </el-form-item>
          <el-form-item label="出库数量" prop="quantity">
            <el-input-number v-model="operationForm.quantity" :min="1" :max="selectedCell?.quantity || 1" style="width:100%" />
          </el-form-item>
        </template>

        <template v-else-if="operationForm.type === 'move'">
          <el-form-item label="货物信息">
            <div>{{ selectedCell?.goodsName }} - {{ selectedCell?.batchNo }}</div>
            <div style="color:#aaa; font-size:12px">数量: {{ selectedCell?.quantity }} 件</div>
          </el-form-item>
          <el-form-item label="目标库位" prop="targetLocationId">
            <el-select v-model="operationForm.targetLocationId" placeholder="请选择目标库位" style="width:100%">
              <el-option 
                v-for="loc in emptyLocations" 
                :key="loc.id" 
                :label="loc.locationCode" 
                :value="loc.id" 
                :disabled="loc.id === selectedCell?.locationId"
              />
            </el-select>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="operationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleOperation" :loading="processingOperation">
          {{ processingOperation ? '处理中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 导出报表对话框 -->
    <el-dialog v-model="exportDialogVisible" title="导出报表" width="450px">
      <div class="export-options">
        <div class="export-section">
          <h4>📊 选择报表类型</h4>
          <el-radio-group v-model="exportForm.type" class="export-type-group">
            <el-radio value="inventory">📦 库存报表</el-radio>
            <el-radio value="inout">📋 出入库记录</el-radio>
            <el-radio value="statistics">📈 统计报表</el-radio>
          </el-radio-group>
        </div>

        <div class="export-section" v-if="exportForm.type === 'inventory'">
          <h4>🔍 筛选条件（可选）</h4>
          <el-select v-model="exportForm.status" placeholder="状态筛选" clearable size="small" style="width: 100%; margin-bottom: 10px;">
            <el-option label="全部状态" value="" />
            <el-option label="空闲库位" value="0" />
            <el-option label="正常库存" value="1" />
            <el-option label="库存预警" value="2" />
            <el-option label="已冻结" value="frozen" />
          </el-select>
          <el-select v-model="exportForm.attribute" placeholder="库位属性筛选" clearable size="small" style="width: 100%; margin-bottom: 10px;">
            <el-option label="全部属性" value="" />
            <el-option label="普通库位" value="NORMAL" />
            <el-option label="冷藏库位" value="COLD" />
            <el-option label="危险品库位" value="DANGEROUS" />
            <el-option label="高价值库位" value="VALUABLE" />
          </el-select>
          <el-select v-model="exportForm.area" placeholder="分区筛选" clearable size="small" style="width: 100%;">
            <el-option label="全部分区" value="" />
            <el-option label="A区" value="A" />
            <el-option label="B区" value="B" />
            <el-option label="C区" value="C" />
          </el-select>
        </div>

        <div class="export-section" v-if="exportForm.type === 'inout'">
          <h4>📅 时间范围（可选）</h4>
          <el-date-picker
            v-model="exportForm.startTime"
            type="datetime"
            placeholder="开始时间"
            size="small"
            style="width: 48%; margin-right: 4%;"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-date-picker
            v-model="exportForm.endTime"
            type="datetime"
            placeholder="结束时间"
            size="small"
            style="width: 48%;"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-select v-model="exportForm.recordType" placeholder="记录类型" clearable size="small" style="width: 100%; margin-top: 10px;">
            <el-option label="全部类型" value="" />
            <el-option label="入库" value="in" />
            <el-option label="出库" value="out" />
            <el-option label="移库" value="move" />
          </el-select>
        </div>

        <div class="export-tip">
          <span>💡 导出格式: Excel (.xlsx)</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doExport" :loading="exporting">
          {{ exporting ? '导出中...' : '📥 导出' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新手引导 -->
    <GuideModal ref="guideModalRef" />
    
    <!-- 帮助中心 -->
    <HelpCenter ref="helpCenterRef" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, VideoPlay } from '@element-plus/icons-vue'
import { useScene } from './composables/useScene.js'
import { useLocationStore } from './stores/location'
import { useGoodsStore } from './stores/goods'
import { goodsApi, locationApi } from './api'
import { debounce, handleApiError } from './utils'
import HistoryPanel from './components/HistoryPanel.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import DataDashboard from './components/DataDashboard.vue'
import GuideModal from './components/GuideModal.vue'
import HelpCenter from './components/HelpCenter.vue'
import BasicManagement from './components/BasicManagement.vue'
import OrderManagement from './components/OrderManagement.vue'
import InventoryManagement from './components/InventoryManagement.vue'
import ReportCenter from './components/ReportCenter.vue'
import SkeletonTable from './components/SkeletonTable.vue'

const containerRef = ref(null)
const historyPanelRef = ref(null)
const dashboardRef = ref(null)
const guideModalRef = ref(null)
const helpCenterRef = ref(null)

const activeTab = ref('dashboard')
const currentView = ref('global')
const selectedZone = ref('B')
const sceneLoading = ref(true)
const currentModule = ref('')
const moduleSubPage = ref('')

const scene = useScene()

const hoverInfo = reactive({ 
  visible: false, x: 0, y: 0, code: '', statusText: '', statusClass: '', 
  goodsName: '', batchNo: '', quantity: 0,
  frozen: false, frozenReason: '', attribute: 'NORMAL', attributeText: '',
  supplier: '', expiryDate: '', inTime: '', goodsCategory: '', storageRule: '',
  isExpiringSoon: false
})

const locationStore = useLocationStore()
const goodsStore = useGoodsStore()
const goodsList = computed(() => goodsStore.goodsList)
const locationsList = computed(() => locationStore.locations)
const selectedCell = ref(null)
const dataLoading = ref(true)
const operationDialogVisible = ref(false)
const processingOperation = ref(false)

const operationForm = reactive({
  type: 'inbound',
  goodsId: null,
  batchNo: '',
  quantity: 1,
  targetLocationId: null
})

const systemSettings = reactive({ 
  warningThreshold: 10, 
  defaultCameraView: 'global', 
  soundEnabled: true 
})

const exportDialogVisible = ref(false)
const exporting = ref(false)
const exportForm = reactive({
  type: 'inventory',
  status: '',
  attribute: '',
  area: '',
  startTime: '',
  endTime: '',
  recordType: ''
})

const keysPressed = reactive({})
let ws = null

const loadGoods = debounce(async () => {
  try {
    dataLoading.value = true
    await goodsStore.fetchGoods()
  } catch (e) {
    console.error('loadGoods error:', e)
    handleApiError(e)
  } finally {
    dataLoading.value = false
  }
}, 300)

const loadLocations = debounce(async () => {
  try {
    dataLoading.value = true
    await locationStore.fetchLocations()
    scene.updateLocations(locationStore.locations, systemSettings.warningThreshold)
    if (dashboardRef.value) dashboardRef.value.updateStats()
  } catch (e) {
    console.error('loadLocations error:', e)
    handleApiError(e)
  } finally {
    dataLoading.value = false
  }
}, 300)

const initWebSocket = () => {
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(`${proto}//${location.host}/topic/stock`)
  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'stockChange') {
        scene.updateLocationStatus(data.locationId, data.newStatus, systemSettings.warningThreshold)
        refreshHistory()
        loadLocations()
      }
    } catch (err) { console.error('ws parse error:', err) }
  }
  ws.onclose = () => setTimeout(initWebSocket, 3000)
}

const refreshHistory = () => {
  if (historyPanelRef.value) historyPanelRef.value.refresh()
}

const onHover = (userData, event) => {
  if (!userData) {
    hoverInfo.visible = false
    return
  }
  const statusKey = userData.frozen ? 4 : (userData.quantity > 0 && userData.quantity <= systemSettings.warningThreshold ? 2 : userData.status)
  hoverInfo.visible = true
  hoverInfo.x = Math.min(event.clientX + 15, window.innerWidth - 180)
  hoverInfo.y = Math.max(event.clientY - 80, 10)
  hoverInfo.code = userData.code
  hoverInfo.statusText = userData.frozen ? '已冻结' : ['空库位', '正常库存', '库存预警', '异常库位'][statusKey] || '未知'
  hoverInfo.statusClass = userData.frozen ? 'frozen' : ['empty', 'normal', 'warning', 'error'][statusKey] || 'empty'
  hoverInfo.goodsName = userData.goodsName || ''
  hoverInfo.batchNo = userData.batchNo || ''
  hoverInfo.quantity = userData.quantity || 0
  hoverInfo.frozen = userData.frozen || false
  hoverInfo.frozenReason = userData.frozenReason || ''
  hoverInfo.attribute = userData.attribute || 'NORMAL'
  hoverInfo.attributeText = { NORMAL: '', COLD: '❄️ 冷藏库', DANGEROUS: '⚠️ 危险品', VALUABLE: '💎 高价值' }[userData.attribute] || ''
  hoverInfo.supplier = userData.supplier || ''
  hoverInfo.expiryDate = userData.expiryDate || ''
  hoverInfo.inTime = userData.inTime || ''
  hoverInfo.goodsCategory = userData.goodsCategory || ''
  hoverInfo.storageRule = userData.storageRule || ''
  if (userData.expiryDate) {
    const expiryTime = new Date(userData.expiryDate).getTime()
    const now = Date.now()
    hoverInfo.isExpiringSoon = expiryTime - now < 7 * 24 * 60 * 60 * 1000
  } else {
    hoverInfo.isExpiringSoon = false
  }
}

const onClickCell = (userData) => {
  selectedCell.value = userData
  operationDialogVisible.value = true
  operationForm.type = userData.status === 0 ? 'inbound' : (userData.frozen ? 'unfreeze' : 'outbound')
  operationForm.goodsId = null
  operationForm.batchNo = ''
  operationForm.quantity = 1
  operationForm.targetLocationId = null
}

const handleDoubleClick = (event) => {
  const rect = containerRef.value?.getBoundingClientRect()
  if (!rect) return
  scene.handleClick(event, containerRef.value, onClickCell)
}

const onLocateCell = (locationCode) => {
  scene.highlightCell(locationCode)
}

const resetOperationForm = () => {
  operationForm.type = 'inbound'
  operationForm.goodsId = null
  operationForm.batchNo = ''
  operationForm.quantity = 1
  operationForm.targetLocationId = null
  selectedCell.value = null
}

const handleOperation = async () => {
  if (!selectedCell.value) return
  
  processingOperation.value = true
  try {
    let url = ''
    let body = {}
    
    if (operationForm.type === 'inbound') {
      if (!operationForm.goodsId || !operationForm.batchNo) {
        ElMessage.warning('请填写完整信息')
        return
      }
      url = '/api/inbound'
      body = { 
        goodsId: operationForm.goodsId, 
        batchNo: operationForm.batchNo, 
        quantity: operationForm.quantity, 
        locationId: selectedCell.value.locationId 
      }
    } else if (operationForm.type === 'outbound') {
      if (operationForm.quantity > (selectedCell.value.quantity || 0)) {
        ElMessage.warning('出库数量不能大于库存')
        return
      }
      url = '/api/outbound'
      body = { 
        goodsInstanceId: selectedCell.value.goodsInstanceId, 
        quantity: operationForm.quantity 
      }
    } else if (operationForm.type === 'move') {
      if (!operationForm.targetLocationId) {
        ElMessage.warning('请选择目标库位')
        return
      }
      url = '/api/move'
      body = { 
        goodsInstanceId: selectedCell.value.goodsInstanceId, 
        targetLocationId: operationForm.targetLocationId 
      }
    }

    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const result = await res.json()
    
    if (result.success) {
      ElMessage.success(operationForm.type === 'inbound' ? '入库成功' : operationForm.type === 'outbound' ? '出库成功' : '移库成功')
      operationDialogVisible.value = false
      await loadLocations()
      refreshHistory()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (e) {
    console.error('operation error:', e)
    ElMessage.error('操作失败，请检查后端服务')
  } finally {
    processingOperation.value = false
  }
}

const emptyLocations = computed(() => {
  return locationsList.value.filter(l => l.status === 0 && l.id !== selectedCell.value?.locationId)
})

const loadSystemSettings = () => {
  try {
    const s = localStorage.getItem('smartvision-settings')
    if (s) Object.assign(systemSettings, JSON.parse(s))
  } catch (e) { /* ignore */ }
}

const onSettingsChange = (s) => {
  Object.assign(systemSettings, s)
  loadLocations()
}

const onRefreshData = debounce(async () => {
  try {
    await Promise.all([locationStore.refresh(), goodsStore.refresh()])
    scene.updateLocations(locationStore.locations, systemSettings.warningThreshold)
    refreshHistory()
    if (dashboardRef.value) dashboardRef.value.updateStats()
    ElMessage.success('数据刷新完成')
  } catch (e) {
    console.error('refresh error:', e)
    handleApiError(e)
  }
}, 500)

const handleExport = () => {
  exportDialogVisible.value = true
  exportForm.type = 'inventory'
  exportForm.status = ''
  exportForm.attribute = ''
  exportForm.area = ''
  exportForm.startTime = ''
  exportForm.endTime = ''
  exportForm.recordType = ''
}

const doExport = async () => {
  exporting.value = true
  try {
    let url = ''
    const params = new URLSearchParams()
    
    if (exportForm.type === 'inventory') {
      url = '/api/export/inventory'
      if (exportForm.status) params.append('status', exportForm.status)
      if (exportForm.attribute) params.append('attribute', exportForm.attribute)
      if (exportForm.area) params.append('area', exportForm.area)
    } else if (exportForm.type === 'inout') {
      url = '/api/export/inout-records'
      if (exportForm.startTime) params.append('startTime', exportForm.startTime)
      if (exportForm.endTime) params.append('endTime', exportForm.endTime)
      if (exportForm.recordType) params.append('type', exportForm.recordType)
    } else if (exportForm.type === 'statistics') {
      url = '/api/export/statistics'
    }
    
    const fullUrl = params.toString() ? `${url}?${params.toString()}` : url
    
    const link = document.createElement('a')
    link.href = fullUrl
    link.download = ''
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('报表导出成功，请查看下载文件')
    exportDialogVisible.value = false
  } catch (e) {
    console.error('export error:', e)
    ElMessage.error('导出失败，请检查后端服务')
  } finally {
    exporting.value = false
  }
}

const locateWarnings = () => {
  const warningLocs = locationsList.value.filter(l => l.status === 2 || l.status === 3)
  if (warningLocs.length > 0) {
    scene.highlightCell(warningLocs[0].locationCode)
    if (warningLocs.length > 1) {
      ElMessage.info(`找到 ${warningLocs.length} 个预警库位，已定位第一个`)
    }
  } else {
    ElMessage.success('暂无预警库位')
  }
}

const setView = (view, zone = null) => {
  currentView.value = view
  if (zone) selectedZone.value = zone
  scene.resetCamera(view, zone)
}

const switchModule = (module, subPage) => {
  if (currentModule.value === module && moduleSubPage.value === subPage) {
    currentModule.value = ''
    moduleSubPage.value = ''
    activeTab.value = 'dashboard'
  } else {
    currentModule.value = module
    moduleSubPage.value = subPage
    activeTab.value = 'module'
  }
}

const goBackTo3D = () => {
  currentModule.value = ''
  moduleSubPage.value = ''
  activeTab.value = 'dashboard'
}

// 显示3D联动提示
const show3DUpdateNotification = (action) => {
  ElMessage.success(`${action}成功，已更新3D库存模型！`)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const resetCamera = () => {
  scene.resetCamera(systemSettings.defaultCameraView)
  currentView.value = systemSettings.defaultCameraView
}

const handleKeyDown = (e) => {
  const target = e.target
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
    return
  }
  
  keysPressed[e.key.toLowerCase()] = true
  
  if (e.key === ' ') {
    e.preventDefault()
    resetCamera()
    return
  }
  
  if (e.key === '1') { setView('global'); return }
  if (e.key === '2') { setView('zone'); return }
  if (e.key === '3') { setView('close'); return }
}

const handleKeyUp = (e) => {
  keysPressed[e.key.toLowerCase()] = false
}

const handleMouseMove = (e) => {
  scene.handleMouseMove(e, containerRef.value, onHover)
}

const handleMouseClick = (e) => {
  scene.handleClick(e, containerRef.value, onClickCell)
}

const handleResize = () => {
  scene.handleResize(containerRef.value)
}

onMounted(async () => {
  loadSystemSettings()

  if (containerRef.value) {
    const canvas = scene.initScene(containerRef.value, systemSettings.defaultCameraView)
    containerRef.value.appendChild(canvas)
    scene.startAnimate()
    sceneLoading.value = false

    await Promise.all([loadGoods(), loadLocations()])
    initWebSocket()

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('click', handleMouseClick)
    window.addEventListener('resize', handleResize)
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keyup', handleKeyUp)
  }
})

onUnmounted(() => {
  scene.dispose()
  if (ws) ws.close()
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('click', handleMouseClick)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { width: 100%; height: 100%; overflow: hidden; background: #0d0d12; }

.dashboard { display: flex; flex-direction: column; width: 100%; height: 100%; }

.main-content { display: flex; flex: 1; min-height: 0; }

.top-bar {
  height: 50px;
  background: rgba(15, 15, 25, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(100, 100, 150, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 100;
}

.logo-section { display: flex; align-items: center; gap: 10px; }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 16px; font-weight: bold; color: #88ccff; }

.view-presets { display: flex; gap: 8px; }
.view-presets .el-button { border-radius: 20px; padding: 4px 16px; }

.top-actions { display: flex; gap: 8px; }

.left-panel { flex: 1; position: relative; overflow: hidden; }
.scene-container { width: 100%; height: 100%; position: relative; }

.loading-overlay {
  position: absolute; inset: 0; z-index: 200;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  background: rgba(13,13,18,0.95); color: #88ccff; gap: 16px; font-size: 14px;
}
.loader {
  width: 40px; height: 40px; border: 3px solid #333; border-top-color: #88ccff;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.hover-panel {
  position: fixed; z-index: 1000; pointer-events: none;
  background: rgba(10,10,20,0.94); backdrop-filter: blur(10px);
  border-radius: 8px; padding: 12px 16px; min-width: 170px;
  border: 1px solid rgba(100,100,150,0.25); box-shadow: 0 4px 20px rgba(0,0,0,0.4);
}
.hover-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.hover-code { font-size: 14px; font-weight: bold; color: #fff; }
.hover-frozen { font-size: 11px; color: #00bfff; background: rgba(0,191,255,0.2); padding: 2px 6px; border-radius: 3px; }
.hover-attr { font-size: 11px; color: #ffaa00; background: rgba(255,170,0,0.2); padding: 2px 6px; border-radius: 3px; margin-bottom: 4px; }
.hover-status { font-size: 11px; margin-bottom: 6px; padding: 2px 8px; border-radius: 3px; display: inline-block; }
.hover-status.status-empty { background: #3a3a5c; color: #aaa; }
.hover-status.status-normal { background: #2d8c4e; color: #fff; }
.hover-status.status-warning { background: #e6a23c; color: #fff; }
.hover-status.status-error { background: #f56c6c; color: #fff; }
.hover-status.status-frozen { background: #00bfff; color: #fff; }
.hover-row { font-size: 12px; color: #bbb; margin-top: 4px; }
.hover-row.expiry-warning { color: #ff6b6b; font-weight: bold; }
.hover-row.frozen-reason { color: #00bfff; font-style: italic; }

.right-panel {
  width: 420px; min-width: 380px; height: 100%;
  background: rgba(15,15,25,0.95); backdrop-filter: blur(15px);
  border-left: 1px solid rgba(100,100,150,0.15);
  display: flex; flex-direction: column;
  box-shadow: -4px 0 20px rgba(0,0,0,0.25);
}
.panel-tabs { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.panel-tabs .el-tabs__content { flex: 1; overflow: auto; padding: 12px; }
.panel-tabs .el-tabs__header { padding: 0 12px; margin: 0; }

.el-dialog { border-radius: 12px; }
.el-dialog__header { background: rgba(20,20,30,0.8); border-radius: 12px 12px 0 0; }
.el-dialog__body { background: rgba(15,15,25,0.9); }
.el-dialog__footer { background: rgba(20,20,30,0.8); border-radius: 0 0 12px 12px; }

.export-options { padding: 10px 0; }
.export-section { margin-bottom: 16px; }
.export-section h4 { color: #ccc; font-size: 13px; margin-bottom: 10px; }
.export-type-group { display: flex; flex-direction: column; gap: 8px; }
.export-type-group .el-radio { margin-right: 0; }
.export-tip { padding: 10px; background: rgba(45,140,78,0.1); border-radius: 6px; color: #2d8c4e; font-size: 12px; }

.main-menu { display: flex; gap: 8px; }
.main-menu .el-button { border-radius: 20px; padding: 4px 16px; }
.menu-btn {
  background: rgba(40, 40, 60, 0.8);
  border-color: rgba(80, 80, 120, 0.5);
  color: #ccc;
  transition: all 0.3s ease;
}
.menu-btn:hover {
  background: rgba(60, 60, 90, 0.9);
  border-color: rgba(100, 100, 150, 0.6);
  color: #fff;
}
.menu-btn.active {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.25) 0%, rgba(102, 177, 255, 0.15) 100%);
  border-color: rgba(64, 158, 255, 0.6);
  color: #409eff;
  box-shadow: 0 0 12px rgba(64, 158, 255, 0.25);
}

.top-actions { display: flex; gap: 8px; }
.help-btn {
  background: rgba(60, 60, 90, 0.6);
  border-color: rgba(100, 100, 140, 0.3);
  color: #aaa;
}
.help-btn:hover {
  background: rgba(80, 80, 120, 0.8);
  color: #fff;
}

.back-to-3d {
  padding: 10px 12px 0;
  display: flex;
  justify-content: flex-start;
}
.back-to-3d .el-button {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.2) 0%, rgba(102, 177, 255, 0.1) 100%);
  border-color: rgba(64, 158, 255, 0.4);
  color: #88ccff;
}
.back-to-3d .el-button:hover {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.35) 0%, rgba(102, 177, 255, 0.2) 100%);
  color: #fff;
}

.report-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 400px; color: #666;
}
.placeholder-icon { font-size: 48px; margin-bottom: 16px; }
.placeholder-title { font-size: 16px; color: #ccc; margin-bottom: 8px; }
.placeholder-desc { font-size: 13px; }
</style>
