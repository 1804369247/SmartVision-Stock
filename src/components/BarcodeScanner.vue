<template>
  <div class="barcode-scanner">
    <!-- 扫码模式切换 -->
    <div class="mode-switch">
      <el-radio-group v-model="mode" @change="handleModeChange">
        <el-radio-button value="camera">
          <el-icon><VideoCamera /></el-icon>
          摄像头扫码
        </el-radio-button>
        <el-radio-button value="manual">
          <el-icon><Edit /></el-icon>
          手动输入
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 摄像头扫码模式 -->
    <div v-if="mode === 'camera'" class="camera-mode">
      <div class="scanner-container">
        <div ref="scannerRef" class="scanner-viewport"></div>
        
        <!-- 扫描框覆盖层 -->
        <div class="scan-overlay">
          <div class="scan-frame">
            <div class="scan-line"></div>
          </div>
          <div class="scan-tip">将条码放入框内，自动扫描</div>
        </div>
      </div>

      <div class="scanner-controls">
        <el-button 
          v-if="!scanning" 
          type="primary" 
          @click="startScanning"
          :loading="initializing"
        >
          <el-icon><VideoPlay /></el-icon>
          开始扫码
        </el-button>
        <el-button v-else type="danger" @click="stopScanning">
          <el-icon><VideoPause /></el-icon>
          停止扫码
        </el-button>
        
        <el-button @click="switchCamera" :disabled="!scanning">
          <el-icon><Switch /></el-icon>
          切换摄像头
        </el-button>
      </div>

      <!-- 扫描结果 -->
      <div v-if="scanResult" class="scan-result">
        <el-alert
          :title="scanResult.type === 'success' ? '扫描成功' : '扫描失败'"
          :type="scanResult.type"
          :description="scanResult.message"
          show-icon
          closable
          @close="scanResult = null"
        />
      </div>
    </div>

    <!-- 手动输入模式 -->
    <div v-if="mode === 'manual'" class="manual-mode">
      <el-input
        v-model="manualBarcode"
        placeholder="请输入条码"
        size="large"
        clearable
        @keyup.enter="handleManualInput"
      >
        <template #prepend>
          <el-icon><Barcode /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleManualInput" type="primary">
            查询
          </el-button>
        </template>
      </el-input>

      <!-- 查询结果 -->
      <div v-if="queryResult" class="query-result">
        <el-card v-if="queryResult.code === 200" class="result-card">
          <template #header>
            <div class="card-header">
              <span>{{ queryResult.data.type === 'goods' ? '📦 商品信息' : '📍 库位信息' }}</span>
            </div>
          </template>
          
          <div v-if="queryResult.data.type === 'goods'" class="goods-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="商品编码">{{ queryResult.data.code }}</el-descriptions-item>
              <el-descriptions-item label="商品名称">{{ queryResult.data.name }}</el-descriptions-item>
              <el-descriptions-item label="规格">{{ queryResult.data.spec }}</el-descriptions-item>
              <el-descriptions-item label="单位">{{ queryResult.data.unit }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ queryResult.data.category }}</el-descriptions-item>
              <el-descriptions-item label="预警数量">{{ queryResult.data.warningQuantity }}</el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div v-else class="location-info">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="库位编码">{{ queryResult.data.locationCode }}</el-descriptions-item>
              <el-descriptions-item label="分区">{{ queryResult.data.area }}区</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(queryResult.data.status)">
                  {{ getStatusText(queryResult.data.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="属性">
                <el-tag>{{ getAttributeText(queryResult.data.attribute) }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
        
        <el-alert
          v-else
          title="未找到结果"
          type="warning"
          :description="queryResult.message"
          show-icon
        />
      </div>
    </div>

    <!-- 扫描历史 -->
    <div v-if="scanHistory.length > 0" class="scan-history">
      <el-divider>扫描历史</el-divider>
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in scanHistory"
          :key="index"
          :timestamp="item.time"
          placement="top"
        >
          <el-card shadow="hover">
            <template #header>
              <span>📍 {{ item.barcode }}</span>
            </template>
            <div>{{ item.result.type === 'goods' ? '📦 商品: ' : '📍 库位: ' }}
              {{ item.result.type === 'goods' ? item.result.name : item.result.locationCode }}
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoCamera, Edit, VideoPlay, VideoPause, Switch } from '@element-plus/icons-vue'
import Quagga from '@ericblade/quagga2'
import { barcodeApi } from '@/api/barcode'

const emit = defineEmits(['scan-success', 'scan-error'])

const mode = ref('camera')
const scanning = ref(false)
const initializing = ref(false)
const scannerRef = ref(null)
const manualBarcode = ref('')
const scanResult = ref(null)
const queryResult = ref(null)
const scanHistory = ref([])
const currentCamera = ref('environment') // 'environment' 或 'user'

let quaggaInitialized = false
let switchTimer = null
let onDetectedHandler = null

// 条码检测回调（具名函数，用于注销）
const handleDetected = (data) => {
  if (data?.codeResult?.code) {
    handleScanSuccess(data.codeResult.code)
  }
}

// 移除 Quagga 检测监听器
const removeDetectedListener = () => {
  if (onDetectedHandler) {
    try {
      Quagga.offDetected(onDetectedHandler)
    } catch (e) {
      // Quagga 可能不支持 offDetected，忽略
    }
    onDetectedHandler = null
  }
}

// 开始扫码
const startScanning = async () => {
  initializing.value = true
  try {
    // 先清理旧监听器和旧实例
    removeDetectedListener()
    if (quaggaInitialized) {
      try { Quagga.stop() } catch (_) {}
      quaggaInitialized = false
    }

    await Quagga.init(
      {
        inputStream: {
          name: 'Live',
          type: 'LiveStream',
          target: scannerRef.value,
          constraints: {
            width: { min: 640 },
            height: { min: 480 },
            aspectRatio: { min: 1, max: 2 },
            facingMode: currentCamera.value
          }
        },
        decoder: {
          readers: [
            'code_128_reader',
            'ean_reader',
            'ean_8_reader',
            'code_39_reader',
            'upc_reader',
            'upc_e_reader',
            'codabar_reader',
            'itf_reader'
          ]
        },
        locate: true,
        numOfWorkers: 0 // 0 = 使用主线程（避免 Web Worker 问题）
      },
      (err) => {
        if (err) {
          console.error('Quagga init error:', err)
          ElMessage.error('摄像头初始化失败：' + err.message)
          initializing.value = false
          return
        }
        
        quaggaInitialized = true
        scanning.value = true
        initializing.value = false
        Quagga.start()

        // 注册检测回调（保存引用以便后续注销）
        onDetectedHandler = handleDetected
        Quagga.onDetected(onDetectedHandler)
      }
    )
  } catch (error) {
    console.error('Start scanning error:', error)
    ElMessage.error('启动扫码失败：' + error.message)
    initializing.value = false
  }
}

// 停止扫码
const stopScanning = () => {
  removeDetectedListener()
  if (quaggaInitialized) {
    try {
      Quagga.stop()
    } catch (_) {}
    quaggaInitialized = false
  }
  scanning.value = false
  initializing.value = false
}

// 切换摄像头
const switchCamera = () => {
  stopScanning()
  currentCamera.value = currentCamera.value === 'environment' ? 'user' : 'environment'
  if (switchTimer) clearTimeout(switchTimer)
  switchTimer = setTimeout(() => {
    startScanning()
  }, 500)
}

// 扫码成功
const handleScanSuccess = async (barcode) => {
  // 防止重复扫描
  if (scanResult.value && scanResult.value.barcode === barcode) {
    return
  }
  
  // 停止扫码
  stopScanning()
  
  // 查询条码
  try {
    const result = await barcodeApi.scanBarcode(barcode)
    
    scanResult.value = {
      type: 'success',
      barcode,
      message: `扫码成功！识别为：${result.data.type === 'goods' ? '商品' : '库位'}`,
      data: result.data
    }
    
    queryResult.value = result
    
    // 添加到历史（存 payload）
    scanHistory.value.unshift({
      barcode,
      result: result.data,
      time: new Date().toLocaleTimeString()
    })
    
    // 限制历史记录数量
    if (scanHistory.value.length > 10) {
      scanHistory.value = scanHistory.value.slice(0, 10)
    }
    
    emit('scan-success', { barcode, result: result.data })
    
    ElMessage.success('扫码成功！')
  } catch (error) {
    scanResult.value = {
      type: 'error',
      barcode,
      message: '查询失败：' + (error.response?.data?.message || error.message)
    }
    
    emit('scan-error', { barcode, error })
    
    ElMessage.error('查询失败')
  }
}

// 手动输入处理
const handleManualInput = async () => {
  if (!manualBarcode.value || manualBarcode.value.trim() === '') {
    ElMessage.warning('请输入条码')
    return
  }
  
  const barcode = manualBarcode.value.trim()
  
  try {
    const result = await barcodeApi.scanBarcode(barcode)
    
    queryResult.value = result.data
    scanResult.value = {
      type: 'success',
      barcode,
      message: `查询成功！识别为：${result.data.type === 'goods' ? '商品' : '库位'}`,
      data: result.data
    }
    
    // 添加到历史
    scanHistory.value.unshift({
      barcode,
      result: result.data,
      time: new Date().toLocaleTimeString()
    })
    
    if (scanHistory.value.length > 10) {
      scanHistory.value = scanHistory.value.slice(0, 10)
    }
    
    emit('scan-success', { barcode, result: result.data })
    
    ElMessage.success('查询成功！')
  } catch (error) {
    queryResult.value = error.response?.data || null
    scanResult.value = {
      type: 'error',
      barcode,
      message: '查询失败：' + (error.response?.data?.message || error.message)
    }
    
    emit('scan-error', { barcode, error })
    
    ElMessage.error('查询失败')
  }
}

// 模式切换
const handleModeChange = () => {
  if (mode.value === 'manual') {
    stopScanning()
  }
}

// 状态类型
const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '空库位', 1: '正常', 2: '预警', 3: '异常' }
  return texts[status] || '未知'
}

const getAttributeText = (attribute) => {
  const texts = { NORMAL: '普通', COLD: '冷藏', DANGEROUS: '危险品', VALUABLE: '高价值' }
  return texts[attribute] || '普通'
}

// 组件销毁时停止扫码
onUnmounted(() => {
  stopScanning()
  if (switchTimer) clearTimeout(switchTimer)
})
</script>

<style scoped>
.barcode-scanner {
  max-width: 100%;
  margin: 0 auto;
}

.mode-switch {
  text-align: center;
  margin-bottom: 20px;
}

.camera-mode {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.scanner-container {
  position: relative;
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.scanner-viewport {
  width: 100%;
  min-height: 300px;
}

.scanner-viewport video {
  width: 100%;
  height: auto;
}

.scan-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.scan-frame {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 250px;
  height: 150px;
  border: 2px solid #409eff;
  border-radius: 8px;
  overflow: hidden;
}

.scan-line {
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #409eff, transparent);
  animation: scan 2s linear infinite;
}

@keyframes scan {
  0% { transform: translateY(0); }
  100% { transform: translateY(150px); }
}

.scan-tip {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  white-space: nowrap;
}

.scanner-controls {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.scan-result {
  margin-top: 15px;
}

.manual-mode {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.query-result {
  margin-top: 20px;
}

.result-card {
  margin-top: 15px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.goods-info,
.location-info {
  padding: 10px 0;
}

.scan-history {
  margin-top: 30px;
}

.el-timeline {
  margin-top: 15px;
}
</style>
