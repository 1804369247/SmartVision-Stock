<template>
  <div class="vision-result">
    <div class="result-header">
      <el-input v-model="uploadIdInput" placeholder="输入上传批次ID" size="small" style="width: 220px; margin-right: 10px;" />
      <el-button type="primary" @click="startRecognition">开始识别</el-button>
      <el-button @click="clearResult">清空结果</el-button>
    </div>
    
    <div v-if="recognitionResult" class="result-content">
      <div class="result-summary">
        <h3>📊 识别结果概览</h3>
        <div class="summary-grid">
          <div class="summary-item">
            <div class="summary-value">{{ recognitionResult.warehouse?.shelves?.length || 0 }}</div>
            <div class="summary-label">货架数量</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ recognitionResult.warehouse?.goods?.length || 0 }}</div>
            <div class="summary-label">货物种类</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ recognitionResult.warehouse?.width }}m × {{ recognitionResult.warehouse?.length }}m</div>
            <div class="summary-label">仓库尺寸</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ recognitionResult.angleCount }}</div>
            <div class="summary-label">拍摄角度</div>
          </div>
        </div>
      </div>
      
      <div class="result-section">
        <h3>🏗️ 货架信息</h3>
        <el-table :data="recognitionResult.warehouse?.shelves || []" border>
          <el-table-column prop="id" label="货架ID" />
          <el-table-column prop="type" label="类型" />
          <el-table-column prop="x" label="X坐标" />
          <el-table-column prop="y" label="Y坐标" />
          <el-table-column prop="z" label="Z坐标" />
          <el-table-column prop="width" label="宽度(m)" />
          <el-table-column prop="height" label="高度(m)" />
          <el-table-column prop="layers" label="层数" />
        </el-table>
      </div>
      
      <div class="result-section">
        <h3>📦 货物分布</h3>
        <el-table :data="recognitionResult.warehouse?.goods || []" border>
          <el-table-column prop="id" label="货物ID" />
          <el-table-column prop="name" label="货物名称" />
          <el-table-column prop="shelfId" label="所在货架" />
          <el-table-column prop="layer" label="所在层" />
          <el-table-column prop="quantity" label="数量" />
        </el-table>
      </div>
      
      <div class="result-section">
        <h3>🛣️ 通道信息</h3>
        <div class="passage-info">
          <div class="passage-item">
            <span class="passage-label">主通道宽度:</span>
            <span class="passage-value">{{ recognitionResult.warehouse?.passages?.mainWidth }}米</span>
          </div>
          <div class="passage-item">
            <span class="passage-label">侧通道宽度:</span>
            <span class="passage-value">{{ recognitionResult.warehouse?.passages?.sideWidth }}米</span>
          </div>
        </div>
      </div>
      
      <div class="action-section">
        <el-button type="success" @click="startReconstruction">🎯 开始三维重建</el-button>
        <el-button @click="exportResult">📥 导出数据</el-button>
      </div>
    </div>
    
    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <div class="empty-text">点击"开始识别"按钮进行仓库识别</div>
      <div class="empty-hint">系统将自动识别货架、货物和通道信息</div>
    </div>
    
    <el-dialog title="三维重建进度" :visible="showReconstruction" @close="showReconstruction = false">
      <div v-if="reconstructionProgress" class="reconstruction-progress">
        <div class="progress-info">
          <span>任务ID: {{ reconstructionProgress.taskId }}</span>
          <span>状态: {{ getStatusText(reconstructionProgress.status) }}</span>
        </div>
        <el-progress :percentage="reconstructionProgress.progress" :status="getProgressStatus(reconstructionProgress.progress)" />
        <div v-if="reconstructionProgress.modelId" class="model-info">
          <span>模型ID: {{ reconstructionProgress.modelId }}</span>
          <el-button size="small" type="primary" @click="viewModel">查看模型</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { visionApi } from '../api/vision'

const recognitionResult = ref(null)
const showReconstruction = ref(false)
const reconstructionProgress = ref(null)
const uploadIdInput = ref('')
let progressInterval = null

const startRecognition = async () => {
  const uploadId = uploadIdInput.value.trim()
  if (!uploadId) {
    ElMessage.warning('请先上传图片或输入上传批次ID')
    return
  }
  ElMessage.info('正在进行仓库识别...')
  
  try {
    const result = await visionApi.recognize(uploadId, 4)
    
    if (result.code === 200) {
      recognitionResult.value = result
      ElMessage.success('识别完成')
    } else {
      ElMessage.error(result.message || '识别失败')
    }
  } catch (e) {
    ElMessage.error('识别失败，请检查网络或后端服务')
  }
}

const clearResult = () => {
  recognitionResult.value = null
}

const startReconstruction = async () => {
  const uploadId = uploadIdInput.value.trim()
  if (!uploadId) {
    ElMessage.warning('请先上传图片或输入上传批次ID')
    return
  }
  try {
    const result = await visionApi.reconstruct(uploadId)
    
    if (result.code === 200) {
      showReconstruction.value = true
      pollProgress(result.taskId)
    } else {
      ElMessage.error(result.message || '重建启动失败')
    }
  } catch (e) {
    ElMessage.error('重建启动失败，请检查网络或后端服务')
  }
}

const pollProgress = async (taskId) => {
  progressInterval = setInterval(async () => {
    try {
      const result = await visionApi.getReconstructionProgress(taskId)
      
      if (result.code === 200 || result.taskId) {
        reconstructionProgress.value = result
        
        if (result.status === 'COMPLETED' || result.status === 'FAILED' || result.status === 'CANCELLED') {
          clearInterval(progressInterval)
          if (result.status === 'COMPLETED') {
            ElMessage.success('三维重建完成')
          } else if (result.status === 'FAILED') {
            ElMessage.error('三维重建失败')
          }
        }
      }
    } catch (e) {
      clearInterval(progressInterval)
    }
  }, 500)
}

const getStatusText = (status) => {
  const statusMap = {
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getProgressStatus = (progress) => {
  if (progress >= 100) return 'success'
  return 'active'
}

const exportResult = () => {
  const data = JSON.stringify(recognitionResult.value, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'warehouse_recognition.json'
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

const viewModel = () => {
  showReconstruction.value = false
  ElMessage.info('即将跳转到模型预览页面')
}

onUnmounted(() => {
  if (progressInterval) {
    clearInterval(progressInterval)
    progressInterval = null
  }
})
</script>

<style scoped>
.vision-result {
  padding: 16px;
}

.result-header {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.result-content {
  max-width: 100%;
}

.result-summary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.result-summary h3 {
  color: white;
  margin-bottom: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.summary-item {
  text-align: center;
}

.summary-value {
  font-size: 32px;
  font-weight: bold;
  color: white;
}

.summary-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
}

.result-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.result-section h3 {
  margin-bottom: 16px;
  color: #333;
}

.passage-info {
  display: flex;
  gap: 32px;
}

.passage-item {
  display: flex;
  gap: 8px;
}

.passage-label {
  color: #666;
}

.passage-value {
  font-weight: bold;
  color: #333;
}

.action-section {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 12px;
}

.empty-state {
  text-align: center;
  padding: 64px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  color: #666;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #999;
}

.reconstruction-progress {
  padding: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.model-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
</style>