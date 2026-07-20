<template>
  <div class="vision-capture">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="图片上传" name="upload">
        <div class="upload-area">
          <div class="upload-box" @click="triggerUpload" @drop.prevent="handleDrop" @dragover.prevent>
            <input ref="fileInput" type="file" accept="image/*" multiple @change="handleFileSelect" style="display: none" />
            <div class="upload-icon">📷</div>
            <div class="upload-text">点击或拖拽上传图片</div>
            <div class="upload-hint">支持 JPG、PNG、GIF 格式</div>
          </div>

          <div v-if="uploadedImages.length > 0" class="image-list">
            <div v-for="(image, index) in uploadedImages" :key="index" class="image-item">
              <img :src="image.url" :alt="image.name" />
              <div class="image-info">
                <span>{{ image.name }}</span>
                <span>{{ formatSize(image.size) }}</span>
              </div>
              <el-button size="small" type="danger" @click="removeImage(index)">删除</el-button>
            </div>
          </div>

          <div class="action-buttons">
            <el-button type="primary" @click="uploadAndRecognize" :disabled="uploadedImages.length === 0 || recognizing">
              {{ recognizing ? '识别中...' : '开始识别' }}
            </el-button>
            <el-button @click="clearImages" :disabled="recognizing">清空</el-button>
          </div>

          <!-- 识别结果区域 -->
          <div v-if="recognitionResult" class="recognition-result">
            <div class="result-header">
              <h4>🔍 识别结果</h4>
              <el-tag :type="recognitionResult.code === 200 ? 'success' : 'danger'">
                {{ recognitionResult.code === 200 ? '识别完成' : '识别失败' }}
              </el-tag>
            </div>
            <div v-if="recognitionResult.code === 200 && recognitionResult.data" class="result-body">
              <!-- 货物识别结果 -->
              <div v-if="recognitionResult.data.items?.length" class="result-items">
                <h5>检测到的货物 ({{ recognitionResult.data.items.length }})</h5>
                <el-table :data="recognitionResult.data.items" border size="small" max-height="300">
                  <el-table-column prop="name" label="货物名称" />
                  <el-table-column prop="confidence" label="置信度" width="90">
                    <template #default="scope">
                      <el-progress :percentage="Math.round((scope.row.confidence || 0) * 100)" :stroke-width="10" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="quantity" label="估算数量" width="80" />
                  <el-table-column prop="locationCode" label="建议库位" width="100" />
                </el-table>
              </div>
              <!-- 文字/标签识别结果 -->
              <div v-else-if="recognitionResult.data.text" class="result-text">
                <h5>文字/标签内容</h5>
                <pre>{{ recognitionResult.data.text }}</pre>
              </div>
              <!-- 原始结果 -->
              <div v-else class="result-raw">
                <pre>{{ JSON.stringify(recognitionResult.data, null, 2) }}</pre>
              </div>

              <!-- 操作按钮 -->
              <div class="result-actions">
                <el-button type="primary" size="small" @click="addToInventory" v-if="recognitionResult.code === 200">
                  📦 入库到系统
                </el-button>
                <el-button type="success" size="small" @click="analyzeShelf" v-if="uploadId">
                  📊 货架分析
                </el-button>
                <el-button size="small" @click="recognitionResult = null">关闭结果</el-button>
              </div>
            </div>
            <div v-else class="error-msg">
              <p>{{ recognitionResult.message || recognitionResult.error || '识别失败，请重试' }}</p>
              <el-button type="primary" size="small" @click="uploadAndRecognize">重新识别</el-button>
            </div>
          </div>

          <!-- 识别进度 -->
          <div v-if="recognizing" class="recognizing-status">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在识别图片，请稍候...</span>
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="摄像头采集" name="camera">
        <div class="camera-area">
          <div class="camera-preview" v-if="!cameraStarted">
            <div class="camera-icon">🎥</div>
            <div class="camera-text">点击开始使用摄像头</div>
            <el-button type="primary" @click="startCamera">开启摄像头</el-button>
          </div>
          
          <div v-else class="camera-feed">
            <video ref="videoElement" autoplay playsinline></video>
            <div class="camera-controls">
              <el-button type="primary" @click="captureImage">📸 拍照</el-button>
              <el-button @click="stopCamera">停止</el-button>
            </div>
          </div>
          
          <div v-if="capturedImages.length > 0" class="captured-list">
            <h4>已拍摄图片</h4>
            <div class="image-list">
              <div v-for="(image, index) in capturedImages" :key="index" class="image-item">
                <img :src="image" />
                <el-button size="small" type="danger" @click="removeCaptured(index)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { visionApi } from '../api/vision'

const activeTab = ref('upload')
const fileInput = ref(null)
const videoElement = ref(null)
const uploadedImages = ref([])
const capturedImages = ref([])
const cameraStarted = ref(false)

// 识别相关状态
const recognizing = ref(false)
const recognitionResult = ref(null)
const uploadId = ref(null)

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  files.forEach(file => {
    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file)
      uploadedImages.value.push({
        file,
        url,
        name: file.name,
        size: file.size
      })
    }
  })
}

const handleDrop = (event) => {
  const files = Array.from(event.dataTransfer.files)
  files.forEach(file => {
    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file)
      uploadedImages.value.push({
        file,
        url,
        name: file.name,
        size: file.size
      })
    }
  })
}

const removeImage = (index) => {
  const image = uploadedImages.value[index]
  URL.revokeObjectURL(image.url)
  uploadedImages.value.splice(index, 1)
}

const clearImages = () => {
  uploadedImages.value.forEach(img => URL.revokeObjectURL(img.url))
  uploadedImages.value = []
}

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/** 上传 + 识别（一步完成） */
const uploadAndRecognize = async () => {
  if (uploadedImages.value.length === 0) return

  recognizing.value = true
  recognitionResult.value = null

  try {
    // 第1步：上传图片
    ElMessage.info('正在上传图片...')
    const files = uploadedImages.value.map(img => img.file)
    const uploadResult = await visionApi.uploadMultipleImages(files)

    if (uploadResult.code !== 200 && !uploadResult?.uploadId) {
      throw new Error(uploadResult?.message || '上传失败')
    }

    uploadId.value = uploadResult.uploadId || uploadResult.data?.uploadId
    ElMessage.success(`图片上传成功，开始识别...`)

    // 第2步：调用识别接口
    const recogResult = await visionApi.recognize(uploadId.value, 4)

    // 处理识别结果
    recognitionResult.value = {
      code: 200,
      success: true,
      data: recogResult?.data || recogResult || { text: '识别完成，暂无结构化数据' },
      message: '识别成功',
    }
    ElMessage.success('图片识别完成！')
  } catch (e) {
    console.error('识别失败:', e)
    recognitionResult.value = {
      success: false,
      error: e.message || e.response?.data?.message || '识别失败，请重试',
      message: e.response?.data?.message || e.message || '识别失败',
    }
    ElMessage.error('识别失败: ' + (e.message || '未知错误'))
  } finally {
    recognizing.value = false
  }
}

/** 保留原上传方法（供单独使用） */
const uploadImages = async () => {
  if (uploadedImages.value.length === 0) return

  ElMessage.info('正在上传图片...')
  try {
    const files = uploadedImages.value.map(img => img.file)
    const result = await visionApi.uploadMultipleImages(files)

    if (result.code === 200) {
      uploadId.value = result.uploadId || result.data?.uploadId
      ElMessage.success('图片上传成功，上传批次ID: ' + (uploadId.value || 'N/A'))
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败，请检查网络或后端服务')
  }
}

/** 将识别结果入库 */
const addToInventory = () => {
  if (!recognitionResult.value?.data) return
  ElMessage.info('正在准备入库数据...')
  // 触发父组件事件，切换到入库表单并预填数据
  const items = recognitionResult.value.data.items
  if (items?.length) {
    // 通过 emit 或全局事件传递到 OrderManagement
    window.dispatchEvent(new CustomEvent('vision-add-to-inventory', {
      detail: { items: items.map(it => ({
        name: it.name || it.label || '未知货物',
        quantity: it.quantity || 1,
        confidence: it.confidence,
      })) }
    }))
    ElMessage.success(`已准备 ${items.length} 条货物入库信息，请在入库单据中确认提交`)
  } else {
    ElMessage.warning('没有可入库的货物数据')
  }
}

/** 货架分析 */
const analyzeShelf = async () => {
  if (!uploadId.value) return
  try {
    ElMessage.info('正在进行货架分析...')
    const result = await visionApi.analyzeShelf(uploadId.value)
    recognitionResult.value = {
      success: true,
      data: result.data || result,
      message: '货架分析完成',
    }
    ElMessage.success('货架分析完成！')
  } catch (e) {
    ElMessage.error('货架分析失败: ' + (e.message || ''))
  }
}

const startCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true })
    videoElement.value.srcObject = stream
    cameraStarted.value = true
  } catch (e) {
    ElMessage.error('无法访问摄像头')
  }
}

const stopCamera = () => {
  if (videoElement.value.srcObject) {
    videoElement.value.srcObject.getTracks().forEach(track => track.stop())
    videoElement.value.srcObject = null
  }
  cameraStarted.value = false
}

const captureImage = () => {
  const canvas = document.createElement('canvas')
  const video = videoElement.value
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0)
  
  const imageUrl = canvas.toDataURL('image/jpeg')
  capturedImages.value.push(imageUrl)
  ElMessage.success('拍照成功')
}

const removeCaptured = (index) => {
  capturedImages.value.splice(index, 1)
}
</script>

<style scoped>
.vision-capture {
  padding: 16px;
}

.upload-area {
  max-width: 100%;
  margin: 0 auto;
}

.upload-box {
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-box:hover {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.05);
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 18px;
  color: #666;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 14px;
  color: #999;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 24px;
}

.image-item {
  position: relative;
  width: 200px;
}

.image-item img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
}

.image-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.camera-area {
  max-width: 100%;
  margin: 0 auto;
}

.camera-preview {
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 64px;
  text-align: center;
}

.camera-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.camera-text {
  font-size: 18px;
  color: #666;
  margin-bottom: 16px;
}

.camera-feed video {
  width: 100%;
  border-radius: 12px;
  background: #000;
}

.camera-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.captured-list {
  margin-top: 24px;
}

.captured-list h4 {
  margin-bottom: 12px;
}

/* ====== 识别结果区域 ====== */
.recognition-result {
  margin-top: 24px;
  border: 1px solid #e6f7ff;
  background: #fafcff;
  border-radius: 12px;
  padding: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-header h4 {
  margin: 0;
  color: #1890ff;
  font-size: 16px;
}

.result-body {
  margin-bottom: 16px;
}

.result-items h5,
.result-text h5 {
  font-size: 14px;
  color: #333;
  margin-bottom: 10px;
}

.result-text pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: 13px;
}

.result-raw pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
  font-size: 12px;
}

.result-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.error-msg {
  text-align: center;
  padding: 20px;
  color: #f56c6c;
}

.error-msg p {
  margin-bottom: 12px;
}

.recognizing-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
  padding: 20px;
  color: #409eff;
  font-size: 15px;
}
</style>