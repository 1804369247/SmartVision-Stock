<template>
  <el-config-provider :locale="zhCN">
    <div id="app" class="app-container">
      <!-- 顶部工具栏 -->
      <header class="app-header">
        <div class="header-left">
          <h1 class="system-title">SmartVision Stock - 智能仓储管理系统</h1>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="showGuide = true">操作指南</el-button>
          <el-button @click="showHelp = true">帮助中心</el-button>
          <el-dropdown @command="handleLanguageChange">
            <el-button>
              语言切换<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">中文</el-dropdown-item>
                <el-dropdown-item command="en">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容区 -->
      <main class="app-main">
        <!-- 侧边栏导航 -->
        <aside class="app-sidebar">
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            @select="handleMenuSelect"
          >
            <el-menu-item index="dashboard">
              <el-icon><data-analysis /></el-icon>
              <span>数据仪表板</span>
            </el-menu-item>
            
            <el-menu-item index="basic">
              <el-icon><setting /></el-icon>
              <span>基础数据管理</span>
            </el-menu-item>
            
            <el-menu-item index="inventory">
              <el-icon><box /></el-icon>
              <span>库存管理</span>
            </el-menu-item>
            
            <el-menu-item index="orders">
              <el-icon><document /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            
            <el-menu-item index="reports">
              <el-icon><trend-chart /></el-icon>
              <span>报表中心</span>
            </el-menu-item>
            
            <el-menu-item index="settings">
              <el-icon><tools /></el-icon>
              <span>系统设置</span>
            </el-menu-item>
          </el-menu>
        </aside>

        <!-- 内容区域 -->
        <section class="app-content">
          <router-view />
        </section>
      </main>

      <!-- 3D 场景容器（全局视图） -->
      <div class="scene-container" v-show="showGlobalScene">
        <StockScene />
        <div class="scene-controls">
          <el-button-group>
            <el-button type="primary" @click="toggleView('global')">全局视图</el-button>
            <el-button @click="toggleView('zone')">分区视图</el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 模态框组件 -->
      <GuideModal v-model:visible="showGuide" />
      <HelpCenter v-model:visible="showHelp" />
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import zhCN from 'element-plus/dist/locale/zh-cn.mjs'
import {
  DataAnalysis,
  Setting,
  Box,
  Document,
  TrendCharts,
  Tools,
  ArrowDown
} from '@element-plus/icons-vue'

import StockScene from './components/StockScene.vue'
import GuideModal from './components/GuideModal.vue'
import HelpCenter from './components/HelpCenter.vue'

const router = useRouter()
const activeMenu = ref('dashboard')
const showGuide = ref(false)
const showHelp = ref(false)
const showGlobalScene = ref(false)

const handleMenuSelect = (index) => {
  activeMenu.value = index
  router.push({ name: index })
}

const handleLanguageChange = (command) => {
  console.log('Language changed to:', command)
  // 实现语言切换逻辑
}

const toggleView = (viewType) => {
  console.log('Switching to view:', viewType)
  showGlobalScene.value = viewType === 'global'
}

onMounted(() => {
  console.log('App mounted')
})
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.system-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.app-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
}

.app-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

.scene-container {
  position: fixed;
  top: 60px;
  left: 220px;
  right: 0;
  bottom: 0;
  background: white;
  z-index: 1000;
}

.scene-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1001;
}
</style>
