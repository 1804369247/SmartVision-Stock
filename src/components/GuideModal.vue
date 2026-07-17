<template>
  <div v-if="visible" class="guide-overlay">
    <el-dialog 
      v-model="visible" 
      title="欢迎使用 SmartVision Stock" 
      width="500px" 
      :close-on-click-modal="false"
      :show-close="false"
      :modal="false"
      class="guide-dialog"
    >
    <div class="guide-content">
      <div class="step-indicator">
        <span 
          v-for="(_, index) in steps" 
          :key="index"
          :class="['step-dot', { active: currentStep === index, done: currentStep > index }]"
        ></span>
      </div>

      <div class="step-content" :key="currentStep">
        <div class="step-icon">{{ steps[currentStep].icon }}</div>
        <h3>{{ steps[currentStep].title }}</h3>
        <p>{{ steps[currentStep].description }}</p>
        <div v-if="steps[currentStep].highlight" class="highlight-box">
          <span>{{ steps[currentStep].highlight }}</span>
        </div>
      </div>

      <div class="guide-actions">
        <el-button 
          v-if="currentStep > 0" 
          @click="prevStep" 
          size="small"
        >上一步</el-button>
        <el-button 
          v-if="currentStep < steps.length - 1" 
          type="primary" 
          @click="nextStep"
          :loading="stepLoading"
        >下一步</el-button>
        <el-button 
          v-else 
          type="primary" 
          @click="finishGuide"
        >开始体验</el-button>
        <el-button v-if="currentStep > 0" @click="skipGuide" size="small">跳过引导</el-button>
      </div>
    </div>
  </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['complete', 'skip'])

const visible = ref(false)
const currentStep = ref(0)
const stepLoading = ref(false)

const steps = [
  {
    icon: '🖱️',
    title: '拖拽旋转视角',
    description: '按住鼠标左键并拖动，可以360度旋转查看仓库场景。尝试从不同角度观察库区布局。',
    highlight: '提示：鼠标左键拖拽'
  },
  {
    icon: '🔍',
    title: '滚轮缩放',
    description: '使用鼠标滚轮可以放大或缩小视图，近距离观察货架细节或查看整个仓库概览。',
    highlight: '提示：滚轮向前放大，向后缩小'
  },
  {
    icon: '👆',
    title: '点击库位操作',
    description: '点击任意库位可以进行入库、出库或移库操作。库位颜色代表不同状态：绿色表示有货，蓝色表示空闲。',
    highlight: '提示：点击绿色库位出库，点击蓝色库位入库'
  },
  {
    icon: '⚡',
    title: '快捷键操作',
    description: '使用键盘快捷键可以更高效地操作：WASD漫游视角、空格键快速重置视角、双击库位直接弹出操作面板。',
    highlight: '提示：按空格键重置视角'
  },
  {
    icon: '📊',
    title: '数据看板',
    description: '右侧面板包含历史记录和数据统计，支持搜索筛选和图表展示。点击历史记录可在3D场景中定位对应库位。',
    highlight: '提示：查看右侧数据看板'
  }
]

const isFirstTime = computed(() => {
  return localStorage.getItem('smartvision-guide-completed') !== '1'
})

const show = () => {
  if (isFirstTime.value) {
    visible.value = true
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const nextStep = () => {
  if (currentStep.value < steps.length - 1) {
    currentStep.value++
  }
}

const skipGuide = () => {
  visible.value = false
  emit('skip')
}

const finishGuide = () => {
  visible.value = false
  localStorage.setItem('smartvision-guide-completed', '1')
  emit('complete')
  ElMessage.success('欢迎使用 SmartVision Stock！')
}

defineExpose({ show })

onMounted(() => {
  setTimeout(() => show(), 1000)
})
</script>

<style scoped>
.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}
.guide-dialog {
  background: rgba(10, 10, 20, 0.95);
}
.guide-content {
  text-align: center;
  padding: 20px 0;
}
.step-indicator {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}
.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #333;
  transition: all 0.3s;
}
.step-dot.active {
  background: #409eff;
  transform: scale(1.2);
}
.step-dot.done {
  background: #67c23a;
}
.step-content {
  margin-bottom: 20px;
}
.step-icon {
  font-size: 48px;
  margin-bottom: 15px;
}
.step-content h3 {
  color: #fff;
  font-size: 18px;
  margin-bottom: 10px;
}
.step-content p {
  color: #aaa;
  font-size: 14px;
  line-height: 1.6;
}
.highlight-box {
  margin-top: 15px;
  padding: 10px 15px;
  background: rgba(64, 158, 255, 0.15);
  border-radius: 8px;
  border-left: 3px solid #409eff;
}
.highlight-box span {
  color: #88ccff;
  font-size: 13px;
}
.guide-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
