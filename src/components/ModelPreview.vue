<template>
  <div class="model-preview">
    <div class="preview-header">
      <el-input placeholder="搜索模型" v-model="searchName" @keyup.enter="loadModels" class="search-input" />
      <el-button type="primary" @click="loadModels">搜索</el-button>
    </div>

    <div class="preview-content">
      <div class="model-list">
        <h3>模型列表</h3>
        <el-table :data="models" border @row-click="selectModel" highlight-current-row>
          <el-table-column prop="name" label="模型名称" />
          <el-table-column prop="format" label="格式" />
          <el-table-column prop="size" label="大小" />
          <el-table-column prop="createdAt" label="创建时间" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click.stop="deleteModel(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="model-viewer">
        <div class="viewer-header">
          <h3>{{ selectedModel?.name || '3D模型预览' }}</h3>
          <div class="viewer-controls">
            <el-button-group>
              <el-button size="small" @click="toggleAutoRotate">
                {{ autoRotate ? '⏸ 停止' : '🔄 旋转' }}
              </el-button>
              <el-button size="small" @click="zoomIn">🔍+</el-button>
              <el-button size="small" @click="zoomOut">🔍-</el-button>
              <el-button size="small" @click="resetView">↺ 重置</el-button>
            </el-button-group>
          </div>
        </div>

        <div class="viewer-canvas" ref="canvasContainer">
          <div v-if="!selectedModel" class="empty-viewer">
            <div class="empty-icon">👆</div>
            <div class="empty-text">从左侧选择一个模型</div>
          </div>
        </div>

        <div class="model-properties" v-if="selectedModel">
          <h4>模型属性</h4>
          <div class="property-list">
            <div class="property-item">
              <span class="property-label">名称:</span>
              <span>{{ selectedModel.name }}</span>
            </div>
            <div class="property-item">
              <span class="property-label">ID:</span>
              <span>{{ selectedModel.id }}</span>
            </div>
            <div class="property-item">
              <span class="property-label">格式:</span>
              <span>{{ selectedModel.format }}</span>
            </div>
            <div class="property-item">
              <span class="property-label">大小:</span>
              <span>{{ formatSize(selectedModel.size) }}</span>
            </div>
            <div class="property-item">
              <span class="property-label">版本:</span>
              <span>{{ selectedModel.version || '1.0' }}</span>
            </div>
            <div class="property-item">
              <span class="property-label">创建时间:</span>
              <span>{{ selectedModel.createdAt }}</span>
            </div>
          </div>
          <div class="model-actions">
            <el-button type="primary" size="small" @click="exportModel">📥 导出模型</el-button>
            <el-button size="small" @click="optimizeModel">⚙️ 优化模型</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog title="优化模型" v-model="showOptimize" width="400px">
      <el-form :model="optimizeForm" label-width="100px">
        <el-form-item label="质量等级">
          <el-slider v-model="optimizeForm.quality" :min="10" :max="100" :step="10" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOptimize = false">取消</el-button>
        <el-button type="primary" @click="doOptimize">确认优化</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as THREE from 'three'
import { modelApi, visionApi } from '../api'

// ---- 数据 ----
const searchName = ref('')
const models = ref([])
const selectedModel = ref(null)
const showOptimize = ref(false)
const autoRotate = ref(true)
const canvasContainer = ref(null)

const optimizeForm = reactive({ quality: 80 })

// ---- Three.js 相关 ----
let scene, camera, renderer, modelGroup, animationId
let currentZoom = 5

const initThreeJS = () => {
  if (!canvasContainer.value) return

  const container = canvasContainer.value
  const width = container.clientWidth
  const height = container.clientHeight

  // 场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xf0f2f5)

  // 相机
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100)
  camera.position.set(3, 2, 5)
  camera.lookAt(0, 0, 0)

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  container.appendChild(renderer.domElement)

  // 光照
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(5, 10, 7)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  // 地面网格
  const gridHelper = new THREE.GridHelper(10, 20, 0xcccccc, 0xe5e5e5)
  scene.add(gridHelper)

  // 模型组
  modelGroup = new THREE.Group()
  scene.add(modelGroup)

  // 默认立方体占位
  const defaultGeometry = new THREE.BoxGeometry(1, 0.5, 1.5)
  const defaultMaterial = new THREE.MeshPhongMaterial({
    color: 0x409eff,
    transparent: true,
    opacity: 0.3,
    wireframe: false
  })
  const defaultCube = new THREE.Mesh(defaultGeometry, defaultMaterial)
  defaultCube.castShadow = true
  modelGroup.add(defaultCube)

  // 线框
  const wireframe = new THREE.LineSegments(
    new THREE.EdgesGeometry(defaultGeometry),
    new THREE.LineBasicMaterial({ color: 0x409eff })
  )
  modelGroup.add(wireframe)

  // 启动渲染循环
  animate()
}

const animate = () => {
  animationId = requestAnimationFrame(animate)

  if (autoRotate.value && modelGroup) {
    modelGroup.rotation.y += 0.005
  }

  renderer.render(scene, camera)
}

const build3DModel = (model) => {
  if (!modelGroup) return

  // 清除旧模型
  while (modelGroup.children.length > 0) {
    modelGroup.remove(modelGroup.children[0])
  }

  // 根据模型格式创建不同的几何体表示
  const format = (model.format || '').toLowerCase()
  let geometry

  if (format === 'glb' || format === 'fbx') {
    // 复杂模型用二十面体表示
    geometry = new THREE.IcosahedronGeometry(1, 1)
  } else if (format === 'stl') {
    // STL 常用于 3D 打印，用小几何体
    geometry = new THREE.CylinderGeometry(0.5, 0.5, 1.5, 8)
  } else {
    // OBJ 等默认用立方体 + 附加细节
    geometry = new THREE.BoxGeometry(1, 0.6, 1.5)

    // 添加代表细节的子部件
    const topGeo = new THREE.CylinderGeometry(0.3, 0.3, 0.4, 6)
    const topMesh = new THREE.Mesh(topGeo,
      new THREE.MeshPhongMaterial({ color: 0x67c23a }))
    topMesh.position.y = 0.5
    modelGroup.add(topMesh)
  }

  const material = new THREE.MeshPhongMaterial({
    color: 0x409eff,
    specular: 0x111111,
    shininess: 30,
    transparent: true,
    opacity: 0.85
  })

  const mesh = new THREE.Mesh(geometry, material)
  mesh.castShadow = true
  mesh.receiveShadow = true
  modelGroup.add(mesh)

  // 线框显示结构
  const edgesGeometry = new THREE.EdgesGeometry(geometry)
  const edgesMaterial = new THREE.LineBasicMaterial({ color: 0x2c6fce })
  const wireframe = new THREE.LineSegments(edgesGeometry, edgesMaterial)
  modelGroup.add(wireframe)

  // 重置变换
  modelGroup.rotation.set(0, 0, 0)
  modelGroup.position.set(0, 0, 0)
  modelGroup.scale.set(1, 1, 1)
}

// ---- API 操作 ----
const loadModels = async () => {
  try {
    const res = await modelApi.listModels({ page: 0, size: 1000, name: searchName.value })
    // 兼容三种返回结构：LIST / PAGE.content / OBJ.data
    const d = res?.data ?? res
    models.value = Array.isArray(d) ? d : (d?.content || d?.data || [])
  } catch (e) {
    ElMessage.error('加载模型失败')
  }
}

const selectModel = (model) => {
  selectedModel.value = model
  nextTick(() => {
    build3DModel(model)
  })
}

const deleteModel = async (model) => {
  try {
    const result = await modelApi.deleteModel(model.id)

    if (result.code === 200) {
      ElMessage.success('删除成功')
      loadModels()
      if (selectedModel.value?.id === model.id) {
        selectedModel.value = null
      }
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ---- 3D 视图控制 ----
const toggleAutoRotate = () => {
  autoRotate.value = !autoRotate.value
  ElMessage.info(autoRotate.value ? '自动旋转已开启' : '自动旋转已停止')
}

const zoomIn = () => {
  currentZoom = Math.max(1.5, currentZoom - 0.8)
  camera.position.set(3 * currentZoom / 5, 2 * currentZoom / 5, currentZoom)
  camera.lookAt(0, 0, 0)
  ElMessage.info('放大视图')
}

const zoomOut = () => {
  currentZoom = Math.min(20, currentZoom + 0.8)
  camera.position.set(3 * currentZoom / 5, 2 * currentZoom / 5, currentZoom)
  camera.lookAt(0, 0, 0)
  ElMessage.info('缩小视图')
}

const resetView = () => {
  currentZoom = 5
  camera.position.set(3, 2, 5)
  camera.lookAt(0, 0, 0)
  if (modelGroup) {
    modelGroup.rotation.set(0, 0, 0)
  }
  autoRotate.value = true
  ElMessage.info('视图已重置')
}

const exportModel = async () => {
  if (!selectedModel.value) return

  try {
    const blob = await visionApi.exportModel(selectedModel.value.id, 'obj')

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${selectedModel.value.id}.obj`
    link.click()
    URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const doOptimize = async () => {
  if (!selectedModel.value) return

  try {
    const result = await visionApi.optimizeModel(selectedModel.value.id, optimizeForm.quality)

    if (result.code === 200) {
      ElMessage.success(`模型已优化，压缩率: ${result.compressionRatio || 'N/A'}`)
      showOptimize.value = false
    } else {
      ElMessage.error(result.message || '优化失败')
    }
  } catch (e) {
    ElMessage.error('优化失败')
  }
}

const optimizeModel = () => {
  showOptimize.value = true
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ---- 窗口 resize ----
const handleResize = () => {
  if (!canvasContainer.value || !renderer) return
  const w = canvasContainer.value.clientWidth
  const h = canvasContainer.value.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

// ---- 生命周期 ----
onMounted(() => {
  loadModels()
  nextTick(() => {
    initThreeJS()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (renderer) renderer.dispose()
  window.removeEventListener('resize', handleResize)
})

// 监听选中模型变化
watch(selectedModel, (newModel) => {
  if (newModel) {
    nextTick(() => build3DModel(newModel))
  }
})
</script>

<style scoped>
.model-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.preview-header {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.search-input {
  width: 100%;
}

.preview-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  flex-direction: column;
}

.model-list {
  width: 100%;
  max-height: 200px;
  border-bottom: 1px solid #eee;
  border-right: none;
  padding: 16px;
  overflow-y: auto;
}

.model-list h3 {
  margin-bottom: 16px;
  color: #333;
}

.model-viewer {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.viewer-header h3 {
  color: #333;
}

.viewer-canvas {
  flex: 1;
  background: #f0f2f5;
  border-radius: 12px;
  min-height: 400px;
  position: relative;
  overflow: hidden;
}

.empty-viewer {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999;
}

.model-properties {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.model-properties h4 {
  margin-bottom: 12px;
  color: #333;
}

.property-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.property-item {
  display: flex;
  gap: 8px;
}

.property-label {
  font-weight: bold;
  color: #666;
}

.model-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: flex-start;
}
</style>
