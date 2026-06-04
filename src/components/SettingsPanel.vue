<template>
  <div class="settings-panel">
    <h3 class="panel-title">系统设置</h3>
    <el-divider margin="8px" />

    <el-form label-width="130px" size="small">
      <el-form-item label="库存预警阈值">
        <el-input-number v-model="settings.warningThreshold" :min="1" :max="1000" />
        <span class="hint">低于此值显示预警色</span>
      </el-form-item>
      <el-form-item label="默认相机视角">
        <el-select v-model="settings.defaultCameraView">
          <el-option label="全局俯瞰" value="global" />
          <el-option label="近景" value="close" />
        </el-select>
      </el-form-item>
      <el-form-item label="音效提示">
        <el-switch v-model="settings.soundEnabled" active-text="开" inactive-text="关" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveSettings">保存</el-button>
        <el-button @click="resetSettings">重置</el-button>
      </el-form-item>
    </el-form>

    <el-divider margin="8px" />

    <div class="legend">
      <h4>库位状态图例</h4>
      <div class="legend-items">
        <span class="legend-item"><i style="background:#3a3a5c" />空库位</span>
        <span class="legend-item"><i style="background:#2d8c4e" />正常库存</span>
        <span class="legend-item"><i style="background:#e6a23c" />库存预警</span>
        <span class="legend-item"><i style="background:#f56c6c" />异常库位</span>
      </div>
    </div>

    <div class="sys-info">
      <h4>系统信息</h4>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="服务状态"><el-tag type="success" size="small">运行中</el-tag></el-descriptions-item>
        <el-descriptions-item label="端口">8080</el-descriptions-item>
        <el-descriptions-item label="数据库">MySQL</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="actions">
      <el-button size="small" @click="$emit('refreshData')">刷新数据</el-button>
      <el-button size="small" @click="$emit('resetCamera')">重置视角</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const settings = reactive({ warningThreshold: 10, defaultCameraView: 'global', soundEnabled: true })
const emit = defineEmits(['settingsChange', 'refreshData', 'resetCamera'])

const loadSettings = () => {
  try {
    const s = localStorage.getItem('smartvision-settings')
    if (s) Object.assign(settings, JSON.parse(s))
  } catch (e) { /* ignore */ }
}

const saveSettings = () => {
  try {
    localStorage.setItem('smartvision-settings', JSON.stringify(settings))
    emit('settingsChange', { ...settings })
    ElMessage.success('设置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const resetSettings = () => {
  Object.assign(settings, { warningThreshold: 10, defaultCameraView: 'global', soundEnabled: true })
  saveSettings()
}

defineExpose({ getSettings: () => ({ ...settings }) })
onMounted(() => loadSettings())
</script>

<style scoped>
.settings-panel { padding: 10px; overflow-y: auto; }
.panel-title { font-size: 15px; font-weight: bold; color: #fff; }
.hint { margin-left: 8px; color: #888; font-size: 11px; }
.legend { margin: 12px 0; }
.legend h4, .sys-info h4 { color: #ccc; font-size: 13px; margin-bottom: 8px; }
.legend-items { display: flex; flex-wrap: wrap; gap: 12px; }
.legend-item { display: flex; align-items: center; gap: 4px; color: #aaa; font-size: 12px; }
.legend-item i { display: inline-block; width: 14px; height: 14px; border-radius: 3px; }
.sys-info { margin: 12px 0; }
.actions { margin-top: 12px; display: flex; gap: 8px; }
</style>
