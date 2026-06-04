<template>
  <el-dialog v-model="visible" title="帮助中心" width="520px">
    <div class="help-content">
      <div class="help-section">
        <h3><i class="help-icon">⌨️</i> 快捷键操作</h3>
        <div class="shortcuts-grid">
          <div class="shortcut-item">
            <div class="keys">
              <kbd>W</kbd>
            </div>
            <span>向前移动视角</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>A</kbd>
            </div>
            <span>向左移动视角</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>S</kbd>
            </div>
            <span>向后移动视角</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>D</kbd>
            </div>
            <span>向右移动视角</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>空格</kbd>
            </div>
            <span>重置视角到默认位置</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>+</kbd>
            </div>
            <span>放大视图</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>-</kbd>
            </div>
            <span>缩小视图</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>双击</kbd>
            </div>
            <span>快速打开库位操作面板</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>ESC</kbd>
            </div>
            <span>关闭弹窗/取消操作</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>1</kbd>
            </div>
            <span>切换到全局视图</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>2</kbd>
            </div>
            <span>切换到分区视图</span>
          </div>
          <div class="shortcut-item">
            <div class="keys">
              <kbd>3</kbd>
            </div>
            <span>切换到货架特写</span>
          </div>
        </div>
      </div>

      <div class="help-section">
        <h3><i class="help-icon">🖱️</i> 鼠标操作</h3>
        <ul class="help-list">
          <li><strong>左键拖拽</strong>：旋转视角，查看仓库各个角度</li>
          <li><strong>滚轮</strong>：放大/缩小视图，调整观察距离</li>
          <li><strong>左键点击</strong>：选中库位，弹出操作面板</li>
          <li><strong>双击</strong>：快速打开库位操作面板</li>
          <li><strong>悬停</strong>：显示库位详情气泡</li>
        </ul>
      </div>

      <div class="help-section">
        <h3><i class="help-icon">📦</i> 库位状态说明</h3>
        <div class="status-list">
          <div class="status-item">
            <span class="status-color" style="background: #3a3a5c"></span>
            <span>空库位</span>
          </div>
          <div class="status-item">
            <span class="status-color" style="background: #2d8c4e"></span>
            <span>正常库存</span>
          </div>
          <div class="status-item">
            <span class="status-color" style="background: #e6a23c"></span>
            <span>库存预警</span>
          </div>
          <div class="status-item">
            <span class="status-color" style="background: #f56c6c"></span>
            <span>异常库位</span>
          </div>
        </div>
      </div>

      <div class="help-section">
        <h3><i class="help-icon">💡</i> 实用技巧</h3>
        <ul class="help-list">
          <li>使用视角预设按钮（顶部）快速切换常用视角</li>
          <li>在右侧面板筛选货物类型，3D场景会高亮对应库位</li>
          <li>点击历史记录中的仓位编号可定位到对应库位</li>
          <li>通过系统设置调整预警阈值和默认视角</li>
          <li>数据看板实时显示库存统计和趋势图表</li>
        </ul>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)

const show = () => {
  visible.value = true
}

defineExpose({ show })
</script>

<style scoped>
.help-content {
  max-height: 500px;
  overflow-y: auto;
}
.help-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(100, 100, 150, 0.2);
}
.help-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.help-section h3 {
  color: #88ccff;
  font-size: 15px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.help-icon {
  font-size: 18px;
}
.shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.shortcut-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(30, 30, 50, 0.6);
  border-radius: 8px;
}
.keys {
  display: flex;
  gap: 4px;
}
kbd {
  display: inline-block;
  padding: 4px 8px;
  font-size: 12px;
  background: rgba(60, 60, 80, 0.8);
  border-radius: 4px;
  border: 1px solid rgba(100, 100, 150, 0.3);
  color: #ccc;
  font-family: monospace;
  min-width: 32px;
  text-align: center;
}
.help-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.help-list li {
  padding: 6px 0;
  color: #aaa;
  font-size: 14px;
}
.help-list li strong {
  color: #ccc;
}
.status-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}
.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}
</style>
