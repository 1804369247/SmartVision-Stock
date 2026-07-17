<template>
  <div class="dashboard">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="bar-left">
        <div class="logo-section">
          <span class="logo-icon">📦</span>
          <span class="logo-text">SmartVision Stock</span>
        </div>

        <el-divider direction="vertical" />

        <div class="view-presets">
          <el-button
            :type="currentView === 'global' ? 'primary' : ''"
            size="small"
            text
            @click="setView('global')"
          >全局</el-button>
          <el-dropdown trigger="hover" @command="(cmd) => setView('zone', cmd)">
            <el-button :type="currentView === 'zone' ? 'primary' : ''" size="small" text>
              分区 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="A">A 区</el-dropdown-item>
                <el-dropdown-item command="B">B 区</el-dropdown-item>
                <el-dropdown-item command="C">C 区</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="hover" @command="(cmd) => setView('close', cmd)">
            <el-button :type="currentView === 'close' ? 'primary' : ''" size="small" text>
              特写 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="A">A 区</el-dropdown-item>
                <el-dropdown-item command="B">B 区</el-dropdown-item>
                <el-dropdown-item command="C">C 区</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-divider direction="vertical" />

        <div class="main-menu">
          <el-dropdown trigger="hover" @command="(cmd) => switchModule('basic', cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'basic' }]" size="small" text>
              基础档案 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="goods">货物档案</el-dropdown-item>
                <el-dropdown-item command="locations">库位管理</el-dropdown-item>
                <el-dropdown-item command="suppliers">供应商</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="hover" @command="(cmd) => handleOrderCmd(cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'order' || currentModule === 'pick' || currentModule === 'return' }]" size="small" text>
              出入库 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="inbound">采购入库</el-dropdown-item>
                <el-dropdown-item command="outbound">销售出库</el-dropdown-item>
                <el-dropdown-item command="inboundList">入库单查询</el-dropdown-item>
                <el-dropdown-item command="outboundList">出库单查询</el-dropdown-item>
                <el-dropdown-item divided command="pick">波次拣货</el-dropdown-item>
                <el-dropdown-item command="return">退货管理</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="hover" @command="(cmd) => handleInventoryCmd(cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'inventory' }]" size="small" text>
              库存 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="query">库存查询</el-dropdown-item>
                <el-dropdown-item command="count">库存盘点</el-dropdown-item>
                <el-dropdown-item command="expiry">效期预警</el-dropdown-item>
                <el-dropdown-item command="warning">库存预警</el-dropdown-item>
                <el-dropdown-item command="transfer">库存调拨</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="hover" @command="(cmd) => handleReportCmd(cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'report' || currentModule === 'print' }]" size="small" text>
              报表 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="stock">库存台账</el-dropdown-item>
                <el-dropdown-item command="flow">库存流水</el-dropdown-item>
                <el-dropdown-item command="utilization">库位利用率</el-dropdown-item>
                <el-dropdown-item command="warningReport">预警报表</el-dropdown-item>
                <el-dropdown-item divided command="print">打印管理</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown trigger="hover" @command="(cmd) => switchModule('system', cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'system' }]" size="small" text>
              系统 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="users">用户管理</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item command="logs">操作日志</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-divider direction="vertical" />

          <!-- AI视觉模块 -->
          <el-dropdown trigger="hover" @command="(cmd) => handleVisionCmd(cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'vision' }, 'menu-btn-ai']" size="small" text>
              <span class="ai-badge">AI</span> 视觉 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="capture">
                  <span class="ai-icon">📷</span> 图片识别
                </el-dropdown-item>
                <el-dropdown-item command="camera">
                  <span class="ai-icon">🎥</span> 摄像头采集
                </el-dropdown-item>
                <el-dropdown-item command="result">
                  <span class="ai-icon">🔍</span> 货架分析
                </el-dropdown-item>
                <el-dropdown-item divided command="reconstruct">
                  <span class="ai-icon">🏗️</span> 3D重建
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 3D建模模块 -->
          <el-dropdown trigger="hover" @command="(cmd) => switchModule('model', cmd)">
            <el-button :class="['menu-btn', { active: currentModule === 'model' || currentModule === 'barcode' }, 'menu-btn-model']" size="small" text>
              建模 <el-icon><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="preview">
                  <span class="ai-icon">📦</span> 模型管理
                </el-dropdown-item>
                <el-dropdown-item divided command="barcode">
                  <span class="ai-icon">📱</span> 条码扫描
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="bar-right">
        <el-tooltip content="消息通知" placement="bottom">
          <el-button class="icon-btn" size="small" text circle @click="switchModule('notification', 'index')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-tooltip>

        <el-tooltip content="帮助中心" placement="bottom">
          <el-button class="icon-btn" size="small" text circle @click="showHelp = true">
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </el-tooltip>

        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="user-avatar-btn">
            <el-avatar :size="28" class="user-avatar">
              {{ (currentUser?.realName || currentUser?.username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <span class="user-name">{{ currentUser?.realName || currentUser?.username || '用户' }}</span>
            <el-icon class="user-arrow"><arrow-down /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                <div class="user-info-header">
                  <strong>{{ currentUser?.realName || currentUser?.username }}</strong>
                  <span>{{ currentUser?.role === 'ADMIN' ? '管理员' : '操作员' }}</span>
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided command="profile">
                <el-icon><User /></el-icon> 个人信息
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon> 系统设置
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 3D场景区域 -->
    <div class="scene-container">
      <StockScene 
        ref="sceneRef"
        :view-mode="currentView"
        :view-zone="currentZone"
        @location-click="handleLocationClick"
      />
    </div>

    <!-- 功能面板弹窗 -->
    <el-dialog 
      v-model="panelVisible" 
      title="功能面板" 
      width="800px"
      top="20px"
      :fullscreen="isFullscreen"
      @close="onPanelClose"
    >
      <div class="panel-content">
        <!-- 基础档案 -->
        <BasicManagement 
          v-if="currentModule === 'basic'" 
          :sub-page="currentSubPage"
          @close="panelVisible = false"
        />
        
        <!-- 出入库管理 -->
        <OrderManagement 
          v-else-if="currentModule === 'order'" 
          :sub-page="currentSubPage"
          @close="panelVisible = false"
        />
        
        <!-- 库存管理 -->
        <InventoryManagement 
          v-else-if="currentModule === 'inventory'" 
          :sub-page="currentSubPage"
          @close="panelVisible = false"
        />
        
        <!-- 报表中心 -->
        <ReportCenter 
          v-else-if="currentModule === 'report'" 
          :sub-page="currentSubPage"
          @close="panelVisible = false"
        />
        
        <!-- 系统管理 -->
        <UserManagement 
          v-else-if="currentModule === 'system' && currentSubPage === 'users'" 
          @close="panelVisible = false"
        />
        
        <SettingsPanel 
          v-else-if="currentModule === 'system' && currentSubPage === 'settings'" 
          @close="panelVisible = false"
        />
        
        <OperationLog 
          v-else-if="currentModule === 'system' && currentSubPage === 'logs'" 
          @close="panelVisible = false"
        />
        
        <!-- 打印管理 -->
        <PrintManager 
          v-else-if="currentModule === 'print'" 
          @close="panelVisible = false"
        />
        
        <!-- 波次拣货 -->
        <WavePicking 
          v-else-if="currentModule === 'pick'" 
          @close="panelVisible = false"
        />
        
        <!-- 退货管理 -->
        <ReturnManagement
          v-else-if="currentModule === 'return'"
          @close="panelVisible = false"
        />

        <!-- AI视觉 - 图片识别/摄像头采集 -->
        <VisionCapture
          v-else-if="currentModule === 'vision' && (currentSubPage === 'capture' || currentSubPage === 'camera')"
          @close="panelVisible = false"
        />

        <!-- AI视觉 - 货架分析/3D重建结果 -->
        <VisionResult
          v-else-if="currentModule === 'vision' && (currentSubPage === 'result' || currentSubPage === 'reconstruct')"
          @close="panelVisible = false"
        />

        <!-- 3D建模 - 模型管理 -->
        <ModelPreview
          v-else-if="currentModule === 'model' && currentSubPage === 'preview'"
          @close="panelVisible = false"
        />

        <!-- 条码扫描 -->
        <BarcodeScanner
          v-else-if="currentModule === 'barcode'"
          @close="panelVisible = false"
        />

        <!-- 效期预警 -->
        <ExpiryWarning
          v-else-if="currentModule === 'expiry'"
        />

        <!-- 消息通知 -->
        <NotificationCenter
          v-else-if="currentModule === 'notification'"
        />

        <!-- 库存盘点 -->
        <StockCount
          v-else-if="currentModule === 'stock-count'"
        />

        <!-- 个人信息 -->
        <Profile
          v-else-if="currentModule === 'profile'"
        />
      </div>
    </el-dialog>

    <!-- 帮助中心弹窗 -->
    <el-dialog 
      v-model="showHelp" 
      title="帮助中心" 
      width="600px"
    >
      <HelpCenter />
    </el-dialog>
  </div>
</template>

<script setup>import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowDown, QuestionFilled, User, Setting, SwitchButton, Bell } from '@element-plus/icons-vue';
import StockScene from './StockScene.vue';
import BasicManagement from './BasicManagement.vue';
import OrderManagement from './OrderManagement.vue';
import InventoryManagement from './InventoryManagement.vue';
import ReportCenter from './ReportCenter.vue';
import UserManagement from './UserManagement.vue';
import SettingsPanel from './SettingsPanel.vue';
import OperationLog from './OperationLog.vue';
import PrintManager from './PrintManager.vue';
import WavePicking from './WavePicking.vue';
import ReturnManagement from './ReturnManagement.vue';
import HelpCenter from './HelpCenter.vue';
import VisionCapture from './VisionCapture.vue';
import VisionResult from './VisionResult.vue';
import ModelPreview from './ModelPreview.vue';
import BarcodeScanner from './BarcodeScanner.vue';
import ExpiryWarning from './ExpiryWarning.vue';
import NotificationCenter from './NotificationCenter.vue';
import StockCount from './StockCount.vue';
import Profile from './Profile.vue';
import { systemApi } from '../api/system';
import { useGoodsStore } from '../stores/goods';
import { useLocationStore } from '../stores/location';
const router = useRouter();
// Store 实例
const goodsStore = useGoodsStore();
const locationStore = useLocationStore();
// 当前视图模式
const currentView = ref('global');
const currentZone = ref('A');
// 当前模块
const currentModule = ref('');
const currentSubPage = ref('');
const panelVisible = ref(false);
const isFullscreen = ref(false);
const showHelp = ref(false);
const showUserMenu = ref(false);
const sceneRef = ref(null);
// 当前用户
const currentUser = ref(null);
// 设置视图
const setView = (view, zone = 'A') => {
 currentView.value = view;
 currentZone.value = zone;
};
// 切换模块
const switchModule = (module, subPage) => {
  currentModule.value = module;
  currentSubPage.value = subPage || 'index';
  panelVisible.value = true;
};
// 出入库模块（含波次拣货、退货管理）
const handleOrderCmd = (cmd) => {
  if (cmd === 'pick' || cmd === 'return') {
    switchModule(cmd, 'index');
  } else {
    switchModule('order', cmd);
  }
};
// 报表模块（含打印管理）
const handleReportCmd = (cmd) => {
  if (cmd === 'print') {
    switchModule('print', 'index');
  } else {
    switchModule('report', cmd);
  }
};
// 库存模块（含效期预警）
const handleInventoryCmd = (cmd) => {
  if (cmd === 'expiry') {
    switchModule('expiry', 'index');
  } else if (cmd === 'count') {
    switchModule('stock-count', 'index');
  } else {
    switchModule('inventory', cmd);
  }
};
// AI视觉模块
const handleVisionCmd = (cmd) => {
  if (cmd === 'capture' || cmd === 'camera') {
    switchModule('vision', cmd);
  } else if (cmd === 'result' || cmd === 'reconstruct') {
    switchModule('vision', cmd);
  }
};
// 个人中心下拉命令
const handleUserCommand = (command) => {
  if (command === 'logout') {
    handleLogout();
  } else if (command === 'settings') {
    switchModule('system', 'settings');
  } else if (command === 'profile') {
    switchModule('profile', 'index');
  }
};
// 处理库位点击
const handleLocationClick = (location) => {
 console.log('Location clicked:', location);
};
// 面板关闭
const onPanelClose = () => {
 panelVisible.value = false;
};
// 退出登录
const handleLogout = async () => {
 try {
 await systemApi.logout();
 }
 catch (e) {
 console.error('Logout error:', e);
 }
 finally {
 localStorage.removeItem('token');
 localStorage.removeItem('refreshToken');
 localStorage.removeItem('user');
 router.replace('/login');
 }
};
// 初始化数据
const initData = async () => {
 try {
 const token = localStorage.getItem('token');
 if (!token) {
 console.warn('No token found, skip data initialization');
 return;
 }
 await goodsStore.fetchGoods();
 await locationStore.fetchLocations();
 }
 catch (e) {
 console.error('Failed to initialize data:', e);
 }
};
// WebSocket连接
let ws = null;
let reconnectDelay = 3000;
let reconnectTimer = null;
let intentionalClose = false;
let consecutiveFailures = 0; // 连续失败次数

const connectWebSocket = () => {
 // 避免重复连接
 if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
   return;
 }

 intentionalClose = false;
 const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
 const token = localStorage.getItem('token');
 const wsUrl = `${protocol}//${window.location.host}/topic/stock`;

 try {
   // 通过 Sec-WebSocket-Protocol 子协议传递 token，避免明文出现在 URL / 代理日志中
   // 后端握手阶段校验 token 并以相同子协议回显；匿名连接不传子协议
   ws = token ? new WebSocket(wsUrl, token) : new WebSocket(wsUrl);
 } catch (e) {
   console.error('WebSocket 创建失败:', e);
   scheduleReconnect();
   return;
 }

 ws.onopen = () => {
   console.log('✅ WebSocket 已连接（实时库存更新已启用）');
   consecutiveFailures = 0;
   reconnectDelay = 3000;
 };

 ws.onmessage = (event) => {
   try {
     const data = JSON.parse(event.data);
     if (data.type === 'stockChange') {
       // 收到库存变更，刷新相关数据
       locationStore.fetchLocations();
       goodsStore.fetchGoods();
     }
   }
   catch (e) {
     console.error('WebSocket 消息解析失败:', e);
   }
 };

 ws.onerror = (error) => {
   // 不打印过多错误信息，onclose 会处理重连逻辑
 };

 ws.onclose = (event) => {
   ws = null;
   if (intentionalClose) {
     console.log('WebSocket 已主动关闭');
     return;
   }

   consecutiveFailures++;

   // code=1006 异常关闭：通常是 Token 失效或后端重启导致密钥变更
   if (event.code === 1006 && consecutiveFailures >= 2) {
     console.warn(`⚠ WebSocket 连续 ${consecutiveFailures} 次失败 (code=1006)，可能原因：`);
     console.warn('  1. 后端刚重启过，Token 密钥已变更 → 请刷新页面重新登录');
     console.warn('  2. 网络代理/防火墙阻止了 WebSocket 升级');
     console.warn('  停止自动重连。库存数据仍可通过页面刷新获取。');
     return; // 停止重连，不影响正常使用
   }

   // 正常断开或其他原因，继续尝试重连
   console.log(`WebSocket 断开 (code=${event.code}, 第${consecutiveFailures}次), ${reconnectDelay}ms 后重连...`);
   scheduleReconnect();
 };
};

const scheduleReconnect = () => {
 if (reconnectDelay > 30000) {
   console.warn('WebSocket 重连次数已达上限，停止自动重连。请刷新页面。');
   return;
 }
 reconnectTimer = setTimeout(() => {
   connectWebSocket();
 }, reconnectDelay);
 reconnectDelay = Math.min(reconnectDelay * 2, 60000);
};

const disconnectWebSocket = () => {
 intentionalClose = true;
 if (reconnectTimer) {
   clearTimeout(reconnectTimer);
   reconnectTimer = null;
 }
 if (ws) {
   ws.close(1000, 'Component unmounting');
   ws = null;
 }
};
onMounted(() => {
 // 获取当前用户信息
 const userStr = localStorage.getItem('user');
 if (userStr) {
 try {
 currentUser.value = JSON.parse(userStr);
 }
 catch (e) {
 console.error('Failed to parse user:', e);
 }
 }
 // 初始化数据
 initData();
 // 连接WebSocket
 connectWebSocket();
});
onUnmounted(() => {
 disconnectWebSocket();
});
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0a0a1a;
}

/* ====== 顶部栏整体 ====== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: linear-gradient(180deg, rgba(15, 20, 40, 0.97) 0%, rgba(10, 14, 30, 0.95) 100%);
  border-bottom: 1px solid rgba(0, 200, 220, 0.18);
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.4), 0 0 30px rgba(0, 180, 220, 0.04);
  flex-shrink: 0;
  z-index: 100;
}

/* 左侧区域 */
.bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

/* 右侧区域 */
.bar-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 12px;
}

/* 分隔线 */
.top-bar :deep(.el-divider--vertical) {
  border-color: rgba(0, 200, 220, 0.2);
  height: 20px;
  margin: 0 6px;
}

/* ====== Logo ====== */
.logo-section {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 20px;
  line-height: 1;
  filter: drop-shadow(0 0 6px rgba(0, 245, 255, 0.5));
}

.logo-text {
  font-size: 14px;
  font-weight: 700;
  color: #00e5ff;
  letter-spacing: 1.5px;
  background: linear-gradient(90deg, #00e5ff, #7b47ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

/* ====== 视图预设按钮组 ====== */
.view-presets {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

/* ====== 主菜单 ====== */
.main-menu {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
}

/* 菜单按钮统一样式 */
.menu-btn :deep(.el-button) {
  color: rgba(200, 215, 235, 0.85) !important;
  border: none !important;
  border-radius: 6px !important;
  font-size: 13px !important;
  font-weight: 500;
  padding: 5px 10px !important;
  transition: all 0.25s ease;
  position: relative;
}

.menu-btn :deep(.el-button:hover),
.view-presets :deep(.el-button:hover) {
  color: #00f5ff !important;
  background: rgba(0, 245, 255, 0.08) !important;
}

.menu-btn.active :deep(.el-button) {
  color: #fff !important;
  background: linear-gradient(135deg, rgba(0, 200, 220, 0.25), rgba(120, 60, 230, 0.2)) !important;
  box-shadow: inset 0 0 0 1px rgba(0, 220, 240, 0.3), 0 0 12px rgba(0, 200, 220, 0.1);
}

.menu-btn :deep(.el-icon),
.view-presets :deep(.el-icon) {
  font-size: 11px;
  margin-left: 2px;
  opacity: 0.7;
  transition: transform 0.2s ease;
}

/* 视图预设激活态 */
.view-presets :deep(.el-button.is-primary) {
  color: #00f5ff !important;
  background: rgba(0, 245, 255, 0.1) !important;
  box-shadow: inset 0 0 0 1px rgba(0, 245, 255, 0.3);
}

/* ====== AI 视觉按钮特殊标记 ====== */
.menu-btn-ai :deep(.el-button) {
  position: relative;
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 10px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff6b35, #f72585);
  color: #fff;
  border-radius: 4px;
  margin-right: 3px;
  letter-spacing: -0.5px;
  box-shadow: 0 0 8px rgba(247, 37, 133, 0.4);
  animation: ai-pulse 2s ease-in-out infinite;
}

@keyframes ai-pulse {
  0%, 100% { box-shadow: 0 0 8px rgba(247, 37, 133, 0.4); }
  50% { box-shadow: 0 0 14px rgba(247, 37, 133, 0.65); }
}

.ai-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* ====== 帮助按钮（圆形图标）===== */
.icon-btn :deep(.el-button) {
  color: rgba(200, 215, 235, 0.6) !important;
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  border: none !important;
}

.icon-btn :deep(.el-button:hover) {
  color: #00f5ff !important;
  background: rgba(0, 245, 255, 0.1) !important;
}

/* ====== 个人中心头像下拉 ====== */
.user-avatar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 3px 8px 3px 3px;
  border-radius: 20px;
  transition: background 0.2s ease;
  border: 1px solid transparent;
}

.user-avatar-btn:hover {
  background: rgba(0, 245, 255, 0.06);
  border-color: rgba(0, 200, 220, 0.2);
}

.user-avatar {
  background: linear-gradient(135deg, #00c6d9, #7b3fff) !important;
  color: #fff !important;
  font-size: 12px !important;
  font-weight: 600;
  box-shadow: 0 0 8px rgba(123, 47, 255, 0.35);
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  color: rgba(210, 225, 245, 0.9);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.user-arrow {
  font-size: 11px;
  color: rgba(200, 215, 235, 0.45);
  transition: transform 0.2s ease;
}

.user-info-header {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.user-info-header strong {
  font-size: 14px;
  color: #1a1a2e;
}

.user-info-header span {
  font-size: 12px;
  color: #888;
}

/* ====== 下拉菜单全局美化 ====== */
.top-bar :deep(.el-dropdown-menu__item) {
  font-size: 13px;
  padding: 6px 16px;
  color: #3a3a50;
  transition: all 0.15s ease;
}

.top-bar :deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(90deg, rgba(0, 200, 220, 0.08), rgba(123, 47, 255, 0.06));
  color: #007acc;
}

/* ====== 场景容器 ====== */
.scene-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.panel-content {
  min-height: 400px;
}

/* 功能面板弹窗约束 */
:deep(.el-dialog) {
  max-height: 95vh;
  display: flex;
  flex-direction: column;
}

:deep(.el-dialog__body) {
  padding: 0 !important;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
  max-height: calc(95vh - 80px);
}

/* ====== 响应式 ====== */
@media (max-width: 1280px) {
  .bar-left {
    gap: 4px;
  }
  .logo-text {
    font-size: 13px;
    letter-spacing: 1px;
  }
  .menu-btn :deep(.el-button) {
    padding: 5px 8px !important;
    font-size: 12px !important;
  }
  .user-name {
    max-width: 60px;
  }
}

@media (max-width: 1100px) {
  .view-presets {
    display: none;
  }
  .top-bar :deep(.el-divider--vertical) {
    display: none;
  }
}

@media (max-width: 900px) {
  .main-menu {
    display: none;
  }
  .logo-text {
    display: none;
  }
}
</style>